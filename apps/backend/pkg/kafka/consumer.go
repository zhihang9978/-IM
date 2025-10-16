package kafka

import (
	"context"
	"log"
	"time"

	"github.com/segmentio/kafka-go"
)

// Consumer Kafka消费者
type Consumer struct {
	reader  *kafka.Reader
	handler MessageHandler
}

// MessageHandler 消息处理接口
type MessageHandler interface {
	Handle(ctx context.Context, message kafka.Message) error
}

// NewConsumer 创建新的Kafka消费者
func NewConsumer(brokers []string, topic, groupID string, handler MessageHandler) *Consumer {
	r := kafka.NewReader(kafka.ReaderConfig{
		Brokers:        brokers,
		Topic:          topic,
		GroupID:        groupID,
		MinBytes:       10e3, // 10KB
		MaxBytes:       10e6, // 10MB
		CommitInterval: time.Second,
		StartOffset:    kafka.LastOffset,
	})

	return &Consumer{
		reader:  r,
		handler: handler,
	}
}

// Start 启动消费者
func (c *Consumer) Start(ctx context.Context) {
	log.Printf("Starting Kafka consumer for topic: %s", c.reader.Config().Topic)

	for {
		select {
		case <-ctx.Done():
			log.Println("Kafka consumer context cancelled")
			return
		default:
			message, err := c.reader.FetchMessage(ctx)
			if err != nil {
				log.Printf("Error fetching message: %v", err)
				continue
			}

			log.Printf("Received message: topic=%s, partition=%d, offset=%d, key=%s",
				message.Topic, message.Partition, message.Offset, string(message.Key))

			// 处理消息
			if err := c.handler.Handle(ctx, message); err != nil {
				log.Printf("Error handling message: %v", err)
				// TODO: 考虑重试或将错误消息发送到死信队列
			} else {
				// 提交offset
				if err := c.reader.CommitMessages(ctx, message); err != nil {
					log.Printf("Error committing message: %v", err)
				}
			}
		}
	}
}

// Close 关闭消费者
func (c *Consumer) Close() error {
	return c.reader.Close()
}

