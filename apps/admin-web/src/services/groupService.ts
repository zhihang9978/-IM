import api from './api';
import { Group, GroupMember, GroupListResponse, CreateGroupRequest } from '../types/group';

class GroupService {
  /**
   * 获取用户的群组列表
   */
  async getGroups(): Promise<GroupListResponse> {
    return api.get('/groups');
  }

  /**
   * 获取群组详情
   */
  async getGroupById(id: number): Promise<Group> {
    return api.get(`/groups/${id}`);
  }

  /**
   * 创建群组
   */
  async createGroup(data: CreateGroupRequest): Promise<{ group: Group }> {
    return api.post('/groups', data);
  }

  /**
   * 更新群组信息
   */
  async updateGroup(id: number, data: Partial<Group>): Promise<void> {
    return api.put(`/groups/${id}`, data);
  }

  /**
   * 添加群成员
   */
  async addMember(groupId: number, userId: number): Promise<void> {
    return api.post(`/groups/${groupId}/members`, { user_id: userId });
  }

  /**
   * 移除群成员
   */
  async removeMember(groupId: number, memberId: number): Promise<void> {
    return api.delete(`/groups/${groupId}/members/${memberId}`);
  }

  /**
   * 获取群成员列表
   */
  async getMembers(groupId: number): Promise<{ members: GroupMember[] }> {
    return api.get(`/groups/${groupId}/members`);
  }

  /**
   * 解散群组（管理员操作）
   */
  async disbandGroup(groupId: number, reason: string): Promise<void> {
    return api.post(`/admin/groups/${groupId}/disband`, { reason });
  }
}

export default new GroupService();

