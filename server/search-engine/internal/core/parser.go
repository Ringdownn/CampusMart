package core

import (
	"campusmart/search-engine/internal/global"
	"fmt"
	"os"

	"gopkg.in/yaml.v2"
)

func Parse() *global.Config {
	configFile := "config.yaml"

	// 检查配置文件是否存在
	if _, err := os.Stat(configFile); err != nil {
		if os.IsNotExist(err) {
			fmt.Printf("配置文件 %s 不存在，使用默认配置\n", configFile)
			return global.GetDefaultConfig()
		}
	}

	// 读取配置文件
	file, err := os.ReadFile(configFile)
	if err != nil {
		fmt.Printf("读取配置文件失败: %v\n", err)
		return global.GetDefaultConfig()
	}

	// 解析 YAML
	config := global.GetDefaultConfig()
	err = yaml.Unmarshal(file, config)
	if err != nil {
		fmt.Printf("解析配置文件失败: %v\n", err)
		return global.GetDefaultConfig()
	}

	return config
}
