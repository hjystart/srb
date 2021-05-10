package com.atguigu.srb.core.service.impl;

import com.atguigu.srb.core.pojo.entity.BorrowerAttach;
import com.atguigu.srb.core.mapper.BorrowerAttachMapper;
import com.atguigu.srb.core.pojo.vo.BorrowerAttachVO;
import com.atguigu.srb.core.pojo.vo.BorrowerVO;
import com.atguigu.srb.core.service.BorrowerAttachService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 借款人上传资源表 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Service
public class BorrowerAttachServiceImpl extends ServiceImpl<BorrowerAttachMapper, BorrowerAttach> implements BorrowerAttachService {

    @Override
    public List<BorrowerAttachVO> selectBorrowerAttachVOList(Long borrowerId) {

        //查询当前借款人的附件数据
        QueryWrapper<BorrowerAttach> borrowerAttachQueryWrapper = new QueryWrapper<>();
        borrowerAttachQueryWrapper.eq("borrower_id",borrowerId);
        List<BorrowerAttach> borrowerAttachList = baseMapper.selectList(borrowerAttachQueryWrapper);

        //组装VO对象
        List<BorrowerAttachVO> borrowerAttachVOArrayList = new ArrayList<>();
        borrowerAttachList.forEach(borrowerAttach ->{
            BorrowerAttachVO borrowerAttachVO = new BorrowerAttachVO();
//            BeanUtils.copyProperties(borrowerAttach,borrowerAttachVO);
            borrowerAttachVO.setImageUrl(borrowerAttach.getImageUrl());
            borrowerAttachVO.setImageType(borrowerAttach.getImageType());
            borrowerAttachVOArrayList.add(borrowerAttachVO);
        });

        return borrowerAttachVOArrayList;
    }
}
