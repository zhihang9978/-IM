import api from './api';
import { User, UserListResponse, UpdateUserRequest } from '../types/user';

class UserService {
  /**
   * 获取用户列表
   */
  async getUsers(page: number = 1, pageSize: number = 20, filters?: {
    status?: string;
    role?: string;
  }): Promise<UserListResponse> {
    const params: any = { page, page_size: pageSize };
    if (filters?.status) params.status = filters.status;
    if (filters?.role) params.role = filters.role;
    
    return api.get('/admin/users', { params });
  }

  /**
   * 搜索用户
   */
  async searchUsers(keyword: string, page: number = 1, pageSize: number = 20): Promise<UserListResponse> {
    return api.get('/users/search', {
      params: { keyword, page, page_size: pageSize }
    });
  }

  /**
   * 获取用户详情
   */
  async getUserById(id: number): Promise<User> {
    return api.get(`/users/${id}`);
  }

  /**
   * 添加用户
   */
  async createUser(data: {
    username: string;
    password: string;
    phone?: string;
    email?: string;
    role?: string;
  }): Promise<{ user: User }> {
    return api.post('/admin/users', data);
  }

  /**
   * 更新用户信息
   */
  async updateUser(id: number, data: UpdateUserRequest): Promise<User> {
    return api.put(`/admin/users/${id}`, data);
  }

  /**
   * 删除用户
   */
  async deleteUser(id: number): Promise<void> {
    return api.delete(`/admin/users/${id}`);
  }

  /**
   * 封禁用户
   */
  async banUser(id: number, reason: string): Promise<void> {
    return api.post(`/admin/users/${id}/ban`, { reason });
  }

  /**
   * 解封用户
   */
  async unbanUser(id: number): Promise<void> {
    return api.post(`/admin/users/${id}/unban`);
  }

  /**
   * 导出用户数据
   */
  async exportUsers(): Promise<Blob> {
    return api.get('/admin/users/export', {
      responseType: 'blob'
    });
  }
}

export default new UserService();

