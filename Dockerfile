# 使用官方 OpenJDK 17 镜像
FROM openjdk:17-jdk-slim

# 设置工作目录
WORKDIR /app

# 复制项目文件
COPY pom.xml .
COPY src ./src

# 安装Maven并构建应用
RUN apt-get update && apt-get install -y maven
RUN mvn clean package -DskipTests

# 暴露端口
EXPOSE 8080

# 启动应用 - 使用正确的Jar包路径
ENTRYPOINT ["java", "-jar", "target/petcare-admin-0.0.1-SNAPSHOT.jar"]