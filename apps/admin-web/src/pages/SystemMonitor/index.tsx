import { useEffect, useState } from 'react'
import { Card, Row, Col, Statistic, Table, Tag, Progress, Alert } from 'antd'
import { 
  DashboardOutlined, 
  ApiOutlined, 
  DatabaseOutlined, 
  CloudServerOutlined,
  CheckCircleOutlined,
  WarningOutlined,
  CloseCircleOutlined
} from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'
import api from '../../services/api'

interface SystemMetrics {
  cpu_usage: number
  memory_usage: number
  disk_usage: number
  network_in: number
  network_out: number
  active_connections: number
  uptime_seconds: number
}

interface ServiceStatus {
  name: string
  status: 'healthy' | 'warning' | 'error'
  response_time: number
  last_check: string
}

function SystemMonitor() {
  const [metrics, setMetrics] = useState<SystemMetrics>({
    cpu_usage: 0,
    memory_usage: 0,
    disk_usage: 0,
    network_in: 0,
    network_out: 0,
    active_connections: 0,
    uptime_seconds: 0
  })

  const [services, setServices] = useState<ServiceStatus[]>([])
  const [cpuHistory, setCpuHistory] = useState<number[]>([])
  const [memoryHistory, setMemoryHistory] = useState<number[]>([])
  const [timeLabels, setTimeLabels] = useState<string[]>([])

  useEffect(() => {
    fetchSystemMetrics()
    fetchServiceStatus()
    
    const interval = setInterval(() => {
      fetchSystemMetrics()
      fetchServiceStatus()
    }, 5000)

    return () => clearInterval(interval)
  }, [])

  const fetchSystemMetrics = async () => {
    try {
      const data = await api.get<SystemMetrics>('/admin/system/metrics')
      setMetrics(data)
      
      const now = new Date()
      const timeLabel = `${now.getHours()}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
      
      setCpuHistory(prev => {
        const newHistory = [...prev, data.cpu_usage]
        return newHistory.slice(-60)
      })
      
      setMemoryHistory(prev => {
        const newHistory = [...prev, data.memory_usage]
        return newHistory.slice(-60)
      })
      
      setTimeLabels(prev => {
        const newLabels = [...prev, timeLabel]
        return newLabels.slice(-60)
      })
    } catch (error) {
      console.error('Failed to fetch system metrics:', error)
    }
  }

  const fetchServiceStatus = async () => {
    try {
      const data = await api.get<ServiceStatus[]>('/admin/system/services')
      setServices(data)
    } catch (error) {
      console.error('Failed to fetch service status:', error)
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'healthy':
        return <CheckCircleOutlined style={{ color: '#52c41a' }} />
      case 'warning':
        return <WarningOutlined style={{ color: '#faad14' }} />
      case 'error':
        return <CloseCircleOutlined style={{ color: '#f5222d' }} />
      default:
        return null
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'healthy':
        return 'success'
      case 'warning':
        return 'warning'
      case 'error':
        return 'error'
      default:
        return 'default'
    }
  }

  const resourceUsageOption = {
    title: { text: 'CPU & 内存使用率趋势' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['CPU', '内存'] },
    xAxis: {
      type: 'category',
      data: timeLabels,
      boundaryGap: false
    },
    yAxis: {
      type: 'value',
      max: 100,
      axisLabel: { formatter: '{value}%' }
    },
    series: [
      {
        name: 'CPU',
        type: 'line',
        smooth: true,
        data: cpuHistory,
        itemStyle: { color: '#3b82f6' },
        areaStyle: { opacity: 0.3 }
      },
      {
        name: '内存',
        type: 'line',
        smooth: true,
        data: memoryHistory,
        itemStyle: { color: '#10b981' },
        areaStyle: { opacity: 0.3 }
      }
    ]
  }

  const serviceColumns = [
    {
      title: '服务名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => (
        <Tag icon={getStatusIcon(status)} color={getStatusColor(status)}>
          {status.toUpperCase()}
        </Tag>
      ),
    },
    {
      title: '响应时间',
      dataIndex: 'response_time',
      key: 'response_time',
      render: (time: number) => `${time}ms`,
    },
    {
      title: '最后检查',
      dataIndex: 'last_check',
      key: 'last_check',
    },
  ]

  const formatUptime = (seconds: number) => {
    const days = Math.floor(seconds / 86400)
    const hours = Math.floor((seconds % 86400) / 3600)
    const minutes = Math.floor((seconds % 3600) / 60)
    return `${days}天 ${hours}时 ${minutes}分`
  }

  const systemHealth = metrics.cpu_usage < 80 && metrics.memory_usage < 85 && metrics.disk_usage < 90

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>系统监控</h1>
      
      {/* 系统健康状态告警 */}
      {!systemHealth && (
        <Alert
          message="系统资源告警"
          description="系统资源使用率过高，请检查服务状态或扩容"
          type="warning"
          showIcon
          closable
          style={{ marginBottom: '1rem' }}
        />
      )}

      {/* 资源使用情况卡片 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="CPU使用率" 
              value={metrics.cpu_usage} 
              suffix="%" 
              prefix={<DashboardOutlined />}
              valueStyle={{ color: metrics.cpu_usage > 80 ? '#f5222d' : '#3b82f6' }}
            />
            <Progress 
              percent={metrics.cpu_usage} 
              strokeColor={metrics.cpu_usage > 80 ? '#f5222d' : '#3b82f6'} 
              showInfo={false}
              style={{ marginTop: '0.5rem' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="内存使用率" 
              value={metrics.memory_usage} 
              suffix="%" 
              prefix={<CloudServerOutlined />}
              valueStyle={{ color: metrics.memory_usage > 85 ? '#f5222d' : '#10b981' }}
            />
            <Progress 
              percent={metrics.memory_usage} 
              strokeColor={metrics.memory_usage > 85 ? '#f5222d' : '#10b981'} 
              showInfo={false}
              style={{ marginTop: '0.5rem' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="磁盘使用率" 
              value={metrics.disk_usage} 
              suffix="%" 
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: metrics.disk_usage > 90 ? '#f5222d' : '#f59e0b' }}
            />
            <Progress 
              percent={metrics.disk_usage} 
              strokeColor={metrics.disk_usage > 90 ? '#f5222d' : '#f59e0b'} 
              showInfo={false}
              style={{ marginTop: '0.5rem' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="活跃连接数" 
              value={metrics.active_connections} 
              prefix={<ApiOutlined />}
              valueStyle={{ color: '#8b5cf6' }}
            />
            <p style={{ marginTop: '0.5rem', fontSize: '0.875rem', color: '#6b7280' }}>
              网络入: {(metrics.network_in / 1024).toFixed(2)} MB/s
            </p>
          </Card>
        </Col>
      </Row>

      {/* 系统信息 */}
      <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
        <Col span={24}>
          <Card title="系统信息">
            <Row gutter={16}>
              <Col span={8}>
                <p><strong>运行时间:</strong> {formatUptime(metrics.uptime_seconds)}</p>
                <p><strong>网络入流量:</strong> {(metrics.network_in / 1024).toFixed(2)} MB/s</p>
              </Col>
              <Col span={8}>
                <p><strong>服务器IP:</strong> 154.40.45.121</p>
                <p><strong>网络出流量:</strong> {(metrics.network_out / 1024).toFixed(2)} MB/s</p>
              </Col>
              <Col span={8}>
                <p><strong>系统版本:</strong> Ubuntu Server 22.04</p>
                <p><strong>Go版本:</strong> 1.21.0</p>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* CPU & 内存趋势图 */}
      <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
        <Col span={24}>
          <Card>
            <ReactECharts 
              option={resourceUsageOption} 
              style={{ height: '20rem' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 服务状态列表 */}
      <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
        <Col span={24}>
          <Card title="服务健康检查">
            <Table 
              columns={serviceColumns} 
              dataSource={services} 
              rowKey="name"
              pagination={false}
            />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default SystemMonitor
