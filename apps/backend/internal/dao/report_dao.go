package dao

import (
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type ReportDAO struct {
	db *gorm.DB
}

func NewReportDAO() *ReportDAO {
	return &ReportDAO{
		db: mysql.GetDB(),
	}
}

// Create 创建举报记录
func (d *ReportDAO) Create(report *model.Report) error {
	return d.db.Create(report).Error
}

// GetByID 根据ID获取举报记录
func (d *ReportDAO) GetByID(id uint) (*model.Report, error) {
	var report model.Report
	err := d.db.Preload("Reporter").Preload("Message").
		Where("id = ?", id).
		First(&report).Error
	return &report, err
}

// GetUserReports 获取用户的举报记录
func (d *ReportDAO) GetUserReports(userID uint, page, pageSize int) ([]model.Report, int64, error) {
	var reports []model.Report
	var total int64
	
	offset := (page - 1) * pageSize
	
	d.db.Model(&model.Report{}).Where("reporter_id = ?", userID).Count(&total)
	
	err := d.db.Where("reporter_id = ?", userID).
		Preload("Message").
		Order("created_at DESC").
		Offset(offset).
		Limit(pageSize).
		Find(&reports).Error
		
	return reports, total, err
}

// GetAllReports 获取所有举报记录（管理员用）
func (d *ReportDAO) GetAllReports(page, pageSize int, status string) ([]model.Report, int64, error) {
	var reports []model.Report
	var total int64
	
	offset := (page - 1) * pageSize
	query := d.db.Model(&model.Report{})
	
	if status != "" {
		query = query.Where("status = ?", status)
	}
	
	query.Count(&total)
	
	err := query.Preload("Reporter").Preload("Message").
		Order("created_at DESC").
		Offset(offset).
		Limit(pageSize).
		Find(&reports).Error
		
	return reports, total, err
}

// UpdateStatus 更新举报状态（管理员操作）
func (d *ReportDAO) UpdateStatus(id uint, status, adminNote string) error {
	return d.db.Model(&model.Report{}).
		Where("id = ?", id).
		Updates(map[string]interface{}{
			"status":     status,
			"admin_note": adminNote,
		}).Error
}

// CheckExists 检查是否已举报过该消息
func (d *ReportDAO) CheckExists(userID, messageID uint) bool {
	var count int64
	d.db.Model(&model.Report{}).
		Where("reporter_id = ? AND message_id = ?", userID, messageID).
		Count(&count)
	return count > 0
}

