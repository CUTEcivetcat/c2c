package com.c2c.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.c2c.ai.entity.Faq;
import com.c2c.ai.mapper.FaqMapper;
import com.c2c.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * FAQ 知识库管理端服务：分页查询、新增、编辑、启停、删除。
 *
 * <p>检索侧由 {@link FaqSearchService} 按 status=1 命中；本类只维护知识库内容，
 * 不改动客服对话链路。</p>
 */
@Service
@RequiredArgsConstructor
public class FaqAdminService {

    private final FaqMapper faqMapper;

    /** 管理端全量 FAQ（含停用），按排序值升序、ID 倒序 */
    public Page<Faq> listAll(int page, int size, String keyword) {
        LambdaQueryWrapper<Faq> w = new LambdaQueryWrapper<Faq>()
                .orderByAsc(Faq::getSortOrder)
                .orderByDesc(Faq::getId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            w.like(Faq::getQuestion, keyword.trim());
        }
        return faqMapper.selectPage(new Page<>(page, size), w);
    }

    /** 新增 FAQ */
    public Faq create(Faq f) {
        checkRequired(f);
        f.setId(null);
        f.setQuestion(f.getQuestion().trim());
        f.setAnswer(f.getAnswer().trim());
        f.setStatus(f.getStatus() == null ? 1 : f.getStatus());
        f.setSortOrder(f.getSortOrder() == null ? 0 : f.getSortOrder());
        faqMapper.insert(f);
        return f;
    }

    /** 编辑 FAQ */
    public Faq update(Long id, Faq f) {
        Faq exist = faqMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("FAQ 不存在");
        }
        if (f.getQuestion() != null) exist.setQuestion(f.getQuestion().trim());
        if (f.getAnswer() != null) exist.setAnswer(f.getAnswer().trim());
        if (f.getCategory() != null) exist.setCategory(f.getCategory());
        if (f.getKeywords() != null) exist.setKeywords(f.getKeywords());
        if (f.getStatus() != null) exist.setStatus(f.getStatus());
        if (f.getSortOrder() != null) exist.setSortOrder(f.getSortOrder());
        faqMapper.updateById(exist);
        return exist;
    }

    /** 启用/停用（1 启用 0 停用） */
    public void changeStatus(Long id, Integer status) {
        Faq exist = faqMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("FAQ 不存在");
        }
        exist.setStatus(status == null || status != 1 ? 0 : 1);
        faqMapper.updateById(exist);
    }

    /** 删除 FAQ */
    public void delete(Long id) {
        faqMapper.deleteById(id);
    }

    private void checkRequired(Faq f) {
        if (f.getQuestion() == null || f.getQuestion().trim().isEmpty()) {
            throw new BusinessException("标准问不能为空");
        }
        if (f.getAnswer() == null || f.getAnswer().trim().isEmpty()) {
            throw new BusinessException("答案不能为空");
        }
    }
}
