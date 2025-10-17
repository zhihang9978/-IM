import { Card, Row, Col, DatePicker, Select, Space } from 'antd'
import ReactECharts from 'echarts-for-react'
import * as echarts from 'echarts'

const { RangePicker } = DatePicker
const { Option } = Select

function DataAnalysis() {
  // 日活趋势图
  const dauOption = {
    title: { text: '日活用户趋势（DAU）' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['日活用户', '新增用户'] },
    xAxis: {
      type: 'category',
      data: ['1/10', '1/11', '1/12', '1/13', '1/14', '1/15', '1/16'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '日活用户',
        type: 'line',
        smooth: true,
        data: [0, 0, 0, 0, 0, 0, 0],
        itemStyle: { color: '#3b82f6' },
      },
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: [0, 0, 0, 0, 0, 0, 0],
        itemStyle: { color: '#10b981' },
      },
    ],
  }

  // 消息量分析
  const messageVolumeOption = {
    title: { text: '消息量统计' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['00:00', '04:00', '08:00', '12:00', '16:00', '20:00', '24:00'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '消息量',
        type: 'bar',
        data: [0, 0, 0, 0, 0, 0, 0],
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#60a5fa' },
            { offset: 1, color: '#3b82f6' },
          ]),
        },
      },
    ],
  }

  // 设备分布饼图
  const deviceDistributionOption = {
    title: { text: '用户设备分布' },
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left' },
    series: [
      {
        name: '设备类型',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: false,
        data: [
          { value: 0, name: 'Android', itemStyle: { color: '#10b981' } },
          { value: 0, name: 'Web浏览器', itemStyle: { color: '#3b82f6' } },
          { value: 0, name: 'iOS', itemStyle: { color: '#8b5cf6' } },
        ],
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

  // 存储空间使用
  const storageUsageOption = {
    title: { text: '存储空间使用情况（GB）' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['图片', '视频', '语音', '文件', '其他'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '存储空间',
        type: 'bar',
        data: [0, 0, 0, 0, 0],
        itemStyle: { color: '#f59e0b' },
      },
    ],
  }

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>数据分析</h1>
      
      {/* 筛选器 */}
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

      {/* 图表展示 */}
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
        <Col xs={24} lg={12}>
          <Card>
            <ReactECharts option={storageUsageOption} style={{ height: '24rem' }} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default DataAnalysis

