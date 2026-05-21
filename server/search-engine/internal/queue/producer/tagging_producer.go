package producer

import (
	"campusmart/search-engine/internal/queue/config"
	"campusmart/search-engine/internal/queue/model"
	searchModel "campusmart/search-engine/internal/searcher/model"
	"encoding/json"
	"log"

	"github.com/streadway/amqp"
)

type TaggingProducer struct {
	Channel *amqp.Channel
}

func NewTaggingProducer(channel *amqp.Channel) *TaggingProducer {
	err := channel.ExchangeDeclare(
		config.Config.TaggingExchange,
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

	return &TaggingProducer{
		Channel: channel,
	}
}

func (p *TaggingProducer) PublishTaggingTask(dbName string, doc *searchModel.IndexDoc) error {
	task := &model.TaggingTask{
		Database: dbName,
		Doc:      doc,
	}

	body, err := json.Marshal(task)
	if err != nil {
		return err
	}

	return p.Channel.Publish(
		config.Config.TaggingExchange,
		config.Config.TaggingKey,
		false,
		false,
		amqp.Publishing{
			ContentType:  "application/json",
			Body:         body,
			DeliveryMode: amqp.Persistent,
		},
	)
}
