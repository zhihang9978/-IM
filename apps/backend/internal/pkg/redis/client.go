package redis

import (
	"context"
	"fmt"
	"log"

	"github.com/go-redis/redis/v8"
	"github.com/lanxin/im-backend/config"
)

var Client *redis.Client
var ctx = context.Background()

// Init initializes Redis connection
func Init(cfg config.RedisConfig) {
	Client = redis.NewClient(&redis.Options{
		Addr:     fmt.Sprintf("%s:%d", cfg.Host, cfg.Port),
		Password: cfg.Password,
		DB:       cfg.DB,
		PoolSize: cfg.PoolSize,
	})

	// Test connection
	if err := Client.Ping(ctx).Err(); err != nil {
		log.Fatalf("Failed to connect to Redis: %v", err)
	}

	log.Println("Redis connected successfully")
}

// Close closes the Redis connection
func Close() {
	if Client != nil {
		if err := Client.Close(); err != nil {
			log.Printf("Failed to close Redis connection: %v", err)
		}
	}
}

// GetClient returns the Redis client
func GetClient() *redis.Client {
	return Client
}

// GetContext returns the context
func GetContext() context.Context {
	return ctx
}

