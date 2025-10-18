package model

import (
	"time"
	"gorm.io/gorm"
)

type Message struct {
	ID             uint           `gorm:"primarykey" json:"id"`
	ConversationID uint           `gorm:"not null;index" json:"conversation_id"`
	SenderID       uint           `gorm:"not null;index" json:"sender_id"`
	ReceiverID     uint           `gorm:"index" json:"receiver_id"` // 群消息时为0
	GroupID        *uint          `gorm:"index" json:"group_id,omitempty"` // 群消息ID，单聊时为null
	Content        string         `gorm:"type:text;not null" json:"content"`
	Type           string         `gorm:"type:enum('text','image','voice','video','file');default:'text'" json:"type"`
	FileURL        string         `gorm:"size:500" json:"file_url,omitempty"`
	FileSize       int64          `json:"file_size,omitempty"`
	Duration       int            `json:"duration,omitempty"` // 语音/视频时长（秒）
	Status         string         `gorm:"type:enum('sent','delivered','read','recalled');default:'sent';index" json:"status"`
	CreatedAt      time.Time      `json:"created_at"`
	UpdatedAt      time.Time      `json:"updated_at"`
	DeletedAt      gorm.DeletedAt `gorm:"index" json:"-"`
	
	// 关联
	Sender   User   `gorm:"foreignKey:SenderID" json:"sender,omitempty"`
	Receiver User   `gorm:"foreignKey:ReceiverID" json:"receiver,omitempty"`
	Group    *Group `gorm:"foreignKey:GroupID" json:"group,omitempty"`
}

func (Message) TableName() string {
	return "messages"
}

// MessageType 常量
const (
	MessageTypeText  = "text"
	MessageTypeImage = "image"
	MessageTypeVoice = "voice"
	MessageTypeVideo = "video"
	MessageTypeFile  = "file"
)

// MessageStatus 常量
const (
	MessageStatusSent      = "sent"
	MessageStatusDelivered = "delivered"
	MessageStatusRead      = "read"
	MessageStatusRecalled  = "recalled"
)

