package mq

import (
	"context"
	"log"
	"time"

	"campusmart/payment-service/internal/repository"
	amqp "github.com/rabbitmq/amqp091-go"
)

const PaymentExchange = "campusmart.payment.exchange"

type OutboxDispatcher struct {
	repo   *repository.PaymentRepository
	conn   *amqp.Connection
	ch     *amqp.Channel
	logger *log.Logger
}

func NewOutboxDispatcher(rabbitURL string, repo *repository.PaymentRepository, logger *log.Logger) (*OutboxDispatcher, error) {
	conn, err := amqp.Dial(rabbitURL)
	if err != nil {
		return nil, err
	}
	ch, err := conn.Channel()
	if err != nil {
		_ = conn.Close()
		return nil, err
	}
	if err := ch.ExchangeDeclare(PaymentExchange, "topic", true, false, false, false, nil); err != nil {
		_ = ch.Close()
		_ = conn.Close()
		return nil, err
	}
	return &OutboxDispatcher{
		repo:   repo,
		conn:   conn,
		ch:     ch,
		logger: logger,
	}, nil
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

func (d *OutboxDispatcher) Close() {
	if d.ch != nil {
		_ = d.ch.Close()
	}
	if d.conn != nil {
		_ = d.conn.Close()
	}
}

func (d *OutboxDispatcher) dispatch(ctx context.Context) {
	events, err := d.repo.ListPendingOutbox(ctx, 20)
	if err != nil {
		d.logger.Printf("list payment outbox failed: %v", err)
		return
	}

	for _, event := range events {
		err := d.ch.PublishWithContext(ctx, PaymentExchange, event.RoutingKey, false, false, amqp.Publishing{
			ContentType:  "application/json",
			DeliveryMode: amqp.Persistent,
			MessageId:    event.EventID,
			Type:         event.EventType,
			Timestamp:    time.Now(),
			Body:         []byte(event.Payload),
		})
		if err != nil {
			d.logger.Printf("publish payment outbox %s failed: %v", event.EventID, err)
			_ = d.repo.MarkOutboxFailed(ctx, event.ID)
			continue
		}
		if err := d.repo.MarkOutboxSent(ctx, event.ID); err != nil {
			d.logger.Printf("mark payment outbox %s sent failed: %v", event.EventID, err)
		}
	}
}
