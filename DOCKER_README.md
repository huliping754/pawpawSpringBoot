# 宠物管理后台 微信云托管部署指南

## 📋 概述

本项目已针对**微信云托管**环境进行优化，提供简洁高效的部署方案：

- `Dockerfile` - 云托管专用镜像构建文件
- `application.yml` - 统一的配置文件（包含生产环境配置）
- `.dockerignore` - 构建优化文件

## 🚀 云托管部署

### 1. 准备部署文件

确保你的项目根目录包含以下文件：

```
📁 项目根目录
├── 📄 Dockerfile                 (云托管优化版)
├── 📄 pom.xml                    (保持不变)
├── 📁 src/                       (源代码)
│   └── 📁 main/
│       ├── 📁 java/
│       └── 📁 resources/
│           └── 📄 application.yml (统一配置文件)
└── 📄 .dockerignore              (精简版)
```

### 2. 本地构建测试

```bash
# 构建项目
mvn clean package -DskipTests

# 本地测试Docker镜像
docker build -t petcare-admin:latest .
docker run -p 8080:8080 petcare-admin:latest
```

### 3. 云托管部署

1. 将项目代码上传到微信云托管
2. 配置环境变量（数据库连接等）
3. 启动部署

## 🔧 环境变量配置

在云托管控制台配置以下环境变量：

| 变量名 | 说明 | 示例值 |
|--------|------|--------|
| `DB_HOST` | 数据库主机 | your-mysql-host |
| `DB_PORT` | 数据库端口 | 3306 |
| `DB_NAME` | 数据库名称 | pawpaw |
| `DB_USERNAME` | 数据库用户名 | root |
| `DB_PASSWORD` | 数据库密码 | your-password |
| `WX_APPID` | 微信小程序AppID | wx44eeb1168aa74f2c |
| `WX_SECRET` | 微信小程序Secret | your-wx-secret |

## 🌐 服务访问

部署成功后，可通过云托管提供的域名访问：

- **应用主页**: <https://your-domain.com>
- **API 文档**: <https://your-domain.com/swagger-ui.html>
- **健康检查**: <https://your-domain.com/actuator/health>

## 📊 常用命令

```bash
# 本地开发
mvn clean package -DskipTests

# 本地Docker测试
docker build -t petcare-admin:latest .
docker run -p 8080:8080 petcare-admin:latest

# 查看容器日志
docker logs <container-id>
```

## 🔍 故障排查

### 1. 构建失败

- 检查 `pom.xml` 依赖是否正确
- 确保 `mvn clean package` 能正常执行

### 2. 启动失败

- 检查环境变量配置
- 确认数据库连接信息正确
- 查看云托管日志

### 3. 数据库连接问题

- 确认数据库服务可访问
- 检查网络连接和防火墙设置
- 验证数据库用户名密码

## 📈 性能优化

### JVM 参数调优

如需自定义JVM参数，可在云托管控制台配置：

```bash
JAVA_OPTS=-Xms512m -Xmx1024m -XX:+UseG1GC
```

### 数据库连接池

在 `application.yml` 中调整：

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10  # 根据云托管资源调整
```

## 🔒 安全建议

1. **环境变量**: 所有敏感信息必须使用环境变量配置
   - 数据库密码：`DB_PASSWORD`
   - 微信小程序密钥：`WX_SECRET`
   - 其他API密钥和令牌
2. **数据库**: 使用云数据库服务，避免本地数据库
3. **HTTPS**: 云托管自动提供HTTPS支持
4. **日志**: 避免在日志中输出敏感信息
5. **代码审查**: 确保代码中不包含硬编码的敏感信息

## 🆘 技术支持

如遇到问题，请检查：

1. 云托管服务状态
2. 环境变量配置
3. 数据库连接
4. 应用日志信息
