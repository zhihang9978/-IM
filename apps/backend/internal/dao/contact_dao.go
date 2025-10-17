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

