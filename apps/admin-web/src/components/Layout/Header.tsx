import { Layout, Dropdown, Avatar, Space } from 'antd'
import { UserOutlined, LogoutOutlined, ProfileOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import authService from '../../services/authService'

const { Header: AntHeader } = Layout

function Header() {
  const navigate = useNavigate()
  const user = authService.getCurrentUser()

  const handleLogout = () => {
    authService.logout()
  }

  const menuItems = [
    {
      key: 'profile',
      icon: <ProfileOutlined />,
      label: '个人中心',
      onClick: () => navigate('/profile'),
    },
    {
      key: 'logout',
      icon: <LogoutOutlined />,
      label: '退出登录',
      onClick: handleLogout,
    },
  ]

  return (
    <AntHeader style={{ background: '#fff', padding: '0 24px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <div />
      <Dropdown menu={{ items: menuItems }} placement="bottomRight">
        <Space style={{ cursor: 'pointer' }}>
          <Avatar icon={<UserOutlined />} src={user?.avatar} />
          <span>{user?.username || '管理员'}</span>
        </Space>
      </Dropdown>
    </AntHeader>
  )
}

export default Header

