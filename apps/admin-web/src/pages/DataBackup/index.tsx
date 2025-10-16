import { useState } from 'react'
import { Card, Table, Button, Space, Tag, message, Alert } from 'antd'
import { DatabaseOutlined, DownloadOutlined, ReloadOutlined, PlayCircleOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'

interface BackupRecord {
  id: number;
  filename: string;
  size: number;
  type: string;
  status: string;
  created_at: string;
}

function DataBackup() {
  const [backups, setBackups] = useState<BackupRecord[]>([])
  const [loading, setLoading] = useState(false)

  // 格式化文件大小
  const formatFileSize = (bytes: number) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return (bytes / Math.pow(k, i)).toFixed(2) + ' ' + sizes[i]
  }

  // 立即备份
  const handleBackupNow = async () => {
    try {
      message.loading('正在执行备份...', 0)
      // TODO: 调用备份API
      setTimeout(() => {
        message.destroy()
        message.success('备份完成')
      }, 2000)
    } catch (error: any) {
      message.error(error?.message || '备份失败')
    }
  }

  // 表格列定义
  const columns: ColumnsType<BackupRecord> = [
    {
      title: '备份文件',
      dataIndex: 'filename',
      key: 'filename',
      ellipsis: true,
      width: 300,
    },
    {
      title: '大小',
      dataIndex: 'size',
      key: 'size',
      width: 120,
      render: (size: number) => formatFileSize(size),
    },
    {
      title: '类型',
      dataIndex: 'type',
      key: 'type',
      width: 120,
      render: (type: string) => (
        <Tag color={type === 'auto' ? 'blue' : 'green'}>
          {type === 'auto' ? '自动备份' : '手动备份'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'success' ? 'success' : 'error'}>
          {status === 'success' ? '成功' : '失败'}
        </Tag>
      ),
    },
    {
      title: '备份时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'actions',
      fixed: 'right',
      width: 200,
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            size="small"
            icon={<DownloadOutlined />}
          >
            下载
          </Button>
          <Button
            type="link"
            size="small"
            icon={<ReloadOutlined />}
          >
            恢复
          </Button>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>数据备份</h1>
      
      <Alert
        message="自动备份策略"
        description="系统每天凌晨2点自动备份数据库，保留最近7天的备份文件。"
        type="info"
        showIcon
        style={{ marginBottom: '1rem' }}
      />

      <Card>
        <div style={{ marginBottom: '1rem' }}>
          <Space>
            <Button
              type="primary"
              icon={<PlayCircleOutlined />}
              onClick={handleBackupNow}
            >
              立即备份
            </Button>
            <span style={{ color: '#6b7280' }}>
              上次自动备份: {dayjs().subtract(10, 'hour').format('YYYY-MM-DD HH:mm:ss')}
            </span>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={backups}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1100 }}
          pagination={{
            showSizeChanger: true,
            showTotal: (total) => `共 ${total} 个备份`,
          }}
        />
      </Card>
    </div>
  )
}

export default DataBackup

