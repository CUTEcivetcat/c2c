# JMeter 压测脚本（M4-2 基线）

> 配套文档：`../docs/压测基线报告.md`（指标记录模板与判定口径）
> 工具：Apache JMeter 5.6.3，JDK17；压测范围仅限 4 个**只读/查询**接口，不压下单、支付、发货、取消等写接口。

## 1. 目录结构

```
jmeter/
├── c2c-baseline.jmx    # 基线压测计划：100 并发 / Ramp-up 10s / 持续 300s
├── c2c-smoke.jmx      # 联调计划：1 线程 1 轮，先跑通链路再切基线
├── README.md          # 本说明
└── result/            # 结果落盘目录（CSV / jtl / HTML 报告），已占位
    └── perf-tokens.csv  # 脚本生成的压测 token（运行产物，不入库；见 §3.1）
```

## 2. 环境前置

| 项 | 值 / 说明 |
|---|---|
| JMeter | `D:\ide\java\jmeter\apache-jmeter-5.6.3\bin\jmeter.bat` |
| JDK | 17。启动前先把 `JAVA_HOME` 指到 JDK17，避免落到 JDK25 |
| 后端地址 | `http://localhost:8080`，context-path `/api/v1` |
| 压测凭证 | 不写死单账号实时登录；先跑 §3.1 脚本从库内读取普通买家并签发 token 导出 CSV，JMeter 直接读取 |
| 启动后端 | 见 §6；确认日志出现 `Started C2cMonolithApplication` 再开 JMeter |

### 启动后端（PowerShell）

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
java -jar target\c2c-monolith.jar
```

## 3. 压测接口清单

context-path 前缀 `/api/v1` 已由全局变量 `basePath` 带入，取样器路径中显式拼接。

| # | 取样器 | 方法 | 路径 | 参数 | 鉴权 | 断言 |
|---|---|---|---|---|---|---|
| 1 | 登录（默认禁用） | POST | `${basePath}/user/login` | body `{"account":"${account}","password":"${password}","loginType":1}` | 公开 | HTTP 200；正则提取 `token` |
| 2 | 商品列表 | GET | `${basePath}/product/list` | `page=1`、`size=20` | 公开（游客可访问） | HTTP 200 |
| 3 | 商品详情 | GET | `${basePath}/product/${productId}` | 路径参数 id | 公开（游客可访问） | HTTP 200 |
| 4 | 个人订单 | GET | `${basePath}/order/list` | `page=1`、`size=20` | `Authorization: Bearer ${token}`（token 来自 CSV） | HTTP 200 |

- 鉴权凭证由 §3.1 脚本预签成 CSV，线程组开头的 CSV Data Set 读入变量 `${userId}` / `${token}` / `${account}`；订单取样器用 `${token}` 拼鉴权头。
- 登录取样器默认 `enabled=false`：基线不压登录接口，避免单账号实时登录被频控/验证码拦截。需要压登录接口时，在 JMeter 里把它重新启用即可（启用后登录正则提取的 `token` 会覆盖 CSV 变量）。
- 鉴权由过滤器解析 Bearer token 后注入 `X-User-Id`，取样器无需手传用户 ID。
- 详情接口的 `productId` 默认 `1`；若该商品不存在，联调时在「全局变量」里改成库中一条在售商品 ID。

### 3.1 生成压测 token（CSV）

压测前先在项目根目录跑测试侧生成器，从库内读取可用普通买家、按当前 `jwt.secret` 复刻登录口径签出 access token，导出 `result/perf-tokens.csv`：

```powershell
$env:JAVA_HOME="C:\Users\wm\.jdks\ms-17.0.17"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
cd D:\Ide_HOME\IDe_HOME\java\c2c
mvn test -Dtest=PerfTokenGeneratorTest
```

- 脚本位置：`src/test/java/com/c2c/common/infra/PerfTokenGeneratorTest.java`（也含 `main` 入口，可在 IDEA 直接运行）。
- CSV 列：`userId,token,account`；筛选 `role=0 AND status=1` 且排除 AI 助手账号；claims 带 `role`、`tv`（令牌版本），与过滤器校验口径一致。
- 当前库内可用普通买家约 7 个，少于 100 线程；CSV Data Set 配置 `recycle=true`，账号循环复用。
- token 有效期 7 天；改密/封禁使令牌版本递增后，旧 token 会失效，需重新跑脚本生成。

## 4. 全局变量（测试计划 → 全局变量）

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

CSV Data Set 读入的变量（不在全局变量里写死，来自 `result/perf-tokens.csv`）：`userId`、`token`、`account`。

## 5. 计划结构与元件

### 5.1 基线计划 `c2c-baseline.jmx`

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

### 5.2 联调计划 `c2c-smoke.jmx`

元件与基线一致，区别：线程组 1 线程、循环 1 轮、不开调度器；结果树启用，便于查看请求/响应；CSV 落 `result/c2c-smoke.csv`。

## 6. 执行步骤

### 6.1 先联调（推荐）

1. 启动后端，确认 8080 可访问。
2. 先按 §3.1 跑生成器，确认 `result/perf-tokens.csv` 已产出（7 行普通买家 token）。
3. GUI 打开 `c2c-smoke.jmx`，运行（绿色三角）。
4. 在「View Results Tree」核对：
   - 商品列表、商品详情返回 200，业务 `code=200`；
   - 个人订单返回 200 而非 401（说明 CSV 读取的 token 与鉴权头生效）。
5. 若订单 401：重新跑 §3.1 脚本生成最新 token（旧 token 可能因改密/封禁已失效）；若详情数据为空：换 `productId`。

### 6.2 正式基线压测

1. 关闭（或保持禁用）View Results Tree，避免渲染拖慢压测端。
2. 打开 `c2c-baseline.jmx`，确认线程组：100 线程、Ramp-up 10s、循环无限、调度器勾选、持续 300s。
3. 启动压测；期间不要在本机做其他重操作，观察后端日志无持续报错、无 OOM。
4. 结束后从 Aggregate Report 或 `result/c2c-baseline.csv` 抄录指标，回填 `../docs/压测基线报告.md` §5。

### 6.3 命令行（非 GUI）执行

在 `jmeter/` 目录下执行（相对路径 `result/...` 才生效）：

```powershell
# 联调
jmeter -n -t c2c-smoke.jmx -l result/c2c-smoke.jtl

# 基线压测 + 生成 HTML 报告（result/report 目录需为空或不存在）
jmeter -n -t c2c-baseline.jmx -l result/c2c-baseline.jtl -e -o result/report
```

> 非 GUI 模式下不展示结果树；聚合数据看 jtl / HTML 报告。命令行调并发或时长，直接改 jmx「全局变量」对应值即可。

## 7. 指标解读（对照基线报告 §5）

| 指标 | 含义 | 参考口径 |
|---|---|---|
| #Samples | 样本数（请求总数） | 4 个取样器分别统计 + 整体合计 |
| Average | 平均响应时间 ms | — |
| P90 / P95 | 90 / 95 分位响应时间 ms | P95 < 500ms 为良好；超过 1s 需关注 |
| Error % | 异常率 | 理想为 0 或接近 0；401/404/500 都计入异常 |
| Throughput | 吞吐 req/s | 前后对比用，不等同生产容量 |

## 8. 注意事项

- 本基线**只压查询接口**；下单/支付/发货/取消等写接口不在范围，避免产生脏订单。
- 压测凭证来自 §3.1 生成的 CSV，库内普通买家数量少于 100 线程，CSV `recycle=true` 循环复用同一批 token；登录接口默认不压。
- 压测端与后端同机部署，吞吐受本机资源限制；结论用于**改造前后相对对比**和瓶颈初判，不等同生产容量。
- JMeter 与后端都用 JDK17 启动；若 JMeter 报版本错误，检查 `JAVA_HOME`。
- `result/` 下的 jtl/csv/HTML 报告为运行产物，可按需清理，不纳入代码。

## 9. 已校验结论（本轮）

- 两个 `.jmx` 均已在 JMeter 5.6.3 非 GUI 下成功建树解析，无 XML/元件转换异常。
- 实际发出的 URL 已确认带 `/api/v1` 前缀：
  - `http://localhost:8080/api/v1/user/login`（登录取样器默认禁用，不发出）
  - `http://localhost:8080/api/v1/product/list?page=1&size=20`
  - `http://localhost:8080/api/v1/product/1`
  - `http://localhost:8080/api/v1/order/list?page=1&size=20`
- 已起后端实跑联调计划：登录禁用，列表/详情/订单 3 条样本均 `success=true`、HTTP 200；脚本签发的 CSV token 直连订单接口返回 200（非 401），鉴权头生效。业务指标（P90/P95/吞吐）需按 §6 跑满 300s 基线后回填。
- 断言类型（ResponseAssertion `test_type` 位掩码，5.6.3）：`1`=正则匹配、`2`=包含、`4`=取反、`8`=相等、`16`=子串。本计划断言响应码等于 `200`，用 `8`；若在 GUI 里改断言，注意别勾上「Not」（取反位 `4`），否则 200 会被误判为失败。
