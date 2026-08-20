package com.c2c.announcement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台公告实体：管理员发布的公告/平台公约/通知，用户在公告页与首页横幅查看。
 */
@Data
@TableName("announcement")
public class Announcement {

    /** 公告 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标题 */
    private String title;

    /** 内容（支持换行文本） */
    private String content;

    /** 类型：1 公告 / 2 平台公约 / 3 通知 */
    private Integer type;

    /** 状态：1 已发布 / 0 已下架 */
    private Integer status;

    /** 是否置顶：1 置顶 / 0 普通 */
    private Integer pinned;

    /** 发布人用户 ID */
    private Long createdBy;

    /** 发布时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
