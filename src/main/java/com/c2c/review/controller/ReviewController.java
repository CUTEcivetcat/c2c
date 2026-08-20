package com.c2c.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.review.dto.ReviewHandleDTO;
import com.c2c.review.service.ReviewService;
import com.c2c.review.vo.AppealVO;
import com.c2c.review.vo.ReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 审核工作台（审核员 role=2 或管理员）：处理商品举报与整改申诉。
 * <p>鉴权由 AuthTokenFilter 的 /review/** 规则完成——仅携带 role=2 或 ADMIN 标记的
 * token 可访问，并注入 X-User-Id / X-User-Role 供审计记录。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "审核工作台", description = "审核员(role=2)与管理员审核商品举报与整改申诉")
public class ReviewController {

    private final ReviewService reviewService;

    @Operation(summary = "举报列表", description = "可按状态筛选：1待处理 2已违规下架 3已驳回；待处理排前")
    @GetMapping(ApiPath.REVIEW_REPORTS)
    public R<Page<ReportVO>> reports(@Parameter(description = "状态筛选，不传查全部") @RequestParam(required = false) Integer status,
                                     @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listReports(status, page, size));
    }

    @Operation(summary = "举报详情", description = "含被举报商品完整信息与举报人/卖家信息")
    @GetMapping(ApiPath.REVIEW_REPORT_DETAIL)
    public R<ReportVO> reportDetail(@Parameter(description = "举报 ID") @PathVariable Long id) {
        return R.ok(reviewService.getReportDetail(id));
    }

    @Operation(summary = "处理举报", description = "action：ban 违规下架（reason 为下架原因必填）/ reject 驳回（reason 为驳回说明必填）")
    @PostMapping(ApiPath.REVIEW_REPORT_HANDLE)
    public R<Void> handleReport(@Parameter(description = "举报 ID") @PathVariable Long id,
                                @Parameter(hidden = true) @RequestHeader("X-User-Id") Long handlerId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole,
                                @RequestBody ReviewHandleDTO dto) {
        reviewService.handleReport(id, handlerId, resolveRole(userRole), dto);
        return R.ok();
    }

    @Operation(summary = "整改申诉列表", description = "可按状态筛选：1待审核 2已通过 3已驳回；待审核排前")
    @GetMapping(ApiPath.REVIEW_APPEALS)
    public R<Page<AppealVO>> appeals(@Parameter(description = "状态筛选，不传查全部") @RequestParam(required = false) Integer status,
                                     @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                     @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.listAppeals(status, page, size));
    }

    @Operation(summary = "整改申诉详情", description = "含商品完整信息与卖家信息")
    @GetMapping(ApiPath.REVIEW_APPEAL_DETAIL)
    public R<AppealVO> appealDetail(@Parameter(description = "申诉 ID") @PathVariable Long id) {
        return R.ok(reviewService.getAppealDetail(id));
    }

    @Operation(summary = "处理整改申诉", description = "action：approve 通过恢复上架 / reject 驳回（reason 为审核回复）")
    @PostMapping(ApiPath.REVIEW_APPEAL_HANDLE)
    public R<Void> handleAppeal(@Parameter(description = "申诉 ID") @PathVariable Long id,
                                @Parameter(hidden = true) @RequestHeader("X-User-Id") Long handlerId,
                                @Parameter(hidden = true) @RequestHeader(value = "X-User-Role", required = false) String userRole,
                                @RequestBody ReviewHandleDTO dto) {
        reviewService.handleAppeal(id, handlerId, resolveRole(userRole), dto);
        return R.ok();
    }

    /** 解析操作人角色：2=审核员，其余（ADMIN 或缺省）按管理员记日志 */
    private Integer resolveRole(String userRole) {
        return "2".equals(userRole) ? 2 : 1;
    }
}
