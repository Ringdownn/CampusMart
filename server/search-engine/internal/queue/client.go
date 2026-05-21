package queue

import (
	"campusmart/search-engine/internal/queue/config"
	"fmt"
	"log"

	"github.com/streadway/amqp"
)

type RabbitMQClient struct {
	Connection *amqp.Connection
	Channel    *amqp.Channel
}

func NewRabbitMQClient() (*RabbitMQClient, error) {
	url := fmt.Sprintf(
		"amqp://%s:%s@%s:%d%s/",
		config.Config.User,
		config.Config.Password,
		config.Config.Host,
		config.Config.Port,
		config.Config.VHost,
	)

	conn, err := amqp.Dial(url)
	if err != nil {
		return nil, err
	}

	ch, err := conn.Channel()
	if err != nil {
		return nil, err
	}

	return &RabbitMQClient{
		Connection: conn,
		Channel:    ch,
	}, nil
}

func (rc *RabbitMQClient) Close() {
	defer func() {
		if r := recover(); r != nil {
			log.Printf("Error closing RabbitMQ: %v", r)
		}
	}()

	if rc.Channel != nil {
		if err := rc.Channel.Close(); err != nil {
			log.Printf("Error closing channel: %v", err)
		}
	}
	if rc.Connection != nil {
		if err := rc.Connection.Close(); err != nil {
			log.Printf("Error closing connection: %v", err)
		}
	}
}

func (rc *RabbitMQClient) ExchangeDeclare(exchangeName, kind string) error {
	return rc.Channel.ExchangeDeclare(
		exchangeName,
		kind,
		true,
		false,
		false,
		false,
		nil,
	)
}

func (rc *RabbitMQClient) QueueDeclare(queueName string) (amqp.Queue, error) {
	return rc.Channel.QueueDeclare(
		queueName,
		true,
		false,
		false,
		false,
		nil,
	)
}

func (rc *RabbitMQClient) QueueBind(queueName string, routingKey, exchangeName string) error {
	return rc.Channel.QueueBind(
		queueName,
		routingKey,
		exchangeName,
		false,
		nil,
	)
}
