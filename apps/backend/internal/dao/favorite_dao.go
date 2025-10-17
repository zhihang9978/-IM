package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type FavoriteDAO struct {
	db *gorm.DB
}

func NewFavoriteDAO() *FavoriteDAO {
	return &FavoriteDAO{
		db: mysql.GetDB(),
	}
}

// Create 添加收藏
func (d *FavoriteDAO) Create(favorite *model.Favorite) error {
	return d.db.Create(favorite).Error
}

// GetUserFavorites 获取用户的收藏列表（分页）
func (d *FavoriteDAO) GetUserFavorites(userID uint, page, pageSize int) ([]model.Favorite, int64, error) {
	var favorites []model.Favorite
	var total int64
	
	offset := (page - 1) * pageSize
	
	// 统计总数
	d.db.Model(&model.Favorite{}).Where("user_id = ?", userID).Count(&total)
	
	// 分页查询
	err := d.db.Where("user_id = ?", userID).
		Preload("Message").
		Order("created_at DESC").
		Offset(offset).
		Limit(pageSize).
		Find(&favorites).Error
		
	return favorites, total, err
}

// Delete 删除收藏
func (d *FavoriteDAO) Delete(favoriteID, userID uint) error {
	return d.db.Where("id = ? AND user_id = ?", favoriteID, userID).
		Delete(&model.Favorite{}).Error
}

// CheckExists 检查消息是否已收藏
func (d *FavoriteDAO) CheckExists(userID, messageID uint) bool {
	var count int64
	d.db.Model(&model.Favorite{}).
		Where("user_id = ? AND message_id = ?", userID, messageID).
		Count(&count)
	return count > 0
}

// GetByID 根据ID获取收藏（含权限验证）
func (d *FavoriteDAO) GetByID(favoriteID, userID uint) (*model.Favorite, error) {
	var favorite model.Favorite
	err := d.db.Where("id = ? AND user_id = ?", favoriteID, userID).
		Preload("Message").
		First(&favorite).Error
	return &favorite, err
}

