package com.atguigu.srb.core.pojo.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author hjystart
 * @create 2020-12-24 16:14
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ApiModel(description="用户信息对象")
public class UserInfoVO {

    @ApiModelProperty(value = "用户姓名")
    private String name;

    @ApiModelProperty(value = "用户昵称")
    private String nickName;

    @ApiModelProperty(value = "1：出借人 2：借款人")
    private Integer userType;

    @ApiModelProperty(value = "用户头像")
    private String headImg;

    @ApiModelProperty(value = "用户手机号")
    private String mobile;


    @ApiModelProperty(value = "JWT访问令牌")
    private String token;
}
