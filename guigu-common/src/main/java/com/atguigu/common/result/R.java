package com.atguigu.common.result;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author hjystart
 * @create 2020-12-14 19:54
 */
@Data
public class R {

    private Integer code;//错误码
    private String message;//响应码
    private Map<String,Object> data = new HashMap<>();

    private R(){}//私有化构造器

    //结果成功的时调用
    public static R ok(){
        R r = new R();
        r.setCode(ResponseEnum.SUCCESS.getCode());
        r.setMessage(ResponseEnum.SUCCESS.getMessage());
        return r;
    }

    public static R error() {
        R r = new R();
        r.setCode(ResponseEnum.ERROR.getCode());
        r.setMessage(ResponseEnum.ERROR.getMessage());
        return r;
    }

    public static R setResult(ResponseEnum responseEnum){
        R r = new R();
        r.setCode(responseEnum.getCode());
        r.setMessage(responseEnum.getMessage());
        return r;
    }

    //组装返回的数据
    public R data(String key,Object value){
        this.data.put(key,value);
        return this;
    }

    public R data(Map<String,Object> map){
        this.setData(map);
        return this;
    }

    public R message(String message) {
        this.setMessage(message);
        return this;
    }

    public R code(Integer code){
        this.setCode(code);
        return this;
    }
}
