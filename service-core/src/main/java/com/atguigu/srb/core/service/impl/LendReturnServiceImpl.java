package com.atguigu.srb.core.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.exception.Assert;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.srb.core.hfb.FormHelper;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.LendItemMapper;
import com.atguigu.srb.core.mapper.LendItemReturnMapper;
import com.atguigu.srb.core.mapper.LendMapper;
import com.atguigu.srb.core.pojo.entity.Lend;
import com.atguigu.srb.core.pojo.entity.LendItem;
import com.atguigu.srb.core.pojo.entity.LendItemReturn;
import com.atguigu.srb.core.pojo.entity.LendReturn;
import com.atguigu.srb.core.mapper.LendReturnMapper;
import com.atguigu.srb.core.service.LendReturnService;
import com.atguigu.srb.core.service.LendService;
import com.atguigu.srb.core.service.UserAccountService;
import com.atguigu.srb.core.service.UserBindService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 还款记录表 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Service
public class LendReturnServiceImpl extends ServiceImpl<LendReturnMapper, LendReturn> implements LendReturnService {

    @Resource
    private UserAccountService userAccountService;

    @Resource
    private UserBindService userBindService;

    @Resource
    private LendMapper lendMapper;

    @Resource
    private LendItemMapper lendItemMapper;

    @Resource
    private LendItemReturnMapper lendItemReturnMapper;

    @Override
    public List<LendReturn> selectByLendId(Long lendId) {

        QueryWrapper<LendReturn> queryWrapper = new QueryWrapper();
        queryWrapper.eq("lend_id", lendId);
        List<LendReturn> lendReturnList = baseMapper.selectList(queryWrapper);
        return lendReturnList;
    }

    @Override
    public String commitReturn(Long lendReturnId, Long userId) {

        //获取还款计划记录
        LendReturn lendReturn = baseMapper.selectById(lendReturnId);

        //判断账号余额是否充足
        BigDecimal amount = userAccountService.getAccount(userId);
        Assert.isTrue(amount.doubleValue() >= lendReturn.getTotal().doubleValue(),
                ResponseEnum.NOT_SUFFICIENT_FUNDS_ERROR);

        //获取借款人code
        String bindCode = userBindService.getBindCodeByUserId(userId);
        //获取标的信息
        Long lendId = lendReturn.getLendId();
        Lend lend = lendMapper.selectById(lendId);

        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        //商户商品名称
        paramMap.put("agentGoodsName", lend.getTitle());
        //批次号
        paramMap.put("agentBatchNo",lendReturn.getReturnNo());
        //还款人绑定协议号
        paramMap.put("fromBindCode", bindCode);
        //还款总额
        paramMap.put("totalAmt", lendReturn.getTotal());
        paramMap.put("note", "");
        //还款明细
        List<Map<String, Object>> lendItemReturnDetailList = this.addRepayDetail(lendReturnId);
        paramMap.put("data", JSONObject.toJSONString(lendItemReturnDetailList));
        //出借人还款总金额
        //String sumTotal = lendItemReturnMapper.selectSumTotalByLendReturnId(lendReturnId);
        //P2P商户手续费 = 借款人还款总金额 - 出借人还款总金额
        //String voteFeeAmt = new BigDecimal(lendReturn.getTotal()).subtract(new BigDecimal(sumTotal)).toString();
        paramMap.put("voteFeeAmt", new BigDecimal(0));
        paramMap.put("notifyUrl", HfbConst.BORROW_RETURN_NOTIFY_URL);
        paramMap.put("returnUrl", HfbConst.BORROW_RETURN_RETURN_URL);
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);

        //构建自动提交表单
        String formStr = FormHelper.buildFrom(HfbConst.BORROW_RETURN_URL, paramMap);
        return formStr;

    }

    private List<Map<String, Object>> addRepayDetail(Long lendReturnId) {

        LendReturn lendReturn = baseMapper.selectById(lendReturnId);
        Long lendId = lendReturn.getLendId();
        Lend lend = lendMapper.selectById(lendId);

        //根据还款id获取回款记录列表
        List<LendItemReturn> lendItemReturnList = this.selectLendItemReturnList(lendReturnId);
        List<Map<String,Object>> mapList = new ArrayList<>();
        for (LendItemReturn lendItemReturn : lendItemReturnList) {
            //回款计划：lendItemReturn
            //通过回款计划获取投资记录
            Long lendItemId = lendItemReturn.getLendItemId();
            LendItem lendItem = lendItemMapper.selectById(lendItemId);
            //获取投资人的bindCode
            String bindCode = userBindService.getBindCodeByUserId(lendItem.getInvestUserId());


            HashMap<String, Object> repayMap = new HashMap<>();
            repayMap.put("agentProjectCode",lend.getId());//还款项目编号
            repayMap.put("voteBillNo",lendItem.getLendItemNo());//投资单号
            repayMap.put("toBindCode",bindCode);//投资人（收款人）
            repayMap.put("transitAmt", lendItemReturn.getTotal());//还款金额
            repayMap.put("baseAmt", lendItemReturn.getPrincipal()); //还款本金
            repayMap.put("benifitAmt", lendItemReturn.getInterest());//还款利息
            repayMap.put("feeAmt", new BigDecimal("0"));//商户手续费

            mapList.add(repayMap);

        }

        return mapList;

    }

    /**
     * 根据还款id获取回款计划列表
     * @param lendReturnId
     * @return
     */
    private List<LendItemReturn> selectLendItemReturnList(Long lendReturnId) {

        QueryWrapper<LendItemReturn> lendItemReturnQueryWrapper = new QueryWrapper<>();
        lendItemReturnQueryWrapper.eq("lend_return_id",lendReturnId);
        return lendItemReturnMapper.selectList(lendItemReturnQueryWrapper);
    }
}
