package com.c2c.review.dto;

import lombok.Data;

/**
 * 审核处理请求（举报处理 / 申诉处理通用）
 * <p>action：
 * <ul>
 *   <li>举报：ban 违规下架（reason 必填，作为下架原因）/ reject 驳回（reason 为驳回说明）</li>
 *   <li>申诉：approve 通过恢复上架 / reject 驳回（reason 为审核回复）</li>
 * </ul>
 */
@Data
public class ReviewHandleDTO {

    /** 处理动作：ban / reject / approve */
    private String action;

    /** 处理理由（下架原因 / 驳回说明 / 审核回复，按动作取义） */
    private String reason;
}
