package model

import "time"

// Favorite 收藏消息模型
type Favorite struct {
	ID        uint      `gorm:"primarykey" json:"id"`
	UserID    uint      `gorm:"not null;index" json:"user_id"`
	MessageID uint      `gorm:"not null;index" json:"message_id"`
	Content   string    `gorm:"type:text;not null" json:"content"`
	Type      string    `gorm:"size:20;not null" json:"type"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
	
	// 关联
	User    User    `gorm:"foreignKey:UserID" json:"user,omitempty"`
	Message Message `gorm:"foreignKey:MessageID" json:"message,omitempty"`
}

func (Favorite) TableName() string {
	return "favorites"
}

