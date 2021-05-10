package com.atguigu.srb.core.controller;


import com.alibaba.fastjson.JSON;
import com.atguigu.common.result.R;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.pojo.vo.UserBindVO;
import com.atguigu.srb.core.service.UserBindService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.spring.web.json.Json;

import javax.annotation.Resource;
import javax.print.DocFlavor;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * <p>
 * 用户绑定表 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Api(tags = "用户绑定接口")
@RestController
@RequestMapping("/api/core/userBind")
@Slf4j
@CrossOrigin
public class UserBindController {

    @Resource
    private UserBindService userBindService;

    @ApiOperation("用户账户绑定")
    @PostMapping("/auth/bind")
    public R bind(@RequestBody UserBindVO userBindVO,
                  HttpServletRequest request){
        String token = request.getHeader("token");
        //内含getClaims()校验token
        Long userId = JwtUtils.getUserId(token);
        String formStr = userBindService.commitBindUser(userBindVO,userId);
        return R.ok().data("formStr",formStr);
    }

    @ApiOperation("账户绑定的异步回调接口")
    @PostMapping("/notify")
    public String notify(HttpServletRequest request){
        Map<String ,String[]> parameterMap = request.getParameterMap();
        Map<String, Object> paramMap = RequestHelper.switchMap(parameterMap);
        log.info("回调参数"+ JSON.toJSONString(paramMap));

        //验签
        if(!RequestHelper.isSignEquals(paramMap)){
            log.error("用户账号绑定验证签名失败");
            return "fail";
        }

        //修改账号绑定状态
        userBindService.notify(paramMap);

        return "success";
    }
}

