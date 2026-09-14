package com.c2c.common.constant;

/**
 * 管理端权限码（模块级）。
 *
 * <p>权限模型：<ul>
 *   <li>总管理员（admin，admin_level=2）：拥有全部权限（adminPerms 恒视为 '*'）。</li>
 *   <li>次级/子级管理员（admin_level=1）：由总管理员在「安全设置」中按模块赋权，权限码存 user.admin_perms。</li>
 *   <li>审核员（role=2）：最低级别，仅可进入用户端审核工作台（/review/**），不能登录管理端口。</li>
 * </ul>
 */
public final class AdminPerm {

    /** 数据大屏 */
    public static final String DASHBOARD = "dashboard";
    /** 用户管理（含删除账户） */
    public static final String USERS = "users";
    /** 商品管理 */
    public static final String PRODUCTS = "products";
    /** 订单管理 */
    public static final String ORDERS = "orders";
    /** 审核管理（举报/申诉/昵称审核） */
    public static final String REVIEW = "review";
    /** 公告管理 */
    public static final String ANNOUNCEMENT = "announcement";
    /** 轮播图管理 */
    public static final String BANNER = "banner";
    /** 权限管理（角色分配，仅总管理员） */
    public static final String PERMISSIONS = "permissions";
    /** 安全设置（设置二级密码为管理员基础能力；次级管理员管理仅总管理员） */
    public static final String SECURITY = "security";

    /** 全部权限（admin_level=2 时恒有） */
    public static final String ALL = "*";

    /** 全部可分配权限码（总管理员给次级/子级管理员勾选） */
    public static final String[] ASSIGNABLE = {
            DASHBOARD, USERS, PRODUCTS, ORDERS, REVIEW,
            ANNOUNCEMENT, BANNER, PERMISSIONS, SECURITY
    };

    private AdminPerm() {
    }
}
