package com.c2c.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.ai.entity.Faq;
import com.c2c.ai.service.FaqAdminService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * FAQ 知识库管理（管理员）：列表 / 新增 / 编辑 / 启停 / 删除。需管理员 ai 权限。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "FAQ 知识库管理", description = "维护 AI 客服规则问答库（需管理员权限）")
public class AdminFaqController {

    private final FaqAdminService faqAdminService;

    @Operation(summary = "FAQ 管理列表（含停用）")
    @GetMapping(ApiPath.AI_FAQ_ADMIN_LIST)
    public R<Page<Faq>> list(@Parameter(description = "页码") @RequestParam(defaultValue = "1") int page,
                             @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
                             @Parameter(description = "标准问关键字") @RequestParam(required = false) String keyword) {
        return R.ok(faqAdminService.listAll(page, size, keyword));
    }

    @Operation(summary = "新增 FAQ")
    @PostMapping(ApiPath.AI_FAQ_ADMIN)
    public R<Faq> create(@RequestBody Faq faq) {
        return R.ok(faqAdminService.create(faq));
    }

    @Operation(summary = "编辑 FAQ")
    @PutMapping(ApiPath.AI_FAQ_ADMIN_ID)
    public R<Faq> update(@Parameter(description = "FAQ ID") @PathVariable Long id,
                         @RequestBody Faq faq) {
        return R.ok(faqAdminService.update(id, faq));
    }

    @Operation(summary = "启用/停用 FAQ", description = "body：{status: 1启用 0停用}")
    @PutMapping(ApiPath.AI_FAQ_ADMIN_STATUS)
    public R<Void> changeStatus(@Parameter(description = "FAQ ID") @PathVariable Long id,
                                @RequestBody Map<String, Integer> body) {
        faqAdminService.changeStatus(id, body.get("status"));
        return R.ok();
    }

    @Operation(summary = "删除 FAQ")
    @DeleteMapping(ApiPath.AI_FAQ_ADMIN_ID)
    public R<Void> delete(@Parameter(description = "FAQ ID") @PathVariable Long id) {
        faqAdminService.delete(id);
        return R.ok();
    }
}
