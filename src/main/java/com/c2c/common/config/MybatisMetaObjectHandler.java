package com.c2c.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 字段自动填充处理器。
 *
 * 项目里各实体的 createdAt / updatedAt 普遍声明了
 * {@code @TableField(fill = FieldFill.INSERT / INSERT_UPDATE)}，
 * 若没有本处理器，这些字段会以 null 原样进入 INSERT/UPDATE SQL。
 * 对 created_at 为 NOT NULL 的表（如 product_comment、product_intent）会直接
 * 抛 SQLIntegrityConstraintViolationException（Column 'created_at' cannot be null）。
 *
 * strictInsertFill / strictUpdateFill 只填充“带对应 fill 注解的字段”，
 * 实体里没有该字段或无注解时为无操作，安全通用。
 */
@Component
public class MybatisMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createdAt", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updatedAt", LocalDateTime.class, LocalDateTime.now());
    }
}
