package com.atguigu.srb.core.controller;


import com.atguigu.common.result.R;
import com.atguigu.srb.core.pojo.entity.Dict;
import com.atguigu.srb.core.service.DictService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 数据字典 前端控制器
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Slf4j
@Api(tags = "数字字典")
@CrossOrigin
@RestController
@RequestMapping("/api/core/dict")
public class DictController {
    @Resource
    private DictService dictService;

    @ApiOperation("根据数据字典编码获取数据字典列表")
    @GetMapping("/findByDictCode/{dictCode}")
    public R findByDictCode(
            @ApiParam(value = "数据字典编码",required = true)
            @PathVariable String dictCode){
       List<Dict> list = dictService.findByDictCode(dictCode);
       return R.ok().data("dictList",list);

    }
}

