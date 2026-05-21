package consumer

import (
	"campusmart/search-engine/internal/queue/config"
	"campusmart/search-engine/internal/queue/model"
	"campusmart/search-engine/internal/web/service"
	"encoding/json"
	"fmt"
	"log"

	"github.com/streadway/amqp"
)

type IndexConsumer struct {
	channel      *amqp.Channel
	indexService *service.Index
	sources      []string
}

func NewIndexConsumer(channel *amqp.Channel, indexService *service.Index, sources []string) *IndexConsumer {
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

	q, err := channel.QueueDeclare(
		config.Config.IndexQueue,
		true,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Failed to declare queue: %v", err)
	}

	for _, source := range sources {
		routingKey := fmt.Sprintf("%s.%s", config.Config.IndexKey, source)
		err = channel.QueueBind(
			q.Name,
			routingKey,
			config.Config.IndexExchange,
			false,
			nil,
		)
		if err != nil {
			log.Fatalf("Failed to bind queue: %v", err)
		}
	}

	return &IndexConsumer{
		channel:      channel,
		indexService: indexService,
		sources:      sources,
	}
}

func (ic *IndexConsumer) Consume() {
	msgs, err := ic.channel.Consume(
		config.Config.IndexQueue,
		"",
		false,
		false,
		false,
		false,
		nil,
	)
	if err != nil {
		log.Fatalf("Failed to register a consumer: %v", err)
	}

	go func() {
		defer func() {
			if r := recover(); r != nil {
				log.Printf("Consumer panic: %v", r)
			}
		}()

		for msg := range msgs {
			var task model.IndexTask

			if err := json.Unmarshal(msg.Body, &task); err != nil {
				log.Printf("Error unmarshalling message: %v", err)
				msg.Nack(false, false)
				continue
			}
			if task.Database == "" || task.Doc == nil {
				log.Printf("Invalid index task from %s: database or doc is empty", task.Source)
				msg.Nack(false, false)
				continue
			}

			if err := ic.indexService.AddIndex(task.Database, task.Doc); err != nil {
				log.Printf("Error adding index: %v", err)
				msg.Nack(false, true)
				continue
			}

			log.Printf("Index updated from %s: %d", task.Source, task.Doc.Id)
			msg.Ack(false)
		}
	}()

	log.Println("Index consumer started")
}
