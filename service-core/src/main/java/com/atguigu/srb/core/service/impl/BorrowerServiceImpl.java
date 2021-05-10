package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.core.enums.BorrowerStatusEnum;
import com.atguigu.srb.core.enums.IntegralEnum;
import com.atguigu.srb.core.mapper.BorrowerAttachMapper;
import com.atguigu.srb.core.mapper.UserInfoMapper;
import com.atguigu.srb.core.mapper.UserIntegralMapper;
import com.atguigu.srb.core.pojo.entity.Borrower;
import com.atguigu.srb.core.mapper.BorrowerMapper;
import com.atguigu.srb.core.pojo.entity.BorrowerAttach;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.pojo.entity.UserIntegral;
import com.atguigu.srb.core.pojo.vo.BorrowerApprovalVO;
import com.atguigu.srb.core.pojo.vo.BorrowerAttachVO;
import com.atguigu.srb.core.pojo.vo.BorrowerDetailVO;
import com.atguigu.srb.core.pojo.vo.BorrowerVO;
import com.atguigu.srb.core.service.BorrowerAttachService;
import com.atguigu.srb.core.service.BorrowerService;
import com.atguigu.srb.core.service.DictService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.apache.poi.hssf.eventusermodel.AbortableHSSFListener;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 借款人 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Service
public class BorrowerServiceImpl extends ServiceImpl<BorrowerMapper, Borrower> implements BorrowerService {

    @Resource
    private UserInfoMapper userInfoMapper;

    @Resource
    private BorrowerAttachMapper borrowerAttachMapper;

    @Resource
    private DictService dictService;

    @Resource
    private BorrowerAttachService borrowerAttachService;

    @Resource
    private UserIntegralMapper userIntegralMapper;

    @Override
    public void saveBorrowerVOByUserId(BorrowerVO borrowerVO, Long userId) {

        //从userInfo中获取用户基本数据
        UserInfo userInfo = userInfoMapper.selectById(userId);

        //保存BorrowerVO到数据表中
        Borrower borrower = new Borrower();
        BeanUtils.copyProperties(borrowerVO, borrower);
        borrower.setUserId(userId);
        borrower.setName(userInfo.getName());
        borrower.setIdCard(userInfo.getIdCard());
        borrower.setMobile(userInfo.getMobile());
        borrower.setStatus(BorrowerStatusEnum.AUTH_RUN.getStatus());
        baseMapper.insert(borrower);
        //保存borrowerAttachList到数据表中
        List<BorrowerAttach> borrowerAttachList = borrowerVO.getBorrowerAttachList();
        borrowerAttachList.forEach(borrowerAttach -> {
            borrowerAttach.setBorrowerId(borrower.getId());
            borrowerAttachMapper.insert(borrowerAttach);
        });

    }

    @Override
    public Integer getStatusByUserId(Long userId) {
        QueryWrapper<Borrower> borrowerQueryWrapper = new QueryWrapper<>();
        borrowerQueryWrapper.select("status").eq("user_id", userId);
        List<Object> objects = baseMapper.selectObjs(borrowerQueryWrapper);
        if (objects.size() == 0) return -2;//未申请

        Integer status = (Integer) objects.get(0);
        return status;
    }

    @Override
    public IPage<Borrower> listPage(Page<Borrower> pageParam, String keyword) {
        QueryWrapper<Borrower> borrowerQueryWrapper = new QueryWrapper<>();
        //按id倒序排
        borrowerQueryWrapper.orderByDesc("id");
        if (StringUtils.isEmpty(keyword)) {
            return baseMapper.selectPage(pageParam, borrowerQueryWrapper);
        }

        borrowerQueryWrapper.like("name", keyword)
                .or().like("id_card", keyword)
                .or().like("mobile", keyword);
//                .orderByDesc("id");
        return baseMapper.selectPage(pageParam, borrowerQueryWrapper);

    }

    @Override
    public BorrowerDetailVO getBorrowerDetailVOById(Long id) {
        //组装借款人基本信息
        Borrower borrower = baseMapper.selectById(id);
        BorrowerDetailVO borrowerDetailVO = new BorrowerDetailVO();

        BeanUtils.copyProperties(borrower, borrowerDetailVO);
        borrowerDetailVO.setMarry(borrower.getMarry() ? "是" : "否");
        borrowerDetailVO.setSex(borrower.getSex() == 1 ? "男" : "女");

        //组装数据字典的内容
        //查询dict表的dict_code字段，如果dict_code="education"，
        //那么查找当前记录的子记录中value = borrower.getEducation()数据的name
        String education = dictService.getNameByParentDictCodeAndValue("education", borrower.getEducation());
        borrowerDetailVO.setEducation(education);
        String industry = dictService.getNameByParentDictCodeAndValue("industry", borrower.getIndustry());
        borrowerDetailVO.setIndustry(industry);
        String income = dictService.getNameByParentDictCodeAndValue("income", borrower.getIncome());
        borrowerDetailVO.setIncome(income);
        String returnSource = dictService.getNameByParentDictCodeAndValue("returnSource", borrower.getReturnSource());
        borrowerDetailVO.setReturnSource(returnSource);
        String relation = dictService.getNameByParentDictCodeAndValue("relation", borrower.getContactsRelation());
        borrowerDetailVO.setContactsRelation(relation);

        //处理status字符串
        String status = BorrowerStatusEnum.getMsgByStatus(borrower.getStatus());
        borrowerDetailVO.setStatusStr(status);

        //组装图片列表
        List<BorrowerAttachVO> borrowerAttachVOList = borrowerAttachService.selectBorrowerAttachVOList(id);
        borrowerDetailVO.setBorrowerAttachVOList(borrowerAttachVOList);

        return borrowerDetailVO;
    }

    @Override
    public void approval(BorrowerApprovalVO borrowerApprovalVO) {
        //更新借款人认证状态
        Long borrowerId = borrowerApprovalVO.getBorrowerId();
        Borrower borrower = baseMapper.selectById(borrowerId);
//        Borrower borrower = new Borrower();
        borrower.setId(borrowerId);
        borrower.setStatus(borrowerApprovalVO.getStatus());
        baseMapper.updateById(borrower);

        //获取用户基本信息数据（积分）
        Long userId = borrower.getUserId();
        UserInfo userInfo = userInfoMapper.selectById(userId);
        //获取初始积分
        Integer integral = userInfo.getIntegral();

        //计算积分:新增用户积分记录
        //基本信息
        UserIntegral userIntegral = new UserIntegral();
        userIntegral.setUserId(userId);
        userIntegral.setContent("借款人基本信息");
        userIntegral.setIntegral(borrowerApprovalVO.getInfoIntegral());
        userIntegralMapper.insert(userIntegral);
        integral += borrowerApprovalVO.getInfoIntegral();

        //判断是否获得身份证积分
        if(borrowerApprovalVO.getIsIdCardOk()){
            userIntegral = new UserIntegral();
            userIntegral.setUserId(userId);
            userIntegral.setContent(IntegralEnum.BORROWER_IDCARD.getMsg());
            userIntegral.setIntegral(IntegralEnum.BORROWER_IDCARD.getIntegral());
            userIntegralMapper.insert(userIntegral);
            integral += IntegralEnum.BORROWER_IDCARD.getIntegral();//积分累加
        }
        //判断是否获得房产积分
        if(borrowerApprovalVO.getIsHouseOk()){
            userIntegral = new UserIntegral();
            userIntegral.setUserId(userId);
            userIntegral.setContent(IntegralEnum.BORROWER_HOUSE.getMsg());
            userIntegral.setIntegral(IntegralEnum.BORROWER_HOUSE.getIntegral());
            userIntegralMapper.insert(userIntegral);
            integral += IntegralEnum.BORROWER_HOUSE.getIntegral();//积分累加
        }
        //判断是否获得车辆积分
        if(borrowerApprovalVO.getIsCarOk()){
            userIntegral = new UserIntegral();
            userIntegral.setUserId(userId);
            userIntegral.setContent(IntegralEnum.BORROWER_CAR.getMsg());
            userIntegral.setIntegral(IntegralEnum.BORROWER_CAR.getIntegral());
            userIntegralMapper.insert(userIntegral);
            integral += IntegralEnum.BORROWER_CAR.getIntegral();//积分累加
        }

        //计算积分：将总积分存入user_info表
        userInfo.setIntegral(integral);//设置总积分

        //修改审核状态
        userInfo.setBorrowAuthStatus(borrowerApprovalVO.getStatus());
        userInfoMapper.updateById(userInfo);

    }
    /*public Integer getStatusByUserId(Long userId) {
        QueryWrapper<Borrower> borrowerQueryWrapper = new QueryWrapper<>();
        borrowerQueryWrapper.eq("user_id",userId);
        Borrower borrower = baseMapper.selectOne(borrowerQueryWrapper);
        Integer status = borrower.getStatus();
        return status;
    }*/
}
