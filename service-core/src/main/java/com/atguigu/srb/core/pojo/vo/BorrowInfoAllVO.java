package com.atguigu.srb.core.pojo.vo;

import com.atguigu.srb.core.pojo.entity.BorrowInfo;
import lombok.Data;

/**
 * @author hjystart
 * @create 2021-01-10 21:49
 */
@Data
public class BorrowInfoAllVO {
    private BorrowInfo borrowInfo;
    private BorrowerDetailVO borrowerDetailVO;
}
