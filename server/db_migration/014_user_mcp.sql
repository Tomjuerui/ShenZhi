-- 014: user-created MCP + public toggle
-- 允许普通用户自建 MCP 并选择是否公用
alter table adi_mcp add column if not exists user_id bigint default 0 not null;
alter table adi_mcp add column if not exists is_public boolean default false not null;
comment on column adi_mcp.user_id is 'Owner user id; 0 means admin-preset/system MCP';
comment on column adi_mcp.is_public is 'Whether a user-created MCP is public to all users';
