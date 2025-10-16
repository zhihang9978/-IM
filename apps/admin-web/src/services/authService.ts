import api from './api';
import { User, LoginRequest, LoginResponse } from '../types/user';

class AuthService {
  /**
   * 用户登录
   */
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<any>('/auth/login', data);
    if (response.data && response.data.token) {
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(response.data.user));
      return response.data;
    }
    throw new Error(response.message || '登录失败');
  }

  /**
   * 用户注册
   */
  async register(data: {
    username: string;
    password: string;
    phone?: string;
    email?: string;
  }): Promise<{ user: User }> {
    const response = await api.post<any>('/auth/register', data);
    if (response.data) {
      return response.data;
    }
    throw new Error(response.message || '注册失败');
  }

  /**
   * 退出登录
   */
  logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    window.location.href = '/login';
  }

  /**
   * 获取当前用户信息
   */
  getCurrentUser(): User | null {
    const userStr = localStorage.getItem('user');
    if (userStr) {
      try {
        return JSON.parse(userStr);
      } catch (e) {
        return null;
      }
    }
    return null;
  }

  /**
   * 检查是否已登录
   */
  isAuthenticated(): boolean {
    return !!localStorage.getItem('token');
  }

  /**
   * 刷新Token
   */
  async refreshToken(): Promise<{ token: string }> {
    const response = await api.post<any>('/auth/refresh');
    if (response.data && response.data.token) {
      localStorage.setItem('token', response.data.token);
      return response.data;
    }
    throw new Error(response.message || 'Token刷新失败');
  }
}

export default new AuthService();

