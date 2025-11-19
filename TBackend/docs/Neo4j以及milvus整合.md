# Spring Boot 整合 Neo4j 与 Milvus 官方驱动指南

本文档指导你在 Spring Boot 项目中整合 Neo4j (neo4j-java-driver) 和 Milvus (milvus-sdk-java) 的官方原生驱动。

## 核心思路：手动配置，自动注入

由于使用的是官方原生驱动而不是 Spring Data 库（如 spring-boot-starter-data-neo4j），Spring Boot 无法“自动配置”它们。因此，我们的策略是：

- **手动配置**：创建 Java 配置类（`@Configuration`），在里面初始化 Neo4j 的 Driver 和 Milvus 的 `MilvusServiceClient` 实例。
- **注册为 Bean**：使用 `@Bean` 注解将实例注册到 Spring 容器。
- **自动注入**：在任何地方使用 `@Autowired` 获取并使用这些实例。

## 步骤一：在 pom.xml 中添加依赖

首先，在你的 `pom.xml` 文件的 `<dependencies>` 标签内添加以下依赖。版本已根据你的需求进行了适配。

```xml
<!-- Neo4j 官方 Java 驱动 -->
<dependency>
    <groupId>org.neo4j.driver</groupId>
    <artifactId>neo4j-java-driver</artifactId>
    <version>5.22.0</version>
</dependency>

<!-- Milvus 官方 Java SDK -->
<dependency>
    <groupId>io.milvus</groupId>
    <artifactId>milvus-sdk-java</artifactId>
    <version>2.4.1</version>
</dependency>
```

请刷新 Maven 依赖以确保正确加载。

## 步骤二：创建 Spring Boot 配置类

在你的项目源码中（例如 `com.example.demo.config` 包下），创建两个 Java 类：`Neo4jConfig.java` 和 `MilvusConfig.java`。

### 1. Neo4j 配置 (`Neo4jConfig.java`)

负责初始化 Neo4j Driver 实例。

```java
package com.example.demo.config;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Neo4jConfig {

    @Value("${neo4j.uri}")
    private String uri;

    @Value("${neo4j.username}")
    private String username;

    @Value("${neo4j.password}")
    private String password;

    @Bean
    public Driver neo4jDriver() {
        return GraphDatabase.driver(uri, AuthTokens.basic(username, password));
    }
}
```

### 2. Milvus 配置 (`MilvusConfig.java`)

负责初始化 `MilvusServiceClient` 实例。

```java
package com.example.demo.config;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusConfig {

    @Value("${milvus.host}")
    private String host;

    @Value("${milvus.port}")
    private Integer port;

    @Bean
    public MilvusServiceClient milvusClient() {
        return new MilvusServiceClient(
            ConnectParam.newBuilder()
                .withHost(host)
                .withPort(port)
                .build()
        );
    }
}
```

## 步骤三：在 application.properties 中添加连接信息

为了让步骤二中的 `@Value` 注解能读到配置，请在 `src/main/resources/application.properties` 或 `application.yml` 文件中添加以下内容。

```properties
# Neo4j 配置
neo4j.uri=bolt://localhost:7687
neo4j.username=neo4j
neo4j.password=password123

# Milvus 配置
milvus.host=localhost
milvus.port=19530
```

## 步骤四：在 Service 中使用已注入的 Bean

现在你可以在任何 Service 或 Component 中通过 `@Autowired` 使用这两个数据库客户端了。

示例 (`MyKgService.java`)：

```java
package com.example.demo.service;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.R;
import io.milvus.param.health.HealthParam;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MyKgService {

    private final Driver neo4jDriver;
    private final MilvusServiceClient milvusClient;

    @Autowired
    public MyKgService(Driver neo4jDriver, MilvusServiceClient milvusClient) {
        this.neo4jDriver = neo4jDriver;
        this.milvusClient = milvusClient;
        
        checkConnections();
    }

    public void checkConnections() {
        // 验证 Neo4j
        try (Session session = neo4jDriver.session()) {
            Result result = session.run("RETURN 'Neo4j connection successful' AS message");
            System.out.println(result.single().get("message").asString());
        } catch (Exception e) {
            System.err.println("Neo4j 连接失败: " + e.getMessage());
        }

        // 验证 Milvus
        try {
            R<Boolean> response = milvusClient.health(HealthParam.newBuilder().build());
            if (response.getData()) {
                System.out.println("Milvus connection successful");
            } else {
                System.err.println("Milvus 连接失败: " + response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Milvus 连接失败: " + e.getMessage());
        }
    }
}
```

当你运行 Spring Boot 项目时，你应该能看到控制台输出 `"Neo4j connection successful"` 和 `"Milvus connection successful"`，这表明整合已经成功完成。
```

此 Markdown 文档详细描述了如何在 Spring Boot 项目中整合 Neo4j 和 Milvus 的官方驱动，并提供完整的代码示例和配置说明。希望对你有所帮助！