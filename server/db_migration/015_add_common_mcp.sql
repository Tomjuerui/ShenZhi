-- 015: 添加常用 MCP（系统预设，user_id=0 即公用）
-- 幂等：按 title 判断不存在才插入，可重复执行

-- GitHub（stdio + npx，需用户填 GITHUB_TOKEN）
insert into adi_mcp (uuid, title, transport_type, stdio_command, stdio_arg, install_type, customized_param_definitions, website, remark, is_enable)
select replace(gen_random_uuid()::text, '-', ''), 'GitHub', 'stdio', 'npx', '-y @modelcontextprotocol/server-github', 'local',
       '[{"name":"GITHUB_TOKEN","title":"GitHub personal access token","require_encrypt":true}]',
       'https://github.com/modelcontextprotocol/servers',
       '# GitHub MCP Server

提供 GitHub 仓库、Issue、Pull Request、文件与代码搜索等能力。

## 配置

需要 GitHub Personal Access Token，请到 https://github.com/settings/tokens 创建（至少勾选 repo 与 read:org 权限）。',
       true
where not exists (select 1 from adi_mcp where title = 'GitHub');

-- Fetch（stdio + npx，无需 key）
insert into adi_mcp (uuid, title, transport_type, stdio_command, stdio_arg, install_type, customized_param_definitions, website, remark, is_enable)
select replace(gen_random_uuid()::text, '-', ''), 'Fetch', 'stdio', 'npx', '-y @modelcontextprotocol/server-fetch', 'local',
       '[]',
       'https://github.com/modelcontextprotocol/servers/tree/main/src/fetch',
       '# Fetch MCP Server

抓取网页并转换为适合大模型阅读的 Markdown 文本。

## 能力

- 抓取任意 URL 内容
- 自动转换为 Markdown
- 支持分页与最大长度控制',
       true
where not exists (select 1 from adi_mcp where title = 'Fetch');

-- Memory（stdio + npx，知识图谱记忆，无需 key）
insert into adi_mcp (uuid, title, transport_type, stdio_command, stdio_arg, install_type, customized_param_definitions, website, remark, is_enable)
select replace(gen_random_uuid()::text, '-', ''), 'Memory', 'stdio', 'npx', '-y @modelcontextprotocol/server-memory', 'local',
       '[]',
       'https://github.com/modelcontextprotocol/servers/tree/main/src/memory',
       '# Memory MCP Server

基于本地知识图谱的持久化记忆，记录实体与关系，供大模型跨会话检索。

## 能力

- 存储实体与关系
- 按主题检索
- 打开/关闭命名空间',
       true
where not exists (select 1 from adi_mcp where title = 'Memory');

-- Sequential Thinking（stdio + npx，无需 key）
insert into adi_mcp (uuid, title, transport_type, stdio_command, stdio_arg, install_type, customized_param_definitions, website, remark, is_enable)
select replace(gen_random_uuid()::text, '-', ''), 'Sequential Thinking', 'stdio', 'npx', '-y @modelcontextprotocol/server-sequential-thinking', 'local',
       '[]',
       'https://github.com/modelcontextprotocol/servers/tree/main/src/sequentialthinking',
       '# Sequential Thinking MCP Server

分步结构化思考工具，适合解决复杂问题、制定计划与逐步修正思路。',
       true
where not exists (select 1 from adi_mcp where title = 'Sequential Thinking');

-- Context7（streamable_http 远程，无需本地运行时，无需 key）
insert into adi_mcp (uuid, title, transport_type, sse_url, sse_timeout, install_type, customized_param_definitions, website, remark, is_enable)
select replace(gen_random_uuid()::text, '-', ''), 'Context7', 'streamable_http', 'https://mcp.context7.com/mcp', 30, 'remote',
       '[]',
       'https://context7.com',
       '# Context7 MCP Server

为 2 万+ 开源库提供实时、最新的文档检索，返回精确的代码示例与 API 参考。',
       true
where not exists (select 1 from adi_mcp where title = 'Context7');
