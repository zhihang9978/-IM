package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

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

// GetByID 根据ID获取群组
func (d *GroupDAO) GetByID(id uint) (*model.Group, error) {
	var group model.Group
	err := d.db.Preload("Owner").Preload("Members").Where("id = ?", id).First(&group).Error
	if err != nil {
		return nil, err
	}
	return &group, nil
}

// Update 更新群组信息
func (d *GroupDAO) Update(group *model.Group) error {
	return d.db.Save(group).Error
}

// Delete 删除群组（软删除）
func (d *GroupDAO) Delete(id uint) error {
	return d.db.Delete(&model.Group{}, id).Error
}

// List 获取群组列表
func (d *GroupDAO) List(page, pageSize int, filters map[string]interface{}) ([]model.Group, int64, error) {
	var groups []model.Group
	var total int64

	query := d.db.Model(&model.Group{}).Preload("Owner")

	// 应用过滤条件
	for key, value := range filters {
		query = query.Where(key+" = ?", value)
	}

	// 统计总数
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	// 分页查询
	offset := (page - 1) * pageSize
	if err := query.Offset(offset).Limit(pageSize).Find(&groups).Error; err != nil {
		return nil, 0, err
	}

	return groups, total, nil
}

// GetUserGroups 获取用户加入的所有群组
func (d *GroupDAO) GetUserGroups(userID uint) ([]model.Group, error) {
	var groups []model.Group
	err := d.db.Table("groups").
		Joins("INNER JOIN group_members ON groups.id = group_members.group_id").
		Where("group_members.user_id = ?", userID).
		Preload("Owner").
		Find(&groups).Error
	return groups, err
}

// AddMember 添加群成员
func (d *GroupDAO) AddMember(member *model.GroupMember) error {
	return d.db.Create(member).Error
}

// RemoveMember 移除群成员
func (d *GroupDAO) RemoveMember(groupID, userID uint) error {
	return d.db.Where("group_id = ? AND user_id = ?", groupID, userID).Delete(&model.GroupMember{}).Error
}

// GetMembers 获取群成员列表
func (d *GroupDAO) GetMembers(groupID uint) ([]model.GroupMember, error) {
	var members []model.GroupMember
	err := d.db.Where("group_id = ?", groupID).Preload("User").Find(&members).Error
	return members, err
}

// UpdateMemberCount 更新群成员数量
func (d *GroupDAO) UpdateMemberCount(groupID uint) error {
	var count int64
	if err := d.db.Model(&model.GroupMember{}).Where("group_id = ?", groupID).Count(&count).Error; err != nil {
		return err
	}
	return d.db.Model(&model.Group{}).Where("id = ?", groupID).Update("member_count", count).Error
}

// IsMember 检查用户是否是群成员
func (d *GroupDAO) IsMember(groupID, userID uint) (bool, error) {
	var count int64
	err := d.db.Model(&model.GroupMember{}).
		Where("group_id = ? AND user_id = ?", groupID, userID).
		Count(&count).Error
	return count > 0, err
}

// GetMemberRole 获取用户在群中的角色
func (d *GroupDAO) GetMemberRole(groupID, userID uint) (string, error) {
	var member model.GroupMember
	err := d.db.Where("group_id = ? AND user_id = ?", groupID, userID).First(&member).Error
	if err != nil {
		return "", err
	}
	return member.Role, nil
}

