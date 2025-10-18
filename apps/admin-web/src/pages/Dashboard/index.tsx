import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Tag } from 'antd'
import { UserOutlined, MessageOutlined, TeamOutlined, FileOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import api from '../../services/api'

interface DashboardStats {
  total_users: number
  total_messages: number
  total_groups: number
  total_files: number
  online_users: number
  today_users: number
  today_messages: number
}

interface UserGrowth {
  date: string
  count: number
}

interface MessageStat {
  type: string
  count: number
}

interface DeviceDistribution {
  name: string
  value: number
}

function Dashboard() {
  const [stats, setStats] = useState<DashboardStats>({
    total_users: 0,
    total_messages: 0,
    total_groups: 0,
    total_files: 0,
    online_users: 0,
    today_users: 0,
    today_messages: 0,
  })

  const [userGrowth, setUserGrowth] = useState<UserGrowth[]>([])
  const [messageStats, setMessageStats] = useState<MessageStat[]>([])
  const [deviceDistribution, setDeviceDistribution] = useState<DeviceDistribution[]>([])

  useEffect(() => {
    fetchDashboardData()
    
    const interval = setInterval(() => {
      fetchDashboardData()
    }, 30000)

    return () => clearInterval(interval)
  }, [])

  const fetchDashboardData = async () => {
    try {
      const [statsData, growthData, msgStatsData, deviceData] = await Promise.all([
        api.get<DashboardStats>('/admin/dashboard/stats'),
        api.get<UserGrowth[]>('/admin/dashboard/user-growth'),
        api.get<MessageStat[]>('/admin/dashboard/message-stats'),
        api.get<DeviceDistribution[]>('/admin/dashboard/device-distribution'),
      ])

      setStats(statsData)
      setUserGrowth(growthData)
      setMessageStats(msgStatsData)
      setDeviceDistribution(deviceData)
    } catch (error) {
      console.error('Failed to fetch dashboard data:', error)
    }
  }

  const userTrendOption = {
    title: { text: '用户增长趋势（最近7天）' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: userGrowth.map(item => item.date),
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: userGrowth.map(item => item.count),
        itemStyle: { color: '#3b82f6' },
        areaStyle: { opacity: 0.3 },
      },
    ],
  }

  const messageStatsOption = {
    title: { text: '消息类型统计' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: messageStats.map(item => {
        const typeNames: Record<string, string> = {
          'text': '文本',
          'image': '图片',
          'voice': '语音',
          'video': '视频',
          'file': '文件',
        }
        return typeNames[item.type] || item.type
      }),
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '消息数量',
        type: 'bar',
        data: messageStats.map(item => item.count),
        itemStyle: { color: '#10b981' },
      },
    ],
  }

  const onlineDistributionOption = {
    title: { text: '在线用户设备分布' },
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '设备类型',
        type: 'pie',
        radius: '50%',
        data: deviceDistribution,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(0, 0, 0, 0.5)',
          },
        },
      },
    ],
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>仪表盘</h1>
      
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="总用户数" 
              value={stats.total_users} 
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3b82f6' }}
            />
            <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: '#6b7280' }}>
              今日新增: {stats.today_users}
            </p>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="消息总数" 
              value={stats.total_messages} 
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#10b981' }}
            />
            <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: '#6b7280' }}>
              今日消息: {stats.today_messages}
            </p>
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="群组数" 
              value={stats.total_groups} 
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#f59e0b' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="文件总数" 
              value={stats.total_files} 
              prefix={<FileOutlined />}
              valueStyle={{ color: '#ef4444' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 数据可视化图表 */}
      <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
        <Col xs={24} lg={12}>
          <Card>
            <ReactECharts 
              option={userTrendOption} 
              style={{ height: '20rem' }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card>
            <ReactECharts 
              option={messageStatsOption} 
              style={{ height: '20rem' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
        <Col xs={24} lg={12}>
          <Card>
            <ReactECharts 
              option={onlineDistributionOption} 
              style={{ height: '20rem' }}
            />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card>
            <h3>系统信息</h3>
            <div style={{ marginTop: '1rem' }}>
              <p><strong>服务器状态：</strong> <Tag color="success">运行中</Tag></p>
              <p><strong>在线用户：</strong> {stats.online_users} 人</p>
              <p><strong>服务器IP：</strong> 154.40.45.121</p>
              <p><strong>数据库：</strong> MySQL 8.0</p>
              <p><strong>缓存：</strong> Redis 7.0</p>
              <p><strong>对象存储：</strong> MinIO</p>
              <p><strong>音视频：</strong> Tencent TRTC</p>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard

