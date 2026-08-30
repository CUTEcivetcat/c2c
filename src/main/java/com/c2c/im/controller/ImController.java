package com.c2c.im.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.im.entity.Conversation;
import com.c2c.im.entity.Message;
import com.c2c.im.service.ImService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 即时通讯（站内信）：会话列表 / 发起会话 / 历史消息 / 发送消息 / 已读 / 未读数。
 * 所有接口均需登录，接收者由会话推导，不额外校验。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "即时通讯", description = "买卖双方站内私信：会话 / 消息 / 已读 / 未读数（均需登录）")
public class ImController {

    private final ImService imService;

    @Operation(summary = "会话列表")
    @GetMapping(ApiPath.IM_CONVERSATION_LIST)
    public R<List<Conversation>> getConversations(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(imService.getConversationList(userId));
    }

    @Operation(summary = "获取或创建会话", description = "body 传 targetUserId 与 productId，不存在则创建")
    @PostMapping(ApiPath.IM_CONVERSATION)
    public R<Conversation> getOrCreate(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                       @RequestBody Map<String, Long> body) {
        return R.ok(imService.getOrCreateConversation(userId, body.get("targetUserId"), body.get("productId")));
    }

    @Operation(summary = "历史消息（分页）", description = "支持 before / after 游标定位，仅会话双方可查看")
    @GetMapping(ApiPath.IM_MESSAGE_ID)
    public R<Page<Message>> getMessages(@Parameter(description = "会话 ID") @PathVariable Long conversationId,
                                        @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                        @Parameter(description = "只取此消息 ID 之前（更早）的消息") @RequestParam(required = false) Long before,
                                        @Parameter(description = "只取此消息 ID 之后（更晚）的消息") @RequestParam(required = false) Long after,
                                        @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int size) {
        return R.ok(imService.getMessages(conversationId, userId, before, after, size));
    }

    /**
     * 发送消息：接收者由会话推导，body 只传 conversationId 与 content。
     * 需登录（AuthTokenFilter 注入 X-User-Id）。
     */
    @Operation(summary = "发送消息", description = "body 只传 conversationId 与 content，接收者由会话推导")
    @PostMapping(ApiPath.IM_MESSAGE)
    public R<Message> sendMessage(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                  @RequestBody Map<String, Object> body) {
        Long conversationId = Long.valueOf(String.valueOf(body.get("conversationId")));
        String content = body.get("content") == null ? null : String.valueOf(body.get("content"));
        return R.ok(imService.sendMessageByUser(conversationId, userId, content));
    }

    @Operation(summary = "标记会话已读")
    @PutMapping(ApiPath.IM_MESSAGE_READ)
    public R<Void> markRead(@Parameter(description = "会话 ID") @PathVariable Long conversationId,
                            @Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        imService.markRead(conversationId, userId);
        return R.ok();
    }

    @Operation(summary = "未读消息总数")
    @GetMapping(ApiPath.IM_UNREAD_COUNT)
    public R<Map<String, Integer>> unreadCount(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(com.c2c.common.utils.MapUtils.of("unreadTotal", imService.getUnreadCount(userId)));
    }

    @Operation(summary = "最新未读消息（右上角提醒）", description = "返回最新一条未读消息的发送人昵称、内容预览、会话 ID 等，无未读时仅返回 unreadTotal=0")
    @GetMapping(ApiPath.IM_UNREAD_LATEST)
    public R<Map<String, Object>> unreadLatest(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId) {
        return R.ok(imService.getLatestUnread(userId));
    }
}
