package com.atguigu.srb.oss.service.impl;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.oss.service.FileService;
import com.atguigu.srb.oss.util.OssProperties;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

/**
 * @author hjystart
 * @create 2020-12-22 15:36
 */
@Service
public class FileServiceImpl implements FileService {

    @Override
    public String upload(InputStream inputStream, String module, String fileName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);
        //文件夹名
        String folder = new DateTime().toString("/yyyy/MM/dd/");
        //文件名/
        String finalName = UUID.randomUUID().toString() + fileName.substring(fileName.lastIndexOf("."));
        String path = module + folder + finalName;

        // 上传文件流。
        ossClient.putObject(OssProperties.BUCKET_NAME, path, inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();

        //
        return "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/" + path;
    }

    @Override
    public void remove(String url) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);
        //objectName
        String host = "https://" + OssProperties.BUCKET_NAME + "." + OssProperties.ENDPOINT + "/";
        String objectName = url.substring(host.length());
        // 删除文件。如需删除文件夹，请将ObjectName设置为对应的文件夹名称。
        // 如果文件夹非空，则需要将文件夹下的所有object删除后才能删除该文件夹。
        ossClient.deleteObject(OssProperties.BUCKET_NAME, objectName);

        // 关闭OSSClient。
        ossClient.shutdown();
    }

    @Override
    public String upload(String url, String module) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(
                OssProperties.ENDPOINT,
                OssProperties.KEY_ID,
                OssProperties.KEY_SECRET);

        String folder = new DateTime().toString("/yyyy/MM/dd/");
        String fileName = UUID.randomUUID().toString();
        String ext = ".jpg";
        String key = module + folder + fileName + ext;

        // 上传网络流。
        InputStream inputStream = null;
        try {
            inputStream = new URL(url).openStream();
        } catch (IOException e) {
            throw new BusinessException(ResponseEnum.UPLOAD_ERROR, e);
        }
        ossClient.putObject(OssProperties.BUCKET_NAME, key, inputStream);

        // 关闭OSSClient。
        ossClient.shutdown();
        //https://srb-file-201223.oss-cn-beijing.aliyuncs.com/avatar/1.jpg
        return "https://" + OssProperties.BUCKET_NAME + "."
                + OssProperties.ENDPOINT
                + "/" + key;
    }
}
