package com.c2c.ai.controller;

import com.c2c.ai.dto.AiPublishRequest;
import com.c2c.ai.dto.AiPublishVO;
import com.c2c.ai.service.AiService;
import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 智能发布（需登录）。
 *
 * <p>多模态识别商品图片 → 一键回填标题 / 描述 / 分类 / 成色 + 建议价区间；
 * 未配置大模型 Key 时自动降级本地规则引擎，功能始终可用。</p>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "AI 智能发布", description = "拍照即发布：多模态识别 + 自动填写 + 估价（需登录）")
public class AiController {

    private final AiService aiService;

    @Operation(summary = "AI 智能填写", description = "body：imageUrl（首图必填）、categoryId（已选分类可选）；source=model/rule")
    @PostMapping(ApiPath.AI_INTELLIGENT_PUBLISH)
    public R<AiPublishVO> intelligentPublish(@Parameter(hidden = true) @RequestHeader("X-User-Id") Long userId,
                                             @RequestBody AiPublishRequest request) {
        return R.ok(aiService.intelligentPublish(request));
    }
}
