package com.linghang.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatAccountConfig {

    private String mpAppId;

    private String openAppId;

    private String openAppSecret;

    private String mchId;

    private String mchKey;

    private String keyPath;

    private String notifyUrl;

    private Map<String, String> templateId;
}
