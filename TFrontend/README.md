# TFrontend - AI 对话前端

这是一个简洁优雅的AI对话前端应用，用于对接 TBackend 的AI对话服务。

## 技术栈

- React 18
- Vite
- Axios

## 功能特性

- ✨ 简洁美观的用户界面
- 💬 实时AI对话
- 🔄 会话记忆功能
- 📱 响应式设计，支持移动端
- ⚡ 快速响应，流畅的动画效果

## 安装与运行

### 1. 安装依赖

```bash
cd TFrontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

前端将在 `http://localhost:3000` 启动。

### 3. 构建生产版本

```bash
npm run build
```

构建产物将输出到 `dist/` 目录。

## 配置说明

前端通过 Vite 的代理功能与后端通信：

- 前端地址：`http://localhost:3000`
- 后端地址：`http://localhost:8080`
- API路径：`/api/chat/memory`

配置文件：`vite.config.js`

## API接口

### 发送消息

**POST** `/api/chat/memory`

请求体：
```json
{
  "conversationId": "可选的会话ID",
  "message": "用户消息内容"
}
```

响应：
```json
{
  "conversationId": "会话ID",
  "answer": "AI回复内容"
}
```

## 项目结构

```
TFrontend/
├── public/           # 静态资源
├── src/
│   ├── components/   # 组件（预留）
│   ├── services/     # API服务
│   │   └── ChatService.js
│   ├── App.jsx       # 主应用组件
│   ├── App.css       # 应用样式
│   ├── main.jsx      # 入口文件
│   └── index.css     # 全局样式
├── index.html        # HTML模板
├── vite.config.js    # Vite配置
└── package.json      # 项目配置
```

## 使用说明

1. 确保后端服务（TBackend）已在 `http://localhost:8080` 运行
2. 启动前端服务
3. 在浏览器中访问 `http://localhost:3000`
4. 开始与AI对话！

点击右上角的"新对话"按钮可以开始新的对话会话。
