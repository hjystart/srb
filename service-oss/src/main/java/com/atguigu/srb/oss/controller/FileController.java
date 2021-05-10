package com.atguigu.srb.oss.controller;

import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.oss.service.FileService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author hjystart
 * @create 2020-12-22 15:41
 */
@Api(tags = "阿里云文件管理")
@CrossOrigin //跨域
@RestController
@RequestMapping("/api/oss/file")
public class FileController {
    @Resource
    private FileService fileService;

    @ApiOperation("阿里云文件上传")
    @PostMapping("/upload")
    public R upload(
            @ApiParam(value = "文件所在模块", required = true)
            @RequestParam String module,

            @ApiParam(value = "文件", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            String name = file.getOriginalFilename();
            String url = fileService.upload(inputStream, module, name);
            return R.ok().message("文件上传成功").data("url", url);
        } catch (IOException e) {
            throw new BusinessException(ResponseEnum.UPLOAD_ERROR, e);
        }
    }

    @ApiOperation("阿里云文件删除")
    @DeleteMapping("/remove")
    public R remove(
            @ApiParam(value = "文件", required = true)
            @RequestParam("url") String url) {
        fileService.remove(url);
        return R.ok().message("文件删除成功");
    }


    @ApiOperation("上传网路文件")
    @PostMapping("/upload-from-url")
    public R uploadFromUrl(
            @ApiParam(value = "文件的url地址",required = true)
            @RequestParam("url") String url,
            @ApiParam(value = "文件所在模块" ,required = true)
            @RequestParam("module") String module){
        String ossUrl = fileService.upload(url, module);
        return R.ok().message("文件上传成功").data("url",ossUrl);

    }


}
