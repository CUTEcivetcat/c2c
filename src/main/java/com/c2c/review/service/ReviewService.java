package com.c2c.review.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.review.dto.AppealCreateDTO;
import com.c2c.review.dto.ReportCreateDTO;
import com.c2c.review.dto.ReviewHandleDTO;
import com.c2c.review.vo.AppealVO;
import com.c2c.review.vo.NicknameAuditVO;
import com.c2c.review.vo.ReportVO;

/**
 * 审核服务接口：商品举报（用户端）+ 整改申诉（卖家端）+ 审核工作台（审核员/管理员）。
 * <p>举报经审核可违规下架或驳回；违规下架商品由卖家整改申诉，通过后恢复上架。
 * 每次处理写 admin_log 审计，并通过站内信（IM 系统消息）通知相关方。</p>
 */
public interface ReviewService {

    /** 提交商品举报（举报人 = 当前登录用户） */
    Long createReport(Long reporterId, ReportCreateDTO dto);

    /** 我的举报列表（举报人视角） */
    Page<ReportVO> myReports(Long userId, int page, int size);

    /** 举报列表（审核工作台，可按状态筛选） */
    Page<ReportVO> listReports(Integer status, int page, int size);

    /** 举报详情（含被举报商品完整信息） */
    ReportVO getReportDetail(Long id);

    /** 处理举报（ban 违规下架 / reject 驳回），写日志并通知卖家或举报人 */
    void handleReport(Long id, Long handlerId, Integer handlerRole, ReviewHandleDTO dto);

    /** 提交整改申诉（卖家对自己的违规下架商品提交，最多 3 次） */
    Long createAppeal(Long sellerId, AppealCreateDTO dto);

    /** 我的整改申诉列表（卖家视角） */
    Page<AppealVO> myAppeals(Long userId, int page, int size);

    /** 整改申诉列表（审核工作台，可按状态筛选） */
    Page<AppealVO> listAppeals(Integer status, int page, int size);

    /** 整改申诉详情（含商品完整信息） */
    AppealVO getAppealDetail(Long id);

    /** 处理整改申诉（approve 恢复上架 / reject 驳回），写日志并通知卖家 */
    void handleAppeal(Long id, Long handlerId, Integer handlerRole, ReviewHandleDTO dto);

    /** 昵称修改审核列表（可按状态筛选：0待审 1通过 2拒绝） */
    Page<NicknameAuditVO> listNicknameAudits(Integer status, int page, int size);

    /** 处理昵称审核（approve 通过生效 / reject 拒绝保留旧昵称），写日志并通知用户 */
    void handleNicknameAudit(Long id, Long handlerId, Integer handlerRole, boolean approve, String reason);
}
