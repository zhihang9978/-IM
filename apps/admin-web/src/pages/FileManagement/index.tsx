import { useState } from 'react'
import { Card, Table, Button, Input, Select, Space, Tag, Progress } from 'antd'
import { FileOutlined, SearchOutlined, DownloadOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

const { Option } = Select

interface FileItem {
  id: number;
  name: string;
  type: string;
  size: number;
  uploader: string;
  url: string;
  created_at: string;
}

function FileManagement() {
  const [files, setFiles] = useState<FileItem[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [fileType, setFileType] = useState<string>()

  // 格式化文件大小
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
  }

  // 表格列定义
  const columns: ColumnsType<FileItem> = [
    {
      title: '文件名',
      dataIndex: 'name',
      key: 'name',
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
          audio: { color: 'green', text: '音频' },
          document: { color: 'orange', text: '文档' },
          other: { color: 'default', text: '其他' },
        }
        const t = typeMap[type] || typeMap.other
        return <Tag color={t.color}>{t.text}</Tag>
      },
    },
    {
      title: '大小',
      dataIndex: 'size',
      key: 'size',
      width: 120,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: '上传者',
      dataIndex: 'uploader',
      key: 'uploader',
      width: 150,
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
      width: 150,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
            onClick={() => window.open(record.url)}
          >
            下载
          </Button>
          <Button
            type="link"
            size="small"
            danger
            icon={<DeleteOutlined />}
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
        {/* 存储统计 */}
        <div style={{ marginBottom: '1.5rem', padding: '1rem', background: '#f8fafc', borderRadius: '0.5rem' }}>
          <Space direction="vertical" style={{ width: '100%' }}>
            <span style={{ fontWeight: 500 }}>存储空间使用</span>
            <Progress percent={45} strokeColor="#3b82f6" />
            <span style={{ fontSize: '0.875rem', color: '#6b7280' }}>
              已使用 45.2 GB / 总共 100 GB
            </span>
          </Space>
        </div>

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
          
          <Button type="primary" icon={<SearchOutlined />}>
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
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个文件`,
          }}
        />
      </Card>
    </div>
  )
}

export default FileManagement

