package producer

import (
	"campusmart/search-engine/internal/queue/config"
	"campusmart/search-engine/internal/queue/model"
	"encoding/json"
	"fmt"
	"log"

	"github.com/streadway/amqp"
)

type IndexProducer struct {
	channel *amqp.Channel
}

func NewIndexProducer(channel *amqp.Channel) *IndexProducer {
	// 声明交换机
	err := channel.ExchangeDeclare(
		config.Config.IndexExchange,
		"direct",
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Failed to declare exchange: %v", err)
	}

	return &IndexProducer{
		channel: channel,
	}
}

func (p *IndexProducer) PublishIndexTask(task *model.IndexTask) error {
	body, err := json.Marshal(task)
	if err != nil {
		return err
	}

	routingKey := fmt.Sprintf("%s.%s", config.Config.IndexKey, task.Source)

	return p.channel.Publish(
		config.Config.IndexExchange,
		routingKey,
		false,
		false,
		amqp.Publishing{
			ContentType:  "application/json",
			Body:         body,
			DeliveryMode: amqp.Persistent,
		},
	)
}
