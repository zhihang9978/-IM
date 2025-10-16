package service

import (
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"encoding/json"
	"fmt"
	"time"
	"compress/zlib"
	"bytes"
)

// TRTCService TRTC服务（仅数据流接口，不涉及UI）
type TRTCService struct {
	sdkAppID  int
	secretKey string
}

// NewTRTCService 创建TRTC服务
func NewTRTCService(sdkAppID int, secretKey string) *TRTCService {
	return &TRTCService{
		sdkAppID:  sdkAppID,
		secretKey: secretKey,
	}
}

// GenerateUserSig 生成UserSig（用于TRTC认证）
// 注意：此方法仅生成数据凭证，不涉及任何UI组件
func (s *TRTCService) GenerateUserSig(userID string, expire int) (string, error) {
	if expire <= 0 {
		expire = 86400 * 7 // 默认7天
	}

	currTime := time.Now().Unix()
	sig := &userSigDoc{
		TLSVer:    "2.0",
		SDKAppID:  s.sdkAppID,
		Expire:    expire,
		Time:      currTime,
		UserID:    userID,
	}

	// 生成签名
	jsonStr, err := json.Marshal(sig)
	if err != nil {
		return "", err
	}

	// 使用zlib压缩
	var compressed bytes.Buffer
	w := zlib.NewWriter(&compressed)
	w.Write(jsonStr)
	w.Close()

	// base64编码
	base64Str := base64.StdEncoding.EncodeToString(compressed.Bytes())

	// HMAC-SHA256签名
	h := hmac.New(sha256.New, []byte(s.secretKey))
	h.Write([]byte(base64Str))
	signature := base64.StdEncoding.EncodeToString(h.Sum(nil))

	// 最终UserSig
	userSig := fmt.Sprintf("%s.%s", signature, base64Str)
	return userSig, nil
}

// userSigDoc UserSig文档结构
type userSigDoc struct {
	TLSVer   string `json:"TLS.ver"`
	SDKAppID int    `json:"TLS.sdkappid"`
	Expire   int    `json:"TLS.expire"`
	Time     int64  `json:"TLS.time"`
	UserID   string `json:"TLS.userid"`
}

// GenerateRoomID 生成房间ID（用于通话）
func (s *TRTCService) GenerateRoomID(user1ID, user2ID uint) string {
	// 使用两个用户ID生成唯一房间ID
	// 确保无论谁发起通话，房间ID都一致
	var roomID string
	if user1ID < user2ID {
		roomID = fmt.Sprintf("room_%d_%d", user1ID, user2ID)
	} else {
		roomID = fmt.Sprintf("room_%d_%d", user2ID, user1ID)
	}
	return roomID
}

// TRTCCredentials TRTC凭证（纯数据，不包含UI）
type TRTCCredentials struct {
	SDKAppID int    `json:"sdk_app_id"`
	UserSig  string `json:"user_sig"`
	RoomID   string `json:"room_id"`
	UserID   string `json:"user_id"`
	ExpireAt string `json:"expires_at"`
}

// GetCallCredentials 获取通话凭证（纯数据接口）
func (s *TRTCService) GetCallCredentials(userID uint, roomID string, expire int) (*TRTCCredentials, error) {
	userIDStr := fmt.Sprintf("%d", userID)
	
	userSig, err := s.GenerateUserSig(userIDStr, expire)
	if err != nil {
		return nil, err
	}

	return &TRTCCredentials{
		SDKAppID: s.sdkAppID,
		UserSig:  userSig,
		RoomID:   roomID,
		UserID:   userIDStr,
		ExpireAt: time.Now().Add(time.Duration(expire) * time.Second).Format(time.RFC3339),
	}, nil
}

