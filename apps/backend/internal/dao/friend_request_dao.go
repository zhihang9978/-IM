package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type FriendRequestDAO struct {
	db *gorm.DB
}

func NewFriendRequestDAO() *FriendRequestDAO {
	return &FriendRequestDAO{
		db: mysql.GetDB(),
	}
}

func (d *FriendRequestDAO) Create(req *model.FriendRequest) error {
	return d.db.Create(req).Error
}

func (d *FriendRequestDAO) GetByID(id uint) (*model.FriendRequest, error) {
	var req model.FriendRequest
	err := d.db.
		Preload("Sender").
		Preload("Receiver").
		First(&req, id).Error
	return &req, err
}

func (d *FriendRequestDAO) GetReceivedRequests(userID uint, page, pageSize int) ([]model.FriendRequest, int64, error) {
	var requests []model.FriendRequest
	var total int64

	query := d.db.Where("receiver_id = ?", userID).
		Preload("Sender").
		Preload("Receiver")

	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	offset := (page - 1) * pageSize
	err := query.Order("created_at DESC").
		Offset(offset).
		Limit(pageSize).
		Find(&requests).Error

	return requests, total, err
}

func (d *FriendRequestDAO) GetSentRequests(userID uint, page, pageSize int) ([]model.FriendRequest, int64, error) {
	var requests []model.FriendRequest
	var total int64

	query := d.db.Where("sender_id = ?", userID).
		Preload("Sender").
		Preload("Receiver")

	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	offset := (page - 1) * pageSize
	err := query.Order("created_at DESC").
		Offset(offset).
		Limit(pageSize).
		Find(&requests).Error

	return requests, total, err
}

func (d *FriendRequestDAO) UpdateStatus(id uint, status string) error {
	return d.db.Model(&model.FriendRequest{}).
		Where("id = ?", id).
		Update("status", status).Error
}

func (d *FriendRequestDAO) CheckExists(senderID, receiverID uint) bool {
	var count int64
	d.db.Model(&model.FriendRequest{}).
		Where("sender_id = ? AND receiver_id = ? AND status = ?", 
			senderID, receiverID, model.FriendRequestStatusPending).
		Count(&count)
	return count > 0
}

func (d *FriendRequestDAO) Delete(id uint) error {
	return d.db.Delete(&model.FriendRequest{}, id).Error
}
