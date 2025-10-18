import { useState, useEffect } from 'react'
import { Card, Table, Button, Input, Space, Tag, message } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import dayjs from 'dayjs'
import api from '../../services/api'

interface Group {
  id: number
  name: string
  owner_id: number
  owner?: {
    name: string
  }
  member_count: number
  created_at: string
}

function GroupManagement() {
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(false)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  useEffect(() => {
    loadGroups()
  }, [pagination.current, pagination.pageSize])

  const loadGroups = async () => {
    setLoading(true)
    try {
      const response = await api.get('/admin/groups', {
        params: {
          page: pagination.current,
          page_size: pagination.pageSize,
          keyword: searchKeyword,
        },
      })
      setGroups(response.list || [])
      setPagination(prev => ({
        ...prev,
        total: response.total || 0,
      }))
    } catch (error) {
      console.error('Failed to load groups:', error)
      message.error('加载群组列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }))
    loadGroups()
  }

  const columns: ColumnsType<Group> = [
    {
      title: '群组ID',
      dataIndex: 'id',
      key: 'id',
      width: 100,
    },
    {
      title: '群组名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '群主',
      dataIndex: 'owner',
      key: 'owner',
      width: 150,
      render: (owner: any, record: Group) => owner?.name || `用户${record.owner_id}`,
    },
    {
      title: '成员数量',
      dataIndex: 'member_count',
      key: 'member_count',
      width: 120,
      render: (count: number) => <Tag color="blue">{count}人</Tag>,
    },
    {
      title: '创建时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>群聊管理</h1>
      
      <Card>
        <Space style={{ marginBottom: '1rem' }} size="middle">
          <Input
            placeholder="搜索群组名称"
            prefix={<SearchOutlined />}
            style={{ width: '16rem' }}
            value={searchKeyword}
            onChange={(e) => setSearchKeyword(e.target.value)}
            onPressEnter={handleSearch}
            allowClear
          />
          
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
            搜索
          </Button>
        </Space>

        <Table
          columns={columns}
          dataSource={groups}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个群组`,
            onChange: (page, pageSize) => {
              setPagination(prev => ({ ...prev, current: page, pageSize }))
            },
          }}
        />
      </Card>
    </div>
  )
}

export default GroupManagement

