-- =============================================================
-- 增量迁移：AI 模型接入运行时配置 2026-11-13
-- 状态：待在 192.168.1.14 执行；新库由全量脚本(data.sql)包含无需执行
-- 说明：ai.base-url / ai.api-key / ai.model / ai.timeout 由管理端「AI 接入配置」
--       页表单或 JSON 导入维护，运行时热更新；留空 Base URL/Key 走本地规则引擎。
-- =============================================================

INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('ai.base-url', '', 'OpenAI 兼容接口 Base URL，如 https://ark.cn-beijing.volces.com/api/v3'),
('ai.api-key', '', '模型 API Key（留空 = 规则引擎模式，不发起真实调用）'),
('ai.model', 'doubao-1-5-vision-pro-250828', '模型名（客服与多模态发布共用，需支持 vision）'),
('ai.timeout', '15000', '模型调用超时（毫秒）');
