package model

import (
	"time"
)

type Conversation struct {
	ID            uint       `gorm:"primarykey" json:"id"`
	Type          string     `gorm:"type:enum('single','group');default:'single';index" json:"type"`
	User1ID       *uint      `gorm:"index" json:"user1_id,omitempty"`
	User2ID       *uint      `gorm:"index" json:"user2_id,omitempty"`
	GroupID       *uint      `gorm:"index" json:"group_id,omitempty"`
	LastMessageID *uint      `json:"last_message_id,omitempty"`
	LastMessageAt *time.Time `gorm:"index" json:"last_message_at,omitempty"`
	CreatedAt     time.Time  `json:"created_at"`
	UpdatedAt     time.Time  `json:"updated_at"`
	
	// 关联
	User1       *User    `gorm:"foreignKey:User1ID" json:"user1,omitempty"`
	User2       *User    `gorm:"foreignKey:User2ID" json:"user2,omitempty"`
	Group       *Group   `gorm:"foreignKey:GroupID" json:"group,omitempty"`
	LastMessage *Message `gorm:"foreignKey:LastMessageID" json:"last_message,omitempty"`
}

func (Conversation) TableName() string {
	return "conversations"
}

// ConversationType 常量
const (
	ConversationTypeSingle = "single"
	ConversationTypeGroup  = "group"
)

