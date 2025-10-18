import { useState, useEffect } from 'react'
import { Card, Table, Button, Input, DatePicker, Select, Space, Tag, message as antdMessage } from 'antd'
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import api from '../../services/api'

const { RangePicker } = DatePicker
const { Option } = Select

interface Message {
  id: number
  sender_id: number
  receiver_id: number
  sender_name?: string
  receiver_name?: string
  content: string
  type: string
  status: string
  created_at: string
}

function MessageManagement() {
  const [messages, setMessages] = useState<Message[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [messageType, setMessageType] = useState<string>()
  const [dateRange, setDateRange] = useState<any>()

  useEffect(() => {
    loadMessages()
  }, [])

  const loadMessages = async () => {
    setLoading(true)
    try {
      const data = await api.get('/admin/messages')
      setMessages(data || [])
    } catch (error) {
      console.error('Failed to load messages:', error)
      antdMessage.error('加载消息列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    loadMessages()
  }

  const handleExport = async () => {
    try {
      const data = await api.get('/admin/messages/export')
      const blob = new Blob([JSON.stringify(data, null, 2)], { type: 'application/json' })
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `messages_${dayjs().format('YYYYMMDD_HHmmss')}.json`
      a.click()
      window.URL.revokeObjectURL(url)
      antdMessage.success('导出成功')
    } catch (error) {
      console.error('Export failed:', error)
      antdMessage.error('导出失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<Message> = [
    {
      title: '消息ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
    },
    {
      title: '发送者',
      dataIndex: 'sender_name',
      key: 'sender_name',
      width: 150,
      render: (name: string, record: Message) => name || `用户${record.sender_id}`,
    },
    {
      title: '接收者',
      dataIndex: 'receiver_name',
      key: 'receiver_name',
      width: 150,
      render: (name: string, record: Message) => name || `用户${record.receiver_id}`,
    },
    {
      title: '消息内容',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      width: 300,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: string) => {
        const typeMap: any = {
          text: { color: 'default', text: '文本' },
          image: { color: 'blue', text: '图片' },
          voice: { color: 'green', text: '语音' },
          video: { color: 'purple', text: '视频' },
          file: { color: 'orange', text: '文件' },
        }
        const t = typeMap[type] || typeMap.text
        return <Tag color={t.color}>{t.text}</Tag>
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const statusMap: any = {
          sent: { color: 'processing', text: '已发送' },
          delivered: { color: 'success', text: '已送达' },
          read: { color: 'success', text: '已读' },
          recalled: { color: 'default', text: '已撤回' },
        }
        const s = statusMap[status] || statusMap.sent
        return <Tag color={s.color}>{s.text}</Tag>
      },
    },
    {
      title: '发送时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>消息管理</h1>
      
      <Card>
        {/* 搜索和筛选栏 */}
        <Space style={{ marginBottom: '1rem', width: '100%', flexWrap: 'wrap' }} size="middle">
          <Input
            placeholder="搜索用户名或消息内容"
            prefix={<SearchOutlined />}
            style={{ width: '16rem' }}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            allowClear
          />
          
          <Select
            placeholder="消息类型"
            style={{ width: '10rem' }}
            value={messageType}
            onChange={setMessageType}
            allowClear
          >
            <Option value="text">文本</Option>
            <Option value="image">图片</Option>
            <Option value="voice">语音</Option>
            <Option value="video">视频</Option>
            <Option value="file">文件</Option>
          </Select>
          
          <RangePicker
            style={{ width: '20rem' }}
            value={dateRange}
            onChange={setDateRange}
          />
          
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
          
          <Button icon={<DownloadOutlined />} onClick={handleExport}>
            导出
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={messages}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条消息`,
          }}
        />
      </Card>
    </div>
  )
}

export default MessageManagement

