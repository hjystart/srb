package com.atguigu.srb.sms.service.impl;

import com.aliyuncs.CommonRequest;
import com.aliyuncs.CommonResponse;
import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.IAcsClient;
import com.aliyuncs.exceptions.ClientException;
import com.aliyuncs.exceptions.ServerException;
import com.aliyuncs.http.MethodType;
import com.aliyuncs.profile.DefaultProfile;
import com.atguigu.common.exception.Assert;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.sms.service.SmsService;
import com.atguigu.srb.sms.util.SmsProperties;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hjystart
 * @create 2020-12-22 20:44
 */
@Service
@Slf4j
public class SmsServiceImpl implements SmsService {

    @Override
    public void send(String mobile, String templateCode, Map<String, Object> param) {

        //创建远程连接的配置对象
        DefaultProfile profile = DefaultProfile.getProfile(
                SmsProperties.REGION_Id,
                SmsProperties.KEY_ID,
                SmsProperties.KEY_SECRET);
        //创建远程连接的客户端对象，涉及到远程连接就有client
        IAcsClient client = new DefaultAcsClient(profile);
        //创建请求对象
        CommonRequest request = new CommonRequest();
        //封装请求
        request.setSysMethod(MethodType.POST);//接口的请求方式
        request.setSysDomain("dysmsapi.aliyuncs.com");//接口的请求地址
        request.setSysVersion("2017-05-25");//接口的版本号
        request.setSysAction("SendSms");//具体要执行的动作
        request.putQueryParameter("RegionId", SmsProperties.REGION_Id);
        request.putQueryParameter("PhoneNumbers", mobile);
        request.putQueryParameter("SignName", SmsProperties.SIGN_NAME);
        request.putQueryParameter("TemplateCode", SmsProperties.TEMPLATE_CODE);
        //将对象转为json字符串
        Gson gson = new Gson();
        String json = gson.toJson(param);
        request.putQueryParameter("TemplateParam", json);
        try {
            //携带请求对象执行远程调用，得到响应结果
            CommonResponse response = client.getCommonResponse(request);
            String data = response.getData();
//            log.info("data = " + data);

            HashMap<String,String> resultMap = gson.fromJson(data, HashMap.class);
            String code = resultMap.get("Code");
            String message = resultMap.get("Message");
            if (!"OK".equals(code)){
                log.error("阿里云短信发送响应结果为：");
                log.error("code = " + code);
                log.error("message = " + message);
            }

            Assert.notEquals("isv.BUSINESS_LIMIT_CONTROL",code,ResponseEnum.ALIYUN_SMS_LIMIT_CONTROL_ERROR);

            Assert.equals("OK",code, ResponseEnum.ALIYUN_SMS_ERROR);
        } catch (ServerException e) {
            log.error("阿里云短信发送SDK调用失败：");
            log.error("ErrorCode=" + e.getErrCode());
            log.error("ErrorMessage=" + e.getErrMsg());
            throw new BusinessException(ResponseEnum.ALIYUN_SMS_ERROR , e);
        }  catch (ClientException e) {
            // 这里可以添加您自己的错误处理逻辑
            // 例如，打印具体的错误信息
            log.error("阿里云短信发送SDK调用失败：");
            log.error("ErrorCode=" + e.getErrCode());
            log.error("ErrorMessage=" + e.getErrMsg());
            throw new BusinessException(ResponseEnum.ALIYUN_SMS_ERROR , e);
        }

    }
}
