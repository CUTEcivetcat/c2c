package com.c2c.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.ai.dto.AiChatLogVO;
import com.c2c.ai.service.AiChatAdminService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 客服对话看板（管理员）：问答抽样、来源分布统计、日志导出数据。需管理员 ai 权限。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "AI 对话看板", description = "助手问答抽样、来源分布与日志导出（需管理员权限）")
public class AdminAiChatController {

    private final AiChatAdminService aiChatAdminService;

    @Operation(summary = "问答对抽样列表")
    @GetMapping(ApiPath.AI_CHAT_ADMIN_LIST)
    public R<Page<AiChatLogVO>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                      @Parameter(description = "回复关键字") @RequestParam(required = false) String keyword,
                                      @Parameter(description = "来源：model / rule") @RequestParam(required = false) String source) {
        return R.ok(aiChatAdminService.listLogs(page, size, keyword, source));
    }

    @Operation(summary = "来源分布统计", description = "total / model / rule 计数")
    @GetMapping(ApiPath.AI_CHAT_ADMIN_STATS)
    public R<Map<String, Object>> stats() {
        return R.ok(aiChatAdminService.stats());
    }

    @Operation(summary = "对话日志导出数据", description = "全量 AI 回复记录（上限 1000 条），供前端导出 CSV")
    @GetMapping(ApiPath.AI_CHAT_ADMIN_EXPORT)
    public R<List<AiChatLogVO>> export(@Parameter(description = "来源：model / rule") @RequestParam(required = false) String source) {
        return R.ok(aiChatAdminService.exportLogs(source));
    }
}
