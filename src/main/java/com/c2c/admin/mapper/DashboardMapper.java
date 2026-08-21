package com.c2c.admin.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 管理端看板数据查询（直接查 DB，绕开 Feign 层，简化聚合）。
 */
@Mapper
public interface DashboardMapper {

    @Select("SELECT DATE(created_at) AS d, COUNT(*) AS cnt FROM `user` WHERE created_at >= #{since} GROUP BY d ORDER BY d")
    List<Map<String, Object>> userTrend(@Param("since") LocalDateTime since);

    @Select("SELECT DATE(created_at) AS d, COUNT(*) AS cnt, COALESCE(SUM(total_amount), 0) AS rev FROM `order` WHERE created_at >= #{since} GROUP BY d ORDER BY d")
    List<Map<String, Object>> orderTrend(@Param("since") LocalDateTime since);
}