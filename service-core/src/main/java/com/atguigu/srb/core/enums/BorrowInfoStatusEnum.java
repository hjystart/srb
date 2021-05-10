package com.atguigu.srb.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author hjystart
 * @create 2021-01-06 0:03
 */
@AllArgsConstructor
@Getter
public enum BorrowInfoStatusEnum {

    CHECK_NO(-2, "未认证"),
    CHECK_RUN(0, "待审核"),
    CHECK_OK(1, "审核通过"),
    CHECK_FAIL(-1, "审核不通过"),
    ;

    private Integer status;
    private String msg;

    public static String getMsgByStatus(int status) {
        BorrowInfoStatusEnum arrObj[] = BorrowInfoStatusEnum.values();
        for (BorrowInfoStatusEnum obj : arrObj) {
            if (status == obj.getStatus().intValue()) {
                return obj.getMsg();
            }
        }
        return "";
    }
}
