package com.atguigu.srb.core.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;

/**
 * @author hjystart
 * @create 2021-01-11 14:54
 */
public class LendNoUtils {
    //流水号
    public static String getNo() {

        LocalDateTime time=LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String strDate = dtf.format(time);

        String result = "";
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            result += random.nextInt(10);
        }

        return strDate + result;
    }

    //标的编号
    public static String getLendNo() {

        return "SRB" + getNo();
    }

    //投资编号
    public static String getLendItemNo() {

        return "INVEST" + getNo();
    }

    //放款编号
    public static String getLoanNo() {

        return "LOAN" + getNo();
    }

    //回款编号
    public static String getReturnNo() {
        return "RETURN" + getNo();
    }

    public static String getWithdrawNo() {
        return "WITHDRAW" + getNo();
    }

    /**
     * 获取交易编码
     * @param userId 会员id
     * @param str 会员id与str构成唯一标识
     * @return
     */
    public synchronized static String getTransNo(Long userId, String str) {
        StringBuilder builder = new StringBuilder(userId.toString());
        builder.append(Math.abs(str.hashCode()));// HASH-CODE
        return builder.toString();
    }


}
