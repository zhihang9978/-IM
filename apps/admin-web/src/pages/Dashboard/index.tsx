import { useState } from "react";
import { Card, Row, Col, Statistic, Tag } from 'antd'
import { UserOutlined, MessageOutlined, TeamOutlined, FileOutlined } from '@ant-design/icons'
import ReactECharts from 'echarts-for-react'

function Dashboard() {
  const [stats] = useState({
    totalUsers: 1128,
    totalMessages: 93489,
    totalGroups: 256,
    totalFiles: 4532,
  })

  // 用户增长趋势图配置
  const userTrendOption = {
    title: { text: '用户增长趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['1月', '2月', '3月', '4月', '5月', '6月', '7月'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '新增用户',
        type: 'line',
        smooth: true,
        data: [120, 200, 150, 180, 220, 250, 300],
        itemStyle: { color: '#3b82f6' },
      },
    ],
  }

  // 消息统计柱状图配置
  const messageStatsOption = {
    title: { text: '消息统计' },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: ['文本', '图片', '语音', '视频', '文件'],
    },
    yAxis: { type: 'value' },
    series: [
      {
        name: '消息数量',
        type: 'bar',
        data: [45000, 25000, 12000, 8000, 3489],
        itemStyle: { color: '#10b981' },
      },
    ],
  }

  // 在线用户分布饼图配置
  const onlineDistributionOption = {
    title: { text: '在线用户分布' },
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '设备类型',
        type: 'pie',
        radius: '50%',
        data: [
          { value: 580, name: 'Android' },
          { value: 420, name: 'iOS' },
          { value: 128, name: 'Web' },
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

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>仪表盘</h1>
      
      {/* 统计卡片 */}
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="总用户数" 
              value={stats.totalUsers} 
              prefix={<UserOutlined />}
              valueStyle={{ color: '#3b82f6' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="消息总数" 
              value={stats.totalMessages} 
              prefix={<MessageOutlined />}
              valueStyle={{ color: '#10b981' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="群组数" 
              value={stats.totalGroups} 
              prefix={<TeamOutlined />}
              valueStyle={{ color: '#f59e0b' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic 
              title="文件总数" 
              value={stats.totalFiles} 
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
              <p><strong>在线用户：</strong> 1,128 人</p>
              <p><strong>服务器域名：</strong> lanxin168.com</p>
              <p><strong>数据库：</strong> MySQL 8.0 (主从架构)</p>
              <p><strong>缓存：</strong> Redis 7.0</p>
              <p><strong>消息队列：</strong> Kafka 3.0</p>
            </div>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Dashboard

