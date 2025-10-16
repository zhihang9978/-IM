import { Layout, Menu } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  DashboardOutlined,
  UserOutlined,
  MessageOutlined,
  TeamOutlined,
  FileOutlined,
  BarChartOutlined,
  SettingOutlined,
  DatabaseOutlined,
} from '@ant-design/icons'

const { Sider } = Layout

function Sidebar() {
  const navigate = useNavigate()
  const location = useLocation()

  const menuItems = [
    { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
    { key: '/users', icon: <UserOutlined />, label: '用户管理' },
    { key: '/messages', icon: <MessageOutlined />, label: '消息管理' },
    { key: '/groups', icon: <TeamOutlined />, label: '群聊管理' },
    { key: '/files', icon: <FileOutlined />, label: '文件管理' },
    { key: '/analytics', icon: <BarChartOutlined />, label: '数据分析' },
    { key: '/settings', icon: <SettingOutlined />, label: '系统设置' },
    { key: '/backup', icon: <DatabaseOutlined />, label: '数据备份' },
  ]

  return (
    <Sider width={200} theme="dark">
      <div style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'white', fontSize: 18, fontWeight: 'bold' }}>
        蓝信管理后台
      </div>
      <Menu
        theme="dark"
        mode="inline"
        selectedKeys={[location.pathname]}
        items={menuItems}
        onClick={({ key }) => navigate(key)}
      />
    </Sider>
  )
}

export default Sidebar

