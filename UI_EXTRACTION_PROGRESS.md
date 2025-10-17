# 野火IM UI提取进度记录

## 提取时间
2025-10-17

## 野火IM路径
`android-chat-master/uikit/`

## 已提取资源

### Drawable资源（气泡背景）
- ✅ `img_bubble_send.9.png` - 发送消息气泡（9-patch）
- ✅ `img_bubble_receive.9.png` - 接收消息气泡（9-patch）
- ✅ `audio_animation_right_wf.xml` - 语音播放动画（适配）
- ✅ `shape_message_ref_bg.xml` - 引用消息背景

### 核心发现

#### 野火IM设计亮点
1. **消息气泡**：
   - 使用9-patch图片（可自动拉伸）
   - 发送消息：浅蓝色气泡 (#A8BDFF)
   - 接收消息：白色气泡
   - 最大宽度：240dp
   - 内边距：16dp左右，12dp上下
   - 字体：16sp

2. **语音消息**：
   - 固定宽度：100dp
   - 固定高度：50dp
   - 带动画效果（3帧切换）
   - 时长显示在气泡外

3. **颜色系统**：
   - 主色：#3B62E0（蓝色）
   - 强调色：#F95569（红色）
   - 成功色：#45C01A（绿色）
   - 文字：#1D1D1D（主）/ #B3B3B3（次）
   - 背景：#EDEDED

4. **输入面板**：
   - 灰色背景：#F8F8F8
   - 包含：菜单、语音、输入框、表情、更多
   - 圆角输入框：shape_session_text_input

#### 与蓝信当前设计对比

| 项目 | 蓝信当前 | 野火IM | 改进点 |
|------|---------|--------|--------|
| 气泡背景 | 纯色shape | 9-patch图片 | ✅ 更精美，有阴影和尾巴 |
| 气泡宽度 | 自适应 | 最大240dp | ✅ 更规范 |
| 字体大小 | 混合 | 统一16sp | ✅ 更统一 |
| 内边距 | 不统一 | 16dp/12dp | ✅ 更规范 |
| 颜色系统 | 基础 | 完整层级 | ✅ 更专业 |
| 链接识别 | 无 | autoLink | ✅ 自动识别 |

## 下一步计划

### 立即执行（优先级P0）
1. ✅ 提取9-patch气泡背景
2. ⏳ 提取颜色配置（合并到蓝信）
3. ⏳ 创建新的消息气泡布局
4. ⏳ 更新ChatAdapter使用新布局

### 后续执行（优先级P1）
5. 提取输入面板布局
6. 提取扩展功能面板
7. 提取语音/图片/视频布局
8. 全面测试

## 资源清单（待提取）

### Drawable - 需要提取
- [ ] audio_animation_left_list.xml
- [ ] shape_session_text_input.xml
- [ ] shape_session_btn_send.xml
- [ ] ic_chat相关图标

### Layout - 需要提取
- [ ] conversation_item_text_send.xml
- [ ] conversation_item_text_receive.xml
- [ ] conversation_item_audio_send.xml
- [ ] conversation_item_audio_receive.xml
- [ ] conversation_item_image_send.xml
- [ ] conversation_item_video_send.xml
- [ ] conversation_item_file_send.xml
- [ ] conversation_input_panel.xml
- [ ] conversation_ext_layout.xml

### Values - 需要合并
- [ ] colors.xml（部分合并）
- [ ] dimens.xml（部分合并）

## 提取原则
✅ 添加Apache 2.0来源注释
✅ 保留蓝信命名规范
✅ 保留蓝信业务逻辑
✅ 渐进式替换（先创建新文件）

