-- 添加外键约束: messages.conversation_id → conversations.id
ALTER TABLE `messages` 
ADD CONSTRAINT `fk_messages_conversation` 
  FOREIGN KEY (`conversation_id`) 
  REFERENCES `conversations`(`id`) 
  ON DELETE CASCADE
  ON UPDATE CASCADE;

