package com.atguigu.srb.core;

import org.junit.jupiter.api.Test;
import org.springframework.util.Assert;

/**
 * @author hjystart
 * @create 2020-12-14 23:38
 */
public class AssertTests {
    @Test
    public void test1(){
        Object o = null;
        if(o == null){
            throw new IllegalArgumentException("用户不存在");
        }
    }

    @Test
    public void test2(){
        Object o = null;
        Assert.notNull(o,"用户不存在");
    }
}
