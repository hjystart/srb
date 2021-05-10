package com.atguigu.srb.core.config;

/**
 * @author hjystart
 * @create 2020-12-28 19:47
 */

import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

/**
 * 使用JSON序列化方案
 * SpringSession Redis序列化
 * *注：bean的名称必须为springSessionDefaultRedisSerializer
 *
 * @see org.springframework.session.data.redis.config.annotation.web.http.RedisHttpSessionConfiguration
 */
//此处名字必须是springSessionDefaultRedisSerializer
//替换默认的序列化器为Jackson序列化器
@Component("springSessionDefaultRedisSerializer")
public class SessionSerializer extends GenericJackson2JsonRedisSerializer {

}
