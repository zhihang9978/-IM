package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type MessageDAO struct {
	db *gorm.DB
}

func NewMessageDAO() *MessageDAO {
	return &MessageDAO{
		db: mysql.GetDB(),
	}
}

// Create 创建消息
func (d *MessageDAO) Create(message *model.Message) error {
	return d.db.Create(message).Error
}

// GetByID 根据ID获取消息
func (d *MessageDAO) GetByID(id uint) (*model.Message, error) {
	var message model.Message
	err := d.db.Preload("Sender").Preload("Receiver").Where("id = ?", id).First(&message).Error
	if err != nil {
		return nil, err
	}
	return &message, nil
}

// GetByConversationID 获取会话的消息列表
func (d *MessageDAO) GetByConversationID(conversationID uint, page, pageSize int) ([]model.Message, int64, error) {
	var messages []model.Message
	var total int64

	query := d.db.Model(&model.Message{}).Where("conversation_id = ?", conversationID)

	// 统计总数
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	// 分页查询，按时间倒序
	offset := (page - 1) * pageSize
	if err := query.Preload("Sender").Preload("Receiver").
		Order("created_at DESC").
		Offset(offset).Limit(pageSize).
		Find(&messages).Error; err != nil {
		return nil, 0, err
	}

	return messages, total, nil
}

// UpdateStatus 更新消息状态
func (d *MessageDAO) UpdateStatus(id uint, status string) error {
	return d.db.Model(&model.Message{}).Where("id = ?", id).Update("status", status).Error
}

// BatchUpdateStatus 批量更新消息状态
func (d *MessageDAO) BatchUpdateStatus(ids []uint, status string) error {
	return d.db.Model(&model.Message{}).Where("id IN ?", ids).Update("status", status).Error
}

// RecallMessage 撤回消息
func (d *MessageDAO) RecallMessage(id uint) error {
	return d.UpdateStatus(id, model.MessageStatusRecalled)
}

// Delete 删除消息（软删除）
func (d *MessageDAO) Delete(id uint) error {
	return d.db.Delete(&model.Message{}, id).Error
}

// GetUnreadCount 获取未读消息数量
func (d *MessageDAO) GetUnreadCount(receiverID uint) (int64, error) {
	var count int64
	err := d.db.Model(&model.Message{}).
		Where("receiver_id = ? AND status IN ?", receiverID, []string{model.MessageStatusSent, model.MessageStatusDelivered}).
		Count(&count).Error
	return count, err
}

// MarkAsRead 标记消息为已读
func (d *MessageDAO) MarkAsRead(conversationID uint, receiverID uint) error {
	return d.db.Model(&model.Message{}).
		Where("conversation_id = ? AND receiver_id = ? AND status != ?", conversationID, receiverID, model.MessageStatusRecalled).
		Update("status", model.MessageStatusRead).Error
}

// GetLatestMessage 获取会话最新消息
func (d *MessageDAO) GetLatestMessage(conversationID uint) (*model.Message, error) {
	var message model.Message
	err := d.db.Where("conversation_id = ?", conversationID).
		Order("created_at DESC").
		First(&message).Error
	if err != nil {
		return nil, err
	}
	return &message, nil
}

// GetHistoryMessages 获取历史消息（分页加载）
// 用途：支持Android客户端下拉加载更早的聊天记录
// 
// 参数说明：
//   conversationID - 会话ID
//   beforeMessageID - 加载此消息ID之前的消息（0表示加载最新的）
//   limit - 返回的消息数量限制（建议20条）
// 
// 返回说明：
//   返回消息列表，按时间正序排列（最早的在前）
//   包含Sender和Receiver的完整信息
// 
// 实现逻辑：
//   1. 查询conversation_id匹配的消息
//   2. 如果beforeMessageID>0，只查询id<beforeMessageID的消息
//   3. 按id降序排列，取limit条
//   4. Preload关联的用户信息
//   5. 反转数组使最早的消息在前
func (d *MessageDAO) GetHistoryMessages(conversationID, beforeMessageID uint, limit int) ([]model.Message, error) {
	var messages []model.Message
	
	// 构建查询条件
	query := d.db.Where("conversation_id = ?", conversationID)
	
	// 如果指定了beforeMessageID，只获取ID更小的消息（更早的消息）
	if beforeMessageID > 0 {
		query = query.Where("id < ?", beforeMessageID)
	}
	
	// 按ID倒序查询（最新的在前），限制数量，加载关联数据
	err := query.
		Order("id DESC").
		Limit(limit).
		Preload("Sender").   // 加载发送者信息
		Preload("Receiver"). // 加载接收者信息
		Find(&messages).Error
		
	if err != nil {
		return nil, err
	}
	
	// 反转数组，使最早的消息在前面（因为前端期望正序）
	for i, j := 0, len(messages)-1; i < j; i, j = i+1, j-1 {
		messages[i], messages[j] = messages[j], messages[i]
	}
	
	return messages, nil
}

// SearchMessages 搜索消息（全文搜索）
// 参数：userID - 当前用户ID（搜索自己相关的消息）
//      keyword - 搜索关键词
//      page, pageSize - 分页参数
// 返回：消息列表和总数
// 
// ✅ 优化：使用MySQL全文索引（FULLTEXT）提升搜索性能
// 如果数据库支持全文索引，使用MATCH...AGAINST语法
// 否则降级为LIKE查询
func (d *MessageDAO) SearchMessages(userID uint, keyword string, page, pageSize int) ([]model.Message, int64, error) {
	var messages []model.Message
	var total int64
	
	offset := (page - 1) * pageSize
	
	// ✅ 尝试使用全文搜索（MySQL 5.7+支持）
	// 搜索条件：消息内容包含关键词，且用户是发送者或接收者
	query := d.db.Model(&model.Message{}).
		Where("(sender_id = ? OR receiver_id = ?)", userID, userID).
		Where("MATCH(content) AGAINST(? IN BOOLEAN MODE)", keyword)
	
	// 统计总数
	if err := query.Count(&total).Error; err != nil {
		// 如果全文索引不存在，降级为LIKE查询
		query = d.db.Model(&model.Message{}).
			Where("(sender_id = ? OR receiver_id = ?) AND content LIKE ?", 
				userID, userID, "%"+keyword+"%")
		query.Count(&total)
	}
	
	// 分页查询
	err := query.
		Preload("Sender").
		Preload("Receiver").
		Order("created_at DESC").
		Offset(offset).
		Limit(pageSize).
		Find(&messages).Error
		
	return messages, total, err
}

