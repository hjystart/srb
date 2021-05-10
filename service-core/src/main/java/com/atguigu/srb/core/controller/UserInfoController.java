package com.atguigu.srb.core.controller;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.RegexValidateUtils;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.pojo.vo.LoginVO;
import com.atguigu.srb.core.pojo.vo.RegisterVO;
import com.atguigu.srb.core.pojo.vo.UserInfoVO;
import com.atguigu.srb.core.service.UserInfoService;
import com.atguigu.srb.core.service.UserIntegralService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * <p>
 * 用户基本信息 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Api(tags = "会员接口")
@RestController
@RequestMapping("/api/core/userInfo")
@Slf4j
@CrossOrigin
public class UserInfoController {

    @Resource
    private RedisTemplate redisTemplate;

    @Resource
    private UserInfoService userInfoService;

    @ApiOperation("会员注册")
    @PostMapping("/register")
    public R register(
            @ApiParam(value = "会员注册表单对象", required = true)
            @RequestBody RegisterVO registerVO) {
        String mobile = registerVO.getMobile();
        String code = registerVO.getCode();
        String password = registerVO.getPassword();

        //校验用户的输入参数
        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.isTrue(RegexValidateUtils.checkCellphone(mobile), ResponseEnum.MOBILE_ERROR);
        Assert.notEmpty(code, ResponseEnum.CODE_NULL_ERROR);
        Assert.notEmpty(password, ResponseEnum.PASSWORD_NULL_ERROR);

        //校验验证码
        String codeRedis = (String) redisTemplate.opsForValue().get("srb:sms:code:" + mobile);

        Assert.equals(code, codeRedis, ResponseEnum.CODE_ERROR);
        //注册业务
        userInfoService.register(registerVO);

        return R.ok().message("注册成功");
    }

    @ApiOperation("会员登录")
    @PostMapping("/login")
    public R login(@RequestBody LoginVO loginVO, HttpServletRequest request) {

        String mobile = loginVO.getMobile();
        String password = loginVO.getPassword();

        Assert.notEmpty(mobile, ResponseEnum.MOBILE_NULL_ERROR);
        Assert.notEmpty(password, ResponseEnum.PASSWORD_NULL_ERROR);

        //业务登录
//        String remoteAddr = request.getRemoteAddr();
        String ip = "";
        if (request.getHeader("x-forwarded-for") == null){
            ip = request.getRemoteAddr();
        }else {
            ip = request.getHeader("x-forwarded-for");
        }

        UserInfoVO userInfoVO = userInfoService.login(loginVO, ip);
        return R.ok().data("userInfoVO", userInfoVO);
    }


    @ApiOperation("校验令牌")
    @GetMapping("/checkToken")
    public R checkToken(HttpServletRequest request){

        String token = request.getHeader("token");
        boolean b = JwtUtils.checkToken(token);
        if (b){
            return R.ok();
        }else {
            return R.setResult(ResponseEnum.LOGIN_AUTH_ERROR);
        }
    }

}

