package mq

import (
	"context"
	"encoding/json"
	"log"

	"campusmart/wallet-service/internal/service"
	amqp "github.com/rabbitmq/amqp091-go"
)

const (
	PaymentExchange        = "campusmart.payment.exchange"
	TradeExchange          = "campusmart.trade.exchange"
	PaymentPaidQueue       = "campusmart.wallet.payment.paid.queue"
	OrderSettledQueue      = "campusmart.wallet.order.settled.queue"
	PaymentPaidRoutingKey  = "payment.paid"
	OrderSettledRoutingKey = "trade.order.settled"
)

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
	if err := c.ch.ExchangeDeclare(PaymentExchange, "topic", true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.ExchangeDeclare(TradeExchange, "topic", true, false, false, false, nil); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(PaymentPaidQueue, true, false, false, false, nil); err != nil {
		return err
	}
	if err := c.ch.QueueBind(PaymentPaidQueue, PaymentPaidRoutingKey, PaymentExchange, false, nil); err != nil {
		return err
	}
	if _, err := c.ch.QueueDeclare(OrderSettledQueue, true, false, false, false, nil); err != nil {
		return err
	}
	return c.ch.QueueBind(OrderSettledQueue, OrderSettledRoutingKey, TradeExchange, false, nil)
}

func (c *Client) StartConsumers(ctx context.Context, walletSvc *service.WalletService) error {
	paidMsgs, err := c.ch.Consume(PaymentPaidQueue, "", false, false, false, false, nil)
	if err != nil {
		return err
	}
	settledMsgs, err := c.ch.Consume(OrderSettledQueue, "", false, false, false, false, nil)
	if err != nil {
		return err
	}

	go c.consumePaymentPaid(ctx, paidMsgs, walletSvc)
	go c.consumeOrderSettled(ctx, settledMsgs, walletSvc)
	return nil
}

func (c *Client) consumePaymentPaid(ctx context.Context, msgs <-chan amqp.Delivery, walletSvc *service.WalletService) {
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
			if err := walletSvc.HandlePaymentPaid(ctx, event); err != nil {
				c.logger.Printf("handle payment paid %d failed: %v", event.OrderID, err)
				_ = msg.Nack(false, true)
				continue
			}
			_ = msg.Ack(false)
		}
	}
}

func (c *Client) consumeOrderSettled(ctx context.Context, msgs <-chan amqp.Delivery, walletSvc *service.WalletService) {
	for {
		select {
		case <-ctx.Done():
			return
		case msg, ok := <-msgs:
			if !ok {
				return
			}
			var event service.OrderSettledEvent
			if err := json.Unmarshal(msg.Body, &event); err != nil {
				_ = msg.Ack(false)
				continue
			}
			if err := walletSvc.HandleOrderSettled(ctx, event); err != nil {
				c.logger.Printf("handle order settled %d failed: %v", event.OrderID, err)
				_ = msg.Nack(false, true)
				continue
			}
			_ = msg.Ack(false)
		}
	}
}
