-- 上游 bug 规避：AbstractSearchEngineService 构造时无条件 initSearchEngine()，
-- 而种子数据 google_setting 的 key/cx 为空串会抛 IllegalArgumentException 导致应用无法启动。
-- 填入占位值让启动通过；真正使用 Google 搜索前需在管理端替换为真实 Key。
UPDATE adi_sys_config
SET value = '{"url":"https://www.googleapis.com/customsearch/v1","key":"not-configured","cx":"not-configured"}'
WHERE name = 'google_setting'
  AND value LIKE '%"key":""%';
