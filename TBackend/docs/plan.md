# TBackend (FlashRAG) 核心实现方案指南

本文档基于 **Spring AI 1.0.0-M3 + Spring Boot 3.3.5 + Neo4j** 构建。

---

## 1. 基础设施搭建 (Docker Compose)

需要部署 **MySQL、Redis、Neo4j**。

**文件位置：** `docker-compose.yml`

```yaml
version: '3.8'
services:

  # 1. MySQL (业务数据)
  mysql:
    image: mysql:8.0
    container_name: rag-mysql
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: flashrag_db
    command: --character-set-server=utf8mb4 --collation-server=utf8mb4_unicode_ci

  # 2. Neo4j (向量 + 图数据)
  neo4j:
    image: neo4j:5.18.0
    container_name: rag-neo4j
    ports:
      - "7474:7474" # HTTP Console
      - "7687:7687" # Bolt Protocol
    environment:
      NEO4J_AUTH: neo4j/password
      NEO4J_dbms_memory_heap_initial__size: 1G
      NEO4J_dbms_memory_heap_max__size: 2G

  # 3. Redis (缓存)
  redis:
    image: redis:7.0
    container_name: rag-redis
    ports:
      - "6379:6379"
```

**启动命令：**

```bash
docker-compose up -d
```

---

## 2. 全局配置 (application.yml)

适配 **Spring AI 1.0.0-M3** 的 Neo4j VectorStore。

**文件位置：** `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: TBackend

  # 1. MySQL
  datasource:
    url: jdbc:mysql://localhost:3306/flashrag_db?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

  # 2. Spring AI
  ai:
    ollama:
      base-url: http://localhost:11434
      chat:
        model: qwen2.5
        options:
          temperature: 0.7
      embedding:
        model: bge-m3

    # 3. Neo4j Vector Store
    vectorstore:
      neo4j:
        uri: bolt://localhost:7687
        username: neo4j
        password: password
        embedding-dimension: 1024  # bge-m3
        index-name: vector_index
        label: Document

# MyBatis Plus
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

---

## 3. 核心代码骨架

得益于 Spring AI 抽象层，从 Milvus 切 Neo4j 对业务零侵入。

---

### 3.1 写入链路 (IngestionService)

负责：

* 解析文件
* 切片
* 写入 Neo4j（自动 embedding + Node）

`IngestionService.java`：

```java
package com.spf.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final VectorStore vectorStore;

    public void ingestFile(MultipartFile file) {
        try {
            Path tempFile = Paths.get(System.getProperty("java.io.tmpdir"), file.getOriginalFilename());
            file.transferTo(tempFile);
            Resource resource = new UrlResource(tempFile.toUri());

            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            TokenTextSplitter splitter = new TokenTextSplitter();
            List<Document> splitDocs = splitter.apply(documents);

            vectorStore.add(splitDocs);

            log.info("存入 Neo4j 成功，生成节点数: {}", splitDocs.size());
        } catch (Exception e) {
            log.error("文件处理失败", e);
            throw new RuntimeException(e);
        }
    }
}
```

---

### 3.2 对话链路 (ChatService)

基于 Spring AI M3 的新特性 **Advisor 模式**。

`ChatService.java`：

```java
package com.spf.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;

    public Flux<String> streamChat(String userQuery) {

        ChatClient chatClient = chatClientBuilder.build();

        return chatClient.prompt()
                .user(userQuery)
                .advisors(
                        new QuestionAnswerAdvisor(
                                vectorStore,
                                SearchRequest.defaults()
                                        .withTopK(3)
                                        .withSimilarityThreshold(0.7)
                        )
                )
                .stream()
                .content();
    }
}
```

---

### 3.3 控制层 (Controller)

`RagController.java`：

```java
package com.spf.controller;

import com.spf.service.ChatService;
import com.spf.service.IngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/rag")
@RequiredArgsConstructor
public class RagController {

    private final IngestionService ingestionService;
    private final ChatService chatService;

    @PostMapping("/upload")
    public String upload(@RequestParam("file") MultipartFile file) {
        ingestionService.ingestFile(file);
        return "Upload Success";
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam("query") String query) {
        return chatService.streamChat(query);
    }
}
```

---

## 4. 常见问题排查 (Neo4j 特有)

### ❌ 连接报错：Connection refused

**排查：**

* `docker ps` 看容器是否启动
* 密码是否与 `application.yml` 一致

---

### ❌ 维度不匹配：Dimension mismatch

**原因：**

Neo4j 索引维度 ≠ 当前 embedding 维度。

**解决：**

* 清空图数据库：

  ```cypher
  MATCH (n) DETACH DELETE n;
  ```

* 删除旧的向量索引

* 确认 embedding 模型维度

    * `bge-m3 = 1024`
    * 某些 Ollama 模型量化版本维度会改变 → 需 `ollama show model`

---

### ❌ Tika 依赖缺失

如果使用 `TikaDocumentReader` 需引入：

```xml
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-tika-document-reader</artifactId>
    <version>${spring-ai.version}</version>
</dependency>
```

---

如需我帮你把这个文档进一步：

* 拆成多文件
* 改成项目 README
* 加上架构图 / 时序图
* 转成 PDF / DOCX
  随时告诉我！
