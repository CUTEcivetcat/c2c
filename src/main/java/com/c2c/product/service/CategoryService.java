package com.c2c.product.service;

import com.c2c.product.dto.CategoryCreateDTO;
import com.c2c.product.dto.CategoryUpdateDTO;
import com.c2c.product.entity.Category;

import java.util.List;

/**
 * 商品分类服务接口
 * <p>提供分类树查询、分类详情、新增、修改与删除能力，分类支持多级层级结构，
 * 删除前会校验是否存在子分类或关联商品。</p>
 */
public interface CategoryService {

    /** 获取分类树（按 sortOrder 升序组装层级结构） */
    List<Category> getTree();

    /** 根据ID查询分类 */
    Category getById(Long id);

    /** 管理端：扁平列表，按 sortOrder 排序 */
    List<Category> listAll();

    /** 新增分类（自动计算层级，父分类不存在时抛异常） */
    Category create(CategoryCreateDTO dto);

    /** 修改分类（支持调整父级，并校验不能移动到自身或其子分类下） */
    Category update(Long id, CategoryUpdateDTO dto);

    /** 删除分类（存在子分类或商品时禁止删除） */
    void delete(Long id);
}
