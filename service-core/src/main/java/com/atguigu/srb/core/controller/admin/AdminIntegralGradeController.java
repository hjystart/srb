package com.atguigu.srb.core.controller.admin;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.core.pojo.entity.IntegralGrade;
import com.atguigu.srb.core.service.IntegralGradeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * 给后台管理系统做接口的类
 *
 * @author hjystart
 * @create 2020-12-13 23:45
 */
@CrossOrigin//开放跨域【只允许当前类里的方法和接口跨域】
@Api(tags = "积分等级管理")//对类的说明
@RestController
@RequestMapping("/admin/core/integralGrade")
@Slf4j
public class AdminIntegralGradeController {

    @Resource
    private IntegralGradeService integrationService;

    @ApiOperation("积分等级列表")//对方法进行说明，省略了value
    @GetMapping("/list")
    public R listAll() {

        log.info("hi i'm info");
        log.warn("hi i'm warn");
        log.error("hi i'm error");

        List<IntegralGrade> list = integrationService.list();
        return R.ok().data("list", list);
    }

    //删除
    @ApiOperation(value = "根据id删除积分等级", notes = "逻辑删除")//notes中进行更详细的说明
    @DeleteMapping("/remove/{id}")
    public R removeById(
            @ApiParam(value = "数据id", required = true, example = "1")//对参数进行说明
            @PathVariable Long id) {
        boolean result = integrationService.removeById(id);
        if (result) {
//            return R.setResult(ResponseEnum.UPLOAD_ERROR);
            return R.ok().message("删除成功");
        } else {
            return R.error().message("删除失败");
        }
    }

    @ApiOperation("根据id获取积分等级")
    @GetMapping("/get/{id}")
    public R getById(
            @ApiParam(value = "数据id", required = true, example = "1")//对参数进行说明
            @PathVariable Long id) {
        IntegralGrade integralGrade = integrationService.getById(id);
        if (integralGrade != null) {
            return R.ok().data("record", integralGrade);
        } else {
            return R.error().message("数据不存在");
        }
    }

    @ApiOperation("新增数据")
    @PostMapping("/save")
    public R save(
            @ApiParam(value = "积分等级对象", required = true)
            @RequestBody IntegralGrade integralGrade) {

//        if (integralGrade.getBorrowAmount() == null){
//            throw new BusinessException(ResponseEnum.BORROW_AMOUNT_NULL_ERROR);
////            return R.setResult(ResponseEnum.BORROW_AMOUNT_NULL_ERROR);//只能用于controller层
//        }

        Assert.notNull(integralGrade.getBorrowAmount(), ResponseEnum.BORROW_AMOUNT_NULL_ERROR);

        boolean result = integrationService.save(integralGrade);
        if (result) {
            return R.ok().message("保存成功");
        } else {
            return R.error().message("保存失败");
        }
    }

//    @ApiOperation("根据id修改数据")
//    @PutMapping("/updateById")
//    public R updateById(
//            @ApiParam(value = "积分等级对象",required = true)
//            @RequestBody IntegralGrade integralGrade){
//        boolean result = integrationService.updateById(integralGrade);
//        if (result){
//            return R.ok().message("修改成功");
//        }else {
//            return R.error().message("修改失败");
//        }
//    }

    @ApiOperation("根据id修改数据")
    @PutMapping("/updateById/{id}")//为了符合resetFul风格【从url能看出要做什么，可读性更高】，增加的id,
    public R updateById(
            @ApiParam(value = "积分等级对象", required = true)
            @RequestBody IntegralGrade integralGrade,
            @ApiParam(value = "数据id", required = true, example = "1")
            @PathVariable Long id) {
        integralGrade.setId(id);
        boolean result = integrationService.updateById(integralGrade);
        if (result) {
            return R.ok().message("修改成功");
        } else {
            return R.error().message("修改失败");
        }
    }

}
