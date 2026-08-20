package com.c2c.im.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 会话实体：买卖双方之间的站内私信会话，user1 与 user2 按 ID 升序固定。
 */
@Data
@TableName("conversation")
public class Conversation {
    /** 会话 ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 会话成员 1 */
    private Long user1Id;

    /** 会话成员 2 */
    private Long user2Id;

    /** 关联商品 ID（可为空，非交易私聊） */
    private Long productId;

    /** 最后一条消息内容（列表预览） */
    private String lastMessage;

    /** 最后一条消息时间 */
    private LocalDateTime lastMessageTime;

    /** user1 的未读数 */
    private Integer user1Unread;

    /** user2 的未读数 */
    private Integer user2Unread;

    /** 创建时间（自动填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（自动填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
