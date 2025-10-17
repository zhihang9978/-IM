package redis

import (
	"time"
)

// AddTokenToBlacklist 将Token加入黑名单
// 参数：token - JWT Token字符串
//      expireTime - Token过期时间（秒）
// 用途：用户登出时，将Token加入黑名单，使其立即失效
func AddTokenToBlacklist(token string, expireTime int) error {
	key := "token:blacklist:" + token
	// 设置过期时间为Token的剩余有效期
	return Client.Set(ctx, key, "1", time.Duration(expireTime)*time.Second).Err()
}

// IsTokenBlacklisted 检查Token是否在黑名单中
// 参数：token - JWT Token字符串
// 返回：bool - true表示已加入黑名单
func IsTokenBlacklisted(token string) bool {
	key := "token:blacklist:" + token
	result, err := Client.Get(ctx, key).Result()
	if err != nil {
		return false // Redis错误时默认不阻止（容错）
	}
	return result == "1"
}

// ClearExpiredTokens Redis自动清理过期key，无需手动清理

