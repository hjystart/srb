package com.atguigu.srb.core.service.impl;

import com.atguigu.common.exception.Assert;
import com.atguigu.common.exception.BusinessException;
import com.atguigu.common.result.R;
import com.atguigu.common.result.ResponseEnum;
import com.atguigu.common.util.HttpClientUtils;
import com.atguigu.common.util.MD5;
import com.atguigu.srb.base.util.JwtUtils;
import com.atguigu.srb.core.client.OssFileClient;
import com.atguigu.srb.core.mapper.UserAccountMapper;
import com.atguigu.srb.core.mapper.UserLoginRecordMapper;
import com.atguigu.srb.core.pojo.entity.UserAccount;
import com.atguigu.srb.core.pojo.entity.UserInfo;
import com.atguigu.srb.core.mapper.UserInfoMapper;
import com.atguigu.srb.core.pojo.entity.UserLoginRecord;
import com.atguigu.srb.core.pojo.query.UserInfoQuery;
import com.atguigu.srb.core.pojo.vo.LoginVO;
import com.atguigu.srb.core.pojo.vo.RegisterVO;
import com.atguigu.srb.core.pojo.vo.UserInfoVO;
import com.atguigu.srb.core.pojo.vo.WxBindVO;
import com.atguigu.srb.core.service.UserInfoService;
import com.baomidou.mybatisplus.core.conditions.query.Query;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.util.StringUtil;
import org.apache.xmlbeans.impl.xb.xsdschema.impl.AttributeImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 用户基本信息 服务实现类
 * </p>
 *
 * @author Helen
 * @since 2020-12-13
 */
@Service
@Slf4j
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Resource
    private UserAccountMapper userAccountMapper;

    @Resource
    private UserLoginRecordMapper userLoginRecordMapper;

    @Resource
    private OssFileClient ossFileClient;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public void register(RegisterVO registerVO) {
        //判断用户当前手机号是否被注册：根据手机号查询当前用户是否存在
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("mobile", registerVO.getMobile());
        Integer count = baseMapper.selectCount(userInfoQueryWrapper);
        Assert.isTrue(count == 0, ResponseEnum.MOBILE_EXIST_ERROR);

        //向UserInfo插入数据
        String mobile = registerVO.getMobile();
        UserInfo userInfo = new UserInfo();
        userInfo.setUserType(registerVO.getUserType());
        userInfo.setNickName(mobile);
        userInfo.setName(mobile);
        userInfo.setMobile(mobile);
        userInfo.setPassword(MD5.encrypt(registerVO.getPassword()));
        userInfo.setStatus(UserInfo.STATUS_NORMAL);
        userInfo.setHeadImg("https://srb-file-200820.oss-cn-beijing.aliyuncs.com/avatar/05.jpg");
        baseMapper.insert(userInfo);

        //向UserAccount插入数据
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userInfo.getId());
        userAccountMapper.insert(userAccount);

    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public UserInfoVO login(LoginVO loginVO, String remoteAddr) {

        String mobile = loginVO.getMobile();
        String password = loginVO.getPassword();
        Integer userType = loginVO.getUserType();
//        System.out.println("mobile"+mobile);
//        System.out.println("password"+password);
//        System.out.println("userType"+userType);
        //查看用户是否存在
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper
                .eq("mobile", mobile)
                .eq("user_type", userType);
        UserInfo userInfo = baseMapper.selectOne(userInfoQueryWrapper);

//        System.out.println(userInfo);

        Assert.notNull(userInfo, ResponseEnum.LOGIN_MOBILE_ERROR);
        //校验密码是否正确
        Assert.equals(MD5.encrypt(password), userInfo.getPassword(), ResponseEnum.LOGIN_PASSWORD_ERROR);
        //用户是否被禁用
        Assert.equals(userInfo.getStatus(), UserInfo.STATUS_NORMAL, ResponseEnum.LOGIN_LOKED_ERROR);
        //记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setIp(remoteAddr);
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecordMapper.insert(userLoginRecord);

        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setToken(token);
        userInfoVO.setName(userInfo.getName());
        userInfoVO.setNickName(userInfo.getNickName());
        userInfoVO.setUserType(userType);
        userInfoVO.setHeadImg(userInfo.getHeadImg());
        userInfoVO.setMobile(mobile);

        return userInfoVO;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public UserInfoVO login(WxBindVO wxBindVO, String remoteAddr) {
        String mobile = wxBindVO.getMobile();
        String openid = wxBindVO.getOpenid();
        Integer userType = wxBindVO.getUserType();

        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper
                .eq("mobile",mobile)
                .eq("openid",openid)
                .eq("user_type",userType);
        UserInfo userInfo = baseMapper.selectOne(userInfoQueryWrapper);
        Assert.notNull(userInfo,ResponseEnum.LOGIN_MOBILE_ERROR);
        Assert.equals(userInfo.getStatus(),UserInfo.STATUS_NORMAL,ResponseEnum.LOGIN_LOKED_ERROR);

        //记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setIp(remoteAddr);
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecordMapper.insert(userLoginRecord);

        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setToken(token);
        userInfoVO.setName(userInfo.getName());
        userInfoVO.setNickName(userInfo.getNickName());
        userInfoVO.setUserType(userType);
        userInfoVO.setHeadImg(userInfo.getHeadImg());
        userInfoVO.setMobile(mobile);

        return userInfoVO;
    }

    @Override
    public IPage<UserInfo> listPage(Page<UserInfo> pageParam, UserInfoQuery userInfoQuery) {

        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();

        //当没有查询条件时
        if (userInfoQuery == null) {
            return baseMapper.selectPage(pageParam, null);
        }

        String mobile = userInfoQuery.getMobile();
        Integer userType = userInfoQuery.getUserType();
        Integer status = userInfoQuery.getStatus();

//        if (!StringUtils.isBlank(mobile)){
//            userInfoQueryWrapper.like("mobile",mobile);
//        }
        userInfoQueryWrapper
                .like(StringUtils.isNotBlank(mobile), "mobile", mobile)
                .eq(status != null, "status", status)
                .eq(userType != null, "user_type", userType);
        return baseMapper.selectPage(pageParam, userInfoQueryWrapper);
    }

    @Override
    public void lock(Long id, Integer status) {
//        UserInfo userInfo = this.getById(id);
//        userInfo.setStatus(status);
//        this.updateById(userInfo);

        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setStatus(status);
        baseMapper.updateById(userInfo);
    }

    @Override
    public UserInfo getByOpenid(String openid) {
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("openid", openid);
        return baseMapper.selectOne(userInfoQueryWrapper);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public UserInfo registerWx(String accessToken, String openid) {

        //获取微信用的个人信息
        String userInfoUrl = "https://api.weixin.qq.com/sns/userinfo";
//                "?access_token=ACCESS_TOKEN&openid=OPENID";
        Map<String, String> userInfoParams = new HashMap<>();
        userInfoParams.put("access_token", accessToken);
        userInfoParams.put("openid", openid);

        HttpClientUtils client = new HttpClientUtils(userInfoUrl, userInfoParams);

        String result = "";
        try {
            client.setHttps(true);
            client.get();
            //响应内容
            result = client.getContent();
            log.info("result = " + result);
        } catch (Exception e) {
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_USERINFO_ERROR, e);
        }

        //解析响应结果
        Gson gson = new Gson();
        HashMap<String, Object> resultMap = gson.fromJson(result, HashMap.class);
        if (resultMap.get("errcode") != null) {
            String message = (String) resultMap.get("errmsg");
            Double errcode = (Double) resultMap.get("errcode");
            log.error("获取access_token失败：message = " + message + ",errcode = " + errcode);
            throw new BusinessException(ResponseEnum.WEIXIN_FETCH_USERINFO_ERROR);
        }

        //获取昵称和头像信息
        String nickname = (String) resultMap.get("nickname");
        //微信中拉取的用户头像
        String headimgurl = (String) resultMap.get("headimgurl");

        //注册用户，将微信的用户信息插入到数据库中。（user_info）
        UserInfo userInfo = new UserInfo();
        userInfo.setOpenid(openid);
        userInfo.setNickName(nickname);
        log.info("开始远程url头像上传");
        //将微信的远程头像拉取到我们的oss服务器上。返回oss服务器上的头像地址，将此地址存储在headImg字段中
        R r = ossFileClient.uploadFromUrl(headimgurl, "avatar");
        String url = (String) r.getData().get("url");
        log.info("上传成功并获取到了oss服务器上的url地址");
        userInfo.setHeadImg(url);//储存用户头像
        userInfo.setName(nickname);
        userInfo.setStatus(UserInfo.STATUS_NORMAL);

        baseMapper.insert(userInfo);

        //向UserAccount插入数据
        UserAccount userAccount = new UserAccount();
        userAccount.setUserId(userInfo.getId());
        userAccountMapper.insert(userAccount);

        return userInfo;
    }

    @Override
    public UserInfoVO getUserInfoVOByOpenid(String openid) {
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper
                .eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(userInfoQueryWrapper);
        UserInfoVO userInfoVO = new UserInfoVO();
        BeanUtils.copyProperties(userInfo, userInfoVO);

        return userInfoVO;
    }

    @Override
    public UserInfoVO bind(WxBindVO wxBindVO, String ip) {
        String mobile = wxBindVO.getMobile();
        String openid = wxBindVO.getOpenid();
        Integer userType = wxBindVO.getUserType();

        //校验手机号是否已被注册
        QueryWrapper<UserInfo> userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("mobile", mobile);
        Integer count = baseMapper.selectCount(userInfoQueryWrapper);
        Assert.isTrue(count == 0, ResponseEnum.MOBILE_EXIST_ERROR);

        //根据openid查找当前用户
        userInfoQueryWrapper = new QueryWrapper<>();
        userInfoQueryWrapper.eq("openid", openid);
        UserInfo userInfo = baseMapper.selectOne(userInfoQueryWrapper);

        //绑定手机号
        userInfo.setMobile(mobile);
        userInfo.setUserType(userType);
        baseMapper.updateById(userInfo);

        //记录登录日志
        UserLoginRecord userLoginRecord = new UserLoginRecord();
        userLoginRecord.setIp(ip);
        userLoginRecord.setUserId(userInfo.getId());
        userLoginRecordMapper.insert(userLoginRecord);

        //生成token
        String token = JwtUtils.createToken(userInfo.getId(), userInfo.getName());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setToken(token);
        userInfoVO.setName(userInfo.getName());
        userInfoVO.setNickName(userInfo.getNickName());
        userInfoVO.setUserType(userType);
        userInfoVO.setHeadImg(userInfo.getHeadImg());
        userInfoVO.setMobile(mobile);

        return userInfoVO;
    }
}
