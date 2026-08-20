package com.c2c.review.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.review.entity.Report;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品举报数据访问层（MyBatis-Plus Mapper）。
 */
@Mapper
public interface ReportMapper extends BaseMapper<Report> {
}
