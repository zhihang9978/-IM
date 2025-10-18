
DROP INDEX IF EXISTS `idx_messages_sender_created` ON `messages`;
DROP INDEX IF EXISTS `idx_messages_conversation_created` ON `messages`;
DROP INDEX IF EXISTS `idx_messages_group_created` ON `messages`;
DROP INDEX IF EXISTS `idx_messages_status` ON `messages`;

DROP INDEX IF EXISTS `idx_contacts_user_status` ON `contacts`;

DROP INDEX IF EXISTS `idx_conversations_user1_type` ON `conversations`;
DROP INDEX IF EXISTS `idx_conversations_user2_type` ON `conversations`;
DROP INDEX IF EXISTS `idx_conversations_updated` ON `conversations`;

DROP INDEX IF EXISTS `idx_groups_owner_status` ON `groups`;
DROP INDEX IF EXISTS `idx_groups_created` ON `groups`;

DROP INDEX IF EXISTS `idx_group_members_user_group` ON `group_members`;

DROP INDEX IF EXISTS `idx_operation_logs_created` ON `operation_logs`;
DROP INDEX IF EXISTS `idx_operation_logs_action` ON `operation_logs`;
DROP INDEX IF EXISTS `idx_operation_logs_user_created` ON `operation_logs`;

DROP INDEX IF EXISTS `idx_friend_requests_receiver_status` ON `friend_requests`;
DROP INDEX IF EXISTS `idx_friend_requests_sender_status` ON `friend_requests`;
