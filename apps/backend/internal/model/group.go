package model

import (
	"time"
	
	"gorm.io/gorm"
)

type Group struct {
	ID          uint      `gorm:"primarykey" json:"id"`
	Name        string    `gorm:"not null;size:100" json:"name"`
	Avatar      string    `gorm:"size:500" json:"avatar"`
	OwnerID     uint      `gorm:"not null;index" json:"owner_id"`
	Description string    `gorm:"type:text" json:"description"`
	MemberCount int       `gorm:"default:0" json:"member_count"`
	MaxMembers  int       `gorm:"default:500" json:"max_members"`
	Status      string    `gorm:"type:enum('active','disbanded');default:'active';index" json:"status"`
	CreatedAt   time.Time `json:"created_at"`
	UpdatedAt   time.Time `json:"updated_at"`
	
	// 关联
	Owner   User          `gorm:"foreignKey:OwnerID" json:"owner,omitempty"`
	Members []GroupMember `gorm:"foreignKey:GroupID" json:"members,omitempty"`
}

func (Group) TableName() string {
	return "groups"
}

type GroupMember struct {
	ID       uint       `gorm:"primarykey" json:"id"`
	GroupID  uint       `gorm:"not null;index" json:"group_id"`
	UserID   uint       `gorm:"not null;index" json:"user_id"`
	Role     string     `gorm:"type:enum('owner','admin','member');default:'member';index" json:"role"`
	Nickname string     `gorm:"size:50" json:"nickname,omitempty"`
	Muted    bool       `gorm:"default:false" json:"muted"`
	JoinedAt time.Time `gorm:"default:CURRENT_TIMESTAMP" json:"joined_at"`
	
	// 关联
	Group Group `gorm:"foreignKey:GroupID" json:"group,omitempty"`
	User  User  `gorm:"foreignKey:UserID" json:"user,omitempty"`
}

func (gm *GroupMember) BeforeCreate(tx *gorm.DB) error {
	if gm.JoinedAt.IsZero() {
		gm.JoinedAt = time.Now()
	}
	return nil
}

func (GroupMember) TableName() string {
	return "group_members"
}

// GroupStatus 常量
const (
	GroupStatusActive    = "active"
	GroupStatusDisbanded = "disbanded"
)

// GroupMemberRole 常量
const (
	GroupMemberRoleOwner  = "owner"
	GroupMemberRoleAdmin  = "admin"
	GroupMemberRoleMember = "member"
)

