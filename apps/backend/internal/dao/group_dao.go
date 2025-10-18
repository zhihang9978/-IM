package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

// ========== GroupDAO ==========

type GroupDAO struct {
	db *gorm.DB
}

func NewGroupDAO() *GroupDAO {
	return &GroupDAO{
		db: mysql.GetDB(),
	}
}

// Create 创建群组
func (d *GroupDAO) Create(group *model.Group) error {
	return d.db.Create(group).Error
}

// GetByID 根据ID获取群组（含完整信息）
func (d *GroupDAO) GetByID(id uint) (*model.Group, error) {
	var group model.Group
	err := d.db.
		Preload("Owner").
		Preload("Members").
		Preload("Members.User").
		Where("id = ?", id).
		First(&group).Error
	return &group, err
}

// Update 更新群组信息
func (d *GroupDAO) Update(group *model.Group) error {
	return d.db.Save(group).Error
}

// Delete 删除群组（软删除）
func (d *GroupDAO) Delete(id uint) error {
	return d.db.Delete(&model.Group{}, id).Error
}

// GetByIDs 根据ID列表获取群组（带分页）
func (d *GroupDAO) GetByIDs(groupIDs []uint, page, pageSize int) ([]model.Group, int64, error) {
	var groups []model.Group
	var total int64

	query := d.db.Where("id IN (?) AND status = ?", groupIDs, model.GroupStatusActive).
		Preload("Owner")

	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	offset := (page - 1) * pageSize
	err := query.Offset(offset).Limit(pageSize).Find(&groups).Error

	return groups, total, err
}

// GetUserGroups 获取用户加入的所有群组
func (d *GroupDAO) GetUserGroups(userID uint) ([]model.Group, error) {
	var groups []model.Group
	err := d.db.
		Joins("JOIN group_members ON groups.id = group_members.group_id").
		Where("group_members.user_id = ? AND groups.status = ?", userID, model.GroupStatusActive).
		Preload("Owner").
		Find(&groups).Error
	return groups, err
}

// UpdateMemberCount 更新群成员数量
func (d *GroupDAO) UpdateMemberCount(groupID uint, count int) error {
	return d.db.Model(&model.Group{}).
		Where("id = ?", groupID).
		Update("member_count", count).Error
}

// ========== GroupMemberDAO ==========

type GroupMemberDAO struct {
	db *gorm.DB
}

func NewGroupMemberDAO() *GroupMemberDAO {
	return &GroupMemberDAO{
		db: mysql.GetDB(),
	}
}

// Create 添加群成员
func (d *GroupMemberDAO) Create(member *model.GroupMember) error {
	return d.db.Create(member).Error
}

// GetMembers 获取群组所有成员
func (d *GroupMemberDAO) GetMembers(groupID uint) ([]model.GroupMember, error) {
	var members []model.GroupMember
	err := d.db.
		Preload("User").
		Where("group_id = ?", groupID).
		Find(&members).Error
	return members, err
}

// IsMember 检查用户是否是群成员
func (d *GroupMemberDAO) IsMember(groupID, userID uint) bool {
	var count int64
	d.db.Model(&model.GroupMember{}).
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Count(&count)
	return count > 0
}

// GetMemberRole 获取成员角色
func (d *GroupMemberDAO) GetMemberRole(groupID, userID uint) (string, error) {
	var member model.GroupMember
	err := d.db.
		Where("group_id = ? AND user_id = ?", groupID, userID).
		First(&member).Error
	return member.Role, err
}

// RemoveMember 移除群成员
func (d *GroupMemberDAO) RemoveMember(groupID, userID uint) error {
	return d.db.
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Delete(&model.GroupMember{}).Error
}

// GetMemberCount 获取群成员数量
func (d *GroupMemberDAO) GetMemberCount(groupID uint) (int64, error) {
	var count int64
	err := d.db.Model(&model.GroupMember{}).
		Where("group_id = ?", groupID).
		Count(&count).Error
	return count, err
}

// GetUserGroups 获取用户加入的所有群组成员关系
func (d *GroupMemberDAO) GetUserGroups(userID uint) ([]model.GroupMember, error) {
	var members []model.GroupMember
	err := d.db.Where("user_id = ?", userID).Find(&members).Error
	return members, err
}

