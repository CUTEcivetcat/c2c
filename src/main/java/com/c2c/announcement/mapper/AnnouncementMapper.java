package com.c2c.announcement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.announcement.entity.Announcement;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公告数据访问层（MyBatis-Plus Mapper）。
 */
@Mapper
public interface AnnouncementMapper extends BaseMapper<Announcement> {
}
