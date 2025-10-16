import { Card, Form, Input, Switch, Button, Select, InputNumber, message, Divider } from 'antd'
import { SaveOutlined } from '@ant-design/icons'

const { Option } = Select
const { TextArea } = Input

function SystemSettings() {
  const [form] = Form.useForm()

  const onSave = async (values: any) => {
    try {
      // TODO: 调用API保存设置
      message.success('设置已保存')
    } catch (error: any) {
      message.error(error?.message || '保存失败')
    }
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>系统设置</h1>
      
      <Card>
        <Form
          form={form}
          layout="vertical"
          onFinish={onSave}
          initialValues={{
            site_name: '蓝信通讯',
            max_file_size: 100,
            message_retention_days: 30,
            allow_registration: true,
            require_email_verification: false,
          }}
        >
          <h3>基本设置</h3>
          <Form.Item
            label="站点名称"
            name="site_name"
          >
            <Input placeholder="蓝信通讯" />
          </Form.Item>

          <Form.Item
            label="站点描述"
            name="site_description"
          >
            <TextArea rows={3} placeholder="企业级即时通讯系统" />
          </Form.Item>

          <Divider />

          <h3>功能设置</h3>
          
          <Form.Item
            label="允许用户注册"
            name="allow_registration"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="需要邮箱验证"
            name="require_email_verification"
            valuePropName="checked"
          >
            <Switch />
          </Form.Item>

          <Form.Item
            label="最大文件上传大小（MB）"
            name="max_file_size"
          >
            <InputNumber min={1} max={1000} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="消息保留天数"
            name="message_retention_days"
          >
            <InputNumber min={7} max={365} style={{ width: '100%' }} />
          </Form.Item>

          <Divider />

          <h3>安全设置</h3>
          
          <Form.Item
            label="登录失败锁定次数"
            name="max_login_attempts"
          >
            <InputNumber min={3} max={10} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item
            label="会话超时时间（小时）"
            name="session_timeout_hours"
          >
            <InputNumber min={1} max={168} style={{ width: '100%' }} />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>
              保存设置
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  )
}

export default SystemSettings

