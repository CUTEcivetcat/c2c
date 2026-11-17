# JMeter 内网压测说明

> 配套：`../docs/压测基线报告.md`（指标记录模板与判定口径）
> 工具：Apache JMeter 5.6.3，JDK 17。压测范围仅限**只读 / 查询**接口，不压下单、支付、发货、取消等写接口，避免产生脏订单与脏余额。

## 1. 压测原则

- **凭证预生成，不实时登录**：压测不依赖单账号反复调 `/user/login`（避免被频控 / 验证码拦截，也不把登录接口计入业务吞吐）。先跑测试侧生成器，从库内读取可用普通买家，按当前 `jwt.secret` 复刻登录签发口径签出 access token，导出 CSV 供 JMeter 直接读取。
- **只压查询链路**：列表、详情、订单查询等只读取样器；写接口不在本计划范围。
- **结论用途**：用于改造前后的相对对比与瓶颈初判；压测端与后端同机部署时，吞吐受本机资源限制，不等同生产容量。

## 2. 目录结构

```
JMeter/
├── c2c-baseline.jmx    # 基线压测计划：100 并发 / Ramp-up 10s / 持续 300s
├── c2c-smoke.jmx       # 联调计划：1 线程 1 轮，先跑通链路再切基线
├── README.md           # 本说明
└── result/             # 结果落盘目录（运行产物，不入库）
    └── perf-tokens.csv # 生成器产出的压测 token（见 §4）
```

## 3. 环境前置

| 项 | 值 / 说明 |
|---|---|
| JMeter | 5.6.3（`bin/jmeter.bat`）；启动前 `JAVA_HOME` 指到 JDK 17，避免落到更高版本 |
| JDK | 17（与后端 `java.version` 一致） |
| 后端地址 | 默认 `http://localhost:8080`，context-path `/api/v1`；端口在 jmx「全局变量」里改 |
| MySQL | 必须可达：生成器要读库内买家与 `token_version`，后端运行也依赖库 |
| Redis | 必须可达：过滤器校验令牌版本 / 登出黑名单走 Redis |
| 配置来源 | `config/application-pro.yml`（含 `jwt.secret`、数据源、Redis 占位符；敏感，不入库） |

### 启动后端（PowerShell，任选其一）

IDEA 直接启动主类 `C2cMonolithApplication`；或命令行：

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
java -jar target\c2c-monolith-1.0.0.jar
```

确认日志出现 `Started C2cMonolithApplication`、8080 可访问后再开 JMeter。

## 4. 生成压测 token（压测前必做）

### 4.1 脚本与产物

| 项 | 值 |
|---|---|
| 生成脚本 | `src/test/java/com/c2c/common/infra/PerfTokenGeneratorTest.java`（含 `main` 入口，IDEA 可直接运行；也可走 Maven 测试） |
| 产物路径 | `JMeter/result/perf-tokens.csv`（相对项目根） |
| CSV 列 | `userId,token,account`；首行为表头，CSV Data Set 跳过首行 |

### 4.2 签发口径（与过滤器校验一致）

- 筛选 `role = 0 AND status = 1` 的普通买家，排除 AI 助手账号（`90000001`）。
- claims 携带：`role`（角色字符串）、`tv`（与库内 `token_version` 一致的令牌版本）；**不带** `type=refresh`（刷新令牌禁止访问业务接口）。
- 签名算法 HS256，密钥取自配置 `jwt.secret`；access token 有效期 7 天（压测几分钟内不会过期）。
- 生成时自检：签出的 token 可解析、`subject` 等于用户 ID、`tv` 与库内一致。

### 4.3 运行命令（项目根目录）

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
mvn test -Dtest=PerfTokenGeneratorTest
```

或在 IDEA 里直接运行该类的 `main` 方法。

### 4.4 失效与重生成

- 账号**改密 / 封禁 / 角色权限变更**会使 `token_version` 递增，旧 token 立即失效；出现 401 时先重跑本节生成器。
- 库内可用买家数量少于并发线程数时，CSV Data Set 配置 `recycle=true`，账号循环复用同一批 token。
- 凭空编造的用户 ID 不可用：用户不存在会被过滤器拒绝（401），必须是库内真实存在的账号。

## 5. 压测接口清单

context-path 前缀 `/api/v1` 由全局变量 `basePath` 带入，取样器路径显式拼接。

| # | 取样器 | 方法 | 路径 | 参数 | 鉴权 | 断言 |
|---|---|---|---|---|---|---|
| 1 | 登录（默认禁用） | POST | `${basePath}/user/login` | body `{"account":"${account}","password":"${password}","loginType":1}` | 公开 | HTTP 200；正则提取 `token` |
| 2 | 商品列表 | GET | `${basePath}/product/list` | `page=1`、`size=20` | 公开（游客可访问） | HTTP 200 |
| 3 | 商品详情 | GET | `${basePath}/product/${productId}` | 路径参数 id | 公开（游客可访问） | HTTP 200 |
| 4 | 个人订单 | GET | `${basePath}/order/list` | `page=1`、`size=20` | `Authorization: Bearer ${token}`（来自 CSV） | HTTP 200 |

- 鉴权凭证由 §4 生成的 CSV 提供：线程组开头的 CSV Data Set 读入变量 `${userId}` / `${token}` / `${account}`；订单取样器用 `${token}` 拼鉴权头。
- 登录取样器默认 `enabled=false`：基线不压登录接口。需要压登录时在 JMeter 里启用，登录正则提取的 `token` 会覆盖 CSV 变量。
- 过滤器解析 Bearer token 后注入 `X-User-Id`，取样器无需手传用户 ID。
- 详情接口 `productId` 默认 `1`；若该商品不存在，在「全局变量」改成库中一条在售商品 ID。

## 6. 全局变量（测试计划 → 全局变量）

| 变量 | 默认值 | 说明 |
|---|---|---|
| `protocol` | `http` | 协议 |
| `host` | `localhost` | 服务端地址 |
| `port` | `8080` | 端口 |
| `basePath` | `/api/v1` | context-path 前缀 |
| `account` | `13800000001` | 登录账号（仅重新启用登录取样器时使用） |
| `password` | `ww123456` | 登录密码（同上） |
| `productId` | `1` | 商品详情取样器的商品 ID |
| `threads` | `100` | 并发线程数（仅基线计划） |
| `rampUp` | `10` | Ramp-up 秒数（仅基线计划） |
| `duration` | `300` | 调度器持续时长，秒（仅基线计划） |

CSV Data Set 读入的变量（不在全局变量写死，来自 `result/perf-tokens.csv`）：`userId`、`token`、`account`。

## 7. 计划结构与元件

### 7.1 基线计划 `c2c-baseline.jmx`

```
Test Plan（c2c 压测基线）
├── 全局变量（Arguments）
├── HTTP Request Defaults      # 协议/主机/端口；路径前缀由取样器显式带 basePath
├── 全局 Header Manager        # Content-Type: application/json
├── 压测线程组（100 线程 / Ramp-up 10s / 循环无限 / 调度器 300s）
│   ├── CSV Data Set（result/perf-tokens.csv：userId,token,account；循环复用）
│   ├── 1. 登录 POST /user/login（默认禁用）
│   │     ├── 正则提取器：token
│   │     └── 断言：HTTP 200
│   ├── 2. 商品列表 GET /product/list
│   │     └── 断言：HTTP 200
│   ├── 3. 商品详情 GET /product/${productId}
│   │     └── 断言：HTTP 200
│   └── 4. 个人订单 GET /order/list
│         ├── 专属 Header Manager：Authorization: Bearer ${token}
│         └── 断言：HTTP 200
├── Aggregate Report           # 聚合报告：#Samples/Average/P90/P95/Error%/Throughput
├── CSV 结果落盘                # result/c2c-baseline.csv（逐样本）
└── View Results Tree（禁用）   # 仅调试用，正式压测保持关闭
```

### 7.2 联调计划 `c2c-smoke.jmx`

元件与基线一致，区别：线程组 1 线程、Ramp-up 1s、循环 1 轮、不开调度器；结果树启用，便于查看请求/响应；CSV 落 `result/c2c-smoke.csv`。

## 8. 执行步骤

### 8.1 先联调（推荐）

1. 启动后端，确认 8080 可访问。
2. 按 §4 跑生成器，确认 `JMeter/result/perf-tokens.csv` 已产出（普通买家 token 行数 > 0）。
3. GUI 打开 `c2c-smoke.jmx`，运行（绿色三角）。
4. 在「View Results Tree」核对：
   - 商品列表、商品详情返回 200，业务 `code=200`；
   - 个人订单返回 200 而非 401（说明 CSV 读取的 token 与鉴权头生效）。
5. 若订单 401：重跑 §4 生成最新 token（旧 token 可能因改密/封禁已失效）；若详情为空：换 `productId`。

### 8.2 正式基线压测

1. 关闭（或保持禁用）View Results Tree，避免渲染拖慢压测端。
2. 打开 `c2c-baseline.jmx`，确认线程组：100 线程、Ramp-up 10s、循环无限、调度器勾选、持续 300s。
3. 启动压测；期间不要在本机做其他重操作，观察后端日志无持续报错、无 OOM。
4. 结束后从 Aggregate Report 或 `result/c2c-baseline.csv` 抄录指标，回填 `../docs/压测基线报告.md`。

### 8.3 命令行（非 GUI）执行

在 `JMeter/` 目录下执行（相对路径 `result/...` 才生效）：

```powershell
# 联调
jmeter -n -t c2c-smoke.jmx -l result/c2c-smoke.jtl

# 基线压测 + 生成 HTML 报告（result/report 目录需为空或不存在）
jmeter -n -t c2c-baseline.jmx -l result/c2c-baseline.jtl -e -o result/report
```

非 GUI 模式不展示结果树；聚合数据看 jtl / HTML 报告。命令行调并发或时长，直接改 jmx「全局变量」对应值即可。

## 9. 鉴权口径与 401 排查

- 过滤器校验顺序：签名与过期 → Redis 登出黑名单（`logout:token:<token>`）→ 拒绝 `type=refresh` → `tv` 令牌版本比对 → 用户存在性。任一不满足返回 401。
- 常见 401 原因：
  - 旧 token 对应用户已改密 / 封禁 / 变更角色，`tv` 已递增 → 重跑 §4 生成器；
  - 用户在库内不存在 → 检查 CSV 中 `userId`；
  - token 被加入登出黑名单 → 重新生成；
  - 误用刷新令牌 → 生成器只签 access token，不要拿 refresh token 压业务接口。
- 管理员 / 审核接口不在本计划范围：其 token 还需 `role=ADMIN`、`adminLevel`、`perms` 等声明，应走 `/admin/login` 获取，不要用本生成器复签。

## 10. 指标解读（对照基线报告）

| 指标 | 含义 | 参考口径 |
|---|---|---|
| #Samples | 样本数（请求总数） | 4 个取样器分别统计 + 整体合计 |
| Average | 平均响应时间 ms | — |
| P90 / P95 | 90 / 95 分位响应时间 ms | P95 < 500ms 为良好；超过 1s 需关注 |
| Error % | 异常率 | 理想为 0 或接近 0；401 / 404 / 500 都计入异常 |
| Throughput | 吞吐 req/s | 前后对比用，不等同生产容量 |

## 11. 注意事项与安全

- 本基线**只压查询接口**；下单 / 支付 / 发货 / 取消等写接口不在范围。
- `jwt.secret` 是敏感信息：生成脚本只在本地 / 内网运行，不要把 secret 写进 jmx、不要把含 secret 的配置提交仓库。
- `result/` 下的 jtl / csv / HTML 报告与 `perf-tokens.csv` 均为运行产物，已在 `.gitignore` 忽略，不纳入代码。
- JMeter 与后端都用 JDK 17 启动；若 JMeter 报版本错误，先检查 `JAVA_HOME`。
- 断言类型（ResponseAssertion `test_type` 位掩码，5.6.3）：`1`=正则匹配、`2`=包含、`4`=取反、`8`=相等、`16`=子串。本计划断言响应码等于 `200`，用 `8`；在 GUI 里改断言时注意别勾上「Not」（取反位 `4`），否则 200 会被误判为失败。

## 12. 历史联调记录（上一轮可连环境）

> 以下为上一轮 MySQL / Redis 可达时的实跑记录，供口径参照；当前环境不可达时，以脚本与配置对齐为准，指标需在环境恢复后按 §8 跑满基线回填。

- 两个 `.jmx` 均在 JMeter 5.6.3 非 GUI 下成功建树解析，无 XML / 元件转换异常。
- 实际发出的 URL 已确认带 `/api/v1` 前缀：
  - `http://localhost:8080/api/v1/user/login`（登录取样器默认禁用，不发出）
  - `http://localhost:8080/api/v1/product/list?page=1&size=20`
  - `http://localhost:8080/api/v1/product/1`
  - `http://localhost:8080/api/v1/order/list?page=1&size=20`
- 实跑联调计划：登录禁用，列表 / 详情 / 订单 3 条样本均 `success=true`、HTTP 200；脚本签发的 CSV token 直连订单接口返回 200（非 401），鉴权头生效。业务指标（P90 / P95 / 吞吐）需跑满 300s 基线后回填。
