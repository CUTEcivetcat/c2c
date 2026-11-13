package com.c2c.suggestion.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.suggestion.dto.SuggestionCreateDTO;
import com.c2c.suggestion.service.SuggestionService;
import com.c2c.suggestion.vo.SuggestionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 意见建议反馈（用户端）：登录用户提交建议、查看自己的建议与回复。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "意见建议反馈", description = "用户提交建议、查看我的建议与管理回复（需登录）")
public class SuggestionController {

    private final SuggestionService suggestionService;

    @Operation(summary = "提交建议", description = "body：content 必填，contact 联系方式可空")
    @PostMapping(ApiPath.SUGGESTION)
    public R<Long> create(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                          @Valid @RequestBody SuggestionCreateDTO dto) {
        return R.ok(suggestionService.create(userId, dto));
    }

    @Operation(summary = "我的建议列表", description = "按时间倒序分页，含管理回复与采纳状态")
    @GetMapping(ApiPath.SUGGESTION_MY)
    public R<Page<SuggestionVO>> my(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                    @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                    @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return R.ok(suggestionService.mySuggestions(userId, page, size));
    }
}
