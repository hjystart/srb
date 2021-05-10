package com.atguigu.srb.core.client.fallback;

import com.atguigu.common.result.R;
import com.atguigu.srb.core.client.OssFileClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author hjystart
 * @create 2020-12-30 18:14
 */
@Service
@Slf4j
public class OssFileClientFallBack implements OssFileClient {
    @Override
    public R uploadFromUrl(String url, String module) {
        log.error("远程调用失败，服务熔断");

        //传递替换url代替远程url
        return R.ok().data("url",url);
    }
}
