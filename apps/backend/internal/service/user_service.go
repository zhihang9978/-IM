package service

import (
	"errors"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
)

type UserService struct {
	userDAO *dao.UserDAO
	logDAO  *dao.OperationLogDAO
}

func NewUserService() *UserService {
	return &UserService{
		userDAO: dao.NewUserDAO(),
		logDAO:  dao.NewOperationLogDAO(),
	}
}

// GetUserByID 根据ID获取用户
func (s *UserService) GetUserByID(id uint) (*model.User, error) {
	return s.userDAO.GetByID(id)
}

// UpdateUser 更新用户信息
func (s *UserService) UpdateUser(userID uint, updates map[string]interface{}, ip, userAgent string) error {
	user, err := s.userDAO.GetByID(userID)
	if err != nil {
		return err
	}

	// 记录变更前的值
	oldValues := make(map[string]interface{})
	if username, ok := updates["username"]; ok {
		oldValues["username"] = user.Username
		user.Username = username.(string)
	}
	if avatar, ok := updates["avatar"]; ok {
		oldValues["avatar"] = user.Avatar
		user.Avatar = avatar.(string)
	}
	if phone, ok := updates["phone"]; ok {
		oldValues["phone"] = user.Phone
		phoneStr := phone.(string)
		if phoneStr == "" {
			user.Phone = nil
		} else {
			user.Phone = &phoneStr
		}
	}
	if email, ok := updates["email"]; ok {
		oldValues["email"] = user.Email
		emailStr := email.(string)
		if emailStr == "" {
			user.Email = nil
		} else {
			user.Email = &emailStr
		}
	}

	err = s.userDAO.Update(user)

	// 记录操作日志
	details := map[string]interface{}{
		"user_id": userID,
		"changes": map[string]interface{}{
			"old": oldValues,
			"new": updates,
		},
	}

	result := model.ResultSuccess
	errorMsg := ""
	if err != nil {
		result = model.ResultFailure
		errorMsg = err.Error()
	}

	s.logDAO.CreateLog(dao.LogRequest{
		Action:       model.ActionUserProfileUpdate,
		UserID:       &userID,
		IP:           ip,
		UserAgent:    userAgent,
		Details:      details,
		Result:       result,
		ErrorMessage: errorMsg,
	})

	return err
}

// SearchUsers 搜索用户
func (s *UserService) SearchUsers(keyword string, page, pageSize int) ([]model.User, int64, error) {
	return s.userDAO.Search(keyword, page, pageSize)
}

// ListUsers 获取用户列表
func (s *UserService) ListUsers(page, pageSize int, filters map[string]interface{}) ([]model.User, int64, error) {
	return s.userDAO.List(page, pageSize, filters)
}

// BanUser 封禁用户（管理员操作）
func (s *UserService) BanUser(adminID, targetUserID uint, reason, ip, userAgent string) error {
	user, err := s.userDAO.GetByID(targetUserID)
	if err != nil {
		return err
	}

	if user.Status == "banned" {
		return errors.New("user already banned")
	}

	oldStatus := user.Status
	user.Status = "banned"
	err = s.userDAO.Update(user)

	// 记录管理员操作日志
	details := map[string]interface{}{
		"target_user_id":   targetUserID,
		"target_username":  user.Username,
		"reason":           reason,
		"status_change":    map[string]string{"from": oldStatus, "to": "banned"},
	}

	result := model.ResultSuccess
	errorMsg := ""
	if err != nil {
		result = model.ResultFailure
		errorMsg = err.Error()
	}

	s.logDAO.CreateLog(dao.LogRequest{
		Action:       model.ActionAdminUserBan,
		AdminID:      &adminID,
		IP:           ip,
		UserAgent:    userAgent,
		Details:      details,
		Result:       result,
		ErrorMessage: errorMsg,
	})

	return err
}

