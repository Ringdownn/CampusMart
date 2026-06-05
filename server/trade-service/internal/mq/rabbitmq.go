package mq

import (
	"context"
	"encoding/json"
	"log"
	"strconv"
	"time"

	"campusmart/trade-service/internal/repository"
	"campusmart/trade-service/internal/service"
	amqp "github.com/rabbitmq/amqp091-go"
)

const (
	TradeExchange          = "campusmart.trade.exchange"
	PaymentExchange        = "campusmart.payment.exchange"
	OrderDelayExchange     = "campusmart.trade.delay.exchange"
	OrderTimeoutExchange   = "campusmart.trade.timeout.exchange"
	OrderDelayQueue        = "campusmart.trade.order.payment.delay.queue"
	OrderTimeoutQueue      = "campusmart.trade.order.payment.timeout.queue"
	PaymentPaidQueue       = "campusmart.trade.payment.paid.queue"
	OrderTimeoutRoutingKey = "trade.order.payment.timeout"
)

type paymentTimeoutEvent struct {
	OrderID int64  `json:"orderId"`
	OrderNo string `json:"orderNo"`
}

type Client struct {
	conn   *amqp.Connection
	ch     *amqp.Channel
	logger *log.Logger
}

func NewClient(rabbitURL string, logger *log.Logger) (*Client, error) {
	conn, err := amqp.Dial(rabbitURL)
	if err != nil {
		return nil, err
	}
	ch, err := conn.Channel()
	if err != nil {
		_ = conn.Close()
		return nil, err
	}
	client := &Client{conn: conn, ch: ch, logger: logger}
	if err := client.declare(); err != nil {
		client.Close()
		return nil, err
	}
	return client, nil
}

func (c *Client) Close() {
	if c.ch != nil {
		_ = c.ch.Close()
	}
	if c.conn != nil {
		_ = c.conn.Close()
	}
}

func (c *Client) declare() error {
	if err := c.ch.ExchangeDeclare(TradeExchange, "topic", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(PaymentExchange, "topic", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(OrderTimeoutExchange, "topic", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(OrderDelayExchange, "direct", true, false, false, false, nil); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(OrderDelayQueue, true, false, false, false, amqp.Table{
		"x-dead-letter-exchange":    OrderTimeoutExchange,
		"x-dead-letter-routing-key": OrderTimeoutRoutingKey,
	}); err != nil {
		return err
	}
	if err := c.ch.QueueBind(OrderDelayQueue, OrderTimeoutRoutingKey, OrderDelayExchange, false, nil); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(OrderTimeoutQueue, true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.QueueBind(OrderTimeoutQueue, OrderTimeoutRoutingKey, OrderTimeoutExchange, false, nil); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(PaymentPaidQueue, true, false, false, false, nil); err != nil {
		return err
	}
	return c.ch.QueueBind(PaymentPaidQueue, "payment.paid", PaymentExchange, false, nil)
}

func (c *Client) PublishPaymentTimeout(ctx context.Context, orderID int64, orderNo string, delay time.Duration) error {
	body, err := json.Marshal(paymentTimeoutEvent{OrderID: orderID, OrderNo: orderNo})
	if err != nil {
		return err
	}
	return c.ch.PublishWithContext(ctx, OrderDelayExchange, OrderTimeoutRoutingKey, false, false, amqp.Publishing{
		ContentType:  "application/json",
		DeliveryMode: amqp.Persistent,
		Expiration:   strconv.FormatInt(delay.Milliseconds(), 10),
		Body:         body,
	})
}

func (c *Client) StartConsumers(ctx context.Context, orderSvc *service.OrderService) error {
	timeoutMsgs, err := c.ch.Consume(OrderTimeoutQueue, "", false, false, false, false, nil)
	if err != nil {
		return err
	}
	paidMsgs, err := c.ch.Consume(PaymentPaidQueue, "", false, false, false, false, nil)
	if err != nil {
		return err
	}

	go c.consumeTimeout(ctx, timeoutMsgs, orderSvc)
	go c.consumePaymentPaid(ctx, paidMsgs, orderSvc)
	return nil
}

func (c *Client) consumeTimeout(ctx context.Context, msgs <-chan amqp.Delivery, orderSvc *service.OrderService) {
	for {
		select {
		case <-ctx.Done():
			return
		case msg, ok := <-msgs:
			if !ok {
				return
			}
			var body paymentTimeoutEvent
			if err := json.Unmarshal(msg.Body, &body); err != nil {
				_ = msg.Ack(false)
				continue
			}
			if err := orderSvc.TimeoutCancel(ctx, body.OrderID); err != nil {
				c.logger.Printf("timeout cancel order %d failed: %v", body.OrderID, err)
				_ = msg.Nack(false, true)
				continue
			}
			_ = msg.Ack(false)
		}
	}
}

func (c *Client) consumePaymentPaid(ctx context.Context, msgs <-chan amqp.Delivery, orderSvc *service.OrderService) {
	for {
		select {
		case <-ctx.Done():
			return
		case msg, ok := <-msgs:
			if !ok {
				return
			}
			var event service.PaymentPaidEvent
			if err := json.Unmarshal(msg.Body, &event); err != nil {
				_ = msg.Ack(false)
				continue
			}
			if err := orderSvc.MarkPaid(ctx, event, string(msg.Body)); err != nil {
				c.logger.Printf("mark order %d paid failed: %v", event.OrderID, err)
				_ = msg.Nack(false, true)
				continue
			}
			_ = msg.Ack(false)
		}
	}
}

type OutboxDispatcher struct {
	repo   *repository.OrderRepository
	client *Client
	logger *log.Logger
}

func NewOutboxDispatcher(repo *repository.OrderRepository, client *Client, logger *log.Logger) *OutboxDispatcher {
	return &OutboxDispatcher{repo: repo, client: client, logger: logger}
}

func (d *OutboxDispatcher) Start(ctx context.Context) {
	ticker := time.NewTicker(2 * time.Second)
	defer ticker.Stop()
	d.dispatch(ctx)
	for {
		select {
		case <-ctx.Done():
			return
		case <-ticker.C:
			d.dispatch(ctx)
		}
	}
}

func (d *OutboxDispatcher) dispatch(ctx context.Context) {
	events, err := d.repo.ListPendingOutbox(ctx, 20)
	if err != nil {
		d.logger.Printf("list trade outbox failed: %v", err)
		return
	}
	for _, event := range events {
		err := d.client.ch.PublishWithContext(ctx, TradeExchange, event.RoutingKey, false, false, amqp.Publishing{
			ContentType:  "application/json",
			DeliveryMode: amqp.Persistent,
			MessageId:    event.EventID,
			Type:         event.EventType,
			Timestamp:    time.Now(),
			Body:         []byte(event.Payload),
		})
		if err != nil {
			d.logger.Printf("publish trade outbox %s failed: %v", event.EventID, err)
			_ = d.repo.MarkOutboxFailed(ctx, event.ID)
			continue
		}
		if err := d.repo.MarkOutboxSent(ctx, event.ID); err != nil {
			d.logger.Printf("mark trade outbox %s sent failed: %v", event.EventID, err)
		}
	}
}

var _ service.TimeoutPublisher = (*Client)(nil)
