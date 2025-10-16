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

