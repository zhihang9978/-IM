package dao

import (
	"encoding/json"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/mysql"
	"gorm.io/gorm"
)

type OperationLogDAO struct {
	db *gorm.DB
}

func NewOperationLogDAO() *OperationLogDAO {
	return &OperationLogDAO{
		db: mysql.GetDB(),
	}
}

// Create 创建操作日志
func (d *OperationLogDAO) Create(log *model.OperationLog) error {
	return d.db.Create(log).Error
}

// LogRequest 日志请求参数
type LogRequest struct {
	Action       string
	UserID       *uint
	AdminID      *uint
	IP           string
	UserAgent    string
	Details      interface{}
	Result       string
	ErrorMessage string
}

// CreateLog 创建操作日志（便捷方法）
func (d *OperationLogDAO) CreateLog(req LogRequest) error {
	// 将Details序列化为JSON
	var detailsJSON string
	if req.Details != nil {
		data, err := json.Marshal(req.Details)
		if err != nil {
			detailsJSON = "{}"
		} else {
			detailsJSON = string(data)
		}
	}

	log := &model.OperationLog{
		Action:       req.Action,
		UserID:       req.UserID,
		AdminID:      req.AdminID,
		IP:           req.IP,
		UserAgent:    req.UserAgent,
		Details:      detailsJSON,
		Result:       req.Result,
		ErrorMessage: req.ErrorMessage,
	}

	return d.Create(log)
}

// List 获取操作日志列表
func (d *OperationLogDAO) List(page, pageSize int, filters map[string]interface{}) ([]model.OperationLog, int64, error) {
	var logs []model.OperationLog
	var total int64

	query := d.db.Model(&model.OperationLog{})

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
	if err := query.Order("created_at DESC").Offset(offset).Limit(pageSize).Find(&logs).Error; err != nil {
		return nil, 0, err
	}

	return logs, total, nil
}

// GetByAction 获取特定操作类型的日志
func (d *OperationLogDAO) GetByAction(action string, page, pageSize int) ([]model.OperationLog, int64, error) {
	var logs []model.OperationLog
	var total int64

	query := d.db.Model(&model.OperationLog{}).Where("action = ?", action)

	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	offset := (page - 1) * pageSize
	if err := query.Order("created_at DESC").Offset(offset).Limit(pageSize).Find(&logs).Error; err != nil {
		return nil, 0, err
	}

	return logs, total, nil
}

// GetByUserID 获取特定用户的操作日志
func (d *OperationLogDAO) GetByUserID(userID uint, page, pageSize int) ([]model.OperationLog, int64, error) {
	var logs []model.OperationLog
	var total int64

	query := d.db.Model(&model.OperationLog{}).Where("user_id = ?", userID)

	if err := query.Count(&total).Error; err != nil {
		return nil, 0, err
	}

	offset := (page - 1) * pageSize
	if err := query.Order("created_at DESC").Offset(offset).Limit(pageSize).Find(&logs).Error; err != nil {
		return nil, 0, err
	}

	return logs, total, nil
}

// DeleteOldLogs 删除过期日志（例如90天前的日志）
func (d *OperationLogDAO) DeleteOldLogs(days int) error {
	cutoffDate := time.Now().AddDate(0, 0, -days)
	return d.db.Where("created_at < ?", cutoffDate).Delete(&model.OperationLog{}).Error
}

