package cos

import (
	"context"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"path"
	"time"

	"github.com/google/uuid"
	"github.com/tencentyun/cos-go-sdk-v5"
)

// Client 腾讯云COS客户端
type Client struct {
	client *cos.Client
	bucket string
	region string
}

// Config COS配置
type Config struct {
	SecretID  string
	SecretKey string
	Bucket    string
	Region    string
	BaseURL   string
}

// NewClient 创建COS客户端
func NewClient(cfg Config) (*Client, error) {
	bucketURL, _ := url.Parse(fmt.Sprintf("https://%s.cos.%s.myqcloud.com", cfg.Bucket, cfg.Region))
	baseURL, _ := url.Parse(fmt.Sprintf("https://cos.%s.myqcloud.com", cfg.Region))

	cosClient := cos.NewClient(&cos.BaseURL{
		BucketURL: bucketURL,
		ServiceURL: baseURL,
	}, &http.Client{
		Transport: &cos.AuthorizationTransport{
			SecretID:  cfg.SecretID,
			SecretKey: cfg.SecretKey,
		},
	})

	return &Client{
		client: cosClient,
		bucket: cfg.Bucket,
		region: cfg.Region,
	}, nil
}

// UploadFile 上传文件
func (c *Client) UploadFile(ctx context.Context, fileReader io.Reader, fileName, contentType string) (string, error) {
	// 生成唯一文件名
	ext := path.Ext(fileName)
	uniqueFileName := fmt.Sprintf("%s%s", uuid.New().String(), ext)

	// 按日期分类存储
	date := time.Now().Format("2006/01/02")
	objectKey := fmt.Sprintf("uploads/%s/%s", date, uniqueFileName)

	// 上传文件
	_, err := c.client.Object.Put(ctx, objectKey, fileReader, &cos.ObjectPutOptions{
		ObjectPutHeaderOptions: &cos.ObjectPutHeaderOptions{
			ContentType: contentType,
		},
	})
	if err != nil {
		return "", err
	}

	// 返回文件URL
	fileURL := fmt.Sprintf("https://%s.cos.%s.myqcloud.com/%s", c.bucket, c.region, objectKey)
	return fileURL, nil
}

// DeleteFile 删除文件
func (c *Client) DeleteFile(ctx context.Context, objectKey string) error {
	_, err := c.client.Object.Delete(ctx, objectKey, nil)
	return err
}

// GetPresignedURL 获取预签名URL（用于临时访问）
func (c *Client) GetPresignedURL(ctx context.Context, objectKey string, expire time.Duration) (string, error) {
	presignedURL, err := c.client.Object.GetPresignedURL(ctx, http.MethodGet, objectKey, c.client.Credentials, expire, nil)
	if err != nil {
		return "", err
	}
	return presignedURL.String(), nil
}

// GenerateUploadToken 生成上传凭证（给客户端直传使用）
func (c *Client) GenerateUploadToken(fileName, fileType string) (*UploadTokenResponse, error) {
	// 生成唯一KEY
	ext := path.Ext(fileName)
	date := time.Now().Format("2006/01/02")
	uniqueKey := fmt.Sprintf("uploads/%s/%s%s", date, uuid.New().String(), ext)

	// 生成预签名URL用于上传
	ctx := context.Background()
	presignedURL, err := c.client.Object.GetPresignedURL(
		ctx,
		http.MethodPut,
		uniqueKey,
		c.client.Credentials,
		30*time.Minute,
		nil,
	)
	if err != nil {
		return nil, err
	}

	return &UploadTokenResponse{
		Token:     presignedURL.String(),
		Bucket:    c.bucket,
		Region:    c.region,
		Key:       uniqueKey,
		ExpiresAt: time.Now().Add(30 * time.Minute).Format(time.RFC3339),
	}, nil
}

// UploadTokenResponse 上传凭证响应
type UploadTokenResponse struct {
	Token     string `json:"token"`
	Bucket    string `json:"bucket"`
	Region    string `json:"region"`
	Key       string `json:"key"`
	ExpiresAt string `json:"expires_at"`
}

