#!/bin/bash

#===============================================================================
# Kafka Docker部署脚本（使用KRaft模式，无需ZooKeeper）
# 用途: 在服务器上快速部署Kafka消息队列
#===============================================================================

echo "开始部署Kafka消息队列..."

# 创建数据目录
mkdir -p /data/kafka

# 部署Kafka（KRaft模式）
docker run -d \
  --name kafka \
  -p 9092:9092 \
  -e KAFKA_NODE_ID=1 \
  -e KAFKA_PROCESS_ROLES=broker,controller \
  -e KAFKA_LISTENERS=PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093 \
  -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 \
  -e KAFKA_CONTROLLER_LISTENER_NAMES=CONTROLLER \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT \
  -e KAFKA_CONTROLLER_QUORUM_VOTERS=1@localhost:9093 \
  -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
  -e KAFKA_TRANSACTION_STATE_LOG_MIN_ISR=1 \
  -e KAFKA_LOG_DIRS=/var/lib/kafka/data \
  -v /data/kafka:/var/lib/kafka/data \
  --restart=always \
  apache/kafka:latest

echo "等待Kafka启动（30秒）..."
sleep 30

# 验证Kafka是否运行
if docker ps | grep -q kafka; then
  echo "✅ Kafka容器运行中"
else
  echo "❌ Kafka启动失败"
  exit 1
fi

# 创建Topic
echo "创建Topic: lanxin_message"
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic lanxin_message \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

echo "创建Topic: lanxin_notification"
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --create \
  --topic lanxin_notification \
  --bootstrap-server localhost:9092 \
  --partitions 3 \
  --replication-factor 1

# 列出所有Topic
echo "当前Topic列表:"
docker exec kafka /opt/kafka/bin/kafka-topics.sh \
  --list \
  --bootstrap-server localhost:9092

echo "✅ Kafka部署完成！"
echo "Kafka地址: localhost:9092"
echo "Topic: lanxin_message, lanxin_notification"

