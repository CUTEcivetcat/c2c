package com.c2c.popup.controller;

import com.c2c.common.constant.ApiPath;
import com.c2c.common.result.R;
import com.c2c.popup.entity.ActivityPopup;
import com.c2c.popup.service.ActivityPopupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 活动弹窗用户端：登录后拉取生效中弹窗。
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "活动弹窗", description = "生效中活动弹窗展示")
public class PopupController {

    private final ActivityPopupService popupService;

    @Operation(summary = "生效中活动弹窗列表")
    @GetMapping(ApiPath.POPUP_ACTIVE)
    public R<List<ActivityPopup>> active(@Parameter(hidden = true) @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        return R.ok(popupService.active(userId));
    }
}
