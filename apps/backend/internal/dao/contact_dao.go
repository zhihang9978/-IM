package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type ContactDAO struct {
	db *gorm.DB
}

func NewContactDAO() *ContactDAO {
	return &ContactDAO{
		db: mysql.GetDB(),
	}
}

// GetUserContacts 获取用户的所有联系人
func (d *ContactDAO) GetUserContacts(userID uint) ([]model.Contact, error) {
	var contacts []model.Contact
	err := d.db.Where("user_id = ?", userID).
		Preload("ContactUser").
		Find(&contacts).Error
	return contacts, err
}

// Create 添加联系人
func (d *ContactDAO) Create(contact *model.Contact) error {
	return d.db.Create(contact).Error
}

// Delete 删除联系人（物理删除）
// 参数：contactID - 联系人记录ID
//      userID - 当前用户ID（权限验证）
// 返回：error
func (d *ContactDAO) Delete(contactID, userID uint) error {
	// 验证权限：只能删除自己的联系人
	return d.db.Where("id = ? AND user_id = ?", contactID, userID).
		Delete(&model.Contact{}).Error
}

// UpdateRemark 更新联系人备注和标签
// 参数：contactID - 联系人记录ID
//      userID - 当前用户ID（权限验证）
//      remark - 新备注
//      tags - 新标签（逗号分隔）
// 返回：error
func (d *ContactDAO) UpdateRemark(contactID, userID uint, remark, tags string) error {
	// 验证权限：只能修改自己的联系人
	return d.db.Model(&model.Contact{}).
		Where("id = ? AND user_id = ?", contactID, userID).
		Updates(map[string]interface{}{
			"remark": remark,
			"tags":   tags,
		}).Error
}

// GetByID 根据ID获取联系人（含权限验证）
// 参数：contactID - 联系人记录ID
//      userID - 当前用户ID（权限验证）
// 返回：联系人对象（含ContactUser关联）
func (d *ContactDAO) GetByID(contactID, userID uint) (*model.Contact, error) {
	var contact model.Contact
	err := d.db.Where("id = ? AND user_id = ?", contactID, userID).
		Preload("ContactUser").
		First(&contact).Error
	return &contact, err
}

// CheckExists 检查联系人是否已存在
// 参数：userID - 当前用户ID
//      contactID - 要添加的联系人用户ID
// 返回：bool - true表示已存在
func (d *ContactDAO) CheckExists(userID, contactID uint) bool {
	var count int64
	d.db.Model(&model.Contact{}).
		Where("user_id = ? AND contact_id = ?", userID, contactID).
		Count(&count)
	return count > 0
}

