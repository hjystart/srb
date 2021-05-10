package com.atguigu.srb.sms.controller;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.RandomUtils;
import com.atguigu.common.util.RegexValidateUtils;
import com.atguigu.srb.sms.service.SmsService;
import com.atguigu.srb.sms.util.SmsProperties;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author hjystart
 * @create 2020-12-22 21:03
 */

@Api(tags = "阿里用短信发送管理")//对类的说明
@Slf4j
@CrossOrigin//开放跨域【只允许当前类里的方法和接口跨域】
@RestController
@RequestMapping("/api/sms")
public class SmsController {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private SmsService smsService;

    @ApiOperation("发送短信，获取验证码")
    @GetMapping("/send/{mobile}")
    public R send(
            @ApiParam(value = "手机号码")
            @PathVariable String mobile) {
        //参数校验
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);

        //生成验证码
        String code = RandomUtils.getFourBitRandom();
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
//        smsService.send(mobile, SmsProperties.TEMPLATE_CODE, map);

        //将验证码存入redis
        redisTemplate.opsForValue().set("srb:sms:code:"+mobile,code,5, TimeUnit.MINUTES);

        return R.ok().message("短信发送成功");
    }

}
