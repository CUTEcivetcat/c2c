package com.c2c.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.review.dto.ReportCreateDTO;
import com.c2c.review.service.ReviewService;
import com.c2c.review.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品举报（用户端）：登录用户对违规商品发起举报，并查看自己的举报记录。
 * 举报数据进入审核工作台由审核员/管理员处理（见 ReviewController）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品举报", description = "用户举报商品、查看我的举报（需登录）")
public class ReportController {

    private final ReviewService reviewService;

    @Operation(summary = "提交举报", description = "body：productId 必填、reason 必填、reportType 1-6、images 逗号分隔")
    @PostMapping(ApiPath.REPORT_CREATE)
    public R<Long> create(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                          @RequestBody ReportCreateDTO dto) {
        return R.ok(reviewService.createReport(userId, dto));
    }

    @Operation(summary = "我的举报列表", description = "举报人视角，按时间倒序分页")
    @GetMapping(ApiPath.REPORT_MY)
    public R<Page<ReportVO>> my(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.myReports(userId, page, size));
    }
}
