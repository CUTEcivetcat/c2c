package com.c2c.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.review.entity.AdminLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理/审核操作日志数据访问层（MyBatis-Plus Mapper）。
 */
@Mapper
public interface AdminLogMapper extends BaseMapper<AdminLog> {
}
