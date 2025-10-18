
CREATE INDEX IF NOT EXISTS `idx_messages_sender_created` ON `messages` (`sender_id`, `created_at` DESC);
CREATE INDEX IF NOT EXISTS `idx_messages_conversation_created` ON `messages` (`conversation_id`, `created_at` DESC);
CREATE INDEX IF NOT EXISTS `idx_messages_group_created` ON `messages` (`group_id`, `created_at` DESC);
CREATE INDEX IF NOT EXISTS `idx_messages_status` ON `messages` (`status`);

CREATE INDEX IF NOT EXISTS `idx_contacts_user_status` ON `contacts` (`user_id`, `status`);

CREATE INDEX IF NOT EXISTS `idx_conversations_user1_type` ON `conversations` (`user1_id`, `type`);
CREATE INDEX IF NOT EXISTS `idx_conversations_user2_type` ON `conversations` (`user2_id`, `type`);
CREATE INDEX IF NOT EXISTS `idx_conversations_updated` ON `conversations` (`updated_at` DESC);

CREATE INDEX IF NOT EXISTS `idx_groups_owner_status` ON `groups` (`owner_id`, `status`);
CREATE INDEX IF NOT EXISTS `idx_groups_created` ON `groups` (`created_at` DESC);

CREATE INDEX IF NOT EXISTS `idx_group_members_user_group` ON `group_members` (`user_id`, `group_id`);

CREATE INDEX IF NOT EXISTS `idx_operation_logs_created` ON `operation_logs` (`created_at` DESC);
CREATE INDEX IF NOT EXISTS `idx_operation_logs_action` ON `operation_logs` (`action`);
CREATE INDEX IF NOT EXISTS `idx_operation_logs_user_created` ON `operation_logs` (`user_id`, `created_at` DESC);

CREATE INDEX IF NOT EXISTS `idx_friend_requests_receiver_status` ON `friend_requests` (`receiver_id`, `status`, `created_at` DESC);
CREATE INDEX IF NOT EXISTS `idx_friend_requests_sender_status` ON `friend_requests` (`sender_id`, `status`, `created_at` DESC);
