package com.atguigu.srb.sms.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author hjystart
 * @create 2020-12-22 19:43
 */
@Setter//自动调用set方法为属性赋值
@Getter
@Component
@ConfigurationProperties(prefix = "aliyun.sms")//去application.yml中读取前缀为aliyun.sms的所有节点
public class SmsProperties implements InitializingBean {
    private String regionId;
    private String keyId;
    private String keySecret;
    private String templateCode;
    private String signName;

    public static String REGION_Id;
    public static String KEY_ID;
    public static String KEY_SECRET;
    public static String TEMPLATE_CODE;
    public static String SIGN_NAME;

    @Override
    public void afterPropertiesSet() throws Exception {
        REGION_Id = regionId;
        KEY_ID = keyId;
        KEY_SECRET = keySecret;
        TEMPLATE_CODE = templateCode;
        SIGN_NAME = signName;
    }
}
