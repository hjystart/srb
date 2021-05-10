package com.atguigu.srb.core.service;

import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.pojo.vo.InvestVO;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * <p>
 * 标的出借记录表 服务类
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
public interface LendItemService extends IService<LendItem> {

    String commitInvest(InvestVO investVO);

    void notify(Map<String, Object> paramMap);

    LendItem getByLendItemNo(String lendItemNo);

    /**
     * 查询某一个标的下面指定的投资记录列表
     * @param lendId
     * @param status
     * @return
     */
    List<LendItem> selectByLendId(Long lendId,Integer status);

    List<LendItem> selectByLendId(Long lendId);
}
