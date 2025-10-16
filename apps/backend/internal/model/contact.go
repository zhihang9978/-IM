package model

import (
	"time"
)

type Contact struct {
	ID        uint      `gorm:"primarykey" json:"id"`
	UserID    uint      `gorm:"not null;index" json:"user_id"`
	ContactID uint      `gorm:"not null;index" json:"contact_id"`
	Remark    string    `gorm:"size:50" json:"remark,omitempty"`
	Tags      string    `gorm:"size:255" json:"tags,omitempty"` // 逗号分隔的标签
	Status    string    `gorm:"type:enum('normal','blocked');default:'normal';index" json:"status"`
	CreatedAt time.Time `json:"created_at"`
	UpdatedAt time.Time `json:"updated_at"`
	
	// 关联
	User        User `gorm:"foreignKey:UserID" json:"user,omitempty"`
	ContactUser User `gorm:"foreignKey:ContactID" json:"contact_user,omitempty"`
}

func (Contact) TableName() string {
	return "contacts"
}

// ContactStatus 常量
const (
	ContactStatusNormal  = "normal"
	ContactStatusBlocked = "blocked"
)

