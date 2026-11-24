# 校园便当

校园便当是一家店的外卖课程项目，业务范围参考《Spring Boot 企业级开发教程（第 2 版）》第 10 章。顾客和商家从同一首页选择各自的入口。

- 顾客可以用手机号登录，浏览菜单、选口味、维护收货地址、下单并查看订单状态。
- 员工可以维护分类、菜品和套餐，查看备餐明细，并将已支付订单标记为已派送和已完成。

后端使用 Java 17、Spring Boot、Spring Security、MyBatis-Plus 和 SQL Server。前端使用 Vue 与 Vite。业务规则放在 Service，数据库访问放在 Mapper，Flyway 按顺序执行数据库迁移。

## 本地运行

准备 JDK 17、Maven 3.6.3 或以上、Node.js 24 和 SQL Server。项目默认连接本机 `127.0.0.1:1433`，使用 Windows 集成认证。

### 1. 创建数据库

在 SQL Server 中创建一个空库 `reggie`。第一次启动时，Flyway 会自动建表。

```sql
CREATE DATABASE reggie;
```

如需连接其他 SQL Server，可通过环境变量 `SPRING_DATASOURCE_URL`、`SPRING_DATASOURCE_USERNAME`、`SPRING_DATASOURCE_PASSWORD` 提供连接配置。

### 2. 启动后端

在项目根目录用 PowerShell 7 执行。第一次需要创建 `admin` 账号时，按提示输入自选密码。成功创建后，普通启动只需执行其中的 Maven 命令。

```powershell
$env:REGGIE_ADMIN_PASSWORD = Read-Host '首次设置 admin 密码' -MaskInput
try {
    mvn "-Dspring-boot.run.profiles=demo" spring-boot:run
} finally {
    Remove-Item Env:REGGIE_ADMIN_PASSWORD -ErrorAction SilentlyContinue
}
```

`demo` 配置用于本机课程演示。顾客验证码显示在登录页，支付按钮执行模拟支付。默认配置关闭这两项演示功能。

### 3. 启动前端

在另一个终端从项目根目录执行。

```powershell
cd frontend
npm.cmd ci
npm.cmd run dev
```

打开 [校园便当首页](http://127.0.0.1:5173/)。新数据库的菜单初始为空。先从商家端添加并启售商品，再从顾客端登录、点餐和模拟支付。商家在订单管理页更新派送状态后，顾客可以在「我的订单」查到。

## 登录会话

员工和顾客使用独立的登录状态。默认闲置超时和浏览器 Cookie 有效期均为 7 天，可以用下列环境变量调整。

| 变量 | 默认值 | 用途 |
|---|---|---|
| `REGGIE_SESSION_TIMEOUT` | `7d` | 服务端会话的闲置超时 |
| `REGGIE_SESSION_COOKIE_MAX_AGE` | `7d` | 浏览器保存会话 Cookie 的时长 |
| `REGGIE_PERSIST_SESSIONS` | `true` | 正常停止后端时保存会话，重启后恢复 |
| `REGGIE_SESSION_STORE_DIR` | `./.local/sessions` | 本地会话文件目录 |

`.gitignore` 已排除本地会话、环境变量文件、数据库文件和构建产物。仓库用源码与迁移脚本重建项目环境。

## 构建

在项目根目录运行后端构建。前端依赖安装完成后，可在 `frontend` 目录单独构建。

```powershell
mvn verify
cd frontend
npm.cmd run build
```

