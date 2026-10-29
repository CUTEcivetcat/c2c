package com.c2c.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统运行配置实体：存储可由管理端「系统设置」面板动态调整的运行时开关。
 */
@Data
@TableName("sys_config")
public class SysConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 配置键 */
    private String configKey;

    /** 配置值 */
    private String configValue;

    /** 配置说明 */
    private String remark;

    /** 最后修改人ID */
    private Long updatedBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
