import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

class ApiClient {
  private instance: AxiosInstance;

  constructor() {
    this.instance = axios.create({
      baseURL: API_BASE_URL,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors() {
    // 请求拦截器 - 添加JWT Token
    this.instance.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // 响应拦截器 - 处理错误和解包后端标准格式
    this.instance.interceptors.response.use(
      (response: AxiosResponse) => {
        // 后端标准响应格式: { code: 0, message: "success", data: {...} }
        const { code, message, data } = response.data;
        
        if (code === 0) {
          // 成功，返回data部分
          return data;
        } else {
          // 业务错误
          return Promise.reject({ code, message });
        }
      },
      (error) => {
        if (error.response) {
          // 服务器返回错误状态码
          const { status, data } = error.response;
          
          if (status === 401) {
            // Token过期或无效，清除token并跳转登录
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            window.location.href = '/login';
          }
          
          return Promise.reject(data || { message: 'Request failed' });
        } else if (error.request) {
          // 请求已发出但没有收到响应
          return Promise.reject({ message: 'No response from server' });
        } else {
          // 请求配置出错
          return Promise.reject({ message: error.message });
        }
      }
    );
  }

  public get<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.get(url, config);
  }

  public post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.post(url, data, config);
  }

  public put<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.put(url, data, config);
  }

  public delete<T = any>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.delete(url, config);
  }

  public patch<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<T> {
    return this.instance.patch(url, data, config);
  }
}

export default new ApiClient();

