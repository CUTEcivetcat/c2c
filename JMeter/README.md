# JMeter 内网压测说明

> 配套：`../docs/压测基线报告.md`（指标记录模板与判定口径）
> 工具：Apache JMeter 5.6.3，JDK 17。压测范围仅限**只读 / 查询**接口，不压下单、支付、发货、取消等写接口，避免产生脏订单与脏余额。

## 1. 压测原则

- **凭证预生成，不实时登录**：压测不依赖单账号反复调 `/user/login`（避免被频控 / 验证码拦截，也不把登录接口计入业务吞吐）。库内普通买家不足并发数时先用造号程序批量扩充（§4.3 第一步），再跑测试侧生成器从库内读取可用普通买家，按当前 `jwt.secret` 复刻登录签发口径签出 access token，导出 CSV 供 JMeter 直接读取。
- **只压查询链路**：列表、详情、订单查询等只读取样器；写接口不在本计划范围。
- **结论用途**：用于改造前后的相对对比与瓶颈初判；压测端与后端同机部署时，吞吐受本机资源限制，不等同生产容量。

## 2. 目录结构

```
JMeter/
├── c2c-baseline.jmx    # 基线压测计划：100 并发 / Ramp-up 10s / 持续 300s
├── c2c-smoke.jmx       # 联调计划：1 线程 1 轮，先跑通链路再切基线
├── seed-perf-users.sql # 备用造号 SQL（不启 Java 环境时使用，见 §4.3）
├── README.md           # 本说明
└── result/             # 结果落盘目录（运行产物，不入库）
    └── perf-tokens.csv # 生成器产出的压测 token（见 §4）
```

## 3. 环境前置与后端启动

### 3.1 前置条件

| 项 | 值 / 说明 |
|---|---|
| JMeter | 5.6.3；本机已装 `D:\ide\java\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat`，命令 `jmeter` 已在 PATH |
| JDK | 17（与后端 `java.version` 一致）；JMeter 启动前 `JAVA_HOME` 指到 JDK 17 |
| 后端地址 | 默认 `http://localhost:8080`，context-path `/api/v1`；端口在 jmx「全局变量」里改 |
| MySQL | 必须可达：生成器要读库内买家与 `token_version`，后端运行也依赖库 |
| Redis | 必须可达：过滤器校验令牌版本 / 登出黑名单走 Redis |
| 配置来源 | `config/application-pro.yml`（含 `jwt.secret`、数据源、Redis 占位符；敏感，不入库） |

### 3.2 启动后端

任选其一，确认日志出现 `Started C2cMonolithApplication`、端口可访问后再开 JMeter。

- **IDEA**：直接运行主类 `C2cMonolithApplication`。
- **命令行（spring-boot:run）**：

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
mvn -o spring-boot:run "-Dspring-boot.run.arguments=--server.port=8080"
```

- **命令行（jar）**：先 `mvn -o package -DskipTests` 打出 `target\c2c-monolith-1.0.0.jar`，再 `java -jar target\c2c-monolith-1.0.0.jar`。

> 临时换端口（如 8081）：启动参数改 `--server.port=8081`，同时把 jmx「全局变量」的 `port` 改成同端口。

## 4. 生成压测 token（压测前必做）

### 4.1 脚本与产物

| 项 | 值 |
|---|---|
| 造号程序 | `src/test/java/com/c2c/common/infra/PerfUserSeedTest.java`（不启 Spring，纯 JDBC；含 `main` 交互入口与 `@Test` 入口）：读 `application-pro.yml` 库配置，按昵称前缀与数量批量插入普通压测买家，密码明文 `ww123456`（BCrypt 现场计算），号段 `15000000001` 起连续分配 |
| 备用造号脚本 | `JMeter/seed-perf-users.sql`（幂等 SQL，不启 Java 环境时使用）：口径与上面测试类一致，默认 50 个、号段 `15000000001~15000000050` |
| 生成脚本 | `src/test/java/com/c2c/common/infra/PerfTokenGeneratorTest.java`（含 `main` 入口，IDEA 可直接运行；也可走 Maven 测试） |
| 产物路径 | `JMeter/result/perf-tokens.csv`（相对项目根） |
| CSV 列 | `userId,token,account`；首行为表头，CSV Data Set 跳过首行 |

### 4.2 签发口径（与过滤器校验一致）

- 筛选 `role = 0 AND status = 1` 的普通买家，排除 AI 助手账号（`90000001`）。
- claims 携带：`role`（角色字符串）、`tv`（与库内 `token_version` 一致的令牌版本）；**不带** `type=refresh`（刷新令牌禁止访问业务接口）。
- 签名算法 HS256，密钥取自配置 `jwt.secret`；access token 有效期 7 天（压测几分钟内不会过期）。
- 生成时自检：签出的 token 可解析、`subject` 等于用户 ID、`tv` 与库内一致。

### 4.3 运行命令（项目根目录）

**第一步：批量造号（库内普通买家不足并发数时执行，幂等可重复）**

优先用测试类（不启 Spring，直连库）：

- IDEA 里直接运行 `PerfUserSeedTest` 的 `main` 方法，启动后按提示填写**昵称前缀**与**数量**（空回车走默认：前缀 `压测用户`、数量 50）；
- 或 Maven 非交互执行，前缀/数量走环境变量（缺省同上默认值）：

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
$env:PERF_SEED_PREFIX="压测用户"   # 可选，默认 压测用户
$env:PERF_SEED_COUNT="50"         # 可选，默认 50，上限 9999
mvn -o test -Dtest=PerfUserSeedTest
```

- 程序先清理 `15000000%` 号段的旧压测号，再插入指定数量的 `role=0, status=1` 普通买家；密码明文 `ww123456`，BCrypt 现场算哈希；手机号按 `15000000001` 起连续分配。
- 造号只影响本号段，不动管理员、审核员与 AI 助手账号；压测结束可直接按同一号段清理。
- 不启 Java 环境时的备用方式：`mysql -uroot -p c2c < JMeter/seed-perf-users.sql`（改脚本末尾递归 CTE 上限调整数量）。

**第二步：生成压测 token**

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
mvn -o test -Dtest=PerfTokenGeneratorTest
```

或在 IDEA 里直接运行该类的 `main` 方法。跑完打开 `JMeter/result/perf-tokens.csv`，确认表头 `userId,token,account` 且数据行数 > 0（新造的压测号会被生成器自动纳入）。

### 4.4 失效与重生成

- 账号**改密 / 封禁 / 角色权限变更**会使 `token_version` 递增，旧 token 立即失效；出现 401 时先重跑本节生成器。
- 库内可用买家数量少于并发线程数时，CSV Data Set 配置 `recycle=true`，账号循环复用同一批 token；想一人一号时先按 §4.3 第一步跑造号程序扩充买家，再重跑生成器。
- 凭空编造的用户 ID 不可用：用户不存在会被过滤器拒绝（401），必须是库内真实存在的账号。

## 5. JMeter 安装、启动与导入计划

### 5.1 安装前提

- 已装 JDK 17 并配好 `JAVA_HOME`（JMeter 是 Java 程序，版本不对会启动报错）。
- 解压 Apache JMeter 5.6.3（绿色软件，无需安装）；本机目录 `D:\ide\java\jmeter\apache-jmeter-5.6.3`。
- 本计划只用 JMeter 标准元件，**不需要**安装第三方插件（Plugins Manager）。

### 5.2 启动 JMeter

**推荐方式：在 `JMeter/` 目录下带计划一起启动**（保证 CSV、结果文件的相对路径 `result/...` 能正确解析）：

```powershell
cd D:\Ide_HOME\IDe_HOME\java\c2c\JMeter
jmeter -t c2c-smoke.jmx
```

`jmeter` 即 `D:\ide\java\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat`；若命令不在 PATH，用全路径：

```powershell
cd D:\Ide_HOME\IDe_HOME\java\c2c\JMeter
D:\ide\java\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat -t c2c-smoke.jmx
```

### 5.3 导入（打开）jmx 计划文件

两种方式任选：

- **方式 A（命令行带文件）**：上面的 `jmeter -t c2c-smoke.jmx`，启动后计划已载入。
- **方式 B（先开 GUI 再打开文件）**：
  1. 双击 `bin\jmeter.bat` 启动空白 GUI；
  2. 顶部菜单 **File → Open**；
  3. 选择 `D:\Ide_HOME\IDe_HOME\java\c2c\JMeter\c2c-smoke.jmx`（基线用 `c2c-baseline.jmx`）；
  4. 左侧元件树出现计划结构即导入成功。

> **相对路径注意**：jmx 里 CSV 与结果文件写的是相对路径 `result/...`，解析基准是启动 JMeter 时的当前目录。请务必从 `JMeter/` 目录启动；若先从 `bin` 目录双击启动导致读不到 CSV，把 CSV Data Set 的 filename 改成 `perf-tokens.csv` 的绝对路径即可。

### 5.4 界面与元件树速览

左侧树形结构从上到下：

| 元件 | 作用 | 在哪改 |
|---|---|---|
| Test Plan（测试计划） | 根节点；下挂「全局变量」 | 点「全局变量」改地址 / 端口 / 并发参数 |
| HTTP Request Defaults | 默认协议、主机、端口 | 一般不动，由全局变量 `${host}` `${port}` 带入 |
| 全局 Header Manager | 公共请求头 `Content-Type: application/json` | 一般不动 |
| 线程组 | 并发线程数、Ramp-up、持续时长 | 基线计划在这里调并发 |
| CSV Data Set | 读 `perf-tokens.csv`，注入 `${userId}` `${token}` `${account}` | 核对文件路径与变量名 |
| HTTP 取样器 | 4 个请求（登录 / 列表 / 详情 / 订单） | 看方法、路径、参数、鉴权头 |
| 监听器 | Aggregate Report、CSV 落盘、View Results Tree | 看结果与指标 |

### 5.5 打开后必做核对清单

1. **后端地址**：点「全局变量」，确认 `host` / `port` 与正在运行的后端一致（默认 `localhost` / `8080`；临时实例改对应端口）。
2. **CSV 数据集**：点线程组下的 **CSV Data Set**，核对：
   - Filename：`result/perf-tokens.csv`，文件存在且行数 > 0；
   - Delimiter：`,`；File encoding：`UTF-8`；
   - Variable Names：`userId,token,account`（顺序与 CSV 表头一致）；
   - Ignore first line：勾选（跳过表头）；Recycle on EOF：勾选（账号不足时循环复用）。
3. **订单鉴权头**：展开「个人订单」取样器下的 Header Manager，确认有 `Authorization: Bearer ${token}`。
4. **商品 ID**：全局变量 `productId` 默认 `1`；若该商品不存在，改成库中一条在售商品 ID。

## 6. 压测接口清单

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

## 7. 全局变量（测试计划 → 全局变量）

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

## 8. 计划结构与元件

### 8.1 基线计划 `c2c-baseline.jmx`

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

### 8.2 联调计划 `c2c-smoke.jmx`

元件与基线一致，区别：线程组 1 线程、Ramp-up 1s、循环 1 轮、不开调度器；结果树启用，便于查看请求/响应；CSV 落 `result/c2c-smoke.csv`。

## 9. 测试执行

### 9.1 第一步：GUI 联调 `c2c-smoke.jmx`

先跑通单线程链路，确认请求地址、鉴权头、响应都正常，再上并发基线。

1. 确认后端已启动、`result/perf-tokens.csv` 已生成（§4）。
2. 按 §5.2 / §5.3 打开 `c2c-smoke.jmx`。
3. 点线程组，确认：Number of threads = 1、Ramp-up = 1、Loops = 1（联调只跑一轮）。
4. 点顶部绿色三角 **Start** 运行（首次运行可能弹「保存测试计划」提示，选否即可）。
5. 打开左侧 **View Results Tree** 监听器：
   - 样本**绿色** = 通过，**红色** = 失败；
   - 点某样本，右侧看 **Sampler result**（响应码、耗时）、**Request**（实际请求头 / URL）、**Response Body**（响应体）。
6. 逐条核对：
   - 商品列表、商品详情：HTTP 200，响应体 `code=200`；
   - 个人订单：HTTP 200 而非 401（说明 CSV 读取的 token 与鉴权头生效；账号无订单时 `records=[]` 属正常）。
7. 失败先看：Request 里 URL / 鉴权头是否正确，Response Body 的 `code` 与 `message`（401 排查见 §10）。
8. 调完点扫帚 **Clear** 清除结果，再重跑。

### 9.2 第二步：基线并发压测 `c2c-baseline.jmx`

1. 打开 `c2c-baseline.jmx`。
2. 点线程组，确认并发参数：
   - Number of threads（`threads`）= 100；
   - Ramp-up period（`rampUp`）= 10 秒；
   - 勾选 **Scheduler**，Duration（`duration`）= 300 秒；
   - Loop Controller = 无限循环（`-1`），由调度器控制总时长。
3. 确认 **View Results Tree** 处于禁用（正式压测不开结果树，避免逐样本渲染拖慢压测端）。
4. 点绿色三角启动；压测期间不要在本机做其他重操作，观察后端日志无持续报错、无 OOM。
5. 结束后看 **Aggregate Report**，或打开落盘文件 `result/c2c-baseline.csv`，把 #Samples / Average / P90 / P95 / Error% / Throughput 回填 `../docs/压测基线报告.md`。

### 9.3 命令行（非 GUI）执行

GUI 只用于联调；正式压测用非 GUI 模式，开销更小。**在 `JMeter/` 目录下执行**（相对路径 `result/...` 才生效）：

```powershell
cd D:\Ide_HOME\IDe_HOME\java\c2c\JMeter

# 联调：输出 jtl 结果文件
jmeter -n -t c2c-smoke.jmx -l result/c2c-smoke.jtl

# 基线压测：输出 jtl
jmeter -n -t c2c-baseline.jmx -l result/c2c-baseline.jtl
```

- `-n` 非 GUI 模式；`-t` 指定计划；`-l` 指定结果日志。
- 非 GUI 不展示结果树；聚合数据看 jtl 或 §9.4 的 HTML 报告。

### 9.4 生成 HTML 测试报告

基线压测后用 `-e -o` 生成可视化报告（`result/report` 目录必须不存在或为空）：

```powershell
cd D:\Ide_HOME\IDe_HOME\java\c2c\JMeter
jmeter -n -t c2c-baseline.jmx -l result/c2c-baseline.jtl -e -o result/report
```

生成后用浏览器打开 `result/report/index.html`，看汇总响应时间、吞吐、错误率、时间线曲线。

### 9.5 常用调参位置

| 想改什么 | 改哪里 |
|---|---|
| 后端地址 / 端口 | 全局变量 `host` / `port` / `protocol` / `basePath` |
| 并发线程数 | 基线线程组 Number of threads，或全局变量 `threads` |
| 爬升时长 | 线程组 Ramp-up，或全局变量 `rampUp` |
| 压测持续时长 | 勾选 Scheduler 后填 Duration，或全局变量 `duration` |
| 商品 ID | 全局变量 `productId` |
| 登录账号 / 密码 | 全局变量 `account` / `password`（仅启用登录取样器时） |

## 10. 鉴权口径与 401 排查

- 过滤器校验顺序：签名与过期 → Redis 登出黑名单（`logout:token:<token>`）→ 拒绝 `type=refresh` → `tv` 令牌版本比对 → 用户存在性。任一不满足返回 401。
- 常见 401 原因：
  - 旧 token 对应用户已改密 / 封禁 / 变更角色，`tv` 已递增 → 重跑 §4 生成器；
  - 用户在库内不存在 → 检查 CSV 中 `userId`；
  - token 被加入登出黑名单 → 重新生成；
  - 误用刷新令牌 → 生成器只签 access token，不要拿 refresh token 压业务接口。
- 其他常见报错：
  - **连接拒绝 / 超时**：后端没起，或 jmx `port` 与后端实际端口不一致；
  - **商品详情 404**：`productId` 不存在或已下架，换一条在售商品；
  - **CSV 读不到 / `${token}` 为空**：检查 §5.5 的文件路径、变量名顺序、首行是否跳过；必要时改用绝对路径。
- 管理员 / 审核接口不在本计划范围：其 token 还需 `role=ADMIN`、`adminLevel`、`perms` 等声明，应走 `/admin/login` 获取，不要用本生成器复签。

## 11. 指标解读（对照基线报告）

| 指标 | 含义 | 参考口径 |
|---|---|---|
| #Samples | 样本数（请求总数） | 4 个取样器分别统计 + 整体合计 |
| Average | 平均响应时间 ms | — |
| P90 / P95 | 90 / 95 分位响应时间 ms | P95 < 500ms 为良好；超过 1s 需关注 |
| Error % | 异常率 | 理想为 0 或接近 0；401 / 404 / 500 都计入异常 |
| Throughput | 吞吐 req/s | 前后对比用，不等同生产容量 |

## 12. 注意事项与安全

- 本基线**只压查询接口**；下单 / 支付 / 发货 / 取消等写接口不在范围。
- `jwt.secret` 是敏感信息：生成脚本只在本地 / 内网运行，不要把 secret 写进 jmx、不要把含 secret 的配置提交仓库。
- `result/` 下的 jtl / csv / HTML 报告与 `perf-tokens.csv` 均为运行产物，已在 `.gitignore` 忽略，不纳入代码。
- JMeter 与后端都用 JDK 17 启动；若 JMeter 报版本错误，先检查 `JAVA_HOME`。
- 断言类型（ResponseAssertion `test_type` 位掩码，5.6.3）：`1`=正则匹配、`2`=包含、`4`=取反、`8`=相等、`16`=子串。本计划断言响应码等于 `200`，用 `8`；在 GUI 里改断言时注意别勾上「Not」（取反位 `4`），否则 200 会被误判为失败。

## 13. 联调与实测记录

### 13.1 本轮复测（2026-11-17，pro 测试库）

- MySQL 3306 / Redis 6379 连通；生成器连库读普通买家（`role=0 AND status=1`）签出 7 个 access token，写入 `result/perf-tokens.csv`，自检（subject、`tv` 一致）通过。
- 临时后端实例（端口 8081，profile `pro`）逐条复核：
  - `GET /product/list?page=1&size=20`（无 token）→ HTTP 200，业务 `code=200`，返回商品记录；
  - `GET /order/list?page=1&size=20`（带 CSV `Bearer ${token}`）7 条 token 全部 HTTP 200、`code=200`（账号无订单时 `records=[]` 属正常）；
  - 对照：订单接口不带 token → HTTP 401 `请先登录`，确认鉴权拦截生效，token 并非被放行。
- 结论：离线复签口径与过滤器校验一致，CSV token 可直接喂鉴权头。基线长压指标（P90 / P95 / 吞吐）仍需按 §9 跑满 300s 后回填。

### 13.2 上一轮建树与 URL 核对记录（供参照）

- 两个 `.jmx` 均在 JMeter 5.6.3 非 GUI 下成功建树解析，无 XML / 元件转换异常。
- 实际发出的 URL 已确认带 `/api/v1` 前缀：
  - `http://localhost:8080/api/v1/user/login`（登录取样器默认禁用，不发出）
  - `http://localhost:8080/api/v1/product/list?page=1&size=20`
  - `http://localhost:8080/api/v1/product/1`
  - `http://localhost:8080/api/v1/order/list?page=1&size=20`
- 联调计划：登录禁用，列表 / 详情 / 订单 3 条样本均 `success=true`、HTTP 200；CSV token 直连订单接口返回 200（非 401），鉴权头生效。
