package com.c2c.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.c2c.ai.entity.Faq;
import com.c2c.ai.mapper.FaqMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * FAQ 知识检索：关键词命中评分（keywords 命中 > 标题包含 > 分类包含），取 Top-N。
 *
 * <p>说明：K50 OlliteRT 无 /embeddings 端点，v1 用关键词检索（种子 30 条覆盖率高）；
 * 将来接云 API 后可平滑替换为向量检索，调用方无感。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FaqSearchService {

    private final FaqMapper faqMapper;

    /** 快捷问题（聊天页 chips）：按 sort_order 取前 N 条 */
    public List<String> suggestions(int limit) {
        return faqMapper.selectList(new LambdaQueryWrapper<Faq>()
                        .eq(Faq::getStatus, 1)
                        .orderByAsc(Faq::getSortOrder)
                        .last("LIMIT " + limit))
                .stream().map(Faq::getQuestion).collect(Collectors.toList());
    }

    /** 按用户输入检索 Top-N FAQ（评分制） */
    public List<Faq> search(String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        List<Faq> all = faqMapper.selectList(new LambdaQueryWrapper<Faq>()
                .eq(Faq::getStatus, 1));
        return all.stream()
                .map(f -> new Object[] { f, score(f, q) })
                .filter(o -> (int) o[1] > 0)
                .sorted((a, b) -> (int) b[1] - (int) a[1])
                .limit(limit)
                .map(o -> (Faq) o[0])
                .collect(Collectors.toList());
    }

    private int score(Faq f, String q) {
        int s = 0;
        if (f.getKeywords() != null) {
            for (String kw : f.getKeywords().split("[,，]")) {
                kw = kw.trim().toLowerCase();
                if (!kw.isEmpty() && q.contains(kw)) {
                    s += 3;
                }
            }
        }
        String question = f.getQuestion() == null ? "" : f.getQuestion().toLowerCase();
        for (String term : q.split("[\\s,，。？?！!]+")) {
            if (term.length() >= 2 && question.contains(term)) {
                s += 1;
            }
        }
        return s;
    }

    /** 便捷方法：取命中最高的一条（规则引擎主路径用） */
    public Faq bestMatch(String query) {
        List<Faq> hits = search(query, 1);
        return hits.isEmpty() ? null : hits.get(0);
    }
}
