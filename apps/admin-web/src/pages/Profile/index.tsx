import { useState } from 'react'
import { Card, Form, Input, Button, Avatar, Upload, message, Divider } from 'antd'
import { UserOutlined, UploadOutlined, SaveOutlined } from '@ant-design/icons'
import authService from '../../services/authService'

function Profile() {
  const [form] = Form.useForm()
  const user = authService.getCurrentUser()
  const [avatarUrl, setAvatarUrl] = useState(user?.avatar || '')

  const onSave = async (values: any) => {
    try {
      // TODO: 调用API更新个人信息
      message.success('个人信息已更新')
    } catch (error: any) {
      message.error(error?.message || '更新失败')
    }
  }

  const handleAvatarChange = (info: any) => {
    if (info.file.status === 'done') {
      // 上传成功，更新头像URL
      message.success('头像上传成功')
      setAvatarUrl(info.file.response.url)
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>个人中心</h1>
      
      <Card>
        <div style={{ display: 'flex', alignItems: 'center', marginBottom: '2rem' }}>
          <Avatar
            size={80}
            src={avatarUrl}
            icon={<UserOutlined />}
            style={{ marginRight: '1.5rem' }}
          />
          <div>
            <h2 style={{ margin: 0, marginBottom: '0.5rem' }}>{user?.username}</h2>
            <p style={{ margin: 0, color: '#6b7280' }}>
              角色: {user?.role === 'admin' ? '管理员' : '普通用户'}
            </p>
          </div>
        </div>

        <Form
          form={form}
          layout="vertical"
          onFinish={onSave}
          initialValues={{
            username: user?.username,
            phone: user?.phone,
            email: user?.email,
          }}
        >
          <h3>基本信息</h3>
          
          <Form.Item label="头像">
            <Upload
              name="file"
              showUploadList={false}
              onChange={handleAvatarChange}
            >
              <Button icon={<UploadOutlined />}>更换头像</Button>
            </Upload>
          </Form.Item>

          <Form.Item
            label="用户名"
            name="username"
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            label="手机号"
            name="phone"
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[{ type: 'email', message: '请输入有效的邮箱' }]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Divider />

          <h3>修改密码</h3>
          
          <Form.Item
            label="当前密码"
            name="old_password"
          >
            <Input.Password placeholder="请输入当前密码" />
          </Form.Item>

          <Form.Item
            label="新密码"
            name="new_password"
          >
            <Input.Password placeholder="请输入新密码" />
          </Form.Item>

          <Form.Item
            label="确认新密码"
            name="confirm_password"
            dependencies={['new_password']}
            rules={[
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('new_password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
          >
            <Input.Password placeholder="请再次输入新密码" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>
              保存修改
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default Profile

