# 「青禾集市」AI 智能客服设计方案

**平台助手（RAG + Function Calling + 规则引擎降级）**

> 版本：2026-10-30 · 状态：待实施（设计定稿）
> 基线：现有 c2c 主分支（含 sys_config 系统设置面板、`com.c2c.ai` 模块的可配置接入与降级模式）
> 定位：买家/卖家的 7×24 站内智能助手——查订单、问规则、找商品、代留言、发起举报；毕业设计的 AI 方向主推方案之一
> 关联文档：《毕业设计可行性分析.md》《毕业设计完善路线.md》《项目分析与优化建议.md》

---

## 1. 目标与定位

### 1.1 解决什么问题

| 现状痛点 | 客服方案 |
|---|---|
| 用户问"我的订单到哪了"只能自己去订单页翻 | 对话中直接查本人订单并回答 |
| 交易规则（多久自动确认收货？如何退款？）散落在公告里没人看 | RAG 检索平台公约/FAQ，问答式触达 |
| 买家想砍价但没有卖家联系方式 | 工具调用直接代买家向卖家发购买意向 |
| 发现违规商品不知道去哪举报 | 对话中一键生成举报 |
| 无大模型 Key 时（答辩/演示环境） | 规则引擎客服兜底，功能始终可用 |

### 1.2 设计原则（继承 AI 智能发布的成熟模式）

1. **可配置接入**：配置 Key 走真实大模型（OpenAI 兼容协议 + Function Calling），未配置自动降级规则客服；
2. **数据锚定**：回答必须基于平台真实数据（订单/商品/公告/FAQ），系统提示词强制"不知道就说不知道"，从机制上压幻觉；
3. **不改现有交易闭环**：IM 模块只复用不修改，AI 客服走独立入口；
4. **成本控制**：Redis 限频 + FAQ 向量缓存，单用户日调用有上限。

### 1.3 与既有能力的边界

- **不是**订单状态变更通知（那是 G4 系统消息，message_type=3 单向推送）——客服是**交互式问答**；
- **不是**买家↔卖家私信（IM 双边会话）——客服是用户↔平台的官方会话；
- 两者共用 `conversation`/`message` 两张表与聊天 UI，来源字段区分（见 3.4）。

---

## 2. 总体架构

```
┌─────────────────────── 用户端前端 (Vue3 / 微信小程序*) ───────────────────────┐
│  ChatView 聊天页：会话列表顶部固定「🤖 平台助手」官方入口                        │
│   ├─ 与助手会话内发送 → POST /ai/chat（替代 /im/message）                      │
│   ├─ 输入区上方 FAQ 快捷问题 chips（点击即问）                                 │
│   └─ AI 回复气泡带「AI 生成」角标；「人工/投诉」提示转人工                       │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │ HTTP /api/v1
┌──────────────────────────────────▼──────────────────────────────────────────┐
│                        后端 (Spring Boot 单体，新代码全部在 com.c2c.ai)         │
│  AiChatController  POST /ai/chat（登录用户）                                   │
│    │                                                                          │
│    ▼                                                                          │
│  AiChatServiceImpl                                                           │
│    1. 会话校验：成员校验 + 对端必须是官方助手账号（复用 checkConversationMember） │
│    2. 写用户消息（复用 ImService.sendMessage，message_type=1）                 │
│    3. 上下文组装：取本会话最近 10 条消息 + 系统提示词                           │
│    4. 知识检索（有 Key 时）：FAQ 向量相似度 Top-3 → 注入上下文                  │
│    5. 大模型调用（OpenAI 兼容 /chat/completions + tools）：                    │
│       模型自主选择工具 → 本地执行（查库均为"当前登录用户"维度，防越权）          │
│       → 结果回注模型 → 生成最终回复                                           │
│    6. 失败/超时/无 Key → 规则引擎客服：                                        │
│       意图关键词匹配 → 工具的正则参数提取（订单号等）→ 模板化回答                │
│    7. 写助手回复（assistant 账号发出，message_type=3 系统消息）                 │
│    8. Redis 限频：单用户客服问答 ≤ 30 次/天（sys_config 可调）                 │
│                                                                              │
│  知识库：faq 表（FAQ/公约/帮助中心，管理端可维护——二期）                        │
└──────────────────────────────────┬──────────────────────────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        ▼                          ▼                          ▼
      MySQL                      Redis                       大模型 API
   user/order/product      限频/FAQ向量缓存/          （OpenAI 兼容，可配置；
   faq/announcement        对话上下文缓存              无 Key 自动规则引擎）
```

\* 微信小程序端二期再做（与 AI 智能发布同口径：本次 Web 优先）。

---

## 3. 核心设计

### 3.1 官方助手账号（入口载体）

- 种子用户：id 固定 `90000001`，username `assistant`，nickname `青禾助手`，role=0，status=1（写入 `data.sql`）；
- 「平台助手」会话 = 当前用户与 90000001 的普通会话，复用 `getOrCreateConversation` 归一化逻辑；
- 前端会话列表把该会话置顶并固定展示（即使无消息记录也可点击进入）；
- **判定规则**：`conversation` 的 user1_id/user2_id 中任一为 90000001 → AI 客服会话（`AiChatService.isAssistantConversation`）。

### 3.2 知识库与 RAG

```sql
CREATE TABLE `faq` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `question` VARCHAR(200) NOT NULL COMMENT '标准问',
  `answer` TEXT NOT NULL COMMENT '答案',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '分类：交易规则/账号/支付/发货等',
  `keywords` VARCHAR(500) DEFAULT NULL COMMENT '检索关键词(逗号分隔)',
  `embedding` TEXT DEFAULT NULL COMMENT 'question 的向量(浮点数组JSON)，有Key时由任务生成',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用0停用',
  `sort_order` INT DEFAULT 0,
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服知识库（FAQ/交易公约）';
```

- 检索两档：**有 Key** → embedding 余弦相似度 Top-3（向量调 API 生成，结果缓存 Redis；FAQ 规模 <500，内存算余弦即可，不引向量库）；**无 Key** → keywords 包含匹配 + category 字典；
- 语料来源（毕设"自建数据集"）：从交易流程倒推整理 30~50 条种子 FAQ（下单/支付/发货/收货/退款/账号/违规），写入 `data.sql`；公告表同时纳入检索范围。

### 3.3 Function Calling 工具集

| 工具名 | 参数 | 本地执行 | 说明 |
|---|---|---|---|
| `query_order_status` | orderNo?（空=最近一笔） | 查当前用户订单+物流字段 | 订单类问题核心工具 |
| `query_product` | productId | 商品详情/在售状态 | |
| `list_announcements` | 无 | 公告列表 Top5 | |
| `query_wallet` | 无 | 余额+最近流水摘要 | |
| `create_intent` | productId, message | 写入 product_intent 代买家留言 | 议价场景闭环到 IM |
| `create_report` | productId, reason | 生成举报草稿（report 表 status=1） | 平台治理入口 |
| `handoff_to_human` | reason | 回复引导语（管理端工单二期） | 投诉/纠纷兜底 |

**安全约束**：
- 所有工具只允许操作**当前登录用户**维度的数据（后端从会话上下文取 userId，模型只传业务参数），从机制上杜绝"问别人订单"；
- 工具入参统一 `@Valid` 校验；productId/orderNo 不存在时返回结构化错误给模型，让模型"承认不知道"。

### 3.4 消息落库与展示

- 用户消息：`message_type=1`，sender=用户；
- AI 回复：sender=90000001，`message_type=3`（系统消息，前端已有系统气泡样式可复用），`extra` JSON 存 `{"ai":true,"source":"model"|"rule"}` 以便前端打角标与毕设统计；
- 单轮问答全部落库 → 天然形成**可导出的对话日志数据集**（毕设实验与迭代分析的直接素材）。

### 3.5 降级三层体系（论文叙事主线）

```
第一层：大模型 + RAG + Function Calling（有 Key，最佳体验）
第二层：大模型直答（有 Key 但检索为空，提示词约束不编造）
第三层：规则引擎客服（无 Key/超时/异常：意图关键词 → 工具正则提取 → 模板回复）
```

- 与 AI 智能发布的降级模式同构，论文可写成"统一的可配置接入与降级架构"（`com.c2c.ai` 两个场景互相印证）；
- 降级对用户透明：`extra.source` 标记来源，管理端可看统计。

### 3.6 限频与安全

- 单用户问答 ≤ 30 次/天（Redis `ai:chat:rate:{userId}`，超出提示"今日咨询次数已达上限，可联系人工"）；次数阈值入 `sys_config`；
- 用户输入过 `SensitiveWordUtils` 过滤；
- 系统提示词强制：不泄露他人隐私/内部实现、不承诺退款结果、不引导脱离平台交易（这本身也是反诈话术，可写进论文安全章节）；
- 助手会话同样禁止双方中的助手账号主动发起（发送接口校验 sender 不得为 90000001）。

---

## 4. 后端设计

### 4.1 文件清单（全部新文件，不动现有类）

| 文件 | 说明 |
|---|---|
| `ai/entity/Faq.java` | 知识库实体 |
| `ai/mapper/FaqMapper.java` | |
| `ai/dto/AiChatRequest.java` | 入参：conversationId、content |
| `ai/dto/AiChatVO.java` | 出参：回复消息 id、content、source(model/rule) |
| `ai/dto/FaqVO.java` | 管理端/快捷问题列表出参（question 脱敏版） |
| `ai/service/AiChatService.java` / `AiChatServiceImpl.java` | 客服主链路（校验→落库→检索→模型/规则→回复落库） |
| `ai/service/AiChatToolExecutor.java` | 工具注册表 + 本地执行（含参数校验与越权防护） |
| `ai/service/FaqSearchService.java` | 知识检索（向量/关键词两档） |
| `ai/controller/AiChatController.java` | `POST /ai/chat`、`GET /ai/chat/faq-suggestions` |
| `ai/client/AiOpenAiClient.java` | **增量修改**：增加 `chatWithTools(messages, tools, model)` 方法（原识别方法不动） |

### 4.2 接口定义

**`POST /api/v1/ai/chat`**（需登录）

```json
// 请求
{ "conversationId": 12, "content": "帮我看看订单 20260615001 到哪了" }

// 响应（R<AiChatVO>）
{
  "code": 200,
  "data": {
    "messageId": 356,
    "content": "您的订单 20260615001 已于 6-17 确认收货，交易已完成。如未收到货物请尽快在订单页发起申诉。",
    "source": "model"
  }
}
```

**`GET /api/v1/ai/chat/faq-suggestions`**（需登录）→ 快捷问题 Top8（按 sort_order + 热度），供聊天页 chips。

### 4.3 配置项

```yaml
app:
  ai:
    # 复用现有：base-url / api-key / model / timeout
    chat-model: ${AI_CHAT_MODEL:}        # 客服对话模型，缺省回落 model
    chat-timeout: ${AI_CHAT_TIMEOUT:12000}
```

`sys_config` 新增两行（管理端「系统设置」面板自动出现，无需改前端）：

| config_key | 默认值 | 说明 |
|---|---|---|
| `sys.ai-assistant.enabled` | `true` | AI 客服总开关（关闭则助手会话提示"客服维护中"） |
| `sys.ai-assistant.daily-limit` | `30` | 单用户每日问答上限 |

### 4.4 常量与鉴权改动点（共 3 处，均为增量）

1. `ApiPath`：`AI_CHAT = "/ai/chat"`、`AI_CHAT_FAQ = "/ai/chat/faq-suggestions"`；
2. `SysConfigKey`：加 `AI_ASSISTANT_ENABLED`、`AI_ASSISTANT_DAILY_LIMIT`；
3. `AuthTokenFilter`：**无需改**——`/ai/**` 默认登录鉴权（不在白名单即需登录）。

### 4.5 SQL（并入全量脚本，遵守 sql/README 规范）

- `init.sql`：`faq` 表（第 21 张）；
- `data.sql`：助手账号 1 行 + FAQ 种子 30~50 行 + sys_config 2 行；
- 表数口径：README/sql README 同步 21 张表（交付时统一改）。

---

## 5. 前端设计

### 5.1 用户端聊天页（`frontend/src/views/ChatView.vue` + `ChatListView.vue`）

1. **入口**：会话列表顶部固定卡片「🤖 平台助手 · 7×24 在线」（点击进入/创建助手会话）；
2. **发送分流**：对端为助手账号的会话，发送走 `POST /ai/chat`，其余走原 `/im/message`；
3. **AI 气泡**：`extra.ai=true` 的消息显示「AI 生成 · 仅供参考」角标；`source=rule` 时角标为「规则回复」；
4. **快捷问题**：进入助手会话加载 `faq-suggestions` 渲染为 chips，点击直接发送；
5. **加载态**：发送按钮转圈（同步等待回复，超时 12s 提示重试）；失败自动 toast 不阻塞；
6. **转人工提示**：回复中含 handoff 标识时，底部展示「联系人工」按钮（跳转公告中的联系方式/申诉入口）。

### 5.2 管理端（二期，可进论文"展望"）

- FAQ 维护页（增删改、停用启用、热度排序）；
- 客服对话看板：问答对抽样、source 分布统计、`ai-extra` 日志导出。

---

## 6. 实施顺序与自测清单

| 阶段 | 内容 | 验证 |
|---|---|---|
| 1 | 表结构 + 助手账号 + sys_config；`AiChatService` 骨架（校验+落库+规则引擎回复，不接大模型） | 无 Key 环境完整对话可用，消息入库正确 |
| 2 | `AiChatToolExecutor` 工具集（规则模式的正则参数提取） | 6 个工具手工用例全过，越权问他人订单被拒 |
| 3 | `AiOpenAiClient.chatWithTools` + RAG 检索 + 系统提示词 | 配 Key：查订单/问规则/代留言 3 类场景正确；拔 Key 自动降级 |
| 4 | 前端聊天页改造（入口/chips/角标/加载态） | `npm run build` + 浏览器全流程 |
| 5 | 限频 + 敏感词 + 统计埋点 | 30 次限频触发；`extra.source` 统计正确 |
| 6 | 收尾 | README 功能表 + Swagger 注释 + 本设计文档状态更新 |

---

## 7. 毕设实验设计（三件套，直接可执行）

| 实验 | 方法 | 指标 |
|---|---|---|
| 意图识别与工具调用正确率 | 标注 50 条测试问句（订单/商品/规则/账号/闲聊/恶意 6 类） | 意图准确率、工具选择准确率、参数提取准确率 |
| 回答事实性对比 | 20 个平台事实问题 × 3 策略（纯LLM / LLM+RAG / 规则） | 无幻觉率、答案完整度（人工双评取均值） |
| 降级可用性与延迟 | 拔 Key 全流程 + 有 Key 50 轮采样 | 规则命中率、P50/P95 响应延迟、单轮成本 |

数据来源：本地造数 + 演示订单循环复用；对话日志（3.4 节）即实验原始数据。

---

## 8. 风险与注意事项

1. **幻觉**：系统提示词 + 数据锚定 + "不知道就承认"的训练语料；论文中如实报告事实性指标而非宣称零幻觉；
2. **成本**：限频 + FAQ 缓存 + 对话模型选低价款；演示现场可预生成典型问答；
3. **越权**：工具层强制当前用户维度，论文把这一点写成"LLM 工具调用的权限收敛设计"——是加分点；
4. **超时**：客服同步返回，12s 超时即降级规则回复，绝不让用户干等；
5. **与 G4 的关系**：G4（订单系统消息）实施时，AI 客服会话需排除系统消息触发（对端=90000001 不发系统通知）；
6. **演示保险**：答辩演示前把典型问答预演一遍并落库（消息在库，断网也能翻历史展示）。

---

## 9. 论文衔接

- **系统叙事**：「买家助手（本方案）+ 卖家助手（AI 智能发布）= 青禾集市 AI 助手体系」；
- **论文题目候选**：《基于大模型工具调用与检索增强的二手交易平台智能客服系统设计与实现》；
- **创新点**：① Function Calling 与交易业务系统打通并做权限收敛；② RAG 锚定平台数据压幻觉（有对比实验）；③ 可配置接入 + 规则引擎的三层降级体系（与 AI 发布互相印证的统一架构）；
- **答辩演示线**：问订单 → 问规则 → 代留言 → 拔 Key 再问（展示降级）。
