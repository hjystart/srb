package com.atguigu.srb.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author hjystart
 * @create 2021-01-12 20:30
 */
@AllArgsConstructor
@Getter
public enum ReturnMethodEnum {

    ONE(1, "等额本息"),
    TWO(2, "等额本金"),
    THREE(3, "每月还息一次还本"),
    FOUR(4, "一次还本还息"),
    ;

    private Integer method;
    private String msg;
}
