package middleware

import (
	"net/http"
	"sync"
	"time"

	"github.com/gin-gonic/gin"
)

type visitor struct {
	limiter  *rateLimiter
	lastSeen time.Time
}

type rateLimiter struct {
	tokens   int
	maxToken int
	refillRate time.Duration
	mu       sync.Mutex
}

var visitors = make(map[string]*visitor)
var mu sync.Mutex

func newRateLimiter(requestsPerMinute int) *rateLimiter {
	return &rateLimiter{
		tokens:     requestsPerMinute,
		maxToken:   requestsPerMinute,
		refillRate: time.Minute / time.Duration(requestsPerMinute),
	}
}

func (rl *rateLimiter) allow() bool {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	if rl.tokens > 0 {
		rl.tokens--
		return true
	}
	return false
}

func (rl *rateLimiter) refill() {
	ticker := time.NewTicker(rl.refillRate)
	defer ticker.Stop()

	for range ticker.C {
		rl.mu.Lock()
		if rl.tokens < rl.maxToken {
			rl.tokens++
		}
		rl.mu.Unlock()
	}
}

// RateLimit 限流中间件
func RateLimit(requestsPerMinute int) gin.HandlerFunc {
	// 清理过期的访问者
	go cleanupVisitors()

	return func(c *gin.Context) {
		ip := c.ClientIP()

		mu.Lock()
		v, exists := visitors[ip]
		if !exists {
			limiter := newRateLimiter(requestsPerMinute)
			go limiter.refill()
			v = &visitor{limiter: limiter}
			visitors[ip] = v
		}
		v.lastSeen = time.Now()
		mu.Unlock()

		if !v.limiter.allow() {
			c.JSON(http.StatusTooManyRequests, gin.H{
				"code":    429,
				"message": "Rate limit exceeded. Please try again later.",
			})
			c.Abort()
			return
		}

		c.Next()
	}
}

// 清理超过3分钟未访问的记录
func cleanupVisitors() {
	ticker := time.NewTicker(1 * time.Minute)
	defer ticker.Stop()

	for range ticker.C {
		mu.Lock()
		for ip, v := range visitors {
			if time.Since(v.lastSeen) > 3*time.Minute {
				delete(visitors, ip)
			}
		}
		mu.Unlock()
	}
}

