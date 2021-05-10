package com.atguigu.srb.core.service.impl;

import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.srb.core.enums.LendStatusEnum;
import com.atguigu.srb.core.enums.ReturnMethodEnum;
import com.atguigu.srb.core.enums.TransTypeEnum;
import com.atguigu.srb.core.hfb.HfbConst;
import com.atguigu.srb.core.hfb.RequestHelper;
import com.atguigu.srb.core.mapper.BorrowerMapper;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.mapper.UserInfoMapper;
import com.atguigu.srb.core.pojo.bo.TransFlowBO;
import com.atguigu.srb.core.pojo.entity.*;
import com.atguigu.srb.core.mapper.LendMapper;
import com.atguigu.srb.core.pojo.vo.BorrowInfoApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.service.*;
import com.atguigu.srb.core.util.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fasterxml.jackson.datatype.jsr310.ser.YearSerializer;
import jdk.net.SocketFlow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.security.auth.callback.Callback;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 标的准备表 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Service
@Slf4j
public class LendServiceImpl extends ServiceImpl<LendMapper, Lend> implements LendService {

    @Resource
    private DictService dictService;

    @Resource
    private BorrowerService borrowerService;

    @Resource
    private BorrowerMapper borrowerMapper;

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private TransFlowService transFlowService;

    @Resource
    private LendItemService lendItemService;

    @Resource
    private LendReturnService lendReturnService;

    @Resource
    private LendItemReturnService lendItemReturnService;

    @Override
    public void createLend(BorrowInfoApprovalVO borrowInfoApprovalVO, BorrowInfo borrowInfo) {
        Lend lend = new Lend();
        lend.setUserId(borrowInfo.getUserId());
        lend.setStatus(borrowInfo.getStatus());
        lend.setBorrowInfoId(borrowInfo.getId());
        lend.setLendNo(LendNoUtils.getLendNo());//生成编号
        lend.setTitle(borrowInfoApprovalVO.getTitle());
        lend.setAmount(borrowInfo.getAmount());
        lend.setPeriod(borrowInfo.getPeriod());
        lend.setLendYearRate(borrowInfoApprovalVO.getLendYearRate().divide(new BigDecimal(100)));//从审批对象中获取
        lend.setServiceRate(borrowInfoApprovalVO.getServiceRate().divide(new BigDecimal(100)));//从审批对象中获取
        lend.setReturnMethod(borrowInfo.getReturnMethod());
        lend.setLowestAmount(new BigDecimal(100));
        lend.setInvestAmount(new BigDecimal(0));
        lend.setInvestNum(0);
        lend.setPublishDate(LocalDateTime.now());
        //起息日期
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate lendStartDate = LocalDate.parse(borrowInfoApprovalVO.getLendStartDate(), dateTimeFormatter);
        lend.setLendStartDate(lendStartDate);
        //结束日期
        LocalDate lendEndDate = lendStartDate.plusMonths(borrowInfo.getPeriod());
        lend.setLendEndDate(lendEndDate);

        lend.setLendInfo(borrowInfoApprovalVO.getLendInfo());

        //预期收益
        //月年化 = 年华 / 12
        BigDecimal monthRate = lend.getLendYearRate().divide(new BigDecimal(12), 8, BigDecimal.ROUND_DOWN);
        //标的金额 * 月年化 * 期数
        BigDecimal expectAmount = lend.getAmount().multiply(monthRate).multiply(new BigDecimal(lend.getPeriod()));
        lend.setExpectAmount(expectAmount);

        //实际收益
        lend.setRealAmount(new BigDecimal(0));
        //状态
        lend.setStatus(LendStatusEnum.INVEST_RUN.getStatus());
        //审核人
        lend.setCheckAdminId(1L);
        baseMapper.insert(lend);

    }

    @Override
    public List<Lend> selectList() {
        List<Lend> lendList = baseMapper.selectList(null);
        lendList.forEach(lend -> {
            packLend(lend);
        });
        return lendList;
    }

    private void packLend(Lend lend) {
        String returnMethod = dictService.getNameByParentDictCodeAndValue("returnMethod", lend.getReturnMethod());
        String status = LendStatusEnum.getMsgByStatus(lend.getStatus());
        lend.getParam().put("returnMethod", returnMethod);
        lend.getParam().put("status", status);
    }

    @Override
    public Map<String, Object> getLendDetail(Long id) {

        //查询标的对象
        Lend lend = baseMapper.selectById(id);
        //组装数据
        packLend(lend);

        //根据user_id获取借款人对象
        QueryWrapper<Borrower> borrowerQueryWrapper = new QueryWrapper<>();
        borrowerQueryWrapper.eq("user_id", lend.getUserId());
        Borrower borrower = borrowerMapper.selectOne(borrowerQueryWrapper);
        //组装借款人对象
        BorrowerDetailVO borrowerDetailVO = borrowerService.getBorrowerDetailVOById(borrower.getId());

        //组装数据
        Map<String, Object> map = new HashMap<>();
        map.put("lend", lend);
        map.put("borrower", borrowerDetailVO);

        return map;
    }

    @Override
    public BigDecimal getInterestCount(BigDecimal invest, BigDecimal yearRate, Integer totalmonth, Integer returnMethod) {

        BigDecimal interestCount;
        //计算总利息
        if (returnMethod.intValue() == ReturnMethodEnum.ONE.getMethod()) {
            interestCount = Amount1Helper.getInterestCount(invest, yearRate, totalmonth);
        } else if (returnMethod.intValue() == ReturnMethodEnum.TWO.getMethod()) {
            interestCount = Amount2Helper.getInterestCount(invest, yearRate, totalmonth);
        } else if(returnMethod.intValue() == ReturnMethodEnum.THREE.getMethod()) {
            interestCount = Amount3Helper.getInterestCount(invest, yearRate, totalmonth);
        } else {
            interestCount = Amount4Helper.getInterestCount(invest, yearRate, totalmonth);
        }
        return interestCount;

    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void makeLoan(Long id) {

        //获取标的对象
        Lend lend = baseMapper.selectById(id);

        //向汇付宝发起远程调用
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("agentId", HfbConst.AGENT_ID);
        paramMap.put("agentProjectCode", lend.getLendNo());
        String agentBillNo = LendNoUtils.getLoanNo();//放款编号
        paramMap.put("agentBillNo", agentBillNo);

        //即平台收益，放款扣除，借款人借款实际金额=借款金额-平台收益
        //月年化
        BigDecimal monthRate = lend.getServiceRate().divide(new BigDecimal(12), 8, BigDecimal.ROUND_DOWN);
        //平台收益 = 标的金额 * 月年化 * 标的期数
        BigDecimal realAmount = lend.getInvestAmount().multiply(monthRate).multiply(new BigDecimal(lend.getPeriod()));
        paramMap.put("mchFee", realAmount);//手续费
        paramMap.put("timestamp", RequestHelper.getTimestamp());
        String sign = RequestHelper.getSign(paramMap);
        paramMap.put("sign", sign);

        //发起远程调用:通过向指定的url地址和参数发起远程调用，得到调用结果
        //同步请求
        JSONObject result = RequestHelper.sendRequest(paramMap, HfbConst.MAKE_LOAD_URL);
        log.info("放款远程调用的执行结果：" + result.toJSONString());

        //判断放款是否成功
        if (!"0000".equals(result.getString("resultCode"))){
            throw new BusinessException(result.getString("resultMsg"));
        }

        //===================当远程调用成功后（同步调用）===================
        //商户平台中同步账户信息
        //修改标的的状态
        lend.setStatus(LendStatusEnum.PAY_RUN.getStatus());
        lend.setPaymentTime(LocalDateTime.now());
        baseMapper.updateById(lend);


        //获取借款人的bindCode
        Long userId = lend.getUserId();
        UserInfo userInfo = userInfoMapper.selectById(userId);
        String bindCode = userInfo.getBindCode();

        //给借款人的账户转入资金
        BigDecimal total = new BigDecimal(result.getString("voteAmt")).subtract(realAmount);
        userAccountMapper.updateAccount(bindCode,total,new BigDecimal(0));

        //为借款人添加交易流水
        //新增交易流水
        TransFlowBO transFlowBO = new TransFlowBO(
                agentBillNo,
                bindCode,
                total,
                TransTypeEnum.BORROW_BACK,
                "借款已放款，项目编号：" + lend.getLendNo() + "，项目名称：" + lend.getTitle());
        transFlowService.saveTransFlow(transFlowBO);

        //从投资人的账户解锁冻结资金（转给借款人）
        List<LendItem> lendItemList = lendItemService.selectByLendId(lend.getId(), 1);
        lendItemList.forEach(item -> {

            Long investUserId = item.getInvestUserId();//投资人的userId
            UserInfo investUserInfo = userInfoMapper.selectById(investUserId);
            String investBindCode = investUserInfo.getBindCode();

            BigDecimal investAmount = item.getInvestAmount();//投资金额
            userAccountMapper.updateAccount(investBindCode,new BigDecimal(0),investAmount.negate());//negate()：取反

            //为投资人新增交易流水
            //新增交易流水
            TransFlowBO investTransFlowBO = new TransFlowBO(
                    LendNoUtils.getTransNo(item.getInvestUserId(),item.getId().toString()),
                    investBindCode,
                    investAmount,
                    TransTypeEnum.INVEST_UNLOCK,
                    "冻结资金已转出，项目编号：" + lend.getLendNo() + "，项目名称：" + lend.getTitle());
            transFlowService.saveTransFlow(investTransFlowBO);


        });


        //生成还款计划和回款计划
        //放款成功生成借款人还款计划和投资人回款计划
        this.repaymentPlan(lend);

    }

    /**
     * 生成还款计划
     * @param lend
     */
    private void repaymentPlan(Lend lend) {

        //======================================================
        //===================生成还款计划=========================

        //创建还款计划列表
        ArrayList<LendReturn> lendReturnList = new ArrayList<>();

        //获取投资期数
        int len = lend.getPeriod().intValue();

        for (int i = 1; i <= len; i++) {

            //生成还款计划
            LendReturn lendReturn = new LendReturn();
            lendReturn.setReturnNo(LendNoUtils.getReturnNo());//设置还款编号
            lendReturn.setLendId(lend.getId());//设置标的id
            lendReturn.setBorrowInfoId(lend.getBorrowInfoId());//设置借款信息id
            lendReturn.setUserId(lend.getUserId());//设置借款人id
//            lendReturn.setAmount(lend.getAmount());//设置借款总金额：满标放款
            lendReturn.setAmount(lend.getInvestAmount());//设置借款总金额：非满标放款
            lendReturn.setBaseAmount(lend.getInvestAmount());//设置计算利息的本金
            lendReturn.setLendYearRate(lend.getLendYearRate());//设置年化
            lendReturn.setCurrentPeriod(i);//当前期数
            lendReturn.setReturnMethod(lend.getReturnMethod());//还款方式

            lendReturn.setFee(new BigDecimal("0"));
            lendReturn.setReturnDate(lend.getLendStartDate().plusMonths(i)); //第二个月开始还款
            lendReturn.setOverdue(false);//是否逾期：默认未逾期
            if (i == len) { //最后一个月
                //标识为最后一次还款
                lendReturn.setLast(true);
            } else {
                lendReturn.setLast(false);
            }
            lendReturn.setStatus(0);//0：未归还 1：已归还
            lendReturnList.add(lendReturn);

        }
        //批量保存还款计划
        lendReturnService.saveBatch(lendReturnList);//列表中所有的对象的id字段在保存后会被回填


        //======================================================
        //===================生成回款计划=========================

        //遍历lendReturnList,将每个成员的期数 currentPeriod 和 id获取出来，组装成Map集合
        Map<Integer, Long> lendReturnMap = lendReturnList.stream()
                .collect(Collectors.toMap(LendReturn::getCurrentPeriod, LendReturn::getId));

        //创建回款计划列表(所有投资人的所有回款计划列表)
        List<LendItemReturn> lendItemReturnAllList = new ArrayList<>();

        //获取所有的成功投资者的投资列表
        List<LendItem> lendItemList = lendItemService.selectByLendId(lend.getId(), 1);

        //投资列表（每个投资人的投资记录）
        for (LendItem lendItem : lendItemList) {

            //lendItem:每笔投资 ==> 投资的回款计划列表
            List<LendItemReturn> currentLendItemReturnList = this.returnInvest(
                    lendItem.getId(),
                    lendReturnMap,
                    lend);
            //将当前投资人的回款计划列表添加到所有投资人的全部的回款计划中
            //addAll() 添加集合到一个集合
            lendItemReturnAllList.addAll(currentLendItemReturnList);

        }

        //设置每期还款的本金，利息以及本息：需要在生成回款计划后再计算（保证数据的一致性）
        for (LendReturn lendReturn : lendReturnList) {

            //lendReturn：还款人当前一期的还款计划
            //还款人的第n期的还款金额 = 所有投资人第n期的回款金额相加

            //本金
            BigDecimal sumPrincipal = lendItemReturnAllList.stream()
                    .filter(item -> item.getLendReturnId().longValue() == lendReturn.getId().longValue())//过滤出一期的回款记录
                    .map(LendItemReturn::getPrincipal)//选择字段：principal---取出本金列
                    .reduce(BigDecimal.ZERO, BigDecimal::add);//数据相加
            //利息
            BigDecimal sumInterest = lendItemReturnAllList.stream()
                    .filter(item -> item.getLendReturnId().longValue() == lendReturn.getId().longValue())//过滤出一期的回款记录
                    .map(LendItemReturn::getInterest)//选择字段：interest---取出利息列
                    .reduce(BigDecimal.ZERO, BigDecimal::add);//数据相加
            //本息
            BigDecimal sumTotal = lendItemReturnAllList.stream()
                    .filter(item -> item.getLendReturnId().longValue() == lendReturn.getId().longValue())//过滤出一期的回款记录
                    .map(LendItemReturn::getTotal)//选择字段：total---取出本息列
                    .reduce(BigDecimal.ZERO, BigDecimal::add);//数据相加

            lendReturn.setPrincipal(sumPrincipal);
            lendReturn.setInterest(sumInterest);
            lendReturn.setTotal(sumTotal);
        }

        lendReturnService.updateBatchById(lendReturnList);
    }

    /**
     * 生成一个投资记录的回款计划
     * @param lendItemId
     * @param lendReturnMap 期数和还款计划id对应的键值对
     * @param lend
     * @return
     */
    private List<LendItemReturn> returnInvest(Long lendItemId,Map<Integer,Long> lendReturnMap,Lend lend) {

        //获取当前投资人的投资信息
        LendItem lendItem = lendItemService.getById(lendItemId);
        //投资金额
        BigDecimal amount = lendItem.getInvestAmount();
        //年化
        BigDecimal lendYearRate = lendItem.getLendYearRate();
        //投资期数
        Integer period = lend.getPeriod();

        //根据当前的还款方式计算出每一期需要偿还的本金和利息
        Map<Integer, BigDecimal> perMonthInterest = null;//每月还款的利息
        Map<Integer, BigDecimal> perMonthPrincipal = null;//每月还款的本金
        int returnMethod = lend.getReturnMethod().intValue();//还款方式
        if (returnMethod == ReturnMethodEnum.ONE.getMethod().intValue()){
            //计算利息
            perMonthInterest = Amount1Helper.getPerMonthInterest(amount, lendYearRate, period);
            //计算本金
            perMonthPrincipal = Amount1Helper.getPerMonthPrincipal(amount, lendYearRate, period);
        }else if (returnMethod == ReturnMethodEnum.TWO.getMethod().intValue()){
            //计算利息
            perMonthInterest = Amount2Helper.getPerMonthInterest(amount, lendYearRate, period);
            //计算本金
            perMonthPrincipal = Amount2Helper.getPerMonthPrincipal(amount, lendYearRate, period);
        }else if (returnMethod == ReturnMethodEnum.THREE.getMethod().intValue()){
            //计算利息
            perMonthInterest = Amount3Helper.getPerMonthInterest(amount, lendYearRate, period);
            //计算本金
            perMonthPrincipal = Amount3Helper.getPerMonthPrincipal(amount, lendYearRate, period);
        }else if (returnMethod == ReturnMethodEnum.FOUR.getMethod().intValue()){
            //计算利息
            perMonthInterest = Amount4Helper.getPerMonthInterest(amount, lendYearRate, period);
            //计算本金
            perMonthPrincipal = Amount4Helper.getPerMonthPrincipal(amount, lendYearRate, period);
        }

        //创建回款计划列表
        List<LendItemReturn> lendItemReturnList = new ArrayList<>();

        //遍历集合（期数和每期利息对应的集合）
        for (Map.Entry<Integer, BigDecimal> entry : perMonthInterest.entrySet()) {
            Integer currentPeriod = entry.getKey();//回款计划期数
            Long lendReturnId = lendReturnMap.get(currentPeriod);//每期对应的还款计划id

            //创建回款计划
            LendItemReturn lendItemReturn = new LendItemReturn();
            lendItemReturn.setLendReturnId(lendReturnId);//设置对应的还款计划id
            lendItemReturn.setLendItemId(lendItemId);//投资记录id
            lendItemReturn.setInvestUserId(lendItem.getInvestUserId());//投资人id
            lendItemReturn.setLendId(lendItem.getLendId());//标的id
            lendItemReturn.setInvestAmount(lendItem.getInvestAmount());//投资金额
            lendItemReturn.setLendYearRate(lend.getLendYearRate());//年化
            lendItemReturn.setCurrentPeriod(currentPeriod);//当前期数
            lendItemReturn.setReturnMethod(lend.getReturnMethod());//还款方式

            //最后一次本金计算
            if (lendItemReturnList.size() > 0 && currentPeriod.intValue() == lend.getPeriod().intValue()) {
                //最后一期本金 = 本金 - 前几次之和
                BigDecimal sumPrincipal = lendItemReturnList.stream()
                        .map(LendItemReturn::getPrincipal)//获取值
                        .reduce(BigDecimal.ZERO, BigDecimal::add);//计算
                //用当前投资人的总投资金额 - 除最后一期外应还本金
                BigDecimal lastPrincipal = lendItem.getInvestAmount().subtract(sumPrincipal);
                lendItemReturn.setPrincipal(lastPrincipal);
            } else {
                lendItemReturn.setPrincipal(perMonthPrincipal.get(currentPeriod));
            }

            lendItemReturn.setInterest(perMonthInterest.get(currentPeriod));//利息

            lendItemReturn.setTotal(lendItemReturn.getPrincipal().add(lendItemReturn.getInterest()));
            lendItemReturn.setFee(new BigDecimal("0"));
            lendItemReturn.setReturnDate(lend.getLendStartDate().plusMonths(currentPeriod));//计划还款日期
            //是否逾期，默认未预期
            lendItemReturn.setOverdue(false);
            lendItemReturn.setStatus(0);

            lendItemReturnList.add(lendItemReturn);
        }
        //批量保存回款计划
        lendItemReturnService.saveBatch(lendItemReturnList);

        return lendItemReturnList;

    }

}
