# Business 模块端到端测试指南

## 测试前提：启动所有依赖服务

### 方式 1：手动启动各服务（推荐）

在 4 个不同的终端窗口中分别启动：

```bash
# 终端 1 - Auth 服务 (端口 8081)
cd auth
../mvnw.cmd spring-boot:run

# 终端 2 - Audit 服务 (端口 8082)
cd audit
../mvnw.cmd spring-boot:run

# 终端 3 - Approval 服务 (端口 8083)
cd approval
../mvnw.cmd spring-boot:run

# 终端 4 - Business 服务 (端口 8084)
cd business
../mvnw.cmd spring-boot:run
```

### 方式 2：使用 IDEA 启动

在 IDEA 中配置 4 个 Spring Boot Run Configuration：
1. AuthServiceApplication - port 8081
2. AuditServiceApplication - port 8082
3. ApprovalServiceApplication - port 8083
4. BusinessServiceApplication - port 8084

全部启动后，运行端到端测试。

## 运行端到端测试

```bash
cd business
../mvnw.cmd test -Dtest=BusinessControllerE2ETest
../mvnw.cmd test -Dtest=ApprovalCallbackControllerE2ETest
```

## 测试说明

端到端测试会真实调用：
- Business Controller → Feign → Approval Service
- Business Controller → Feign → Auth Service

所有服务使用 H2 内存数据库，测试数据不会持久化。
