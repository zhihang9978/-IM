import { useState, useEffect } from 'react'
import { Card, Table, Button, Input, Select, Space, Tag, Modal, Form, message, Popconfirm } from 'antd'
import { SearchOutlined, PlusOutlined, EditOutlined, DeleteOutlined, ExportOutlined, StopOutlined, CheckCircleOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import userService from '../../services/userService'
import { User } from '../../types/user'
import dayjs from 'dayjs'

const { Option } = Select

function UserManagement() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [searchKeyword, setSearchKeyword] = useState('')
  const [statusFilter, setStatusFilter] = useState<string>()
  const [roleFilter, setRoleFilter] = useState<string>()
  const [modalVisible, setModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState<User | null>(null)
  const [form] = Form.useForm()

  // 加载用户列表
  const loadUsers = async () => {
    setLoading(true)
    try {
      const response = await userService.getUsers(page, pageSize, {
        status: statusFilter,
        role: roleFilter,
      })
      setUsers(response.data)
      setTotal(response.total)
    } catch (error: any) {
      message.error(error?.message || '加载用户列表失败')
    } finally {
      setLoading(false)
    }
  }

  // 搜索用户
  const handleSearch = async () => {
    if (!searchKeyword.trim()) {
      loadUsers()
      return
    }
    
    setLoading(true)
    try {
      const response = await userService.searchUsers(searchKeyword, page, pageSize)
      setUsers(response.data)
      setTotal(response.total)
    } catch (error: any) {
      message.error(error?.message || '搜索失败')
    } finally {
      setLoading(false)
    }
  }

  // 添加/编辑用户
  const handleSave = async (values: any) => {
    try {
      if (editingUser) {
        await userService.updateUser(editingUser.id, values)
        message.success('更新成功')
      } else {
        await userService.createUser(values)
        message.success('添加成功')
      }
      setModalVisible(false)
      form.resetFields()
      setEditingUser(null)
      loadUsers()
    } catch (error: any) {
      message.error(error?.message || '操作失败')
    }
  }

  // 删除用户
  const handleDelete = async (id: number) => {
    try {
      await userService.deleteUser(id)
      message.success('删除成功')
      loadUsers()
    } catch (error: any) {
      message.error(error?.message || '删除失败')
    }
  }

  // 封禁/解封用户
  const handleBan = async (user: User) => {
    try {
      if (user.status === 'banned') {
        await userService.unbanUser(user.id)
        message.success('解封成功')
      } else {
        await userService.banUser(user.id, '管理员操作')
        message.success('封禁成功')
      }
      loadUsers()
    } catch (error: any) {
      message.error(error?.message || '操作失败')
    }
  }

  // 导出用户
  const handleExport = async () => {
    try {
      const blob = await userService.exportUsers()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `users_${dayjs().format('YYYY-MM-DD')}.xlsx`
      a.click()
      message.success('导出成功')
    } catch (error: any) {
      message.error(error?.message || '导出失败')
    }
  }

  // 打开编辑弹窗
  const handleEdit = (user: User) => {
    setEditingUser(user)
    form.setFieldsValue(user)
    setModalVisible(true)
  }

  // 打开添加弹窗
  const handleAdd = () => {
    setEditingUser(null)
    form.resetFields()
    setModalVisible(true)
  }

  useEffect(() => {
    loadUsers()
  }, [page, pageSize, statusFilter, roleFilter])

  // 表格列定义
  const columns: ColumnsType<User> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 80,
    },
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      width: 150,
    },
    {
      title: '蓝信号',
      dataIndex: 'lanxin_id',
      key: 'lanxin_id',
      width: 150,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 150,
    },
    {
      title: '邮箱',
      dataIndex: 'email',
      key: 'email',
      width: 200,
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      width: 100,
      render: (role: string) => (
        <Tag color={role === 'admin' ? 'blue' : 'default'}>
          {role === 'admin' ? '管理员' : '普通用户'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: string) => {
        const statusMap = {
          active: { color: 'success', text: '正常' },
          banned: { color: 'error', text: '已封禁' },
          deleted: { color: 'default', text: '已删除' },
        }
        const s = statusMap[status as keyof typeof statusMap] || statusMap.active
        return <Tag color={s.color}>{s.text}</Tag>
      },
    },
    {
      title: '注册时间',
      dataIndex: 'created_at',
      key: 'created_at',
      width: 180,
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '最后登录',
      dataIndex: 'last_login_at',
      key: 'last_login_at',
      width: 180,
      render: (time: string) => time ? dayjs(time).format('YYYY-MM-DD HH:mm:ss') : '-',
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
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            编辑
          </Button>
          <Button
            type="link"
            size="small"
            danger={record.status !== 'banned'}
            icon={record.status === 'banned' ? <CheckCircleOutlined /> : <StopOutlined />}
            onClick={() => handleBan(record)}
          >
            {record.status === 'banned' ? '解封' : '封禁'}
          </Button>
          <Popconfirm
            title="确定删除此用户吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              size="small"
              danger
              icon={<DeleteOutlined />}
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <h1 style={{ marginBottom: '1.5rem', fontSize: '1.5rem', fontWeight: 600 }}>用户管理</h1>
      
      <Card>
        {/* 搜索和筛选栏 */}
        <Space style={{ marginBottom: '1rem', width: '100%', justifyContent: 'space-between', flexWrap: 'wrap' }} size="middle">
          <Space size="middle" wrap>
            <Input
              placeholder="搜索用户名/手机号/邮箱/蓝信号"
              prefix={<SearchOutlined />}
              style={{ width: '20rem' }}
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onPressEnter={handleSearch}
              allowClear
            />
            
            <Select
              placeholder="状态筛选"
              style={{ width: '10rem' }}
              value={statusFilter}
              onChange={setStatusFilter}
              allowClear
            >
              <Option value="active">正常</Option>
              <Option value="banned">已封禁</Option>
              <Option value="deleted">已删除</Option>
            </Select>
            
            <Select
              placeholder="角色筛选"
              style={{ width: '10rem' }}
              value={roleFilter}
              onChange={setRoleFilter}
              allowClear
            >
              <Option value="user">普通用户</Option>
              <Option value="admin">管理员</Option>
            </Select>
            
            <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>
              搜索
            </Button>
          </Space>
          
          <Space size="middle">
            <Button icon={<ExportOutlined />} onClick={handleExport}>
              导出
            </Button>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
              添加用户
            </Button>
          </Space>
        </Space>

        {/* 用户表格 */}
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            current: page,
            pageSize: pageSize,
            total: total,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
            onChange: (page, pageSize) => {
              setPage(page)
              setPageSize(pageSize)
            },
          }}
        />
      </Card>

      {/* 添加/编辑用户弹窗 */}
      <Modal
        title={editingUser ? '编辑用户' : '添加用户'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false)
          form.resetFields()
          setEditingUser(null)
        }}
        onOk={() => form.submit()}
        width="40rem"
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleSave}
        >
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              label="密码"
              name="password"
              rules={[{ required: true, message: '请输入密码' }]}
            >
              <Input.Password placeholder="请输入密码" />
            </Form.Item>
          )}

          <Form.Item
            label="手机号"
            name="phone"
          >
            <Input placeholder="请输入手机号" />
          </Form.Item>

          <Form.Item
            label="邮箱"
            name="email"
            rules={[{ type: 'email', message: '请输入有效的邮箱' }]}
          >
            <Input placeholder="请输入邮箱" />
          </Form.Item>

          <Form.Item
            label="角色"
            name="role"
            initialValue="user"
          >
            <Select>
              <Option value="user">普通用户</Option>
              <Option value="admin">管理员</Option>
            </Select>
          </Form.Item>

          {editingUser && (
            <Form.Item
              label="状态"
              name="status"
            >
              <Select>
                <Option value="active">正常</Option>
                <Option value="banned">已封禁</Option>
                <Option value="deleted">已删除</Option>
              </Select>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}

export default UserManagement

