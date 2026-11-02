package com.c2c.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.announcement.entity.Announcement;
import com.c2c.announcement.mapper.AnnouncementMapper;
import com.c2c.common.enums.OrderStatus;
import com.c2c.order.entity.Order;
import com.c2c.order.mapper.OrderMapper;
import com.c2c.product.entity.Product;
import com.c2c.product.entity.ProductImage;
import com.c2c.product.mapper.ProductImageMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.user.entity.User;
import com.c2c.user.mapper.UserMapper;
import com.c2c.wallet.entity.WalletLog;
import com.c2c.wallet.mapper.WalletLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 客服工具执行器：大模型 Function Calling 的本地工具集。
 *
 * <p>安全约定（论文"权限收敛"素材）：所有工具只允许操作【当前登录用户】维度的数据，
 * userId 由调用方（AiChatService）从会话上下文传入，模型只传业务参数。</p>
 *
 * <p>v1 为只读工具 + 转人工引导；写操作（代留言/生成举报草稿）二期开放。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatToolExecutor {

    private final OrderMapper orderMapper;
    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final AnnouncementMapper announcementMapper;
    private final UserMapper userMapper;
    private final WalletLogMapper walletLogMapper;

    /** 订单号：14位时间戳+8位随机 */
    private static final Pattern ORDER_NO_PATTERN = Pattern.compile("(\\d{22})");

    public static final String TOOL_ORDER = "query_order_status";
    public static final String TOOL_PRODUCT = "query_product";
    public static final String TOOL_ANNOUNCEMENT = "list_announcements";
    public static final String TOOL_WALLET = "query_wallet";
    public static final String TOOL_HANDOFF = "handoff_to_human";

    /** OpenAI tools 协议描述（注入请求体） */
    public String toolsJson() {
        return "["
                + tool(TOOL_ORDER, "查询当前登录用户的订单状态。orderNo 可选：提供则查指定订单，缺省查最近一笔。",
                        "{\"type\":\"object\",\"properties\":{\"orderNo\":{\"type\":\"string\",\"description\":\"22位订单号，可空\"}}}") + ","
                + tool(TOOL_PRODUCT, "查询商品详情与在售状态。参数 productId 为商品ID。",
                        "{\"type\":\"object\",\"properties\":{\"productId\":{\"type\":\"integer\"}},\"required\":[\"productId\"]}") + ","
                + tool(TOOL_ANNOUNCEMENT, "列出平台最新公告（最多5条）。无参数。", "{\"type\":\"object\",\"properties\":{}}") + ","
                + tool(TOOL_WALLET, "查询当前登录用户的余额与最近流水（最多5条）。无参数。", "{\"type\":\"object\",\"properties\":{}}") + ","
                + tool(TOOL_HANDOFF, "用户要求人工/投诉/纠纷时调用，返回人工处理指引。无参数。", "{\"type\":\"object\",\"properties\":{}}")
                + "]";
    }

    private String tool(String name, String desc, String params) {
        return "{\"type\":\"function\",\"function\":{\"name\":\"" + name + "\","
                + "\"description\":\"" + desc + "\",\"parameters\":" + params + "}}";
    }

    /** 执行工具调用，返回给模型/规则层的文本结果 */
    public String execute(String name, String argumentsJson, Long currentUserId) {
        try {
            switch (name) {
                case TOOL_ORDER:
                    return queryOrder(jsonArg(argumentsJson, "orderNo"), currentUserId);
                case TOOL_PRODUCT:
                    return queryProduct(Long.valueOf(jsonArg(argumentsJson, "productId")), currentUserId);
                case TOOL_ANNOUNCEMENT:
                    return listAnnouncements();
                case TOOL_WALLET:
                    return queryWallet(currentUserId);
                case TOOL_HANDOFF:
                    return handoff();
                default:
                    return "工具不存在：" + name;
            }
        } catch (Exception e) {
            log.warn("AI 工具执行失败: name={}, err={}", name, e.getMessage());
            return "查询失败：" + e.getMessage();
        }
    }

    /** 规则引擎兜底用：不依赖模型参数，从原始输入直接提取意图 */
    public String directQueryFromText(String userText, Long currentUserId) {
        String text = userText == null ? "" : userText;
        if (text.contains("余额") || text.contains("流水") || text.contains("钱包")) {
            return queryWallet(currentUserId);
        }
        if (text.contains("订单")) {
            Matcher m = ORDER_NO_PATTERN.matcher(text.replaceAll("\\s", ""));
            return queryOrder(m.find() ? m.group(1) : null, currentUserId);
        }
        if (text.contains("公告")) {
            return listAnnouncements();
        }
        return null;
    }

    private String queryOrder(String orderNo, Long userId) {
        Order order;
        if (orderNo != null && !orderNo.isEmpty()) {
            order = orderMapper.selectOne(new LambdaQueryWrapper<Order>().eq(Order::getOrderNo, orderNo));
            if (order == null) {
                return "未找到订单号 " + orderNo + "，请核对后重试。";
            }
            if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
                return "该订单不属于当前账号，无权查看。";
            }
        } else {
            List<Order> orders = orderMapper.selectList(new LambdaQueryWrapper<Order>()
                    .and(w -> w.eq(Order::getBuyerId, userId).or().eq(Order::getSellerId, userId))
                    .orderByDesc(Order::getCreatedAt)
                    .last("LIMIT 1"));
            if (orders.isEmpty()) {
                return "当前账号还没有订单。";
            }
            order = orders.get(0);
        }
        String status = OrderStatus.getTextByCode(order.getStatus());
        String role = order.getBuyerId().equals(userId) ? "买家" : "卖家";
        return "订单 " + order.getOrderNo() + "（" + order.getProductTitle() + "），金额 ¥"
                + order.getTotalAmount() + "，状态：" + status + "（你是" + role + "）。"
                + hintByStatus(order.getStatus());
    }

    private String hintByStatus(int status) {
        switch (status) {
            case 0: return "请在30分钟内完成支付，超时将自动取消。";
            case 1: return "已支付待发货，可提醒卖家尽快发货。";
            case 2: return "已发货，收到商品后请在订单页确认收货。";
            case 4: return "交易已完成，可进入订单页评价。";
            case 5: return "订单已取消。";
            default: return "";
        }
    }

    private String queryProduct(Long productId, Long userId) {
        Product p = productMapper.selectById(productId);
        if (p == null) {
            return "商品不存在（ID: " + productId + "）。";
        }
        String status = com.c2c.common.enums.ProductStatus.getTextByCode(p.getStatus());
        String cover = "";
        ProductImage img = productImageMapper.selectOne(new LambdaQueryWrapper<ProductImage>()
                .eq(ProductImage::getProductId, p.getId()).orderByAsc(ProductImage::getSortOrder).last("LIMIT 1"));
        if (img != null) {
            cover = "，图片：" + img.getUrl();
        }
        return "《"+ p.getTitle() + "》售价 ¥" + p.getPrice() + "，状态：" + status + cover
                + "。商品链接：/product/" + p.getId();
    }

    private String listAnnouncements() {
        List<Announcement> list = announcementMapper.selectList(new LambdaQueryWrapper<Announcement>()
                .eq(Announcement::getStatus, 1)
                .orderByDesc(Announcement::getPinned)
                .orderByDesc(Announcement::getCreatedAt)
                .last("LIMIT 5"));
        if (list.isEmpty()) {
            return "暂无已发布的平台公告。";
        }
        StringBuilder sb = new StringBuilder("最新平台公告：");
        for (Announcement a : list) {
            sb.append("\n· ").append(a.getTitle());
        }
        return sb.toString();
    }

    private String queryWallet(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            return "用户不存在。";
        }
        List<WalletLog> logs = walletLogMapper.selectList(new LambdaQueryWrapper<WalletLog>()
                .eq(WalletLog::getUserId, userId)
                .orderByDesc(WalletLog::getCreatedAt)
                .last("LIMIT 5"));
        StringBuilder sb = new StringBuilder("当前余额 ¥" + user.getBalance() + "。最近流水：");
        if (logs.isEmpty()) {
            sb.append("暂无。");
        } else {
            for (WalletLog l : logs) {
                sb.append("\n· ").append(l.getType()).append(" ")
                        .append(l.getAmount().compareTo(java.math.BigDecimal.ZERO) >= 0 ? "+" : "")
                        .append(l.getAmount()).append("（").append(l.getRemark() == null ? "" : l.getRemark()).append("）");
            }
        }
        return sb.toString();
    }

    private String handoff() {
        return "如需人工处理（投诉/纠纷/违规申诉），请：商品页点「举报该商品」、订单页点「联系对方」，"
                + "或在「个人中心 → 我的整改申诉」提交；管理员会在审核工作台处理。";
    }

    /** 从模型返回的 arguments JSON 里取字符串参数（容错解析） */
    private String jsonArg(String json, String key) {
        if (json == null) {
            return null;
        }
        try {
            cn.hutool.json.JSONObject obj = cn.hutool.json.JSONUtil.parseObj(json);
            return obj.getStr(key);
        } catch (Exception e) {
            return null;
        }
    }
}
