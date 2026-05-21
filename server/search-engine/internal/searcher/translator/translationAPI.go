package translator

import (
	"fmt"

	"github.com/alibabacloud-go/alimt-20181012/v2/client"
	openapi "github.com/alibabacloud-go/darabonba-openapi/v2/client"
	util "github.com/alibabacloud-go/tea-utils/v2/service"
	"github.com/alibabacloud-go/tea/tea"
	"github.com/aliyun/credentials-go/credentials"
)

var alimtClient *client.Client

// 初始化阿里云翻译客户端
func init() {
	var err error
	alimtClient, err = createClient()
	if err != nil {
		fmt.Printf("初始化翻译客户端失败: %v\n", err)
	} else {
		fmt.Println("翻译客户端初始化成功")
	}
}

// createClient 创建并返回阿里云翻译客户端
// createClient 创建并返回阿里云翻译客户端
func createClient() (*client.Client, error) {
	// 使用默认凭据链，会自动从环境变量中读取AccessKey
	cred, err := credentials.NewCredential(nil)
	if err != nil {
		return nil, fmt.Errorf("创建凭证失败: %v", err)
	}

	openapiConfig := &openapi.Config{
		Credential: cred,
	}
	// 设置翻译服务的Endpoint
	openapiConfig.Endpoint = tea.String("mt.aliyuncs.com")

	return client.NewClient(openapiConfig)
}

// CallTranslationAPIToEn 将中文翻译为英文
func CallTranslationAPIToEn(word string) string {
	if alimtClient == nil {
		fmt.Println("翻译客户端未初始化，返回原词")
		return word
	}

	request := &client.TranslateGeneralRequest{
		SourceLanguage: tea.String("zh"),
		TargetLanguage: tea.String("en"),
		SourceText:     tea.String(word),
		FormatType:     tea.String("text"),
		Scene:          tea.String("general"),
	}

	runtime := &util.RuntimeOptions{}
	resp, err := alimtClient.TranslateGeneralWithOptions(request, runtime)
	if err != nil {
		fmt.Printf("翻译失败: %v\n", err)
		return word
	}

	//fmt.Printf("[LOG] %v\n", resp)

	if resp.Body != nil && resp.Body.Data != nil && resp.Body.Data.Translated != nil {
		return tea.StringValue(resp.Body.Data.Translated)
	}

	return word
}

// CallTranslationAPIToCn 将英文翻译为中文
func CallTranslationAPIToCn(word string) string {
	if alimtClient == nil {
		fmt.Println("翻译客户端未初始化，返回原词")
		return word
	}

	request := &client.TranslateGeneralRequest{
		SourceLanguage: tea.String("en"),
		TargetLanguage: tea.String("zh"),
		SourceText:     tea.String(word),
		FormatType:     tea.String("text"),
		Scene:          tea.String("general"),
	}

	runtime := &util.RuntimeOptions{}
	resp, err := alimtClient.TranslateGeneralWithOptions(request, runtime)
	if err != nil {
		fmt.Printf("翻译失败: %v\n", err)
		return word
	}

	if resp.Body != nil && resp.Body.Data != nil && resp.Body.Data.Translated != nil {
		return tea.StringValue(resp.Body.Data.Translated)
	}
	
	return word
}
