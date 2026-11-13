package com.c2c.suggestion.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.suggestion.dto.SuggestionReplyDTO;
import com.c2c.suggestion.service.SuggestionService;
import com.c2c.suggestion.vo.SuggestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 建议反馈管理端：查看用户建议并回复是否采纳。需管理员 review 权限。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "建议反馈管理", description = "查看用户建议并回复采纳/未采纳")
public class AdminSuggestionController {

    private final SuggestionService suggestionService;

    @Operation(summary = "建议反馈列表", description = "可按状态筛选：1待处理 2已采纳 3未采纳；待处理排前")
    @GetMapping(ApiPath.ADMIN_SUGGESTION_LIST)
    public R<Page<SuggestionVO>> list(@Parameter(description = "状态筛选，不传查全部") @RequestParam(required = false) Integer status,
                                      @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                      @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                                      @Parameter(description = "内容关键字") @RequestParam(required = false) String keyword) {
        return R.ok(suggestionService.adminList(status, page, size, keyword));
    }

    @Operation(summary = "回复建议并标记是否采纳", description = "body：{adopt: true采纳/false未采纳, reply: 回复意见}")
    @PostMapping(ApiPath.ADMIN_SUGGESTION_REPLY)
    public R<Void> reply(@Parameter(description = "建议 ID") @PathVariable Long id,
                         @Parameter(hidden = true) @RequestHeader("X-User-Id") Long adminId,
                         @RequestBody SuggestionReplyDTO dto) {
        suggestionService.reply(id, adminId, dto);
        return R.ok();
    }
}
