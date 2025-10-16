package kafka

import (
	"context"
	"encoding/json"
	"log"
	"time"

	"github.com/segmentio/kafka-go"
)

// Producer Kafka生产者
type Producer struct {
	writer *kafka.Writer
}

// NewProducer 创建新的Kafka生产者
func NewProducer(brokers []string, topic string) *Producer {
	w := &kafka.Writer{
		Addr:         kafka.TCP(brokers...),
		Topic:        topic,
		Balancer:     &kafka.LeastBytes{},
		RequiredAcks: kafka.RequireAll, // 等待所有副本确认
		Async:        false,             // 同步发送，保证可靠性
		Compression:  kafka.Snappy,
		MaxAttempts:  3,
		WriteTimeout: 10 * time.Second,
	}

	return &Producer{writer: w}
}

// SendMessage 发送消息到Kafka
func (p *Producer) SendMessage(ctx context.Context, key, value []byte) error {
	message := kafka.Message{
		Key:   key,
		Value: value,
		Time:  time.Now(),
	}

	err := p.writer.WriteMessages(ctx, message)
	if err != nil {
		log.Printf("Failed to write message to Kafka: %v", err)
		return err
	}

	return nil
}

// SendMessageWithPartition 发送消息到指定分区
func (p *Producer) SendMessageWithPartition(ctx context.Context, partition int, key, value []byte) error {
	message := kafka.Message{
		Partition: partition,
		Key:       key,
		Value:     value,
		Time:      time.Now(),
	}

	err := p.writer.WriteMessages(ctx, message)
	if err != nil {
		log.Printf("Failed to write message to Kafka: %v", err)
		return err
	}

	return nil
}

// SendJSON 发送JSON格式的消息
func (p *Producer) SendJSON(ctx context.Context, key string, data interface{}) error {
	value, err := json.Marshal(data)
	if err != nil {
		return err
	}

	return p.SendMessage(ctx, []byte(key), value)
}

// Close 关闭生产者
func (p *Producer) Close() error {
	return p.writer.Close()
}

// MessageData 消息数据结构
type MessageData struct {
	ID             uint   `json:"id"`
	ConversationID uint   `json:"conversation_id"`
	SenderID       uint   `json:"sender_id"`
	ReceiverID     uint   `json:"receiver_id"`
	Content        string `json:"content"`
	Type           string `json:"type"`
	FileURL        string `json:"file_url,omitempty"`
	CreatedAt      int64  `json:"created_at"`
}

