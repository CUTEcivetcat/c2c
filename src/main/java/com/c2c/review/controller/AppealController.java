package com.c2c.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.review.dto.AppealCreateDTO;
import com.c2c.review.service.ReviewService;
import com.c2c.review.vo.AppealVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 商品整改申诉（卖家端）：违规下架商品由卖家提交整改说明申请重新上架，
 * 并查看自己的申诉记录。申诉数据进入审核工作台处理（见 ReviewController）。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "商品整改申诉", description = "卖家对违规下架商品整改申诉、查看我的申诉（需登录）")
public class AppealController {

    private final ReviewService reviewService;

    @Operation(summary = "提交整改申诉", description = "body：productId 必填（须自己违规下架的商品）、appealReason 必填、images 逗号分隔。同商品最多 3 次")
    @PostMapping(ApiPath.APPEAL_CREATE)
    public R<Long> create(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long sellerId,
                          @RequestBody AppealCreateDTO dto) {
        return R.ok(reviewService.createAppeal(sellerId, dto));
    }

    @Operation(summary = "我的整改申诉列表", description = "卖家视角，按时间倒序分页")
    @GetMapping(ApiPath.APPEAL_MY)
    public R<Page<AppealVO>> my(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                @Parameter(description = "页码，从 1 开始") @RequestParam(defaultValue = "1") int page,
                                @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size) {
        return R.ok(reviewService.myAppeals(userId, page, size));
    }
}
