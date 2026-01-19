# AIDeepIn

AI 应用平台，集成 AI 对话、知识库（RAG）、工作流编排、长短期记忆与 MCP 工具能力，可用于快速搭建智能业务助手。

## 技术栈

- **后端**：Java 17 + Spring Boot 3 + langchain4j + langgraph4j + MyBatis-Plus
- **存储**：PostgreSQL（pgvector 向量检索）、Neo4j / Apache AGE（图存储）、Redis
- **前端**：Vue 3 + Vite + TypeScript + Naive UI

## 目录结构

| 目录 | 说明 |
|:-----|:-----|
| `server/` | 后端服务，Maven 多模块 |
| ├─ `adi-common` | 实体、DTO、Mapper、模型适配、RAG、工作流引擎等公共能力 |
| ├─ `adi-chat` | 用户端接口 |
| ├─ `adi-admin` | 管理端接口 |
| └─ `adi-bootstrap` | 启动模块，聚合以上三个模块 |
| `user-web/` | 用户端前端 |
| `admin-web/` | 管理端前端 |
| `docker/` | 部署编排 |

## 功能模块

| 模块 | 说明 |
|:-----|:-----|
| AI 对话 | 多角色多会话，可配置提示词、模型与参数，支持流式输出 |
| 知识库 | 文档切片向量化，支持向量检索与知识图谱（GraphRAG）两种方式 |
| AI 工作流 | 可视化编排，支持条件分支与并行执行，内置 LLM 调用、知识库检索、人工反馈等节点 |
| 图片生成 | 文生图与图片编辑 |
| ASR / TTS | 语音识别与语音合成，支持文字与语音的组合输入输出 |
| 长短期记忆 | 自动从对话中提取关键信息并沉淀，支持基于历史上下文的个性化回复 |
| MCP 工具 | 接入 MCP 服务，扩展模型可用的工具与数据源 |
| Open API | 为角色、知识库、工作流提供 RESTful API，支持流式与阻塞两种响应 |

## 快速开始

### 1. 准备依赖服务

- PostgreSQL，需安装 [pgvector](https://github.com/pgvector/pgvector) 扩展，用于向量检索
- Redis
- 图存储二选一：Neo4j 或 PostgreSQL + [Apache AGE](https://github.com/apache/age)

### 2. 初始化数据库

按顺序执行 `server/db_migration/` 下的 SQL 脚本，其中 `001_3.21.0.sql` 为基础建表，其余为增量迁移。

### 3. 启动后端

复制配置样板并按实际环境修改：

```bash
cp server/adi-bootstrap/src/main/resources/application-dev.yml.example \
   server/adi-bootstrap/src/main/resources/application-dev.yml
```

然后构建并启动：

```bash
cd server
mvn clean install
mvn spring-boot:run -pl adi-bootstrap
```

### 4. 启动前端

```bash
cd user-web      # 或 admin-web
pnpm install
pnpm run dev
```

## 部署

`docker/` 目录下提供了完整的编排配置：

```bash
cd docker
cp .env.prod .env
docker compose up -d
```

## 说明

`server/local-repo/` 存放了 Maven Central 上缺失的两个依赖（`Happy-Captcha` 验证码、`age-jdbc` 图数据库驱动），后端通过 `file://` 本地仓库引入，构建时请勿删除该目录。
