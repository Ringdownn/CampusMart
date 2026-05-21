# 构建阶段
FROM golang:1.26-alpine AS builder

WORKDIR /app

# 设置国内 Go 模块镜像源
ENV GOPROXY=https://goproxy.cn,direct

# 复制 go.mod 和 go.sum
COPY go.mod go.sum ./
RUN go mod download

# 复制源代码
COPY . .

# 构建应用
RUN CGO_ENABLED=0 GOOS=linux go build -a -installsuffix cgo -o main ./cmd/main.go

# 运行阶段
FROM alpine:latest

RUN apk --no-cache add ca-certificates

WORKDIR /root/

# 从构建阶段复制可执行文件和配置文件
COPY --from=builder /app/main .
COPY --from=builder /app/config.yaml .

# 暴露端口
EXPOSE 5678

# 运行应用
CMD ["./main"]
