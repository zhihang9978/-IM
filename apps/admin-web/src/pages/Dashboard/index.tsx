import { Card, Row, Col, Statistic } from 'antd'
import { UserOutlined, MessageOutlined, TeamOutlined, FileOutlined } from '@ant-design/icons'

function Dashboard() {
  return (
    <div>
      <h1 style={{ marginBottom: 24 }}>仪表盘</h1>
      <Row gutter={[16, 16]}>
        <Col span={6}>
          <Card>
            <Statistic title="总用户数" value={1128} prefix={<UserOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="消息总数" value={93489} prefix={<MessageOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="群组数" value={256} prefix={<TeamOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic title="文件总数" value={4532} prefix={<FileOutlined />} />
          </Card>
        </Col>
      </Row>
      <Card style={{ marginTop: 24 }}>
        <h3>欢迎使用蓝信通讯后台管理系统</h3>
        <p>这是一个基于React + TypeScript + Ant Design构建的现代化管理后台。</p>
        <p>您可以通过左侧菜单访问各个功能模块。</p>
      </Card>
    </div>
  )
}

export default Dashboard

