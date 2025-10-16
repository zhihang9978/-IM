package config

import (
	"fmt"
	"os"
	"github.com/spf13/viper"
)

type Config struct {
	Server    ServerConfig    `mapstructure:"server"`
	Database  DatabaseConfig  `mapstructure:"database"`
	Redis     RedisConfig     `mapstructure:"redis"`
	Kafka     KafkaConfig     `mapstructure:"kafka"`
	JWT       JWTConfig       `mapstructure:"jwt"`
	Storage   StorageConfig   `mapstructure:"storage"`
	TRTC      TRTCConfig      `mapstructure:"trtc"`
	WebSocket WebSocketConfig `mapstructure:"websocket"`
	Security  SecurityConfig  `mapstructure:"security"`
}

type ServerConfig struct {
	Port   int    `mapstructure:"port"`
	Mode   string `mapstructure:"mode"`
	Domain string `mapstructure:"domain"`
}

type DatabaseConfig struct {
	MySQL MySQLConfig `mapstructure:"mysql"`
}

type MySQLConfig struct {
	Host         string `mapstructure:"host"`
	Port         int    `mapstructure:"port"`
	Username     string `mapstructure:"username"`
	Password     string `mapstructure:"password"`
	Database     string `mapstructure:"database"`
	Charset      string `mapstructure:"charset"`
	MaxIdleConns int    `mapstructure:"max_idle_conns"`
	MaxOpenConns int    `mapstructure:"max_open_conns"`
	LogMode      bool   `mapstructure:"log_mode"`
}

type RedisConfig struct {
	Host     string `mapstructure:"host"`
	Port     int    `mapstructure:"port"`
	Password string `mapstructure:"password"`
	DB       int    `mapstructure:"db"`
	PoolSize int    `mapstructure:"pool_size"`
}

type KafkaConfig struct {
	Brokers []string     `mapstructure:"brokers"`
	Topic   TopicConfig  `mapstructure:"topic"`
}

type TopicConfig struct {
	Message      string `mapstructure:"message"`
	Notification string `mapstructure:"notification"`
}

type JWTConfig struct {
	Secret             string `mapstructure:"secret"`
	ExpireHours        int    `mapstructure:"expire_hours"`
	RefreshExpireHours int    `mapstructure:"refresh_expire_hours"`
}

type StorageConfig struct {
	COS COSConfig `mapstructure:"cos"`
}

type COSConfig struct {
	SecretID  string `mapstructure:"secret_id"`
	SecretKey string `mapstructure:"secret_key"`
	Bucket    string `mapstructure:"bucket"`
	Region    string `mapstructure:"region"`
	BaseURL   string `mapstructure:"base_url"` // 自建COS服务地址
}

type TRTCConfig struct {
	SDKAppID   int    `mapstructure:"sdk_app_id"`
	SecretKey  string `mapstructure:"secret_key"`
	ExpireTime int    `mapstructure:"expire_time"`
}

type WebSocketConfig struct {
	ReadBufferSize    int `mapstructure:"read_buffer_size"`
	WriteBufferSize   int `mapstructure:"write_buffer_size"`
	HeartbeatInterval int `mapstructure:"heartbeat_interval"`
	MaxMessageSize    int `mapstructure:"max_message_size"`
}

type SecurityConfig struct {
	BcryptCost int             `mapstructure:"bcrypt_cost"`
	RateLimit  RateLimitConfig `mapstructure:"rate_limit"`
	CORS       CORSConfig      `mapstructure:"cors"`
}

type RateLimitConfig struct {
	Enabled           bool `mapstructure:"enabled"`
	RequestsPerMinute int  `mapstructure:"requests_per_minute"`
}

type CORSConfig struct {
	AllowedOrigins []string `mapstructure:"allowed_origins"`
	AllowedMethods []string `mapstructure:"allowed_methods"`
	AllowedHeaders []string `mapstructure:"allowed_headers"`
}

// Load reads configuration from file and environment variables
func Load() *Config {
	viper.SetConfigName("config")
	viper.SetConfigType("yaml")
	viper.AddConfigPath("./config")
	viper.AddConfigPath(".")

	// Read from environment variables
	viper.AutomaticEnv()

	if err := viper.ReadInConfig(); err != nil {
		panic(fmt.Sprintf("Failed to read config file: %v", err))
	}

	var config Config
	if err := viper.Unmarshal(&config); err != nil {
		panic(fmt.Sprintf("Failed to unmarshal config: %v", err))
	}

	// Override with environment variables
	if password := os.Getenv("MYSQL_PASSWORD"); password != "" {
		config.Database.MySQL.Password = password
	}
	if password := os.Getenv("REDIS_PASSWORD"); password != "" {
		config.Redis.Password = password
	}
	if secret := os.Getenv("JWT_SECRET"); secret != "" {
		config.JWT.Secret = secret
	}
	// 自建COS配置
	if secretID := os.Getenv("COS_SECRET_ID"); secretID != "" {
		config.Storage.COS.SecretID = secretID
	}
	if secretKey := os.Getenv("COS_SECRET_KEY"); secretKey != "" {
		config.Storage.COS.SecretKey = secretKey
	}
	// 腾讯云TRTC配置
	if secretKey := os.Getenv("TRTC_SECRET_KEY"); secretKey != "" {
		config.TRTC.SecretKey = secretKey
	}

	return &config
}

