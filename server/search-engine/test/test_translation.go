package main

import (
	"campusmart/search-engine/internal/searcher/translator"
	"fmt"
)

func main() {
	// 测试中文翻译为英文
	enResult := translator.CallTranslationAPIToEn("你好")
	fmt.Printf("中文翻译为英文: 你好 -> %s\n", enResult)

	// 测试英文翻译为中文
	cnResult := translator.CallTranslationAPIToCn("Hello")
	fmt.Printf("英文翻译为中文: Hello -> %s\n", cnResult)

	// 测试其他词汇
	enResult2 := translator.CallTranslationAPIToEn("世界")
	fmt.Printf("中文翻译为英文: 世界 -> %s\n", enResult2)

	cnResult2 := translator.CallTranslationAPIToCn("World")
	fmt.Printf("英文翻译为中文: World -> %s\n", cnResult2)
}
