package com.c2c.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 私信消息实体：会话内的单条消息，按会话查询与分页。
 */
@Data
@TableName("message")
public class Message {
    /** 消息 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话 ID */
    private Long conversationId;

    /** 发送者用户 ID */
    private Long senderId;

    /** 接收者用户 ID */
    private Long receiverId;

    /** 消息内容 */
    private String content;

    /** 消息类型：1 文字 / 2 图片 / 3 订单卡片 */
    private Integer messageType;

    /** 扩展数据（JSON 字符串，图片/卡片时使用） */
    private String extra;

    /** 是否已读：1 已读 / 0 未读 */
    private Integer isRead;

    /** 发送时间 */
    private LocalDateTime createdAt;

    /** 发送人昵称（查询时填充，非数据库列，供前端头像/显示用） */
    @TableField(exist = false)
    private String senderName;
}
