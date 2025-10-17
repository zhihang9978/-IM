package service

import (
	"errors"
	"fmt"
	"time"

	"github.com/lanxin/im-backend/config"
	"github.com/lanxin/im-backend/internal/dao"
	"github.com/lanxin/im-backend/internal/model"
	"github.com/lanxin/im-backend/internal/pkg/jwt"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

type AuthService struct {
	userDAO *dao.UserDAO
	cfg     *config.Config
}

func NewAuthService(cfg *config.Config) *AuthService {
	return &AuthService{
		userDAO: dao.NewUserDAO(),
		cfg:     cfg,
	}
}

// Register 用户注册
func (s *AuthService) Register(username, password, phone, email string) (*model.User, error) {
	// 检查用户名是否已存在
	if _, err := s.userDAO.GetByUsername(username); err == nil {
		return nil, errors.New("username already exists")
	} else if !errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, err
	}

	// 检查手机号是否已存在
	if phone != "" {
		if _, err := s.userDAO.GetByPhone(phone); err == nil {
			return nil, errors.New("phone already exists")
		} else if !errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, err
		}
	}

	// 检查邮箱是否已存在
	if email != "" {
		if _, err := s.userDAO.GetByEmail(email); err == nil {
			return nil, errors.New("email already exists")
		} else if !errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, err
		}
	}

	// 加密密码
	hashedPassword, err := bcrypt.GenerateFromPassword([]byte(password), s.cfg.Security.BcryptCost)
	if err != nil {
		return nil, err
	}

	// 生成蓝信号（使用时间戳+随机数）
	lanxinID := generateLanxinID()

	var phonePtr, emailPtr *string
	if phone != "" {
		phonePtr = &phone
	}
	if email != "" {
		emailPtr = &email
	}

	user := &model.User{
		Username: username,
		Password: string(hashedPassword),
		Phone:    phonePtr,
		Email:    emailPtr,
		LanxinID: lanxinID,
		Role:     "user",
		Status:   "active",
	}

	if err := s.userDAO.Create(user); err != nil {
		return nil, err
	}

	return user, nil
}

// Login 用户登录
func (s *AuthService) Login(identifier, password string) (string, *model.User, error) {
	var user *model.User
	var err error

	// 支持用户名/手机号/邮箱/蓝信号登录
	if user, err = s.userDAO.GetByUsername(identifier); err != nil {
		if user, err = s.userDAO.GetByPhone(identifier); err != nil {
			if user, err = s.userDAO.GetByEmail(identifier); err != nil {
				if user, err = s.userDAO.GetByLanxinID(identifier); err != nil {
					return "", nil, errors.New("user not found")
				}
			}
		}
	}

	// 检查账号状态
	if user.Status == "banned" {
		return "", nil, errors.New("account is banned")
	}
	if user.Status == "deleted" {
		return "", nil, errors.New("account is deleted")
	}

	// 验证密码
	if err := bcrypt.CompareHashAndPassword([]byte(user.Password), []byte(password)); err != nil {
		return "", nil, errors.New("incorrect password")
	}

	// 更新最后登录时间
	if err := s.userDAO.UpdateLastLogin(user.ID); err != nil {
		// 记录错误但不影响登录流程
	}

	// 生成JWT Token
	token, err := jwt.GenerateToken(user.ID, user.Username, user.Role, s.cfg.JWT.Secret, s.cfg.JWT.ExpireHours)
	if err != nil {
		return "", nil, err
	}

	return token, user, nil
}

// RefreshToken 刷新令牌
func (s *AuthService) RefreshToken(oldToken string) (string, error) {
	return jwt.RefreshToken(oldToken, s.cfg.JWT.Secret, s.cfg.JWT.ExpireHours)
}

// generateLanxinID 生成蓝信号
func generateLanxinID() string {
	// 使用时间戳作为基础
	timestamp := time.Now().Unix()
	return "LX" + fmt.Sprintf("%d", timestamp)
}

