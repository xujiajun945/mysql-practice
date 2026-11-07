package com.sqlpractice.mapper;

import com.sqlpractice.entity.Question;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface QuestionMapper {

    int countAll();

    List<Question> findAll();

    List<Question> listEnabled();

    Question findById(@Param("id") Long id);

    int insert(Question question);

    int update(Question question);

    int deleteById(@Param("id") Long id);
}
