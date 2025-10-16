export interface User {
  id: number;
  username: string;
  phone: string;
  email: string;
  avatar: string;
  lanxin_id: string;
  role: 'user' | 'admin';
  status: 'active' | 'banned' | 'deleted';
  last_login_at: string;
  created_at: string;
}

export interface LoginRequest {
  identifier: string; // 用户名/手机号/邮箱/蓝信号
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

export interface UserListResponse {
  total: number;
  page: number;
  page_size: number;
  data: User[];
}

export interface UpdateUserRequest {
  username?: string;
  phone?: string;
  email?: string;
  avatar?: string;
  role?: 'user' | 'admin';
  status?: 'active' | 'banned' | 'deleted';
}

