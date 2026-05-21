#!/bin/bash

# CampusMart 项目初始化脚本
# 将 MixFound 的服务复制到 CampusMart 项目中

echo "========================================"
echo "CampusMart 项目初始化"
echo "========================================"

# 复制 search-engine
echo "[1/3] 复制 search-engine 服务..."
if [ ! -d "../MixFound/services/search-engine" ]; then
    echo "错误: 找不到 MixFound/services/search-engine 目录"
    exit 1
fi

cp -r ../MixFound/services/search-engine ../CampusMart/server/
echo "      ✓ search-engine 已复制"

# 复制 ai-tagging
echo "[2/3] 复制 ai-tagging 服务..."
if [ ! -d "../MixFound/services/ai-tagging" ]; then
    echo "错误: 找不到 MixFound/services/ai-tagging 目录"
    exit 1
fi

cp -r ../MixFound/services/ai-tagging ../CampusMart/server/
echo "      ✓ ai-tagging 已复制"

# 复制 go.work 文件（如果需要）
if [ -f "../MixFound/go.work" ]; then
    cp ../MixFound/go.work ../CampusMart/server/
    echo "      ✓ go.work 已复制"
fi

echo "[3/3] 验证文件..."
if [ -d "../CampusMart/server/search-engine" ] && [ -d "../CampusMart/server/ai-tagging" ]; then
    echo "      ✓ 初始化完成!"
else
    echo "      ✗ 初始化失败"
    exit 1
fi

echo ""
echo "========================================"
echo "项目结构:"
echo "========================================"
echo "CampusMart/"
echo "├── server/"
echo "│   ├── gateway/"
echo "│   ├── base-service/"
echo "│   ├── message-service-go/"
echo "│   ├── search-engine/        (新增)"
echo "│   └── ai-tagging/           (新增)"
echo "└── deploy/"
echo "    └── docker/"
echo "        └── docker-compose.yml"
echo ""
echo "启动命令:"
echo "  cd ../CampusMart/deploy/docker"
echo "  docker-compose up -d"
echo "========================================"
