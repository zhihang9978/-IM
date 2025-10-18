import { Card, Row, Col, DatePicker, Select, Space, Spin } from 'antd'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'
import { useState, useEffect } from 'react'
import api from '../../services/api'

const { RangePicker } = DatePicker
const { Option } = Select

function DataAnalysis() {
  const [loading, setLoading] = useState(false)
  const [userGrowth, setUserGrowth] = useState<any[]>([])
  const [messageStats, setMessageStats] = useState<any[]>([])
  const [deviceDistribution, setDeviceDistribution] = useState<any[]>([])

  useEffect(() => {
    loadData()
  }, [])

  const loadData = async () => {
    setLoading(true)
    try {
      const [growthData, msgData, deviceData] = await Promise.all([
        api.get('/admin/dashboard/user-growth'),
        api.get('/admin/dashboard/message-stats'),
        api.get('/admin/dashboard/device-distribution')
      ])
      setUserGrowth(growthData || [])
      setMessageStats(msgData || [])
      setDeviceDistribution(deviceData || [])
    } catch (error) {
      console.error('Failed to load data:', error)
    } finally {
      setLoading(false)
    }
  }

  const dauOption = {
    title: { text: '用户增长趋势' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['新增用户'] },
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
      },
    ],
  }

  // 消息量分析
  const messageVolumeOption = {
    title: { text: '消息类型统计' },
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '消息类型',
        type: 'pie',
        radius: '60%',
        data: messageStats.map(item => ({
          value: item.count,
          name: item.type,
        })),
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

  // 设备分布饼图
  const deviceDistributionOption = {
    title: { text: '在线设备分布' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '设备类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        data: deviceDistribution.map((item, index) => ({
          value: item.count,
          name: item.device_type,
          itemStyle: { 
            color: ['#10b981', '#3b82f6', '#8b5cf6', '#f59e0b'][index % 4] 
          }
        })),
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
    <Spin spinning={loading}>
      <div>
        <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>数据分析</h1>
        
        <Card style={{ marginBottom: '1rem' }}>
          <Space size="middle">
            <span>时间范围:</span>
            <RangePicker />
            <Select defaultValue="all" style={{ width: '10rem' }}>
              <Option value="all">全部数据</Option>
              <Option value="today">今日</Option>
              <Option value="week">本周</Option>
              <Option value="month">本月</Option>
            </Select>
          </Space>
        </Card>

        <Row gutter={[16, 16]}>
          <Col xs={24} lg={12}>
            <Card>
              <ReactECharts option={dauOption} style={{ height: '24rem' }} />
            </Card>
          </Col>
          <Col xs={24} lg={12}>
            <Card>
              <ReactECharts option={messageVolumeOption} style={{ height: '24rem' }} />
            </Card>
          </Col>
        </Row>

        <Row gutter={[16, 16]} style={{ marginTop: '1rem' }}>
          <Col xs={24} lg={12}>
            <Card>
              <ReactECharts option={deviceDistributionOption} style={{ height: '24rem' }} />
            </Card>
          </Col>
        </Row>
      </div>
    </Spin>
  )
}

export default DataAnalysis

