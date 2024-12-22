# QuickFIX/J Spring Boot Demo

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)

这是一个基于 Spring Boot 的 QuickFIX/J 完整示例项目，展示了如何在 Spring Boot 应用中集成和使用 QuickFIX/J 来实现 FIX 协议通信。

## 功能特性

- 完整的 FIX 协议服务端（Acceptor）实现
- 完整的 FIX 协议客户端（Initiator）实现
- 支持 FIX 4.4 协议版本
- 集成 Spring Boot 框架
- 支持动态会话管理
- 包含订单创建和取消的完整业务流程示例

## 项目结构

.

├── client-initiator/ # FIX 客户端模块

│ ├── src/main/java/ # Java 源代码

│ └── src/main/resources/ # 配置文件

└── server-acceptor/ # FIX 服务端模块

├── src/main/java/ # Java 源代码

└── src/main/resources/ # 配置文件

## 快速开始

### 环境要求

- JDK 1.8+
- Maven 3.6+
- Spring Boot 2.x

### 构建项目

```bash
mvn clean package
```

### 运行服务端

```bash
cd server-acceptor
mvn spring-boot:run
```

### 运行客户端

```bash
cd client-initiator
mvn spring-boot:run
```

## 配置说明

### 服务端配置

服务端的主要配置文件位于 `server-acceptor/src/main/resources/server.cfg`：

```properties
# FIX 服务端配置
ConnectionType=acceptor
SocketAcceptPort=9876
StartTime=00:00:00
EndTime=00:00:00
```

### 客户端配置

客户端的主要配置文件位于 `client-initiator/src/main/resources/client.cfg`：

```properties
# FIX 客户端配置
ConnectionType=initiator
SocketConnectHost=127.0.0.1
SocketConnectPort=9876
```

## 使用示例

### 发送新订单

```java
FixClientApplication client = context.getBean(FixClientApplication.class);
client.createOrder();
```

### 取消订单

```java
FixClientApplication client = context.getBean(FixClientApplication.class);
client.cancelOrder();
```

## 详细文档

更多详细信息请参考：[项目文档](https://www.yuque.com/honghuan-doc/snze8s/avbgl81hsgfolns5)

## 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交改动 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 联系方式

- 作者：honghuan
- 邮箱：[your-email@example.com]
- 项目地址：[GitHub Repository URL]

## 致谢

- [QuickFIX/J](https://www.quickfixj.org/)
- [Spring Boot](https://spring.io/projects/spring-boot)
