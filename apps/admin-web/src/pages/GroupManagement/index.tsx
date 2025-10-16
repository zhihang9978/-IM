import { useState, useEffect } from 'react'
import { Card, Table, Button, Space, Tag, Modal, Form, Input, message, Popconfirm } from 'antd'
import { TeamOutlined, UserOutlined, DeleteOutlined, EyeOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import groupService from '../../services/groupService'
import { Group } from '../../types/group'
import dayjs from 'dayjs'

function GroupManagement() {
  const [groups, setGroups] = useState<Group[]>([])
  const [loading, setLoading] = useState(false)
  const [membersVisible, setMembersVisible] = useState(false)
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null)
  const [members, setMembers] = useState<any[]>([])

  // 加载群组列表
  const loadGroups = async () => {
    setLoading(true)
    try {
      const response = await groupService.getGroups()
      setGroups(response.groups)
    } catch (error: any) {
      message.error(error?.message || '加载群组列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 查看群成员
  const handleViewMembers = async (group: Group) => {
    setSelectedGroup(group)
    try {
      const response = await groupService.getMembers(group.id)
      setMembers(response.members)
      setMembersVisible(true)
    } catch (error: any) {
      message.error(error?.message || '加载群成员失败')
    }
  }

  // 解散群组
  const handleDisbandGroup = async (group: Group) => {
    try {
      await groupService.disbandGroup(group.id, '管理员操作')
      message.success('群组已解散')
      loadGroups()
    } catch (error: any) {
      message.error(error?.message || '解散群组失败')
    }
  }

  useEffect(() => {
    loadGroups()
  }, [])

  // 群组表格列定义
  const columns: ColumnsType<Group> = [
    {
      title: '群ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '群名称',
      dataIndex: 'name',
      key: 'name',
      width: 200,
    },
    {
      title: '群主',
      dataIndex: ['owner', 'username'],
      key: 'owner',
      width: 150,
      render: (_, record) => record.owner?.username || '-',
    },
    {
      title: '成员数',
      dataIndex: 'member_count',
      key: 'member_count',
      width: 100,
      render: (count: number, record: Group) => (
        <span>
          <UserOutlined /> {count}/{record.max_members}
        </span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => (
        <Tag color={status === 'active' ? 'success' : 'default'}>
          {status === 'active' ? '正常' : '已解散'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
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
            icon={<EyeOutlined />}
            onClick={() => handleViewMembers(record)}
          >
            查看成员
          </Button>
          {record.status === 'active' && (
            <Popconfirm
              title="确定解散此群组吗？"
              onConfirm={() => handleDisbandGroup(record)}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="link"
                size="small"
                danger
                icon={<DeleteOutlined />}
              >
                解散
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ]

  // 群成员表格列定义
  const memberColumns: ColumnsType<any> = [
    {
      title: '用户名',
      dataIndex: ['user', 'username'],
      key: 'username',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role: string) => {
        const roleMap = {
          owner: { color: 'red', text: '群主' },
          admin: { color: 'blue', text: '管理员' },
          member: { color: 'default', text: '成员' },
        }
        const r = roleMap[role as keyof typeof roleMap] || roleMap.member
        return <Tag color={r.color}>{r.text}</Tag>
      },
    },
    {
      title: '加入时间',
      dataIndex: 'joined_at',
      key: 'joined_at',
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>群聊管理</h1>
      
      <Card>
        <div style={{ marginBottom: '1rem' }}>
          <Space>
            <TeamOutlined style={{ fontSize: '1.25rem', color: '#3b82f6' }} />
            <span style={{ fontSize: '1rem', fontWeight: 500 }}>共 {groups.length} 个群组</span>
          </Space>
        </div>

        <Table
          columns={columns}
          dataSource={groups}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1200 }}
          pagination={{
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 个群组`,
          }}
        />
      </Card>

      {/* 群成员弹窗 */}
      <Modal
        title={`${selectedGroup?.name} - 群成员列表`}
        open={membersVisible}
        onCancel={() => setMembersVisible(false)}
        footer={null}
        width="50rem"
      >
        <Table
          columns={memberColumns}
          dataSource={members}
          rowKey="id"
          pagination={false}
        />
      </Modal>
    </div>
  )
}

export default GroupManagement

