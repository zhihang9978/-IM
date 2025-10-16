package model

import (
	"time"
)

// OperationLog 操作日志模型
type OperationLog struct {
	ID           uint      `gorm:"primarykey" json:"id"`
	Action       string    `gorm:"not null;size:50;index" json:"action"` // 操作类型
	UserID       *uint     `gorm:"index" json:"user_id,omitempty"`       // 操作用户ID
	AdminID      *uint     `gorm:"index" json:"admin_id,omitempty"`      // 管理员ID（如果是管理员操作）
	IP           string    `gorm:"size:50" json:"ip"`
	UserAgent    string    `gorm:"size:500" json:"user_agent"`
	Details      string    `gorm:"type:text" json:"details"`       // JSON格式的详细信息
	Result       string    `gorm:"size:20" json:"result"`          // success, failure
	ErrorMessage string    `gorm:"size:500" json:"error_message"`  // 错误信息（如果失败）
	CreatedAt    time.Time `gorm:"index" json:"timestamp"`
}

func (OperationLog) TableName() string {
	return "operation_logs"
}

// 操作类型常量

// 用户操作
const (
	ActionUserLogin          = "user_login"
	ActionUserLogout         = "user_logout"
	ActionUserRegister       = "user_register"
	ActionPasswordChange     = "password_change"
	ActionUserProfileUpdate  = "user_profile_update"
)

// 消息操作
const (
	ActionMessageSend   = "message_send"
	ActionMessageRecall = "message_recall"
	ActionMessageDelete = "message_delete"
)

// 联系人操作
const (
	ActionContactAdd    = "contact_add"
	ActionContactDelete = "contact_delete"
	ActionContactBlock  = "contact_block"
)

// 文件操作
const (
	ActionFileUpload   = "file_upload"
	ActionFileDownload = "file_download"
	ActionFileDelete   = "file_delete"
)

// 通话操作
const (
	ActionCallInitiated     = "call_initiated"
	ActionCallAnswered      = "call_answered"
	ActionCallEnded         = "call_ended"
	ActionScreenShareStart  = "screen_share_start"
	ActionScreenShareEnd    = "screen_share_end"
)

// 管理员操作
const (
	ActionAdminUserBan           = "admin_user_ban"
	ActionAdminUserUnban         = "admin_user_unban"
	ActionAdminMessageDelete     = "admin_message_delete"
	ActionAdminGroupDisband      = "admin_group_disband"
	ActionAdminSystemConfigChange = "admin_system_config_change"
)

// 操作结果常量
const (
	ResultSuccess = "success"
	ResultFailure = "failure"
)

