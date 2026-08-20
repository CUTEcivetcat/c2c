package com.c2c.common.constant;

/**
 * 接口 URL 常量（统一管理）。
 *
 * <p>所有 Controller 的 {@code @RequestMapping} 与各方法映射统一引用本接口的常量，
 * 避免魔法字符串散落在各控制器里。修改路径时只需改这里一处。
 *
 * <p>注意：本常量<b>不含</b> context-path（{@code /api/v1}），
 * 与 {@code AuthTokenFilter} 白名单使用的路径一致。
 */
public interface ApiPath {

    // ==================== 用户模块 /user ====================
    String USER = "/user";
    String USER_SMS_SEND = "/user/sms/send";
    String USER_LOGIN = "/user/login";
    String USER_REGISTER = "/user/register";
    String USER_RESET_PASSWORD = "/user/reset-password";
    String USER_BIND_PHONE = "/user/bind-phone";
    String USER_LOGOUT = "/user/logout";
    String USER_PROFILE = "/user/profile";
    String USER_PROFILE_ID = "/user/profile/{userId}";
    String USER_REPUTATION_ID = "/user/reputation/{userId}";
    String USER_STATUS = "/user/{userId}/status";
    String USER_ADMIN_LIST = "/user/admin/list";
    String USER_ADMIN_COUNT = "/user/admin/count";

    // ==================== 收货地址 /user/address ====================
    String ADDRESS = "/user/address";
    String ADDRESS_ID = "/user/address/{id}";
    String ADDRESS_DEFAULT = "/user/address/{id}/default";

    // ==================== 商品 /product ====================
    String PRODUCT = "/product";
    String PRODUCT_ID = "/product/{id}";
    String PRODUCT_LIST = "/product/list";
    String PRODUCT_STATUS = "/product/{id}/status";
    String PRODUCT_MY_PUBLISHED = "/product/my/published";
    String PRODUCT_USER_ID = "/product/user/{userId}";
    String PRODUCT_ADMIN_LIST = "/product/admin/list";
    String PRODUCT_ADMIN_COUNT = "/product/admin/count";
    String PRODUCT_ADMIN_BAN = "/product/admin/{id}/ban";
    String PRODUCT_ADMIN_RESTORE = "/product/admin/{id}/restore";

    // ==================== 分类 /product/category ====================
    String CATEGORY = "/product/category";
    String CATEGORY_ADMIN = "/product/admin/category";
    String CATEGORY_ADMIN_ID = "/product/admin/category/{id}";

    // ==================== 购买意向 /product/intent ====================
    String INTENT = "/product/intent";
    String INTENT_CREATE = "/product/intent/{productId}";
    String INTENT_MY = "/product/intent/my";
    String INTENT_SELLER = "/product/intent/seller";
    String INTENT_REPLY = "/product/intent/{id}/reply";
    String INTENT_CLOSE = "/product/intent/{id}/close";
    String INTENT_DEAL = "/product/intent/{id}/deal";

    // ==================== 商品评论 /product/comment ====================
    String COMMENT = "/product/comment";
    String COMMENT_ID = "/product/comment/{id}";

    // ==================== 文件上传 /upload ====================
    String UPLOAD = "/upload";
    String UPLOAD_IMAGE = "/upload/image";
    String UPLOAD_IMAGES = "/upload/images";

    // ==================== 订单 /order ====================
    String ORDER = "/order";
    String ORDER_ID = "/order/{id}";
    String ORDER_LIST = "/order/list";
    String ORDER_SELL_LIST = "/order/sell/list";
    String ORDER_PAY = "/order/{id}/pay";
    String ORDER_SHIP = "/order/{id}/ship";
    String ORDER_RECEIVE = "/order/{id}/receive";
    String ORDER_CANCEL = "/order/{id}/cancel";
    String ORDER_ADMIN_LIST = "/order/admin/list";
    String ORDER_ADMIN_COUNT_TODAY = "/order/admin/count-today";

    // ==================== 即时通讯 /im ====================
    String IM = "/im";
    String IM_CONVERSATION_LIST = "/im/conversation/list";
    String IM_CONVERSATION = "/im/conversation";
    String IM_MESSAGE_ID = "/im/message/{conversationId}";
    String IM_MESSAGE = "/im/message";
    String IM_MESSAGE_READ = "/im/message/{conversationId}/read";
    String IM_UNREAD_COUNT = "/im/unread/count";

    // ==================== 收藏 /favorite ====================
    String FAVORITE = "/favorite";
    String FAVORITE_ID = "/favorite/{productId}";
    String FAVORITE_LIST = "/favorite/list";
    String FAVORITE_CHECK = "/favorite/check/{productId}";

    // ==================== 评分 /rating ====================
    String RATING = "/rating";
    String RATING_USER_ID = "/rating/user/{userId}";
    String RATING_ORDER_ID = "/rating/order/{orderId}";

    // ==================== 举报 /report（登录用户可提交） ====================
    String REPORT = "/report";
    String REPORT_CREATE = "/report";
    String REPORT_MY = "/report/my";

    // ==================== 整改申诉 /appeal（卖家可提交） ====================
    String APPEAL = "/appeal";
    String APPEAL_CREATE = "/appeal";
    String APPEAL_MY = "/appeal/my";

    // ==================== 审核工作台 /review（审核员 role=2 或管理员） ====================
    String REVIEW_REPORTS = "/review/reports";
    String REVIEW_REPORT_DETAIL = "/review/reports/{id}";
    String REVIEW_REPORT_HANDLE = "/review/reports/{id}/handle";
    String REVIEW_APPEALS = "/review/appeals";
    String REVIEW_APPEAL_DETAIL = "/review/appeals/{id}";
    String REVIEW_APPEAL_HANDLE = "/review/appeals/{id}/handle";

    // ==================== 管理端 /admin ====================
    String ADMIN = "/admin";
    String ADMIN_LOGIN = "/admin/login";
    String ADMIN_DASHBOARD_SUMMARY = "/admin/dashboard/summary";
    String ADMIN_DASHBOARD_TRENDS = "/admin/dashboard/trends";
    String ADMIN_USERS = "/admin/users";
    String ADMIN_USER_STATUS = "/admin/users/{userId}/status";
    String ADMIN_USER_ROLES = "/admin/users/roles";
    String ADMIN_USER_ROLE = "/admin/users/{userId}/role";
    String ADMIN_PRODUCTS = "/admin/products";
    String ADMIN_PRODUCT_STATUS = "/admin/products/{id}/status";
    String ADMIN_ORDERS = "/admin/orders";
}
