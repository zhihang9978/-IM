import api from './api';
import { User, LoginRequest, LoginResponse } from '../types/user';

class AuthService {
  /**
   * 用户登录
   */
  async login(data: LoginRequest): Promise<LoginResponse> {
    const response = await api.post<LoginResponse>('/auth/login', data);
    if (response.token) {
      localStorage.setItem('token', response.token);
      localStorage.setItem('user', JSON.stringify(response.user));
    }
    return response;
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
    return api.post('/auth/register', data);
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
    return api.post('/auth/refresh');
  }
}

export default new AuthService();

