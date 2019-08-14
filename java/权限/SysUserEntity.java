package com.gaoh.mybatisplus.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author gaoh
 * @since 2019-08-06
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "sys_user")
public class SysUserEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id",type = IdType.AUTO)
    private Integer id;

    /**
     * 姓名
     */
    private String userName;

    /**
     * 用户名
     */
    private String userCode;

    /**
     * 用户名大写
     */
    private String personId;

    private String password;

    /**
     * 手机号
     */
    private Integer telNo;

    private String email;

    private String qq;

    /**
     * 登录次数
     */
    private Integer loginNum;

    /**
     * 人员状态（0：锁定  1：可用）
     */
    private String status;

    /**
     * 创建人ID
     */
    private String createUserId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 最后更新人ID
     */
    private String updateUserId;

    /**
     * 最后更新时间
     */
    private Date updateTime;
}
