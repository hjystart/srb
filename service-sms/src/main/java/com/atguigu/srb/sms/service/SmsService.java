package com.atguigu.srb.sms.service;

import java.util.Map;

/**
 * @author hjystart
 * @create 2020-12-22 20:43
 */
public interface SmsService {
    void send(String mobile, String templateCode, Map<String,Object> param);
}
