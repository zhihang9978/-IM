package service

import (
	"errors"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
)

type GroupService struct {
	groupDAO *dao.GroupDAO
	userDAO  *dao.UserDAO
	logDAO   *dao.OperationLogDAO
}

func NewGroupService() *GroupService {
	return &GroupService{
		groupDAO: dao.NewGroupDAO(),
		userDAO:  dao.NewUserDAO(),
		logDAO:   dao.NewOperationLogDAO(),
	}
}

// CreateGroup 创建群组
func (s *GroupService) CreateGroup(ownerID uint, name, description string, memberIDs []uint, ip, userAgent string) (*model.Group, error) {
	// 验证群名称
	if name == "" {
		return nil, errors.New("group name required")
	}

	// 创建群组
	group := &model.Group{
		Name:        name,
		Description: description,
		OwnerID:     ownerID,
		MemberCount: 1 + len(memberIDs), // 群主 + 成员
		MaxMembers:  500,
		Status:      model.GroupStatusActive,
	}

	if err := s.groupDAO.Create(group); err != nil {
		return nil, err
	}

	// 添加群主
	ownerMember := &model.GroupMember{
		GroupID: group.ID,
		UserID:  ownerID,
		Role:    model.GroupMemberRoleOwner,
	}
	if err := s.groupDAO.AddMember(ownerMember); err != nil {
		return nil, err
	}

	// 添加其他成员
	for _, memberID := range memberIDs {
		member := &model.GroupMember{
			GroupID: group.ID,
			UserID:  memberID,
			Role:    model.GroupMemberRoleMember,
		}
		if err := s.groupDAO.AddMember(member); err != nil {
			// 继续添加其他成员
			continue
		}
	}

	// 更新成员数量
	s.groupDAO.UpdateMemberCount(group.ID)

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

	return group, nil
}

// GetGroup 获取群组信息
func (s *GroupService) GetGroup(groupID uint) (*model.Group, error) {
	return s.groupDAO.GetByID(groupID)
}

// GetUserGroups 获取用户的群组列表
func (s *GroupService) GetUserGroups(userID uint) ([]model.Group, error) {
	return s.groupDAO.GetUserGroups(userID)
}

// UpdateGroup 更新群组信息
func (s *GroupService) UpdateGroup(groupID, operatorID uint, updates map[string]interface{}, ip, userAgent string) error {
	// 验证权限（群主或管理员）
	role, err := s.groupDAO.GetMemberRole(groupID, operatorID)
	if err != nil {
		return errors.New("not a group member")
	}
	if role != model.GroupMemberRoleOwner && role != model.GroupMemberRoleAdmin {
		return errors.New("permission denied")
	}

	group, err := s.groupDAO.GetByID(groupID)
	if err != nil {
		return err
	}

	// 应用更新
	if name, ok := updates["name"].(string); ok {
		group.Name = name
	}
	if description, ok := updates["description"].(string); ok {
		group.Description = description
	}
	if avatar, ok := updates["avatar"].(string); ok {
		group.Avatar = avatar
	}

	err = s.groupDAO.Update(group)

	// 记录操作日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_update",
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id": groupID,
			"updates":  updates,
		},
		Result: model.ResultSuccess,
	})

	return err
}

// AddMember 添加群成员
func (s *GroupService) AddMember(groupID, operatorID, newMemberID uint, ip, userAgent string) error {
	// 验证权限
	role, err := s.groupDAO.GetMemberRole(groupID, operatorID)
	if err != nil {
		return errors.New("not a group member")
	}
	if role != model.GroupMemberRoleOwner && role != model.GroupMemberRoleAdmin {
		return errors.New("permission denied")
	}

	// 检查群是否已满
	group, err := s.groupDAO.GetByID(groupID)
	if err != nil {
		return err
	}
	if group.MemberCount >= group.MaxMembers {
		return errors.New("group is full")
	}

	// 检查是否已是成员
	isMember, _ := s.groupDAO.IsMember(groupID, newMemberID)
	if isMember {
		return errors.New("user is already a member")
	}

	// 添加成员
	member := &model.GroupMember{
		GroupID: groupID,
		UserID:  newMemberID,
		Role:    model.GroupMemberRoleMember,
	}
	if err := s.groupDAO.AddMember(member); err != nil {
		return err
	}

	// 更新成员数量
	s.groupDAO.UpdateMemberCount(groupID)

	// 记录操作日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_member_add",
		UserID:    &operatorID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":      groupID,
			"new_member_id": newMemberID,
		},
		Result: model.ResultSuccess,
	})

	return nil
}

// RemoveMember 移除群成员
func (s *GroupService) RemoveMember(groupID, operatorID, memberID uint, ip, userAgent string) error {
	// 验证权限
	role, err := s.groupDAO.GetMemberRole(groupID, operatorID)
	if err != nil {
		return errors.New("not a group member")
	}
	if role != model.GroupMemberRoleOwner && role != model.GroupMemberRoleAdmin {
		return errors.New("permission denied")
	}

	// 不能移除群主
	memberRole, _ := s.groupDAO.GetMemberRole(groupID, memberID)
	if memberRole == model.GroupMemberRoleOwner {
		return errors.New("cannot remove group owner")
	}

	// 移除成员
	if err := s.groupDAO.RemoveMember(groupID, memberID); err != nil {
		return err
	}

	// 更新成员数量
	s.groupDAO.UpdateMemberCount(groupID)

	// 记录操作日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    "group_member_remove",
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

// DisbandGroup 解散群组（管理员操作）
func (s *GroupService) DisbandGroup(groupID, adminID uint, reason, ip, userAgent string) error {
	group, err := s.groupDAO.GetByID(groupID)
	if err != nil {
		return err
	}

	group.Status = model.GroupStatusDisbanded
	err = s.groupDAO.Update(group)

	// 记录管理员操作日志
	s.logDAO.CreateLog(dao.LogRequest{
		Action:    model.ActionAdminGroupDisband,
		AdminID:   &adminID,
		IP:        ip,
		UserAgent: userAgent,
		Details: map[string]interface{}{
			"group_id":   groupID,
			"group_name": group.Name,
			"reason":     reason,
		},
		Result: model.ResultSuccess,
	})

	return err
}

// GetMembers 获取群成员列表
func (s *GroupService) GetMembers(groupID uint) ([]model.GroupMember, error) {
	return s.groupDAO.GetMembers(groupID)
}

