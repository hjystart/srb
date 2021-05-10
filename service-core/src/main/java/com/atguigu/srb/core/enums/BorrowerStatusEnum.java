package com.atguigu.srb.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author hjystart
 * @create 2021-01-04 20:20
 */
@AllArgsConstructor
@Getter
//@ToString
public enum BorrowerStatusEnum {

    AUTH_RUN(0, "认证中"),
    AUTH_OK(1, "认证成功"),
    AUTH_FAIL(-1, "认证失败"),
    ;

    private Integer status;
    private String msg;

    public static String getMsgByStatus(int status) {
        //获取枚举数组
        BorrowerStatusEnum arrObj[] = BorrowerStatusEnum.values();
        //遍历枚举
        for (BorrowerStatusEnum obj : arrObj) {
            //返回指定status的msg
            if (status == obj.getStatus().intValue()) {
                return obj.getMsg();
            }
        }
        return "";
    }
}
