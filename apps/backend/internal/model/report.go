package model

import "time"

// Report 举报消息模型
type Report struct {
	ID         uint      `gorm:"primarykey" json:"id"`
	ReporterID uint      `gorm:"not null;index" json:"reporter_id"`
	MessageID  uint      `gorm:"not null;index" json:"message_id"`
	Reason     string    `gorm:"size:50;not null" json:"reason"`
	Status     string    `gorm:"size:20;not null;default:'pending';index" json:"status"`
	AdminNote  string    `gorm:"type:text" json:"admin_note,omitempty"`
	CreatedAt  time.Time `json:"created_at"`
	UpdatedAt  time.Time `json:"updated_at"`
	
	// 关联
	Reporter User    `gorm:"foreignKey:ReporterID" json:"reporter,omitempty"`
	Message  Message `gorm:"foreignKey:MessageID" json:"message,omitempty"`
}

func (Report) TableName() string {
	return "reports"
}

// ReportStatus 常量
const (
	ReportStatusPending  = "pending"
	ReportStatusReviewed = "reviewed"
	ReportStatusResolved = "resolved"
)

