package service

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"time"

	goredis "github.com/go-redis/redis/v8"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/redis"
	"github.com/lanxin/im-backend/internal/websocket"
	"github.com/lanxin/im-backend/pkg/kafka"
)

type MessageService struct {
	messageDAO      *dao.MessageDAO
	conversationDAO *dao.ConversationDAO
	userDAO         *dao.UserDAO
	logDAO          *dao.OperationLogDAO
	hub             *websocket.Hub
	producer        *kafka.Producer
	redisClient     *goredis.Client
}

func NewMessageService(hub *websocket.Hub, producer *kafka.Producer) *MessageService {
	return &MessageService{
		messageDAO:      dao.NewMessageDAO(),
		conversationDAO: dao.NewConversationDAO(),
		userDAO:         dao.NewUserDAO(),
		logDAO:          dao.NewOperationLogDAO(),
		hub:             hub,
		producer:        producer,
		redisClient:     redis.GetClient(),
	}
}

// SendMessage 发送消息
func (s *MessageService) SendMessage(senderID, receiverID uint, content, msgType string, fileURL *string, fileSize *int64, duration *int, ip, userAgent string) (*model.Message, error) {
	// 验证接收者存在
	_, err := s.userDAO.GetByID(receiverID)
	if err != nil {
		return nil, errors.New("receiver not found")
	}

	// 获取或创建会话
	conversationID, err := s.conversationDAO.GetOrCreateSingleConversation(senderID, receiverID)
	if err != nil {
		return nil, errors.New("failed to get or create conversation")
	}

	// 创建消息
	message := &model.Message{
		ConversationID: conversationID,
		SenderID:       senderID,
		ReceiverID:     receiverID,
		Content:        content,
		Type:           msgType,
		Status:         model.MessageStatusSent,
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

	// 更新会话的最后一条消息
	now := time.Now()
	s.conversationDAO.UpdateLastMessage(conversationID, message.ID, &now)

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

		// ✅ Kafka发送失败处理（最多重试3次）
		maxRetries := 3
		for i := 0; i < maxRetries; i++ {
			if err := s.producer.SendJSON(ctx, string(message.ID), messageData); err != nil {
				if i == maxRetries-1 {
					// 最后一次失败，记录错误日志
					s.logDAO.CreateLog(dao.LogRequest{
						Action: "kafka_send_failed",
						UserID: &senderID,
						Details: map[string]interface{}{
							"message_id":  message.ID,
							"retry_count": maxRetries,
							"error":       err.Error(),
						},
						Result:       model.ResultFailure,
						ErrorMessage: err.Error(),
					})
				} else {
					// 等待后重试
					time.Sleep(time.Duration(i+1) * 100 * time.Millisecond)
					continue
				}
			} else {
				// 发送成功
				break
			}
		}
	}()

	// 通过WebSocket实时推送给接收者
	go func() {
		if s.hub.IsUserOnline(receiverID) {
			// 在线: 尝试推送
			err := s.hub.SendMessageNotification(receiverID, message)
			if err == nil {
				// 推送成功,更新状态为已送达
				s.messageDAO.UpdateStatus(message.ID, model.MessageStatusDelivered)
			} else {
				// 推送失败,存入离线队列
				s.saveToOfflineQueue(receiverID, message.ID)
			}
		} else {
			// 离线: 存入离线消息队列
			s.saveToOfflineQueue(receiverID, message.ID)
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
		"message_id":       messageID,
		"conversation_id":  message.ConversationID,
		"receiver_id":      message.ReceiverID,
		"original_type":    message.Type,
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

// GetHistoryMessages 获取历史消息（业务层）
// 直接调用DAO层，未来可在此添加业务逻辑（如权限验证、敏感词过滤等）
func (s *MessageService) GetHistoryMessages(conversationID, beforeMessageID uint, limit int) ([]model.Message, error) {
	// 限制每次最多查询100条，防止数据量过大
	if limit > 100 {
		limit = 100
	}

	if limit <= 0 {
		limit = 20 // 默认20条
	}

	return s.messageDAO.GetHistoryMessages(conversationID, beforeMessageID, limit)
}

// SearchMessages 搜索消息
func (s *MessageService) SearchMessages(userID uint, keyword string, page, pageSize int) ([]model.Message, int64, error) {
	if keyword == "" {
		return nil, 0, nil
	}
	return s.messageDAO.SearchMessages(userID, keyword, page, pageSize)
}

// saveToOfflineQueue 保存消息到离线队列
func (s *MessageService) saveToOfflineQueue(userID uint, messageID uint) error {
	key := fmt.Sprintf("offline_msg:%d", userID)
	ctx := context.Background()
	
	// 存入Redis List (RPUSH = 从右边插入)
	err := s.redisClient.RPush(ctx, key, messageID).Err()
	if err != nil {
		return err
	}
	
	// 设置7天过期
	s.redisClient.Expire(ctx, key, 7*24*time.Hour)
	
	return nil
}

// GetOfflineMessages 获取用户的离线消息
func (s *MessageService) GetOfflineMessages(userID uint) ([]model.Message, error) {
	key := fmt.Sprintf("offline_msg:%d", userID)
	ctx := context.Background()
	
	// 从Redis读取所有消息ID
	messageIDs, err := s.redisClient.LRange(ctx, key, 0, -1).Result()
	if err != nil {
		return nil, err
	}
	
	if len(messageIDs) == 0 {
		return []model.Message{}, nil
	}
	
	// 从数据库加载完整消息
	messages := []model.Message{}
	for _, idStr := range messageIDs {
		id, err := strconv.ParseUint(idStr, 10, 32)
		if err != nil {
			continue
		}
		
		msg, err := s.messageDAO.GetByID(uint(id))
		if err == nil {
			messages = append(messages, *msg)
		}
	}
	
	// 清空离线队列
	s.redisClient.Del(ctx, key)
	
	return messages, nil
}
