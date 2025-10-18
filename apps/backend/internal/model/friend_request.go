package model

import (
	"time"
)

type FriendRequest struct {
	ID          uint      `gorm:"primarykey" json:"id"`
	SenderID    uint      `gorm:"not null;index" json:"sender_id"`
	ReceiverID  uint      `gorm:"not null;index" json:"receiver_id"`
	Message     string    `gorm:"size:255" json:"message,omitempty"`
	Status      string    `gorm:"type:enum('pending','accepted','rejected','expired');default:'pending';index" json:"status"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
	
	Sender      User      `gorm:"foreignKey:SenderID" json:"sender,omitempty"`
	Receiver    User      `gorm:"foreignKey:ReceiverID" json:"receiver,omitempty"`
}

func (FriendRequest) TableName() string {
	return "friend_requests"
}

const (
	FriendRequestStatusPending  = "pending"
	FriendRequestStatusAccepted = "accepted"
	FriendRequestStatusRejected = "rejected"
	FriendRequestStatusExpired  = "expired"
)
