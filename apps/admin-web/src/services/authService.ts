import api from './api';
import { User, LoginRequest, LoginResponse } from '../types/user';

class AuthService {
  /**
   * 用户登录
   */
  async login(data: LoginRequest): Promise<LoginResponse> {
    console.log('[AuthService] 发送登录请求:', data);
    const response = await api.post<any>('/auth/login', data);
    console.log('[AuthService] 收到响应:', response);
    console.log('[AuthService] response.data:', response.data);
    console.log('[AuthService] response.data.token:', response.data?.token);
    
    if (response.data && response.data.token) {
      console.log('[AuthService] 保存token到localStorage');
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('user', JSON.stringify(response.data.user));
      console.log('[AuthService] 登录成功，返回数据');
      return response.data;
    }
    console.error('[AuthService] 响应格式错误:', response);
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

