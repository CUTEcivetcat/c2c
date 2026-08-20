package com.c2c.product.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.common.exception.BusinessException;
import com.c2c.product.dto.CategoryCreateDTO;
import com.c2c.product.dto.CategoryUpdateDTO;
import com.c2c.product.entity.Category;
import com.c2c.product.entity.Product;
import com.c2c.product.mapper.CategoryMapper;
import com.c2c.product.mapper.ProductMapper;
import com.c2c.product.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 商品分类服务实现类
 * <p>基于 MyBatis-Plus 实现分类树的构建与增删改，支持层级校验、
 * 递归刷新子分类层级，并通过 Redis 缓存分类树以提升查询性能。</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final String TREE_CACHE_KEY = "category:tree";

    private final CategoryMapper categoryMapper;
    private final ProductMapper productMapper;
    private final StringRedisTemplate redisTemplate;

    @Override
    public List<Category> getTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
        List<Category> tree = buildTree(all, 0L);

        // 标记缓存键，保证与写操作的失效逻辑一致
        redisTemplate.opsForValue().set(TREE_CACHE_KEY, "cached", 24, TimeUnit.HOURS);
        return tree;
    }

    @Override
    public Category getById(Long id) {
        return categoryMapper.selectById(id);
    }

    @Override
    public List<Category> listAll() {
        return categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().orderByAsc(Category::getSortOrder));
    }

    @Override
    public Category create(CategoryCreateDTO dto) {
        long parentId = dto.getParentId() == null ? 0L : dto.getParentId();
        int level = 1;
        if (parentId != 0L) {
            Category parent = categoryMapper.selectById(parentId);
            if (parent == null) {
                throw new BusinessException("父分类不存在");
            }
            level = parent.getLevel() + 1;
        }

        Category category = new Category();
        category.setName(dto.getName());
        category.setParentId(parentId);
        category.setLevel(level);
        category.setSortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder());
        category.setIconUrl(dto.getIconUrl());
        categoryMapper.insert(category);

        redisTemplate.delete(TREE_CACHE_KEY);
        return category;
    }

    @Override
    public Category update(Long id, CategoryUpdateDTO dto) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        if (StrUtil.isNotBlank(dto.getName())) {
            category.setName(dto.getName());
        }
        if (dto.getSortOrder() != null) {
            category.setSortOrder(dto.getSortOrder());
        }
        if (StrUtil.isNotBlank(dto.getIconUrl())) {
            category.setIconUrl(dto.getIconUrl());
        }

        if (dto.getParentId() != null && !dto.getParentId().equals(category.getParentId())) {
            long parentId = dto.getParentId();
            if (parentId == id) {
                throw new BusinessException("父分类不能是自己");
            }
            if (parentId != 0L) {
                Category parent = categoryMapper.selectById(parentId);
                if (parent == null) {
                    throw new BusinessException("父分类不存在");
                }
                if (isDescendant(parentId, id)) {
                    throw new BusinessException("不能将分类移动到其子分类下");
                }
                category.setLevel(parent.getLevel() + 1);
            } else {
                category.setLevel(1);
            }
            category.setParentId(parentId);
            recomputeChildLevels(id);
        }

        categoryMapper.updateById(category);
        redisTemplate.delete(TREE_CACHE_KEY);
        return category;
    }

    @Override
    public void delete(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BusinessException("分类不存在");
        }

        Long childCount = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        if (childCount > 0) {
            throw new BusinessException("该分类下还有子分类，无法删除");
        }

        Long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException("该分类下还有商品，无法删除");
        }

        categoryMapper.deleteById(id);
        redisTemplate.delete(TREE_CACHE_KEY);
    }

    private List<Category> buildTree(List<Category> all, Long parentId) {
        return all.stream()
                .filter(c -> c.getParentId().equals(parentId))
                .peek(c -> c.setChildren(buildTree(all, c.getId())))
                .collect(Collectors.toList());
    }

    /** 判断 id 是否为 ancestorId 的后代 */
    private boolean isDescendant(Long ancestorId, Long id) {
        List<Category> children = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        for (Category child : children) {
            if (child.getId().equals(ancestorId)) {
                return true;
            }
            if (isDescendant(ancestorId, child.getId())) {
                return true;
            }
        }
        return false;
    }

    /** 移动分类后，递归刷新其所有后代分类的 level */
    private void recomputeChildLevels(Long id) {
        List<Category> children = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>().eq(Category::getParentId, id));
        for (Category child : children) {
            Category parent = categoryMapper.selectById(child.getParentId());
            child.setLevel(parent.getLevel() + 1);
            categoryMapper.updateById(child);
            recomputeChildLevels(child.getId());
        }
    }
}
