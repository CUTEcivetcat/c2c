package com.c2c.review.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.common.enums.ProductStatus;
import com.c2c.common.exception.BusinessException;
import com.c2c.im.entity.Conversation;
import com.c2c.im.service.ImService;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductImage;
import com.c2c.product.mapper.ProductImageMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.ProductService;
import com.c2c.review.dto.AppealCreateDTO;
import com.c2c.review.dto.ReportCreateDTO;
import com.c2c.review.dto.ReviewHandleDTO;
import com.c2c.review.entity.AdminLog;
import com.c2c.review.entity.ProductAppeal;
import com.c2c.review.entity.Report;
import com.c2c.review.enums.AppealStatus;
import com.c2c.review.enums.ReportStatus;
import com.c2c.review.enums.ReportType;
import com.c2c.review.mapper.AdminLogMapper;
import com.c2c.review.mapper.ProductAppealMapper;
import com.c2c.review.mapper.ReportMapper;
import com.c2c.review.service.ReviewService;
import com.c2c.review.vo.AppealVO;
import com.c2c.review.vo.ReportVO;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审核服务实现类
 * <p>实现举报提交/查询、申诉提交/查询、举报审核处理、申诉审核处理及审计日志记录。
 * 举报与申诉列表批量关联商品、用户与封面图组装视图；处理结果通过站内信通知相关方。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReportMapper reportMapper;
    private final ProductAppealMapper appealMapper;
    private final AdminLogMapper adminLogMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final UserMapper userMapper;
    private final ProductService productService;
    private final ImService imService;

    // ==================== 举报 ====================

    @Override
    @Transactional
    public Long createReport(Long reporterId, ReportCreateDTO dto) {
        if (dto.getProductId() == null) {
            throw new BusinessException("请选择要举报的商品");
        }
        if (StrUtil.isBlank(dto.getReason())) {
            throw new BusinessException("请填写举报理由");
        }
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (product.getSellerId().equals(reporterId)) {
            throw new BusinessException("不能举报自己发布的商品");
        }
        // 防刷：同一用户对同一商品只保留一条待处理举报
        Long pending = reportMapper.selectCount(new LambdaQueryWrapper<Report>()
                .eq(Report::getProductId, dto.getProductId())
                .eq(Report::getReporterId, reporterId)
                .eq(Report::getStatus, ReportStatus.PENDING.getCode()));
        if (pending > 0) {
            throw new BusinessException("您已举报该商品，请等待处理结果");
        }

        Report report = new Report();
        report.setReporterId(reporterId);
        report.setProductId(dto.getProductId());
        report.setReportType(dto.getReportType() == null ? ReportType.OTHER.getCode() : dto.getReportType());
        report.setReason(dto.getReason());
        report.setImages(dto.getImages());
        report.setStatus(ReportStatus.PENDING.getCode());
        reportMapper.insert(report);
        log.info("商品举报提交：reportId={}, productId={}, reporterId={}", report.getId(), dto.getProductId(), reporterId);
        return report.getId();
    }

    @Override
    public Page<ReportVO> myReports(Long userId, int page, int size) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>()
                .eq(Report::getReporterId, userId)
                .orderByDesc(Report::getCreatedAt);
        return toReportVoPage(reportMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public Page<ReportVO> listReports(Integer status, int page, int size) {
        LambdaQueryWrapper<Report> wrapper = new LambdaQueryWrapper<Report>();
        if (status != null) {
            wrapper.eq(Report::getStatus, status);
        }
        wrapper.orderByAsc(Report::getStatus).orderByDesc(Report::getCreatedAt);
        return toReportVoPage(reportMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public ReportVO getReportDetail(Long id) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        return buildReportVOs(Collections.singletonList(report)).get(0);
    }

    @Override
    @Transactional
    public void handleReport(Long id, Long handlerId, Integer handlerRole, ReviewHandleDTO dto) {
        Report report = reportMapper.selectById(id);
        if (report == null) {
            throw new BusinessException("举报不存在");
        }
        if (report.getStatus() != ReportStatus.PENDING.getCode()) {
            throw new BusinessException("该举报已处理");
        }
        if (StrUtil.isBlank(dto.getAction())) {
            throw new BusinessException("请选择处理动作");
        }
        Product product = productMapper.selectById(report.getProductId());
        if (product == null) {
            throw new BusinessException("被举报商品不存在");
        }

        String action = dto.getAction().trim().toLowerCase();
        String reason = StrUtil.blankToDefault(dto.getReason(), "");

        if ("ban".equals(action)) {
            if (StrUtil.isBlank(reason)) {
                throw new BusinessException("违规下架必须填写原因");
            }
            productService.ban(product.getId(), reason);
            report.setStatus(ReportStatus.BANNED.getCode());
            report.setHandleRemark(reason);
            writeLog(handlerId, handlerRole, "ban", "product", product.getId(),
                    "举报违规下架商品「" + product.getTitle() + "」，原因：" + reason);
            notifyUser(handlerId, product.getSellerId(), product.getId(),
                    "您的商品「" + product.getTitle() + "」经审核被判定违规并下架，原因：" + reason
                            + "。您可整改后提交申诉申请重新上架。");
        } else if ("reject".equals(action)) {
            if (StrUtil.isBlank(reason)) {
                throw new BusinessException("驳回必须填写说明");
            }
            report.setStatus(ReportStatus.REJECTED.getCode());
            report.setHandleRemark(reason);
            writeLog(handlerId, handlerRole, "report_handle", "report", report.getId(),
                    "举报驳回，商品「" + product.getTitle() + "」，说明：" + reason);
            notifyUser(handlerId, report.getReporterId(), product.getId(),
                    "您举报的商品「" + product.getTitle() + "」经审核予以驳回，说明：" + reason);
        } else {
            throw new BusinessException("无效的处理动作：" + dto.getAction());
        }
        report.setHandledBy(handlerId);
        report.setHandledAt(LocalDateTime.now());
        reportMapper.updateById(report);
        log.info("举报处理完成：reportId={}, action={}, handlerId={}", id, action, handlerId);
    }

    // ==================== 整改申诉 ====================

    @Override
    @Transactional
    public Long createAppeal(Long sellerId, AppealCreateDTO dto) {
        if (dto.getProductId() == null) {
            throw new BusinessException("请选择要申诉的商品");
        }
        if (StrUtil.isBlank(dto.getAppealReason())) {
            throw new BusinessException("请填写整改说明");
        }
        Product product = productMapper.selectById(dto.getProductId());
        if (product == null) {
            throw new BusinessException("商品不存在");
        }
        if (!product.getSellerId().equals(sellerId)) {
            throw new BusinessException("只能申诉自己发布的商品");
        }
        if (product.getStatus() == null || product.getStatus() != ProductStatus.BANNED.getCode()) {
            throw new BusinessException("仅违规下架的商品可提交整改申诉");
        }
        Long pending = appealMapper.selectCount(new LambdaQueryWrapper<ProductAppeal>()
                .eq(ProductAppeal::getProductId, dto.getProductId())
                .eq(ProductAppeal::getStatus, AppealStatus.PENDING.getCode()));
        if (pending > 0) {
            throw new BusinessException("已有待审核的申诉，请等待处理结果");
        }
        List<ProductAppeal> history = appealMapper.selectList(new LambdaQueryWrapper<ProductAppeal>()
                .eq(ProductAppeal::getProductId, dto.getProductId())
                .orderByDesc(ProductAppeal::getAppealCount));
        int times = history.isEmpty() ? 0 : (history.get(0).getAppealCount() == null ? 0 : history.get(0).getAppealCount());
        if (times >= 3) {
            throw new BusinessException("该商品申诉次数已达上限（3 次），无法再次申诉");
        }

        ProductAppeal appeal = new ProductAppeal();
        appeal.setProductId(dto.getProductId());
        appeal.setSellerId(sellerId);
        appeal.setAppealReason(dto.getAppealReason());
        appeal.setImages(dto.getImages());
        appeal.setStatus(AppealStatus.PENDING.getCode());
        appeal.setAppealCount(times + 1);
        appealMapper.insert(appeal);
        log.info("整改申诉提交：appealId={}, productId={}, sellerId={}, 第{}次", appeal.getId(), dto.getProductId(), sellerId, times + 1);
        return appeal.getId();
    }

    @Override
    public Page<AppealVO> myAppeals(Long userId, int page, int size) {
        LambdaQueryWrapper<ProductAppeal> wrapper = new LambdaQueryWrapper<ProductAppeal>()
                .eq(ProductAppeal::getSellerId, userId)
                .orderByDesc(ProductAppeal::getCreatedAt);
        return toAppealVoPage(appealMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public Page<AppealVO> listAppeals(Integer status, int page, int size) {
        LambdaQueryWrapper<ProductAppeal> wrapper = new LambdaQueryWrapper<ProductAppeal>();
        if (status != null) {
            wrapper.eq(ProductAppeal::getStatus, status);
        }
        wrapper.orderByAsc(ProductAppeal::getStatus).orderByDesc(ProductAppeal::getCreatedAt);
        return toAppealVoPage(appealMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @Override
    public AppealVO getAppealDetail(Long id) {
        ProductAppeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        return buildAppealVOs(Collections.singletonList(appeal)).get(0);
    }

    @Override
    @Transactional
    public void handleAppeal(Long id, Long handlerId, Integer handlerRole, ReviewHandleDTO dto) {
        ProductAppeal appeal = appealMapper.selectById(id);
        if (appeal == null) {
            throw new BusinessException("申诉不存在");
        }
        if (appeal.getStatus() != AppealStatus.PENDING.getCode()) {
            throw new BusinessException("该申诉已处理");
        }
        if (StrUtil.isBlank(dto.getAction())) {
            throw new BusinessException("请选择处理动作");
        }
        Product product = productMapper.selectById(appeal.getProductId());
        if (product == null) {
            throw new BusinessException("申诉商品不存在");
        }

        String action = dto.getAction().trim().toLowerCase();
        String reply = StrUtil.blankToDefault(dto.getReason(), "");

        if ("approve".equals(action)) {
            productService.restore(product.getId());
            appeal.setStatus(AppealStatus.APPROVED.getCode());
            appeal.setReply(reply);
            writeLog(handlerId, handlerRole, "restore", "product", product.getId(),
                    "整改申诉通过，商品「" + product.getTitle() + "」恢复上架，" + (StrUtil.isBlank(reply) ? "" : "回复：" + reply));
            notifyUser(handlerId, appeal.getSellerId(), product.getId(),
                    "您的商品「" + product.getTitle() + "」整改审核通过，已重新上架。"
                            + (StrUtil.isBlank(reply) ? "" : "审核回复：" + reply));
        } else if ("reject".equals(action)) {
            if (StrUtil.isBlank(reply)) {
                throw new BusinessException("驳回必须填写说明");
            }
            appeal.setStatus(AppealStatus.REJECTED.getCode());
            appeal.setReply(reply);
            writeLog(handlerId, handlerRole, "appeal_handle", "appeal", appeal.getId(),
                    "整改申诉驳回，商品「" + product.getTitle() + "」，说明：" + reply);
            notifyUser(handlerId, appeal.getSellerId(), product.getId(),
                    "您的商品「" + product.getTitle() + "」整改申诉被驳回：" + reply + "。您仍可继续整改后再次申诉（限 3 次）。");
        } else {
            throw new BusinessException("无效的处理动作：" + dto.getAction());
        }
        appeal.setHandledBy(handlerId);
        appeal.setHandledAt(LocalDateTime.now());
        appealMapper.updateById(appeal);
        log.info("整改申诉处理完成：appealId={}, action={}, handlerId={}", id, action, handlerId);
    }

    // ==================== 私有辅助 ====================

    /** 写审核/管理操作日志 */
    private void writeLog(Long operatorId, Integer operatorRole, String action,
                          String targetType, Long targetId, String detail) {
        AdminLog log = new AdminLog();
        log.setOperatorId(operatorId);
        log.setOperatorRole(operatorRole);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        adminLogMapper.insert(log);
    }

    /** 以操作人身份给目标用户发送站内信系统通知（messageType=4） */
    private void notifyUser(Long fromUserId, Long toUserId, Long productId, String content) {
        try {
            Conversation conv = imService.getOrCreateConversation(fromUserId, toUserId, productId);
            imService.sendMessage(conv.getId(), fromUserId, toUserId, content, 4, null);
        } catch (Exception e) {
            log.warn("审核通知发送失败：from={}, to={}, productId={}, err={}", fromUserId, toUserId, productId, e.getMessage());
        }
    }

    private Page<ReportVO> toReportVoPage(Page<Report> page) {
        Page<ReportVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(buildReportVOs(page.getRecords()));
        return result;
    }

    /** 批量组装举报 VO（一次关联商品 / 用户 / 封面图） */
    private List<ReportVO> buildReportVOs(List<Report> reports) {
        if (reports == null || reports.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = reports.stream().map(Report::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Set<Long> userIds = reports.stream().map(Report::getReporterId).collect(Collectors.toSet());
        productMap.values().forEach(p -> userIds.add(p.getSellerId()));
        Map<Long, String> nickMap = selectNicknames(userIds);
        Map<Long, String> coverMap = selectCovers(productIds);

        return reports.stream().map(r -> {
            Product p = productMap.get(r.getProductId());
            ReportVO vo = new ReportVO();
            vo.setId(r.getId());
            vo.setReporterId(r.getReporterId());
            vo.setReporterNickname(nickMap.getOrDefault(r.getReporterId(), "未知用户"));
            vo.setProductId(r.getProductId());
            if (p != null) {
                vo.setProductTitle(p.getTitle());
                vo.setProductCover(coverMap.get(p.getId()));
                vo.setProductPrice(p.getPrice());
                vo.setProductStatus(p.getStatus());
                vo.setProductReviewReason(p.getReviewReason());
                vo.setSellerId(p.getSellerId());
                vo.setSellerNickname(nickMap.getOrDefault(p.getSellerId(), "未知用户"));
            }
            vo.setReportType(r.getReportType());
            vo.setReportTypeText(ReportType.getTextByCode(r.getReportType()));
            vo.setReason(r.getReason());
            vo.setImages(r.getImages());
            vo.setStatus(r.getStatus());
            vo.setStatusText(ReportStatus.getTextByCode(r.getStatus()));
            vo.setHandleRemark(r.getHandleRemark());
            vo.setHandledBy(r.getHandledBy());
            vo.setHandledAt(r.getHandledAt());
            vo.setCreatedAt(r.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    private Page<AppealVO> toAppealVoPage(Page<ProductAppeal> page) {
        Page<AppealVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(buildAppealVOs(page.getRecords()));
        return result;
    }

    /** 批量组装申诉 VO（一次关联商品 / 用户 / 封面图） */
    private List<AppealVO> buildAppealVOs(List<ProductAppeal> appeals) {
        if (appeals == null || appeals.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> productIds = appeals.stream().map(ProductAppeal::getProductId).collect(Collectors.toSet());
        Map<Long, Product> productMap = productMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Set<Long> userIds = appeals.stream().map(ProductAppeal::getSellerId).collect(Collectors.toSet());
        Map<Long, String> nickMap = selectNicknames(userIds);
        Map<Long, String> coverMap = selectCovers(productIds);

        return appeals.stream().map(a -> {
            Product p = productMap.get(a.getProductId());
            AppealVO vo = new AppealVO();
            vo.setId(a.getId());
            vo.setProductId(a.getProductId());
            if (p != null) {
                vo.setProductTitle(p.getTitle());
                vo.setProductCover(coverMap.get(p.getId()));
                vo.setProductPrice(p.getPrice());
                vo.setProductStatus(p.getStatus());
                vo.setProductReviewReason(p.getReviewReason());
                vo.setSellerId(p.getSellerId());
                vo.setSellerNickname(nickMap.getOrDefault(p.getSellerId(), "未知用户"));
            }
            vo.setAppealReason(a.getAppealReason());
            vo.setImages(a.getImages());
            vo.setStatus(a.getStatus());
            vo.setStatusText(AppealStatus.getTextByCode(a.getStatus()));
            vo.setAppealCount(a.getAppealCount());
            vo.setReply(a.getReply());
            vo.setHandledBy(a.getHandledBy());
            vo.setHandledAt(a.getHandledAt());
            vo.setCreatedAt(a.getCreatedAt());
            return vo;
        }).collect(Collectors.toList());
    }

    private Map<Long, String> selectNicknames(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return map;
        }
        userMapper.selectBatchIds(userIds).forEach(u -> map.put(u.getId(), u.getNickname()));
        return map;
    }

    private Map<Long, String> selectCovers(Set<Long> productIds) {
        Map<Long, String> map = new HashMap<>();
        if (productIds == null || productIds.isEmpty()) {
            return map;
        }
        productImageMapper.selectList(new LambdaQueryWrapper<ProductImage>()
                        .in(ProductImage::getProductId, productIds)
                        .orderByAsc(ProductImage::getSortOrder))
                .forEach(img -> map.putIfAbsent(img.getProductId(), img.getUrl()));
        return map;
    }
}
