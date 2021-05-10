package com.atguigu.srb.core.client;

import com.atguigu.common.result.R;
import com.atguigu.srb.core.client.fallback.OssFileClientFallBack;
import io.swagger.annotations.ApiParam;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @author hjystart
 * @create 2020-12-29 23:26
 */
@FeignClient(value = "service-oss",fallback = OssFileClientFallBack.class)
public interface OssFileClient {
    @PostMapping("/api/oss/file/upload-from-url")
    R uploadFromUrl(
            @RequestParam("url") String url,
            @RequestParam("module") String module);
}
