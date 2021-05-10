package com.atguigu.srb.sms;

import com.atguigu.srb.sms.util.SmsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @author hjystart
 * @create 2020-12-22 20:07
 */
@SpringBootTest
public class PropertiesTests {
    
    @Test
    public void testPropertiesValue(){

        System.out.println(SmsProperties.KEY_ID);
    }
}
