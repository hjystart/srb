package com.atguigu.srb.core.controller;


import io.swagger.annotations.Api;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 积分等级表 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Api(tags = "积分等级")
@RestController
@RequestMapping("/api/core/integralGrade")
public class IntegralGradeController {

    @GetMapping("/list")
    public String listAll(){
        return "list";
    }
}

