import api from './api';
import { User, LoginRequest, LoginResponse } from '../types/user';

class AuthService {
  /**
   * 用户登录
   */
  async login(data: LoginRequest): Promise<LoginResponse> {
    console.log('[AuthService] 发送登录请求:', data);
    const response = await api.post<LoginResponse>('/auth/login', data);
    console.log('[AuthService] 收到响应:', response);
    console.log('[AuthService] Token:', response.token);
    console.log('[AuthService] User:', response.user);
    
    if (response.token) {
      console.log('[AuthService] 保存token到localStorage');
      localStorage.setItem('token', response.token);
      localStorage.setItem('user', JSON.stringify(response.user));
      console.log('[AuthService] 登录成功，返回数据');
      return response;
    }
    console.error('[AuthService] 响应格式错误，缺少token');
    throw new Error('登录失败：响应数据不完整');
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
    const response = await api.post<{ user: User }>('/auth/register', data);
    return response;
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
    const response = await api.post<{ token: string }>('/auth/refresh');
    if (response.token) {
      localStorage.setItem('token', response.token);
    }
    return response;
  }
}

export default new AuthService();

