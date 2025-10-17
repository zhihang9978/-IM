package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type ConversationDAO struct {
	db *gorm.DB
}

func NewConversationDAO() *ConversationDAO {
	return &ConversationDAO{
		db: mysql.GetDB(),
	}
}

// GetUserConversations 获取用户的所有会话
func (d *ConversationDAO) GetUserConversations(userID uint) ([]model.Conversation, error) {
	var conversations []model.Conversation
	err := d.db.Where("user1_id = ? OR user2_id = ?", userID, userID).
		Preload("User1").
		Preload("User2").
		Order("last_message_at DESC").
		Find(&conversations).Error
	return conversations, err
}

// Create 创建会话
func (d *ConversationDAO) Create(conversation *model.Conversation) error {
	return d.db.Create(conversation).Error
}

