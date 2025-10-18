package dao

import (
	"time"

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
//
//	conversationID - 会话ID
//	userID - 当前用户ID（作为接收者）
//
// 返回说明：
//
//	返回该会话中，接收者为userID且状态不为"read"的消息数量
//
// 使用场景：
//
//	在会话列表中显示未读徽章
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

// UpdateSettings 更新会话设置
func (d *ConversationDAO) UpdateSettings(conversationID, userID uint, settings map[string]interface{}) error {
	// 验证会话属于当前用户
	var conv model.Conversation
	err := d.db.Where("id = ? AND (user1_id = ? OR user2_id = ?)", conversationID, userID, userID).
		First(&conv).Error
	if err != nil {
		return err
	}

	// 更新设置
	return d.db.Model(&model.Conversation{}).
		Where("id = ?", conversationID).
		Updates(settings).Error
}

// GetConversationSettings 获取会话设置
func (d *ConversationDAO) GetConversationSettings(conversationID, userID uint) (*model.Conversation, error) {
	var conv model.Conversation
	err := d.db.Where("id = ? AND (user1_id = ? OR user2_id = ?)", conversationID, userID, userID).
		Select("is_muted", "is_top", "is_starred", "is_blocked").
		First(&conv).Error
	return &conv, err
}

// GetOrCreateSingleConversation 获取或创建单聊会话
//
// 功能说明:
//   - 如果user1和user2之间已有会话,返回现有会话ID
//   - 如果没有会话,创建新会话并返回ID
//   - 自动处理user1ID和user2ID的顺序(小的在前)
//
// 参数:
//   - user1ID: 用户1的ID
//   - user2ID: 用户2的ID
//
// 返回:
//   - conversationID: 会话ID
//   - error: 错误信息
func (d *ConversationDAO) GetOrCreateSingleConversation(user1ID, user2ID uint) (uint, error) {
	// 确保user1ID < user2ID (避免user1↔user2和user2↔user1两个会话)
	if user1ID > user2ID {
		user1ID, user2ID = user2ID, user1ID
	}

	// 查询是否已存在会话
	var conv model.Conversation
	err := d.db.Where(
		"type = ? AND ((user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?))",
		model.ConversationTypeSingle,
		user1ID, user2ID, user2ID, user1ID,
	).First(&conv).Error

	if err == nil {
		// 会话已存在,返回ID
		return conv.ID, nil
	}

	// 会话不存在,创建新会话
	newConv := &model.Conversation{
		Type:    model.ConversationTypeSingle,
		User1ID: &user1ID,
		User2ID: &user2ID,
	}

	if err := d.db.Create(newConv).Error; err != nil {
		return 0, err
	}

	return newConv.ID, nil
}

// UpdateLastMessage 更新会话的最后一条消息
//
// 功能说明:
//   - 更新会话的last_message_id和last_message_at字段
//   - 用于会话列表排序和显示最新消息
//
// 参数:
//   - conversationID: 会话ID
//   - messageID: 消息ID
//   - timestamp: 消息时间
func (d *ConversationDAO) UpdateLastMessage(conversationID, messageID uint, timestamp *time.Time) error {
	return d.db.Model(&model.Conversation{}).
		Where("id = ?", conversationID).
		Updates(map[string]interface{}{
			"last_message_id": messageID,
			"last_message_at": timestamp,
		}).Error
}

// GetOrCreateGroupConversation 获取或创建群聊会话
// 
// 功能说明:
//   - 如果群组已有会话,返回现有会话ID
//   - 如果没有会话,创建新会话并返回ID
// 
// 参数:
//   - groupID: 群组ID
// 
// 返回:
//   - conversationID: 会话ID
//   - error: 错误信息
func (d *ConversationDAO) GetOrCreateGroupConversation(groupID uint) (uint, error) {
	// 查询是否已存在群会话
	var conv model.Conversation
	err := d.db.Where("type = ? AND group_id = ?", model.ConversationTypeGroup, groupID).
		First(&conv).Error
	
	if err == nil {
		// 会话已存在,返回ID
		return conv.ID, nil
	}
	
	// 会话不存在,创建新会话
	newConv := &model.Conversation{
		Type:    model.ConversationTypeGroup,
		GroupID: &groupID,
	}
	
	if err := d.db.Create(newConv).Error; err != nil {
		return 0, err
	}
	
	return newConv.ID, nil
}
