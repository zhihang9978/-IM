package service

import (
	"errors"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/websocket"
)

type GroupService struct {
	groupDAO       *dao.GroupDAO
	groupMemberDAO *dao.GroupMemberDAO
	conversationDAO *dao.ConversationDAO
	userDAO        *dao.UserDAO
	messageDAO     *dao.MessageDAO
	logDAO         *dao.OperationLogDAO
	hub            *websocket.Hub
}

func NewGroupService(hub *websocket.Hub) *GroupService {
	return &GroupService{
		groupDAO:        dao.NewGroupDAO(),
		groupMemberDAO:  dao.NewGroupMemberDAO(),
		conversationDAO: dao.NewConversationDAO(),
		userDAO:         dao.NewUserDAO(),
		messageDAO:      dao.NewMessageDAO(),
		logDAO:          dao.NewOperationLogDAO(),
		hub:             hub,
	}
}

// CreateGroup 创建群组
func (s *GroupService) CreateGroup(ownerID uint, name, avatar string, memberIDs []uint, ip, userAgent string) (*model.Group, error) {
	// 验证群名称
	if name == "" {
		return nil, errors.New("group name cannot be empty")
	}

	// 验证成员数量
	if len(memberIDs) == 0 {
		return nil, errors.New("at least one member required")
	}

	// 验证成员是否存在
	for _, memberID := range memberIDs {
		if _, err := s.userDAO.GetByID(memberID); err != nil {
			return nil, errors.New("member not found")
		}
	}

	// 创建群组
	group := &model.Group{
		Name:        name,
		Avatar:      avatar,
		OwnerID:     ownerID,
		Type:        model.GroupTypeNormal,
		MemberCount: len(memberIDs) + 1, // +1 包含群主
		Status:      model.GroupStatusActive,
	}

	if err := s.groupDAO.Create(group); err != nil {
		return nil, err
	}

	// 添加群主为成员
	if err := s.addMemberInternal(group.ID, ownerID, model.GroupRoleOwner); err != nil {
		return nil, err
	}

	// 添加其他成员
	for _, memberID := range memberIDs {
		if err := s.addMemberInternal(group.ID, memberID, model.GroupRoleMember); err != nil {
			// 记录错误但继续
			continue
		}
	}

	// 记录操作日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_create",
		UserID:    &ownerID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":     group.ID,
			"group_name":   name,
			"member_count": len(memberIDs) + 1,
		},
		Result: model.ResultSuccess,
	})

	// 通知所有成员
	allMemberIDs := append(memberIDs, ownerID)
	for _, memberID := range allMemberIDs {
		if s.hub.IsUserOnline(memberID) {
			s.hub.SendToUser(memberID, map[string]interface{}{
				"type": "group_created",
				"data": group,
			})
		}
	}

	return group, nil
}

// GetGroupInfo 获取群组信息
func (s *GroupService) GetGroupInfo(groupID uint) (*model.Group, error) {
	return s.groupDAO.GetByID(groupID)
}

// GetMembers 获取群成员列表
func (s *GroupService) GetMembers(groupID uint) ([]model.GroupMember, error) {
	return s.groupMemberDAO.GetMembers(groupID)
}

// AddMembers 添加群成员
func (s *GroupService) AddMembers(groupID, operatorID uint, memberIDs []uint, ip, userAgent string) error {
	// 验证操作者权限
	role, err := s.groupMemberDAO.GetMemberRole(groupID, operatorID)
	if err != nil {
		return errors.New("not a group member")
	}

	if role != model.GroupRoleOwner && role != model.GroupRoleAdmin {
		return errors.New("no permission to add members")
	}

	// 获取群组信息
	group, err := s.groupDAO.GetByID(groupID)
	if err != nil {
		return err
	}

	// 添加成员
	successCount := 0
	for _, memberID := range memberIDs {
		// 检查是否已是成员
		if s.groupMemberDAO.IsMember(groupID, memberID) {
			continue
		}

		if err := s.addMemberInternal(groupID, memberID, model.GroupRoleMember); err != nil {
			continue
		}

		successCount++

		// 通知新成员
		if s.hub.IsUserOnline(memberID) {
			s.hub.SendToUser(memberID, map[string]interface{}{
				"type": "group_member_added",
				"data": map[string]interface{}{
					"group_id":   groupID,
					"group_name": group.Name,
				},
			})
		}
	}

	// 更新群成员数量
	group.MemberCount += successCount
	s.groupDAO.Update(group)

	// 记录日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_add_member",
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":      groupID,
			"member_ids":    memberIDs,
			"success_count": successCount,
		},
		Result: model.ResultSuccess,
	})

	return nil
}

// RemoveMember 移除群成员
func (s *GroupService) RemoveMember(groupID, operatorID, memberID uint, ip, userAgent string) error {
	// 验证操作者权限
	role, err := s.groupMemberDAO.GetMemberRole(groupID, operatorID)
	if err != nil {
		return errors.New("not a group member")
	}

	if role != model.GroupRoleOwner && role != model.GroupRoleAdmin {
		return errors.New("no permission to remove members")
	}

	// 不能移除群主
	memberRole, _ := s.groupMemberDAO.GetMemberRole(groupID, memberID)
	if memberRole == model.GroupRoleOwner {
		return errors.New("cannot remove group owner")
	}

	// 移除成员
	if err := s.groupMemberDAO.RemoveMember(groupID, memberID); err != nil {
		return err
	}

	// 更新群成员数量
	count, _ := s.groupMemberDAO.GetMemberCount(groupID)
	s.groupDAO.UpdateMemberCount(groupID, int(count))

	// 通知被移除的成员
	if s.hub.IsUserOnline(memberID) {
		s.hub.SendToUser(memberID, map[string]interface{}{
			"type": "group_member_removed",
			"data": map[string]interface{}{
				"group_id": groupID,
			},
		})
	}

	// 记录日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_remove_member",
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":  groupID,
			"member_id": memberID,
		},
		Result: model.ResultSuccess,
	})

	return nil
}

// SendGroupMessage 发送群消息
func (s *GroupService) SendGroupMessage(groupID, senderID uint, content, msgType string, fileURL *string, fileSize *int64, duration *int) (*model.Message, error) {
	// 验证发送者是否是群成员
	if !s.groupMemberDAO.IsMember(groupID, senderID) {
		return nil, errors.New("not a group member")
	}

	// 获取或创建群会话
	conversationID, err := s.conversationDAO.GetOrCreateGroupConversation(groupID)
	if err != nil {
		return nil, errors.New("failed to get or create group conversation")
	}

	// 创建消息
	groupIDPtr := &groupID
	message := &model.Message{
		ConversationID: conversationID,
		SenderID:       senderID,
		GroupID:        groupIDPtr,
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

	// 保存消息到数据库
	if err := s.messageDAO.Create(message); err != nil {
		return nil, err
	}

	// 获取所有群成员
	members, err := s.groupMemberDAO.GetMembers(groupID)
	if err != nil {
		return message, nil
	}

	// 推送给在线成员（除了发送者自己）
	for _, member := range members {
		if member.UserID != senderID && s.hub.IsUserOnline(member.UserID) {
			s.hub.SendMessageNotification(member.UserID, message)
		}
	}

	return message, nil
}

// UpdateGroup 更新群组信息
func (s *GroupService) UpdateGroup(groupID, operatorID uint, name, avatar string, ip, userAgent string) error {
	// 验证操作者权限
	role, err := s.groupMemberDAO.GetMemberRole(groupID, operatorID)
	if err != nil {
		return errors.New("not a group member")
	}

	if role != model.GroupRoleOwner && role != model.GroupRoleAdmin {
		return errors.New("no permission to update group")
	}

	// 获取群组
	group, err := s.groupDAO.GetByID(groupID)
	if err != nil {
		return err
	}

	// 更新字段
	if name != "" {
		group.Name = name
	}
	if avatar != "" {
		group.Avatar = avatar
	}

	// 保存更新
	if err := s.groupDAO.Update(group); err != nil {
		return err
	}

	// 记录日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_update",
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id": groupID,
			"name":     name,
			"avatar":   avatar,
		},
		Result: model.ResultSuccess,
	})

	return nil
}

// DisbandGroup 解散群组
func (s *GroupService) DisbandGroup(groupID, operatorID uint, ip, userAgent string) error {
	// 只有群主可以解散
	role, err := s.groupMemberDAO.GetMemberRole(groupID, operatorID)
	if err != nil || role != model.GroupRoleOwner {
		return errors.New("only owner can disband group")
	}

	// 获取所有成员
	members, _ := s.groupMemberDAO.GetMembers(groupID)

	// 删除群组（软删除）
	if err := s.groupDAO.Delete(groupID); err != nil {
		return err
	}

	// 通知所有成员
	for _, member := range members {
		if s.hub.IsUserOnline(member.UserID) {
			s.hub.SendToUser(member.UserID, map[string]interface{}{
				"type": "group_disbanded",
				"data": map[string]interface{}{
					"group_id": groupID,
				},
			})
		}
	}

	// 记录日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_disband",
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id": groupID,
		},
		Result: model.ResultSuccess,
	})

	return nil
}

// 内部方法: 添加单个成员
func (s *GroupService) addMemberInternal(groupID, userID uint, role string) error {
	member := &model.GroupMember{
		GroupID: groupID,
		UserID:  userID,
		Role:    role,
	}
	return s.groupMemberDAO.Create(member)
}

