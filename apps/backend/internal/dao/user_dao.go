package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type UserDAO struct {
	db *gorm.DB
}

func NewUserDAO() *UserDAO {
	return &UserDAO{
		db: mysql.GetDB(),
	}
}

// Create 创建用户
func (d *UserDAO) Create(user *model.User) error {
	return d.db.Create(user).Error
}

// GetByID 根据ID获取用户
func (d *UserDAO) GetByID(id uint) (*model.User, error) {
	var user model.User
	err := d.db.Where("id = ?", id).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

// GetByUsername 根据用户名获取用户
func (d *UserDAO) GetByUsername(username string) (*model.User, error) {
	var user model.User
	err := d.db.Where("username = ?", username).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

// GetByPhone 根据手机号获取用户
func (d *UserDAO) GetByPhone(phone string) (*model.User, error) {
	var user model.User
	err := d.db.Where("phone = ?", phone).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

// GetByEmail 根据邮箱获取用户
func (d *UserDAO) GetByEmail(email string) (*model.User, error) {
	var user model.User
	err := d.db.Where("email = ?", email).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

// GetByLanxinID 根据蓝信号获取用户
func (d *UserDAO) GetByLanxinID(lanxinID string) (*model.User, error) {
	var user model.User
	err := d.db.Where("lanxin_id = ?", lanxinID).First(&user).Error
	if err != nil {
		return nil, err
	}
	return &user, nil
}

// Update 更新用户信息
func (d *UserDAO) Update(user *model.User) error {
	return d.db.Save(user).Error
}

// Delete 删除用户（软删除）
func (d *UserDAO) Delete(id uint) error {
	return d.db.Delete(&model.User{}, id).Error
}

// List 获取用户列表
func (d *UserDAO) List(page, pageSize int, filters map[string]interface{}) ([]model.User, int64, error) {
	var users []model.User
	var total int64

	query := d.db.Model(&model.User{})

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
	if err := query.Offset(offset).Limit(pageSize).Find(&users).Error; err != nil {
		return nil, 0, err
	}

	return users, total, nil
}

// Search 搜索用户（模糊匹配）
func (d *UserDAO) Search(keyword string, page, pageSize int) ([]model.User, int64, error) {
	var users []model.User
	var total int64

	query := d.db.Model(&model.User{}).Where(
		"username LIKE ? OR phone LIKE ? OR email LIKE ? OR lanxin_id LIKE ?",
		"%"+keyword+"%", "%"+keyword+"%", "%"+keyword+"%", "%"+keyword+"%",
	)

	// 统计总数
	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	// 分页查询
	offset := (page - 1) * pageSize
	if err := query.Offset(offset).Limit(pageSize).Find(&users).Error; err != nil {
		return nil, 0, err
	}

	return users, total, nil
}

// UpdateLastLogin 更新最后登录时间
func (d *UserDAO) UpdateLastLogin(id uint) error {
	return d.db.Model(&model.User{}).Where("id = ?", id).Update("last_login_at", gorm.Expr("NOW()")).Error
}

