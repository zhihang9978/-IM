import { useState, useEffect } from 'react'
import { Card, Table, Button, Input, Select, Space, Tag, Progress, message as antdMessage, Modal } from 'antd'
import { SearchOutlined, DeleteOutlined, ExclamationCircleOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import api from '../../services/api'

const { Option } = Select

interface FileItem {
  id: number
  content: string
  type: string
  file_size?: number
  sender_id: number
  sender?: {
    name: string
  }
  created_at: string
}

interface StorageStats {
  total_files: number
  total_storage: number
  used_storage: number
  free_storage: number
  usage_percent: number
}

function FileManagement() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [fileType, setFileType] = useState<string>()
  const [storageStats, setStorageStats] = useState<StorageStats | null>(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  useEffect(() => {
    loadFiles()
    loadStorageStats()
  }, [pagination.current, pagination.pageSize])

  const loadFiles = async () => {
    setLoading(true)
    try {
      const response = await api.get('/admin/files', {
        params: {
          page: pagination.current,
          page_size: pagination.pageSize,
          keyword: searchKeyword,
          type: fileType,
        },
      })
      setFiles(response.list || [])
      setPagination(prev => ({
        ...prev,
        total: response.total || 0,
      }))
    } catch (error) {
      console.error('Failed to load files:', error)
      antdMessage.error('加载文件列表失败')
    } finally {
      setLoading(false)
    }
  }

  const loadStorageStats = async () => {
    try {
      const data = await api.get('/admin/storage/stats')
      setStorageStats(data)
    } catch (error) {
      console.error('Failed to load storage stats:', error)
    }
  }

  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }))
    loadFiles()
  }

  const handleDelete = (fileId: number) => {
    Modal.confirm({
      title: '确认删除',
      icon: <ExclamationCircleOutlined />,
      content: '确定要删除这个文件吗？此操作不可恢复。',
      okText: '确认',
      cancelText: '取消',
      onOk: async () => {
        try {
          await api.delete(`/admin/files/${fileId}`)
          antdMessage.success('文件已删除')
          loadFiles()
          loadStorageStats()
        } catch (error) {
          console.error('Failed to delete file:', error)
          antdMessage.error('删除文件失败')
        }
      },
    })
  }

  // 格式化文件大小
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
  }

  const columns: ColumnsType<FileItem> = [
    {
      title: '文件ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
    },
    {
      title: '文件信息',
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
      width: 250,
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => {
        const typeMap: any = {
          image: { color: 'blue', text: '图片' },
          video: { color: 'purple', text: '视频' },
          voice: { color: 'green', text: '语音' },
          file: { color: 'orange', text: '文件' },
        }
        const t = typeMap[type] || { color: 'default', text: type }
        return <Tag color={t.color}>{t.text}</Tag>
      },
    },
    {
      title: '大小',
      dataIndex: 'file_size',
      key: 'file_size',
      width: 120,
      render: (size: number) => size ? formatFileSize(size) : '-',
    },
    {
      title: '上传者',
      dataIndex: 'sender',
      key: 'sender',
      width: 150,
      render: (sender: any, record: FileItem) => sender?.name || `用户${record.sender_id}`,
    },
    {
      title: '上传时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 120,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            删除
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>文件管理</h1>
      
      <Card>
        {storageStats && (
          <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f8fafc', borderRadius: '0.5rem' }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <span style={{ fontWeight: 500 }}>存储空间使用</span>
              <Progress percent={Math.round(storageStats.usage_percent)} strokeColor="#3b82f6" />
              <span style={{ fontSize: '0.875rem', color: '#6b7280' }}>
                已使用 {formatFileSize(storageStats.used_storage)} / 总共 {formatFileSize(storageStats.total_storage)}
              </span>
            </Space>
          </div>
        )}

        {/* 搜索和筛选 */}
        <Space style={{ marginBottom: '1rem' }} size="middle">
          <Input
            placeholder="搜索文件名"
            prefix={<SearchOutlined />}
            style={{ width: '16rem' }}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            allowClear
          />
          
          <Select
            placeholder="文件类型"
            style={{ width: '10rem' }}
            value={fileType}
            onChange={setFileType}
            allowClear
          >
            <Option value="image">图片</Option>
            <Option value="video">视频</Option>
            <Option value="audio">音频</Option>
            <Option value="document">文档</Option>
          </Select>
          
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={files}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1200 }}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个文件`,
            onChange: (page, pageSize) => {
              setPagination(prev => ({ ...prev, current: page, pageSize }))
            },
          }}
        />
      </Card>
    </div>
  )
}

export default FileManagement

