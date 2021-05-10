package com.atguigu.srb.oss.service;

import java.io.InputStream;

/**
 * @author hjystart
 * @create 2020-12-22 15:32
 */
public interface FileService {

    public String upload(InputStream inputStream,String module,String fileName);

    void remove(String url);

    String upload(String url,String module);
}
