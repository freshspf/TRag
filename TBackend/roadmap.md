# 《基于知识图谱与RAG的电力绿色低碳领域学术知识问答系统》技术栈与环境准备清单

欢迎开始你的项目！这份清单根据你的开题报告整理，旨在帮你梳理所需的技术栈，并一步步搭建好开发环境。

## 一、 核心技术栈总览

根据你的设计方案，你的系统将是一个**微服务架构**，主要由一个 Java 后端主服务和一个 Python 算法服务构成。

1. **后端主服务 (Java):**

   - **框架:** `Spring Boot` (用于构建 RESTful API, 处理核心业务逻辑)。
   - **职责:** 接收前端请求、调度RAG流程、与数据库交互、调用 Python 服务和 LLM API。

2. **算法服务 (Python):**

   - 框架: FastAPI (用于构建轻量级、高性能的

     Python API)。

   - **职责:** 封装算法模型，提供如“文本向量化”、“命名实体识别(NER)”和“关系抽取(RE)”等能力的接口。

   - 核心库: Hugging Face Transformers (用于加载和使用预训练模型，如 BERT)

     , sentence-transformers (用于生成文本向量), LangChain (辅助处理文档和RAG流程)。

3. **数据存储 (异构数据库):**

   - **图数据库:** `Neo4j` (用于存储结构化的知识三元组，如：[光伏电池A] -[转换效率是]-> [25%])。
   - **向量数据库:** `Milvus` (用于存储非结构化的文本块及其对应的向量，实现语义检索)。

4. **大语言模型 (LLM):**

   - **方式:** API 调用（无需本地部署）。
   - **职责:** 作为RAG流程的“大脑”，根据你提供的上下文（从 Neo4j 和 Milvus 检索到的知识）来生成最终答案。

## 二、 开发环境准备 (Step-by-Step)

建议你按照这个顺序来准备你的开发环境，会非常高效。

### 1. 基础工具 (必备)

- **Git:** 用于代码版本控制。

- Docker & Docker Compose: 强烈推荐！

  这是最关键的工具，可以让你用一行命令就启动和管理 Neo4j 和 Milvus 数据库，而不需要在你的电脑上分别安装和配置它们，极大简化了环境问题。

### 2. 数据库服务 (使用 Docker)

你不需要单独下载 Neo4j 和 Milvus。只需创建一个 `docker-compose.yml` 文件，内容如下：

```
# docker-compose.yml
version: '3.8'

services:
  neo4j:
    image: neo4j:5-community  # 使用 Neo4j 5 社区版
    container_name: my_neo4j
    ports:
      - "7474:7474"  # Web 界面
      - "7687:7687"  # Bolt 驱动连接端口
    environment:
      - NEO4J_AUTH=neo4j/password123  # 设置用户名 neo4j，密码 password123
    volumes:
      - ./neo4j-data:/data

  # Milvus 包含多个组件，使用官方提供的 standalone-docker-compose.yml 更方便
  # 你可以从 Milvus 官网获取最新的 docker-compose.yml (for standalone)
  # 这里提供一个简化示例，但推荐使用官方文件
  milvus-standalone:
    image: milvusdb/milvus:v2.4.4-standalone  # 使用 Milvus 2.4.4
    container_name: my_milvus
    ports:
      - "19530:19530"  # Milvus gRPC 端口
      - "9091:9091"    # Milvus Web 界面 (Attu)
    volumes:
      - ./milvus-data:/var/lib/milvus
    environment:
      - "ETCD_USE_EMBED=true"
      - "ETCD_DATA_DIR=/var/lib/milvus/etcd"
      - "COMMON_STORAGETYPE=local"
```

**如何使用:**

1. 将上述内容保存为 `docker-compose.yml` 文件。
2. 在文件所在目录打开终端，运行 `docker-compose up -d`。
3. Docker 就会自动帮你下载并启动 Neo4j 和 Milvus。

### 3. Java 后端环境

- **JDK:** 确保安装了 Java 17 或更高版本。
- **构建工具:** `Maven` 或 `Gradle` (Spring Boot 官方推荐 Maven)。
- **IDE:** `IntelliJ IDEA` (社区版免费) 或 `VS Code` (需安装 Java 插件包)。
- **关键依赖库 (在 `pom.xml` 中添加):**
  - `spring-boot-starter-web` (构建 Web API)
  - `neo4j-java-driver` (连接 Neo4j)
  - `milvus-sdk-java` (连接 Milvus)
  - `spring-boot-starter-webflux` (用于 `WebClient` 异步调用 Python API)

### 4. Python 算法环境

- **Python:** 确保安装了 Python 3.10 或更高版本。

- 环境管理 (推荐): Anaconda 或 Python

  自带的 venv，用于创建独立的项目环境，防止库冲突。

- **IDE:** `PyCharm` (社区版免费) 或 `VS Code` (需安装 Python 插件)。

- **关键依赖库 (使用 `pip install ...` 安装):**

  - `fastapi` (构建 API)
  - `uvicorn` (运行 FastAPI 服务)
  - `transformers` (Hugging Face 核心库)
  - `torch` (Pytorch，Transformers 依赖)
  - `sentence-transformers` (文本向量化)
  - `langchain` (可选，但推荐，用于快速构建 RAG 流程)
  - `neo4j` (Python 驱动)
  - `pymilvus` (Python 驱动)

## 三、 建议的开发第一步 (验证环境)

环境搭好后，不要急着写复杂功能。先做几个“Hello World”来验证所有组件都通了：

1. **启动数据库:** 运行 `docker-compose up -d`，确保 Neo4j 和 Milvus 成功启动。

   - 访问 `http://localhost:7474` (Neo4j 浏览器)。
   - 访问 `http://localhost:9091` (Milvus Attu 界面)。

2. **测试 Python 服务:**

   - 创建一个简单的 FastAPI 服务，只提供一个 /embed

     接口，它接收一段文本，使用 sentence-transformers

     返回一个向量。

3. **测试 Java 服务:**

   - 创建一个简单的 Spring Boot 服务，提供三个接口：
     - `/test-python`: 调用 Python 服务的 `/embed` 接口，并打印结果。
     - `/test-neo4j`: 连接 Neo4j，创建一个节点 `(:Test {name:"Hello"})` 并查询它。
     - `/test-milvus`: 连接 Milvus，创建一个 Collection 并打印版本。

**当这三步全部跑通，就证明你的微服务架构和异构数据库环境已经彻底打通了。** 此时，你就可以开始正式开发你的核心功能了（如阶段一：知识构建）。