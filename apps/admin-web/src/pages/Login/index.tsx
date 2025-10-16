import { Form, Input, Button, Card, message } from 'antd'
import { UserOutlined, LockOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import authService from '../../services/authService'
import { setCredentials } from '../../store/authSlice'
import './style.css'

function Login() {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const [form] = Form.useForm()

  const onFinish = async (values: { identifier: string; password: string }) => {
    try {
      console.log('开始登录...', values)
      const response = await authService.login(values)
      console.log('登录响应:', response)
      console.log('Token:', response.token)
      console.log('User:', response.user)
      
      dispatch(setCredentials({ user: response.user, token: response.token }))
      message.success('登录成功！')
      
      console.log('准备跳转到 /dashboard')
      navigate('/dashboard')
      console.log('navigate 已调用')
    } catch (error: any) {
      console.error('登录错误:', error)
      message.error(error?.message || '登录失败，请检查用户名和密码')
    }
  }

  return (
    <div className="login-container">
      <div className="login-sidebar">
        <div>
          <h1 className="login-title">蓝信通讯</h1>
          <p className="login-subtitle">后台管理系统</p>
          <ul className="feature-list">
            <li>✓ 用户管理与权限控制</li>
            <li>✓ 实时消息监控与分析</li>
            <li>✓ 高级数据可视化图表</li>
            <li>✓ 安全加密与数据保护</li>
          </ul>
        </div>
        <div className="copyright">© 2025 蓝信通讯. 版权所有</div>
      </div>

      <div className="login-form-container">
        <Card className="login-card">
          <h2 className="form-title">欢迎回来</h2>
          <p className="form-subtitle">请登录您的管理员账户</p>

          <Form form={form} onFinish={onFinish} size="large">
            <Form.Item
              name="identifier"
              rules={[{ required: true, message: '请输入账号/手机号/邮箱' }]}
            >
              <Input prefix={<UserOutlined />} placeholder="账号/手机号/邮箱" />
            </Form.Item>

            <Form.Item
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="密码" />
            </Form.Item>

            <Form.Item>
              <Button type="primary" htmlType="submit" block>
                登录
              </Button>
            </Form.Item>
          </Form>
        </Card>
      </div>
    </div>
  )
}

export default Login

