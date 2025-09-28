# 简化版本 - 专门为微信云托管优化
FROM openjdk:17-jre-slim

# 设置时区
RUN ln -sf /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo "Asia/Shanghai" > /etc/timezone

# 设置工作目录
WORKDIR /app

# 复制jar包（云托管构建时会自动处理）
COPY target/*.jar app.jar

# 暴露端口（必须与application.yml中的server.port一致）
EXPOSE 8080

# 启动应用（云托管会自动处理健康检查）
ENTRYPOINT ["java", "-jar", "app.jar"]
