package model

import (
	"time"
	"gorm.io/gorm"
)

type User struct {
	ID          uint           `gorm:"primarykey" json:"id"`
	Username    string         `gorm:"uniqueIndex;not null;size:50" json:"username"`
	Phone       *string        `gorm:"uniqueIndex;size:20" json:"phone"`
	Email       *string        `gorm:"uniqueIndex;size:100" json:"email"`
	Password    string         `gorm:"not null;size:255" json:"-"`
	Avatar      string         `gorm:"size:500" json:"avatar"`
	LanxinID    string         `gorm:"uniqueIndex;not null;size:50;column:lanxin_id" json:"lanxin_id"`
	Role        string         `gorm:"type:enum('user','admin');default:'user'" json:"role"`
	Status      string         `gorm:"type:enum('active','banned','deleted');default:'active'" json:"status"`
	LastLoginAt *time.Time     `json:"last_login_at"`
	CreatedAt   time.Time      `json:"created_at"`
	UpdatedAt   time.Time      `json:"updated_at"`
	DeletedAt   gorm.DeletedAt `gorm:"index" json:"-"`
}

func (User) TableName() string {
	return "users"
}

// UserResponse 用于API响应的用户信息（不包含敏感信息）
type UserResponse struct {
	ID          uint       `json:"id"`
	Username    string     `json:"username"`
	Phone       *string    `json:"phone"`
	Email       *string    `json:"email"`
	Avatar      string     `json:"avatar"`
	LanxinID    string     `json:"lanxin_id"`
	Role        string     `json:"role"`
	Status      string     `json:"status"`
	LastLoginAt *time.Time `json:"last_login_at"`
	CreatedAt   time.Time  `json:"created_at"`
}

func (u *User) ToResponse() *UserResponse {
	return &UserResponse{
		ID:          u.ID,
		Username:    u.Username,
		Phone:       u.Phone,
		Email:       u.Email,
		Avatar:      u.Avatar,
		LanxinID:    u.LanxinID,
		Role:        u.Role,
		Status:      u.Status,
		LastLoginAt: u.LastLoginAt,
		CreatedAt:   u.CreatedAt,
	}
}

