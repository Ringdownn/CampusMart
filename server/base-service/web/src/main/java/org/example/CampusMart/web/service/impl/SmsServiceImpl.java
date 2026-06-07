package org.example.CampusMart.web.service.impl;

import com.aliyun.auth.credentials.Credential;
import com.aliyun.auth.credentials.provider.StaticCredentialProvider;
import com.aliyun.sdk.service.dypnsapi20170525.AsyncClient;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeRequest;
import com.aliyun.sdk.service.dypnsapi20170525.models.SendSmsVerifyCodeResponse;
import darabonba.core.client.ClientOverrideConfiguration;
import org.example.CampusMart.common.exception.CampusMartException;
import org.example.CampusMart.common.result.ResultCodeEnum;
import org.example.CampusMart.common.sms.AliyunSMSProperties;
import org.example.CampusMart.web.service.SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class SmsServiceImpl implements SmsService {
    @Autowired
    private AliyunSMSProperties properties;

    @Override
    public void sendCode(String phone, String code) throws ExecutionException, InterruptedException {
        if (!StringUtils.hasText(properties.getAccessKeyId()) || !StringUtils.hasText(properties.getAccessKeySecret())) {
            throw new CampusMartException(ResultCodeEnum.SERVICE_ERROR);
        }

        try (AsyncClient client = createClient()) {
            SendSmsVerifyCodeRequest request = SendSmsVerifyCodeRequest.builder()
                    .signName(properties.getSignName())
                    .templateCode(properties.getTemplateCode())
                    .phoneNumber(phone)
                    .templateParam("{\"code\":\"" + code + "\",\"min\":\"10\"}")
                    .build();

            CompletableFuture<SendSmsVerifyCodeResponse> response = client.sendSmsVerifyCode(request);
            response.get();
        }
    }

    private AsyncClient createClient() {
        StaticCredentialProvider provider = StaticCredentialProvider.create(Credential.builder()
                .accessKeyId(properties.getAccessKeyId())
                .accessKeySecret(properties.getAccessKeySecret())
                .build());

        return AsyncClient.builder()
                .region(properties.getRegion())
                .credentialsProvider(provider)
                .overrideConfiguration(
                        ClientOverrideConfiguration.create()
                                .setEndpointOverride(properties.getEndpoint())
                )
                .build();
    }
}
