package redis

import (
	"encoding/json"
	"time"
)

// CacheUser 缓存用户信息
// 参数：userID - 用户ID
//      userData - 用户数据（任意结构）
//      expireTime - 缓存过期时间（秒，默认1小时）
func CacheUser(userID uint, userData interface{}, expireTime int) error {
	key := getUserCacheKey(userID)
	
	data, err := json.Marshal(userData)
	if err != nil {
		return err
	}
	
	if expireTime == 0 {
		expireTime = 3600 // 默认1小时
	}
	
	return Client.Set(ctx, key, data, time.Duration(expireTime)*time.Second).Err()
}

// GetCachedUser 获取缓存的用户信息
// 参数：userID - 用户ID
//      result - 接收结果的指针
// 返回：bool - 是否找到缓存
func GetCachedUser(userID uint, result interface{}) (bool, error) {
	key := getUserCacheKey(userID)
	
	data, err := Client.Get(ctx, key).Result()
	if err != nil {
		return false, err
	}
	
	if err := json.Unmarshal([]byte(data), result); err != nil {
		return false, err
	}
	
	return true, nil
}

// InvalidateUserCache 使用户缓存失效
func InvalidateUserCache(userID uint) error {
	key := getUserCacheKey(userID)
	return Client.Del(ctx, key).Err()
}

// getUserCacheKey 生成用户缓存key
func getUserCacheKey(userID uint) string {
	return "user:info:" + string(rune(userID))
}

// CacheConversation 缓存会话信息
func CacheConversation(conversationID uint, data interface{}, expireTime int) error {
	key := "conversation:info:" + string(rune(conversationID))
	
	jsonData, err := json.Marshal(data)
	if err != nil {
		return err
	}
	
	if expireTime == 0 {
		expireTime = 600 // 默认10分钟
	}
	
	return Client.Set(ctx, key, jsonData, time.Duration(expireTime)*time.Second).Err()
}

// InvalidateConversationCache 使会话缓存失效
func InvalidateConversationCache(conversationID uint) error {
	key := "conversation:info:" + string(rune(conversationID))
	return Client.Del(ctx, key).Err()
}

