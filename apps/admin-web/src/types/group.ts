export interface Group {
  id: number;
  name: string;
  avatar: string;
  owner_id: number;
  description: string;
  member_count: number;
  max_members: number;
  status: 'active' | 'disbanded';
  created_at: string;
  updated_at: string;
  owner?: {
    id: number;
    username: string;
    avatar: string;
  };
}

export interface GroupMember {
  id: number;
  group_id: number;
  user_id: number;
  role: 'owner' | 'admin' | 'member';
  nickname: string;
  muted: boolean;
  joined_at: string;
  user?: {
    id: number;
    username: string;
    avatar: string;
  };
}

export interface CreateGroupRequest {
  name: string;
  description?: string;
  member_ids?: number[];
}

export interface GroupListResponse {
  groups: Group[];
}

