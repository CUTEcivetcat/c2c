package com.c2c.suggestion.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.c2c.suggestion.entity.Suggestion;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SuggestionMapper extends BaseMapper<Suggestion> {
}
