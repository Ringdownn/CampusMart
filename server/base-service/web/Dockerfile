FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 配置国内镜像源（阿里云）并安装Maven
RUN sed -i 's/ports.ubuntu.com/mirrors.aliyun.com/g' /etc/apt/sources.list.d/ubuntu.sources && \
    apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# 复制Maven配置文件（使用阿里云镜像）
COPY mvn-settings.xml /root/.m2/settings.xml

# 复制所有必要的文件
COPY . .

# 构建项目
RUN mvn -pl common,model,web -am clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/web/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
