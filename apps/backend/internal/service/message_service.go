package service

import (
	"context"
	"errors"
	"time"

	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/websocket"
	"github.com/lanxin/im-backend/pkg/kafka"
)

type MessageService struct {
	messageDAO *dao.MessageDAO
	userDAO    *dao.UserDAO
	logDAO     *dao.OperationLogDAO
	hub        *websocket.Hub
	producer   *kafka.Producer
}

func NewMessageService(hub *websocket.Hub, producer *kafka.Producer) *MessageService {
	return &MessageService{
		messageDAO: dao.NewMessageDAO(),
		userDAO:    dao.NewUserDAO(),
		logDAO:     dao.NewOperationLogDAO(),
		hub:        hub,
		producer:   producer,
	}
}

// SendMessage 发送消息
func (s *MessageService) SendMessage(senderID, receiverID uint, content, msgType string, fileURL *string, fileSize *int64, duration *int, ip, userAgent string) (*model.Message, error) {
	// 验证接收者存在
	_, err := s.userDAO.GetByID(receiverID)
	if err != nil {
		return nil, errors.New("receiver not found")
	}

	// 创建消息
	message := &model.Message{
		SenderID:   senderID,
		ReceiverID: receiverID,
		Content:    content,
		Type:       msgType,
		Status:     model.MessageStatusSent,
	}

	if fileURL != nil {
		message.FileURL = *fileURL
	}
	if fileSize != nil {
		message.FileSize = *fileSize
	}
	if duration != nil {
		message.Duration = *duration
	}

	// 保存到数据库
	if err := s.messageDAO.Create(message); err != nil {
		// 记录失败日志
		s.logDAO.CreateLog(dao.LogRequest{
			Action:       model.ActionMessageSend,
			UserID:       &senderID,
			IP:           ip,
			UserAgent:    userAgent,
			Details:      map[string]interface{}{"receiver_id": receiverID, "type": msgType},
			Result:       model.ResultFailure,
			ErrorMessage: err.Error(),
		})
		return nil, err
	}

	// 发送到Kafka（异步持久化和处理）
	go func() {
		ctx := context.Background()
		messageData := kafka.MessageData{
			ID:             message.ID,
			ConversationID: message.ConversationID,
			SenderID:       senderID,
			ReceiverID:     receiverID,
			Content:        content,
			Type:           msgType,
			FileURL:        message.FileURL,
			CreatedAt:      message.CreatedAt.Unix(),
		}
		if err := s.producer.SendJSON(ctx, string(message.ID), messageData); err != nil {
			// TODO: 处理Kafka发送失败
		}
	}()

	// 通过WebSocket实时推送给接收者
	go func() {
		if s.hub.IsUserOnline(receiverID) {
			s.hub.SendMessageNotification(receiverID, message)
			// 更新消息状态为已送达
			s.messageDAO.UpdateStatus(message.ID, model.MessageStatusDelivered)
		}
	}()

	// 记录成功日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionMessageSend,
		UserID:    &senderID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"message_id":  message.ID,
			"receiver_id": receiverID,
			"type":        msgType,
		},
		Result: model.ResultSuccess,
	})

	return message, nil
}

// RecallMessage 撤回消息
func (s *MessageService) RecallMessage(messageID, userID uint, ip, userAgent string) error {
	message, err := s.messageDAO.GetByID(messageID)
	if err != nil {
		return err
	}

	// 只能撤回自己发送的消息
	if message.SenderID != userID {
		return errors.New("can only recall your own messages")
	}

	// 检查是否在2分钟内
	if time.Since(message.CreatedAt) > 2*time.Minute {
		return errors.New("can only recall messages within 2 minutes")
	}

	// 更新消息状态
	err = s.messageDAO.RecallMessage(messageID)

	// 通知接收者
	go func() {
		if s.hub.IsUserOnline(message.ReceiverID) {
			s.hub.SendMessageStatusUpdate(message.ReceiverID, messageID, model.MessageStatusRecalled)
		}
	}()

	// 记录操作日志
	details := map[string]interface{}{
		"message_id":      messageID,
		"conversation_id": message.ConversationID,
		"receiver_id":     message.ReceiverID,
		"original_type":   message.Type,
		"recall_time_diff": time.Since(message.CreatedAt).Seconds(),
	}

	result := model.ResultSuccess
	errorMsg := ""
	if err != nil {
		result = model.ResultFailure
		errorMsg = err.Error()
	}

	s.logDAO.CreateLog(dao.LogRequest{
		Action:       model.ActionMessageRecall,
		UserID:       &userID,
		IP:           ip,
		UserAgent:    userAgent,
		Details:      details,
		Result:       result,
		ErrorMessage: errorMsg,
	})

	return err
}

// MarkAsRead 标记消息为已读并发送已读回执
func (s *MessageService) MarkAsRead(conversationID, userID uint) error {
	// 标记会话中所有未读消息为已读
	err := s.messageDAO.MarkAsRead(conversationID, userID)
	if err != nil {
		return err
	}
	
	// 获取该会话中userID作为接收者的所有消息，找到发送者
	messages, _, err := s.messageDAO.GetByConversationID(conversationID, 1, 100)
	if err != nil || len(messages) == 0 {
		return err
	}
	
	// 找出对方用户ID（发送者）
	var senderID uint
	for _, msg := range messages {
		if msg.ReceiverID == userID {
			senderID = msg.SenderID
			break
		}
	}
	
	if senderID == 0 {
		return nil // 没有找到对方
	}
	
	// 通过WebSocket发送已读回执给所有发送者
	go func() {
		if s.hub.IsUserOnline(senderID) {
			// 发送已读回执通知
			readReceipt := map[string]interface{}{
				"conversation_id": conversationID,
				"reader_id":       userID,
				"read_at":         time.Now().Format(time.RFC3339),
			}
			s.hub.SendToUser(senderID, map[string]interface{}{
				"type": "read_receipt",
				"data": readReceipt,
			})
		}
	}()
	
	return nil
}

// GetMessages 获取消息列表
func (s *MessageService) GetMessages(conversationID uint, page, pageSize int) ([]model.Message, int64, error) {
	return s.messageDAO.GetByConversationID(conversationID, page, pageSize)
}

