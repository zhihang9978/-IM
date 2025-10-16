# 蓝信通讯 API 接口文档

## 基础信息

**Base URL**: `https://api.lanxin168.com/api/v1`  
**协议**: HTTPS (TLS 1.3)  
**认证方式**: JWT Bearer Token  
**请求格式**: JSON  
**响应格式**: JSON

**重要说明**: 
- **COS对象存储为自建服务**（非腾讯云），支持S3协议兼容（如MinIO）
- **TRTC音视频服务**使用腾讯云SDK，仅调用数据流接口，不使用UI组件

## 通用响应格式

### 成功响应
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 错误响应
```json
{
  "code": 错误码,
  "message": "错误描述",
  "data": null
}
```

### 错误码定义
- `0`: 成功
- `400`: 请求参数错误
- `401`: 未授权（Token无效或过期）
- `403`: 无权限
- `404`: 资源不存在
- `429`: 请求过于频繁
- `500`: 服务器内部错误

---

## 1. 认证模块

### 1.1 用户注册
**POST** `/auth/register`

**请求体**:
```json
{
  "username": "string (3-20字符)",
  "password": "string (6-32字符)",
  "phone": "string (可选，手机号)",
  "email": "string (可选，邮箱)"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "user": {
      "id": 1,
      "username": "zhangsan",
      "lanxin_id": "lx123456",
      "role": "user",
      "status": "active",
      "created_at": "2025-01-16T10:00:00Z"
    }
  }
}
```

### 1.2 用户登录
**POST** `/auth/login`

**请求体**:
```json
{
  "identifier": "string (用户名/手机号/邮箱/蓝信号)",
  "password": "string"
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "username": "zhangsan",
      "phone": "13800138000",
      "email": "zhangsan@example.com",
      "avatar": "https://cdn.lanxin168.com/avatars/1.jpg",
      "lanxin_id": "lx123456",
      "role": "user",
      "status": "active",
      "last_login_at": "2025-01-16T10:00:00Z",
      "created_at": "2025-01-15T08:00:00Z"
    }
  }
}
```

**操作日志记录**:
```json
{
  "action": "user_login",
  "user_id": 1,
  "ip": "192.168.1.100",
  "timestamp": "2025-01-16T10:00:00Z",
  "details": {
    "identifier": "zhangsan",
    "device": "Android/11",
    "login_method": "username"
  }
}
```

### 1.3 刷新Token
**POST** `/auth/refresh`

**请求头**:
```
Authorization: Bearer {old_token}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs..."
  }
}
```

### 1.4 退出登录
**POST** `/auth/logout`

**请求头**:
```
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**操作日志记录**:
```json
{
  "action": "user_logout",
  "user_id": 1,
  "timestamp": "2025-01-16T11:00:00Z"
}
```

---

## 2. 用户模块

### 2.1 获取当前用户信息
**GET** `/users/me`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "phone": "13800138000",
    "email": "zhangsan@example.com",
    "avatar": "https://cdn.lanxin168.com/avatars/1.jpg",
    "lanxin_id": "lx123456",
    "role": "user",
    "status": "active"
  }
}
```

### 2.2 更新用户信息
**PUT** `/users/me`

**请求体**:
```json
{
  "username": "string (可选)",
  "avatar": "string (可选，URL)",
  "phone": "string (可选)",
  "email": "string (可选)"
}
```

**操作日志记录**:
```json
{
  "action": "user_profile_update",
  "user_id": 1,
  "timestamp": "2025-01-16T10:30:00Z",
  "changes": {
    "username": {"old": "zhangsan", "new": "zhangsan123"},
    "avatar": {"old": "old_url", "new": "new_url"}
  }
}
```

### 2.3 修改密码
**PUT** `/users/me/password`

**请求体**:
```json
{
  "old_password": "string",
  "new_password": "string"
}
```

**操作日志记录**:
```json
{
  "action": "password_change",
  "user_id": 1,
  "timestamp": "2025-01-16T10:45:00Z",
  "ip": "192.168.1.100"
}
```

### 2.4 搜索用户
**GET** `/users/search?keyword=张三&page=1&page_size=20`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 5,
    "page": 1,
    "page_size": 20,
    "users": [
      {
        "id": 2,
        "username": "zhangsan",
        "avatar": "url",
        "lanxin_id": "lx123456"
      }
    ]
  }
}
```

---

## 3. 联系人模块

### 3.1 获取联系人列表
**GET** `/contacts?page=1&page_size=50`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 100,
    "contacts": [
      {
        "id": 1,
        "contact_id": 2,
        "user": {
          "id": 2,
          "username": "lisi",
          "avatar": "url",
          "lanxin_id": "lx654321"
        },
        "remark": "李四备注",
        "tags": "同事,项目组",
        "status": "normal",
        "created_at": "2025-01-10T08:00:00Z"
      }
    ]
  }
}
```

### 3.2 添加联系人
**POST** `/contacts`

**请求体**:
```json
{
  "contact_id": 2,
  "remark": "string (可选)",
  "tags": "string (可选)"
}
```

**操作日志记录**:
```json
{
  "action": "contact_add",
  "user_id": 1,
  "timestamp": "2025-01-16T11:00:00Z",
  "details": {
    "contact_id": 2,
    "contact_username": "lisi"
  }
}
```

### 3.3 删除联系人
**DELETE** `/contacts/:id`

**操作日志记录**:
```json
{
  "action": "contact_delete",
  "user_id": 1,
  "timestamp": "2025-01-16T11:05:00Z",
  "details": {
    "contact_id": 2,
    "contact_username": "lisi"
  }
}
```

---

## 4. 消息模块

### 4.1 获取会话列表
**GET** `/conversations?page=1&page_size=20`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "conversations": [
      {
        "id": 1,
        "type": "single",
        "user": {
          "id": 2,
          "username": "lisi",
          "avatar": "url"
        },
        "last_message": {
          "id": 100,
          "content": "你好",
          "type": "text",
          "status": "read",
          "created_at": "2025-01-16T10:30:00Z"
        },
        "unread_count": 3,
        "updated_at": "2025-01-16T10:30:00Z"
      }
    ]
  }
}
```

### 4.2 获取消息历史
**GET** `/conversations/:id/messages?page=1&page_size=50`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 200,
    "messages": [
      {
        "id": 100,
        "conversation_id": 1,
        "sender_id": 2,
        "receiver_id": 1,
        "content": "你好",
        "type": "text",
        "status": "read",
        "created_at": "2025-01-16T10:30:00Z"
      }
    ]
  }
}
```

### 4.3 发送消息
**POST** `/messages`

**请求体**:
```json
{
  "receiver_id": 2,
  "content": "string",
  "type": "text", // text, image, voice, video, file
  "file_url": "string (type非text时必填)",
  "file_size": 1024,
  "duration": 60 // 语音/视频时长（秒）
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "message": {
      "id": 101,
      "conversation_id": 1,
      "sender_id": 1,
      "receiver_id": 2,
      "content": "你好",
      "type": "text",
      "status": "sent",
      "created_at": "2025-01-16T10:35:00Z"
    }
  }
}
```

### 4.4 撤回消息
**POST** `/messages/:id/recall`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

**操作日志记录**:
```json
{
  "action": "message_recall",
  "user_id": 1,
  "timestamp": "2025-01-16T10:36:00Z",
  "details": {
    "message_id": 101,
    "conversation_id": 1,
    "original_content": "你好",
    "recall_time_diff": "60s"
  }
}
```

### 4.5 标记消息已读
**POST** `/conversations/:id/read`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": null
}
```

---

## 5. 文件上传模块

### 5.1 获取上传凭证
**GET** `/files/upload-token?file_type=image&file_name=test.jpg`

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "token": "upload_token_string",
    "bucket": "lanxin-files",
    "region": "ap-guangzhou",
    "key": "images/2025/01/16/uuid.jpg",
    "expires_at": "2025-01-16T11:00:00Z"
  }
}
```

### 5.2 上传完成回调
**POST** `/files/upload-callback`

**请求体**:
```json
{
  "key": "images/2025/01/16/uuid.jpg",
  "url": "https://cdn.lanxin168.com/images/2025/01/16/uuid.jpg",
  "size": 102400,
  "content_type": "image/jpeg"
}
```

**操作日志记录**:
```json
{
  "action": "file_upload",
  "user_id": 1,
  "timestamp": "2025-01-16T10:40:00Z",
  "details": {
    "file_key": "images/2025/01/16/uuid.jpg",
    "file_size": 102400,
    "file_type": "image/jpeg"
  }
}
```

---

## 6. TRTC音视频模块

### 6.1 获取通话凭证
**POST** `/trtc/user-sig`

**请求体**:
```json
{
  "room_id": "string",
  "user_id": 1
}
```

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "sdk_app_id": 1400000000,
    "user_sig": "eJwtzE...",
    "room_id": "room_123",
    "expires_at": "2025-01-16T14:00:00Z"
  }
}
```

**注意**: 此接口仅返回数据流凭证，不涉及UI组件。

### 6.2 发起通话
**POST** `/trtc/call`

**请求体**:
```json
{
  "receiver_id": 2,
  "call_type": "audio" // audio, video
}
```

**操作日志记录**:
```json
{
  "action": "call_initiated",
  "user_id": 1,
  "timestamp": "2025-01-16T11:00:00Z",
  "details": {
    "receiver_id": 2,
    "call_type": "audio",
    "room_id": "room_123"
  }
}
```

---

## 7. WebSocket 接口

### 7.1 连接地址
**WebSocket** `wss://api.lanxin168.com/ws?token={jwt_token}`

### 7.2 消息格式

#### 客户端发送心跳
```json
{
  "type": "ping",
  "timestamp": 1705392000000
}
```

#### 服务器响应心跳
```json
{
  "type": "pong",
  "timestamp": 1705392000000
}
```

#### 接收新消息
```json
{
  "type": "message",
  "data": {
    "id": 102,
    "conversation_id": 1,
    "sender_id": 2,
    "content": "你好",
    "type": "text",
    "created_at": "2025-01-16T11:05:00Z"
  }
}
```

#### 消息状态更新
```json
{
  "type": "message_status",
  "data": {
    "message_id": 101,
    "status": "read", // sent, delivered, read
    "timestamp": "2025-01-16T11:06:00Z"
  }
}
```

#### 通话邀请
```json
{
  "type": "call_invite",
  "data": {
    "caller_id": 1,
    "caller_username": "zhangsan",
    "room_id": "room_123",
    "call_type": "audio"
  }
}
```

---

## 8. 管理员API

### 8.1 获取用户列表
**GET** `/admin/users?page=1&page_size=20&status=active`

**权限**: admin

### 8.2 封禁用户
**POST** `/admin/users/:id/ban`

**权限**: admin

**操作日志记录**:
```json
{
  "action": "admin_user_ban",
  "admin_id": 1,
  "timestamp": "2025-01-16T11:10:00Z",
  "details": {
    "target_user_id": 5,
    "target_username": "baduser",
    "reason": "违规行为",
    "ban_duration": "7d"
  }
}
```

### 8.3 获取系统日志
**GET** `/admin/logs?page=1&page_size=50&action=user_login`

**权限**: admin

**响应**:
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 1000,
    "logs": [
      {
        "id": 1,
        "action": "user_login",
        "user_id": 1,
        "ip": "192.168.1.100",
        "timestamp": "2025-01-16T10:00:00Z",
        "details": {}
      }
    ]
  }
}
```

---

## 9. 操作日志记录规范

所有后台管理操作、敏感操作均需记录日志，包括但不限于：

### 9.1 用户操作
- `user_login`: 登录
- `user_logout`: 退出
- `user_register`: 注册
- `password_change`: 修改密码
- `user_profile_update`: 更新资料

### 9.2 消息操作
- `message_send`: 发送消息
- `message_recall`: 撤回消息
- `message_delete`: 删除消息

### 9.3 联系人操作
- `contact_add`: 添加联系人
- `contact_delete`: 删除联系人
- `contact_block`: 拉黑联系人

### 9.4 文件操作
- `file_upload`: 上传文件
- `file_download`: 下载文件
- `file_delete`: 删除文件

### 9.5 通话操作
- `call_initiated`: 发起通话
- `call_answered`: 接听通话
- `call_ended`: 结束通话
- `screen_share_start`: 开始屏幕共享
- `screen_share_end`: 结束屏幕共享

### 9.6 管理员操作
- `admin_user_ban`: 封禁用户
- `admin_user_unban`: 解封用户
- `admin_message_delete`: 删除消息
- `admin_group_disband`: 解散群聊
- `admin_system_config_change`: 系统配置变更

### 9.7 日志存储格式
```json
{
  "id": "log_uuid",
  "action": "操作类型",
  "user_id": 1,
  "admin_id": null,
  "ip": "IP地址",
  "user_agent": "客户端信息",
  "timestamp": "ISO8601时间戳",
  "details": {},
  "result": "success/failure",
  "error_message": "错误信息（如果失败）"
}
```

---

## 10. 限流规则

### 10.1 接口限流
- 登录接口: 5次/分钟/IP
- 注册接口: 3次/分钟/IP
- 发送消息: 60次/分钟/用户
- 文件上传: 20次/分钟/用户
- 其他接口: 100次/分钟/用户

### 10.2 限流响应
```json
{
  "code": 429,
  "message": "请求过于频繁，请稍后再试",
  "data": {
    "retry_after": 60
  }
}
```

---

**文档版本**: v1.0  
**最后更新**: 2025-01-16  
**维护者**: LanXin Development Team

