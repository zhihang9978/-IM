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

// GetUserConversations 获取用户的所有会话（含完整关联数据）
func (d *ConversationDAO) GetUserConversations(userID uint) ([]model.Conversation, error) {
	var conversations []model.Conversation
	err := d.db.Where("user1_id = ? OR user2_id = ?", userID, userID).
		Preload("User1").              // 加载User1完整信息
		Preload("User2").              // 加载User2完整信息
		Preload("Group").              // 加载Group信息（如果是群聊）
		Preload("LastMessage").        // ✅ 加载最后一条消息
		Preload("LastMessage.Sender"). // ✅ 加载消息发送者信息
		Order("updated_at DESC").      // 按更新时间倒序
		Find(&conversations).Error
	return conversations, err
}

// Create 创建会话
func (d *ConversationDAO) Create(conversation *model.Conversation) error {
	return d.db.Create(conversation).Error
}

// GetUnreadCount 获取会话的未读消息数量
// 
// 参数说明：
//   conversationID - 会话ID
//   userID - 当前用户ID（作为接收者）
// 
// 返回说明：
//   返回该会话中，接收者为userID且状态不为"read"的消息数量
// 
// 使用场景：
//   在会话列表中显示未读徽章
func (d *ConversationDAO) GetUnreadCount(conversationID, userID uint) int {
	var count int64
	
	// 统计条件：
	// 1. 属于该会话
	// 2. 接收者是当前用户
	// 3. 状态不是"read"（包括sent和delivered）
	d.db.Model(&model.Message{}).
		Where("conversation_id = ? AND receiver_id = ? AND status != ?", 
			conversationID, 
			userID, 
			model.MessageStatusRead).
		Count(&count)
		
	return int(count)
}

