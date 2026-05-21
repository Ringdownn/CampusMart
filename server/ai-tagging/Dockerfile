FROM python:3.12-slim

WORKDIR /app

# 安装系统依赖
RUN apt-get update && apt-get install -y \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# 复制依赖文件
COPY requirements.txt .

# 安装Python依赖（使用阿里云镜像源，增加超时时间）
RUN pip install --no-cache-dir -r requirements.txt -i https://mirrors.aliyun.com/pypi/simple/ --timeout 300

# 复制应用代码
COPY . .

# 暴露端口
EXPOSE 8080

# 运行应用
CMD ["python", "cmd/main.py"]
