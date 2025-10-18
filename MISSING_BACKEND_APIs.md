# 缺失的后端API列表

本文档记录了Android客户端需要但后端尚未实现的API接口。

## ❌ 1. 消息搜索API

### API详情
- **端点**: `GET /api/v1/messages/search`
- **功能**: 搜索所有会话中的消息内容
- **使用场景**: SearchActivity - 全局消息搜索功能

### 请求参数
```json
{
  "keyword": "搜索关键词",
  "page": 1,
  "page_size": 20
}
```

### 响应示例
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 10,
    "messages": [
      {
        "id": 1,
        "conversation_id": 1,
        "sender_id": 1,
        "content": "包含关键词的消息内容",
        "type": "text",
        "created_at": 1634567890000
      }
    ]
  }
}
```

### 客户端实现位置
- `SearchActivity.kt:102` - performSearch()方法调用此API

---

## ❌ 2. 修改密码API

### API详情
- **端点**: `PUT /api/v1/users/me/password`
- **功能**: 修改当前用户密码
- **使用场景**: SettingsActivity - 修改密码功能

### 请求参数
```json
{
  "old_password": "旧密码",
  "new_password": "新密码"
}
```

### 响应示例
```json
{
  "code": 0,
  "message": "密码修改成功",
  "data": null
}
```

### 客户端实现位置
- `SettingsActivity.kt` - 密码修改功能
- `ApiService.kt:38-39` - changePassword()接口已定义

---

## ❌ 3. 删除联系人API

### API详情
- **端点**: `DELETE /api/v1/contacts/{id}`
- **功能**: 删除指定联系人
- **使用场景**: ContactsFragment - 删除好友功能

### 请求参数
- Path参数: `id` - 联系人ID

### 响应示例
```json
{
  "code": 0,
  "message": "删除成功",
  "data": null
}
```

### 客户端实现位置
- `ContactsFragment.kt` - 长按删除联系人
- `ApiService.kt:59-60` - deleteContact()接口已定义

---

## ❌ 4. 好友请求相关API

### 4.1 获取好友请求列表

- **端点**: `GET /api/v1/friend-requests`
- **功能**: 获取收到的好友申请列表
- **使用场景**: NewFriendsActivity - 新的朋友页面

### 请求参数
```json
{
  "page": 1,
  "page_size": 20,
  "status": "pending"  // pending/accepted/rejected
}
```

### 响应示例
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 5,
    "requests": [
      {
        "id": 1,
        "requester_id": 2,
        "requester": {
          "id": 2,
          "username": "张三",
          "avatar": "http://example.com/avatar.jpg",
          "lanxin_id": "user123"
        },
        "message": "你好，我是张三",
        "status": "pending",
        "created_at": 1634567890000
      }
    ]
  }
}
```

### 4.2 处理好友请求

- **端点**: `POST /api/v1/friend-requests/{id}/accept` 或 `/reject`
- **功能**: 接受/拒绝好友申请
- **使用场景**: NewFriendsActivity - 处理好友申请

### 请求参数
```json
{
  "remark": "备注名"  // 可选，接受时可添加备注
}
```

### 客户端实现位置
- `NewFriendsActivity.kt:40-48` - 需要实现好友申请列表功能
- `ApiService.kt` - 需要添加相关接口定义

---

## ❌ 5. 群组列表API

### API详情
- **端点**: `GET /api/v1/groups`
- **功能**: 获取当前用户加入的所有群组
- **使用场景**: GroupListActivity - 群聊列表

### 请求参数
```json
{
  "page": 1,
  "page_size": 20
}
```

### 响应示例
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "total": 3,
    "groups": [
      {
        "id": 1,
        "name": "技术交流群",
        "avatar": "http://example.com/group-avatar.jpg",
        "owner_id": 1,
        "type": "normal",
        "member_count": 50,
        "status": "active",
        "created_at": 1634567890000
      }
    ]
  }
}
```

### 客户端实现位置
- `GroupListActivity.kt:40-51` - loadGroups()方法需要此API
- `ApiService.kt` - 需要添加getGroups()接口

---

## ❌ 6. 扫一扫相关API

### 6.1 生成个人二维码

- **端点**: `GET /api/v1/users/me/qrcode`
- **功能**: 生成当前用户的二维码数据
- **使用场景**: QRCodeActivity - 个人二维码展示

### 响应示例
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "qrcode_data": "lanxin://user/12345",
    "expires_at": 1634567890000
  }
}
```

### 6.2 解析二维码

- **端点**: `POST /api/v1/qrcode/parse`
- **功能**: 解析扫描的二维码内容
- **使用场景**: ScanActivity - 扫一扫功能

### 请求参数
```json
{
  "qrcode_data": "lanxin://user/12345"
}
```

### 响应示例
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "type": "user",  // user/group
    "user": {
      "id": 12345,
      "username": "张三",
      "avatar": "http://example.com/avatar.jpg",
      "lanxin_id": "user123"
    }
  }
}
```

### 客户端实现位置
- `MainActivity.kt` - 扫一扫菜单项点击事件
- 需要创建QRCodeActivity和ScanActivity

---

## ⚠️ 7. 其他需要确认的API

### 7.1 会话设置API

当前已定义但需要确认后端是否实现：

- `GET /api/v1/conversations/{id}/settings` - 获取会话设置
- `PUT /api/v1/conversations/{id}/settings` - 更新会话设置（置顶、免打扰等）

### 7.2 文件上传回调API

- `POST /api/v1/files/upload-callback` - MinIO上传完成后的回调

### 7.3 TRTC相关API

- `POST /api/v1/trtc/user-sig` - 获取TRTC UserSig
- `POST /api/v1/trtc/call` - 发起音视频通话

---

## 📋 API优先级建议

### P0 - 必须实现（核心功能）
1. ❌ 消息搜索API - 全局搜索功能
2. ❌ 好友请求列表API - 添加好友流程
3. ❌ 群组列表API - 群聊功能

### P1 - 重要功能
4. ❌ 修改密码API - 账号安全
5. ❌ 删除联系人API - 联系人管理
6. ❌ 扫一扫相关API - 社交功能

### P2 - 增强功能
7. ⚠️ 会话设置API - 用户体验优化
8. ⚠️ 文件上传回调API - 文件管理
9. ⚠️ TRTC相关API - 音视频通话

---

## 📝 备注

1. ✅ 标记的API已经实现且正常工作
2. ❌ 标记的API尚未实现，需要后端开发
3. ⚠️ 标记的API已定义但需要确认后端实现状态

## 更新时间

最后更新: 2025-10-18

## 联系人

如有疑问，请联系Android开发团队。
