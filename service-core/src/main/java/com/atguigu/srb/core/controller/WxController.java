package com.atguigu.srb.core.controller;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.HttpClientUtils;
import com.atguigu.common.util.RegexValidateUtils;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.vo.UserInfoVO;
import com.atguigu.srb.core.pojo.vo.WxBindVO;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.util.CoreProperties;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.ehcache.config.ResourceType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import sun.plugin2.message.Message;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author hjystart
 * @create 2020-12-28 18:30
 */
@Slf4j
@Api(tags = "微信登录")
@Controller
@CrossOrigin
@RequestMapping("/api/core/wx")
public class WxController {

    @Resource
    private UserInfoService userInfoService;

    @Resource
    private RedisTemplate redisTemplate;

    @GetMapping("/login")
    public String getQrCode(HttpSession session) {
        String url = "https://open.weixin.qq.com/connect/qrconnect" +
                "?appid=%s" +
                "&redirect_uri=%s" +
                "&response_type=code" +
                "&scope=snsapi_login" +
                "&state=%s" +
                "#wechat_redirect";

        String redirectUri = "";
        try {
            redirectUri = URLEncoder.encode(CoreProperties.REDIRECT_URI, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new BusinessException(ResponseEnum.ERROR, e);
        }

        //生成state
        String state = UUID.randomUUID().toString();
        log.info("生成的state = " + state);
        //存入session（将会由spring session进行session的同步管理，将session中的信息同步到redis）
        session.setAttribute("wx_state", state);

        String finalUrl = String.format(url,
                CoreProperties.APP_ID,
                redirectUri,
                state);
        return "redirect:" + finalUrl;
    }

    @GetMapping("/callback")
    public String callback(String code, String state, HttpSession session) {
        log.info("微信向尚融宝发起了回调请求。。。");
        log.info("code = " + code);
        log.info("state = " + state);

        String sessionState = (String) session.getAttribute("wx_state");
        log.info("session 中的state = " + sessionState);

        //校验参数是否合法
        //WEIXIN_CALLBACK_PARAM_ERROR(-601, "回调参数不正确"),
        Assert.notEmpty(code, ResponseEnum.WEIXIN_CALLBACK_PARAM_ERROR);
        Assert.notEmpty(state, ResponseEnum.WEIXIN_CALLBACK_PARAM_ERROR);
        Assert.equals(state, sessionState, ResponseEnum.WEIXIN_CALLBACK_PARAM_ERROR);

        //携带code和appid以及appsecret换取access_token
        //组装请求对象
        String accessTokenUrl = "https://api.weixin.qq.com/sns/oauth2/access_token";

        HashMap<String, String> accessTokenParams = new HashMap<>();
        accessTokenParams.put("appid", CoreProperties.APP_ID);
        accessTokenParams.put("secret", CoreProperties.APP_SECRET);
        accessTokenParams.put("code", code);
        accessTokenParams.put("grant_type", "authorization_code");
        HttpClientUtils client = new HttpClientUtils(accessTokenUrl, accessTokenParams);

        String result = "";
        try {
            client.setHttps(true);
            client.get();
            //响应内容
            result = client.getContent();
            log.info("result = " + result);
        } catch (Exception e) {
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_ACCESSTOKEN_ERROR, e);
        }

        //解析响应结果
        Gson gson = new Gson();
        HashMap<String, Object> resultMap = gson.fromJson(result, HashMap.class);
        if (resultMap.get("errcode") != null) {
            String message = (String) resultMap.get("errmsg");
            Double errcode = (Double) resultMap.get("errcode");
            log.error("获取access_token失败：message = " + message + ",errcode = " + errcode);
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_ACCESSTOKEN_ERROR);
        }

        //微信获取access_token响应成功
        String accessToken = (String) resultMap.get("access_token");
        String openid = (String) resultMap.get("openid");
        log.info("accessToken = " + accessToken);
        log.info("openid = " + openid);

        UserInfo userInfo = userInfoService.getByOpenid(openid);
        if (userInfo == null) {
            log.info("用户不存在，获取用户信息进行注册");
            //访问微信的资源服务器，获取微信个人信息，并将个人信息插入到本地数据库中。
            log.info("将用户信息存入数据库");
            userInfo = userInfoService.registerWx(accessToken, openid);

            log.info("提示用户绑定手机号码");

            return "redirect:http://localhost:3000/bind?openid=" + openid + "&is_bind=0";
        } else if (StringUtils.isEmpty(userInfo.getMobile())) {
            log.info("用户已扫码，但尚未绑定手机号");

            log.info("绑定手机号");
            return "redirect:http://localhost:3000/bind?openid=" + openid + "&is_bind=0";
        } else {
            log.info("用户已存在，且已绑定手机号");

            log.info("用户登录");
            return "redirect:http://localhost:3000/bind?openid=" + openid + "&is_bind=1";
        }

    }

    @ApiOperation("根据用户openid获取用户信息")
    @ResponseBody
    @GetMapping("/getUserInfoVO/{openid}")
    public R getUserInfo(
            @ApiParam(value = "微信用户openid", required = true)
            @PathVariable String openid) {
        UserInfoVO userInfoVO = userInfoService.getUserInfoVOByOpenid(openid);
        return R.ok().data("userInfoVO", userInfoVO);
    }

    @ApiOperation("绑定手机号")
    @ResponseBody
    @PostMapping("/bind")
    public R register(
            @ApiParam(value = "微信绑定对象", required = true)
            @RequestBody WxBindVO wxBindVO,
            HttpServletRequest request) {
        String mobile = wxBindVO.getMobile();
        String code = wxBindVO.getCode();

        //校验用户的输入参数
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);
        Assert.notEmpty(code, ResponseEnum.CODE_NULL_ERROR);

        //校验验证码
        String codeRedis = (String) redisTemplate.opsForValue().get("srb:sms:code:" + mobile);
        Assert.equals(code, codeRedis, ResponseEnum.CODE_ERROR);

        String ip = "";
        if (request.getHeader("x-forwarded-for") == null) {
            ip = request.getRemoteAddr();
        } else {
            ip = request.getHeader("x-forwarded-for");
        }
        UserInfoVO userInfoVO = userInfoService.bind(wxBindVO,ip);

        return R.ok().data("userInfoVO", userInfoVO);
    }


    @ApiOperation("微信用户登录")
    @ResponseBody
    @PostMapping("/login")
    public R login(
            @ApiParam(value = "微信绑定对象", required = true)
            @RequestBody WxBindVO wxBindVO,
            HttpServletRequest request) {

        String ip = "";
        if (request.getHeader("x-forwarded-for") == null) {
            ip = request.getRemoteAddr();
        } else {
            ip = request.getHeader("x-forwarded-for");
        }
        UserInfoVO userInfoVO = userInfoService.login(wxBindVO,ip);

        return R.ok().data("userInfoVO", userInfoVO);
    }
}


