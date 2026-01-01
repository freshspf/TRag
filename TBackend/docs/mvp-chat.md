# TBackend 最小对话 MVP（不含 RAG）

## 目标
- 在现有 **Spring Boot 3.4.5 + Spring AI 1.0.0（GA）** 技术栈上，实现“最基础可用”的对话接口（HTTP API）。
- 先把“对话闭环”跑通：前端/调用方传入用户消息 → 后端调用大模型 → 返回模型回复。

## 非目标（本阶段明确不做）
- 不做 RAG / 向量检索 / Neo4j/Milvus 召回。
- 不做工具调用（Tool/Function Calling）、Agent、工作流编排。
- 不做多模型路由/自动切换供应商。
- 不做流式输出（SSE/WebSocket）、文件上传、多模态。
- 不做鉴权、配额、审计、Prompt 管理后台等“产品化”能力。

## 现状约束（以仓库为准）
- 版本以 `TBackend/pom.xml` 为准：`spring-ai.version=1.0.0`，`spring-ai-alibaba.version=1.0.0.4`。
- 当前 `application.yml` 中 `spring.ai.chat.client.enabled=false`，因此 `ChatClient` 由 `TBackend/src/main/java/com/spf/tbackend/config/MultiChatClientConfigs.java` 手动创建并注入（示例为 `openAiChatClient`）。
- 当前 `TBackend/src/main/java/com/spf/tbackend/controller/ChatController.java` 仅为硬编码示例（没有使用请求入参、也没有多轮上下文）。

## MVP 功能范围（必须做）
1. **临时对话（无记忆）**：用户传入 `message`，返回模型 `answer`（不保存历史）。
2. **记忆对话（最小多轮）**：支持 `conversationId`，同一 `conversationId` 下保留最近 N 轮历史，用于上下文连续性。
3. **基本校验与错误返回**：空消息/超长消息/模型调用失败，返回稳定的错误结构。

## API 设计（MVP 固化，以当前代码为准）

### 1) 记忆对话
- Method: `POST`
- Path: `/api/chat/memory`
- Content-Type: `application/json`

Request Body：
```json
{
  "conversationId": "可选；不传则创建新会话",
  "message": "必填；用户输入"
}
```

Response（成功，HTTP 200）：
```json
{
  "conversationId": "会话ID（不传则服务端生成并返回）",
  "answer": "模型回复"
}
```

Response（失败，HTTP 4xx/5xx）：
```json
{
  "error": {
    "code": "INVALID_ARGUMENT | MODEL_ERROR | INTERNAL_ERROR",
    "message": "面向调用方的简短错误信息"
  }
}
```

### 2) 临时对话（无记忆）
- Method: `GET`
- Path: `/api/chat/temp`
- Query: `message`

Response（成功，HTTP 200）：
- 返回纯字符串（模型回复）

## 对话上下文（最小多轮策略）

### 会话 ID
- `conversationId` 不传：服务端生成（建议 UUID v4 字符串）。
- `conversationId` 传入：直接使用。

### 记忆存储（MVP：内存实现）
- 使用进程内存保存会话历史（`Map<conversationId, List<Message>>`），每次请求取出并拼接上下文。
- 仅保留最近 `maxHistoryTurns` 轮（建议默认 10 轮；实现时做成配置项）。
- 不引入 Redis/JDBC 作为强依赖（避免额外部署与数据结构设计）；后续需要持久化时再替换存储实现。

> 说明：仓库已存在 MySQL/Redis 依赖与配置，但这些属于“环境基础设施”，不代表本 MVP 必须用它们实现对话记忆。

临时对话不读写记忆存储。

## Prompt 策略（最小且可控）
- 固定一个全局 `systemPrompt`（实现时做成配置项），例如：`你是一个严谨的中文助手。`
- 每次请求：`systemPrompt` + 历史消息（如开启多轮）+ 本次用户 `message`。
- 不做复杂模板/角色体系/动态注入检索结果（RAG 后再做）。

## 代码结构（实现时按此落地）
> 这里只定义“要落地的最小结构”，避免引入多余层级。

1. `controller`
   - `ChatController`
   - DTO：
     - `ChatRequest { String conversationId; String message; }`
     - `ChatResponse { String conversationId; String answer; }`
     - `ErrorResponse { ErrorBody error; }`
2. `service`
   - `ChatService`
     - 输入：`conversationId`、`message`
     - 输出：`answer`（以及最终 `conversationId`）
3. `memory`（新增一个小包即可）
   - `ConversationMemoryStore`（接口）
   - `InMemoryConversationMemoryStore`（实现：ConcurrentHashMap + 截断保留）

依赖注入：
- `ChatService` 依赖一个 `ChatClient`（先用现有 `openAiChatClient`，或后续统一命名为 `chatClient`）。
- `ChatService` 依赖 `ConversationMemoryStore`（MVP 先用内存实现）。

## 配置项（实现时新增到 `application.yml`）
建议新增一组自定义配置（例如 `trag.chat.*`），用于避免把业务配置塞进 `spring.ai.*`：
- `trag.chat.system-prompt`：默认中文助手提示词
- `trag.chat.max-history-turns`：默认 10
- `trag.chat.max-message-length`：默认 2000（或按你业务需要）

## 验收标准（实现完成后必须满足）
1. `GET /api/chat/temp`：
   - `message` 正常时返回 200，且返回体为非空字符串。
2. `POST /api/chat/memory`：
   - `message` 正常时返回 200，且 `answer` 非空字符串。
3. 多轮（同一 `conversationId`）：
   - 第二次提问能“记住”第一次提问的上下文（至少在语言表现上可观察）。
4. 参数校验：
   - `message` 为空/全空白 → 400，`code=INVALID_ARGUMENT`。
5. 异常兜底：
   - 模型调用抛异常 → 502（固定），`code=MODEL_ERROR`。

## 本文档之外的注意事项（不实现，但要遵守）
- `application.yml` 中的 `api-key` 等敏感信息应迁移到环境变量或 `.env`（避免提交到仓库）；此项属于工程卫生，不属于本 MVP 的功能需求。
