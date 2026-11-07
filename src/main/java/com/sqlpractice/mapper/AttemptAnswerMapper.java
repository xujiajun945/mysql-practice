package com.sqlpractice.mapper;

import com.sqlpractice.entity.AttemptAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttemptAnswerMapper {

    List<AttemptAnswer> findByAttempt(@Param("attemptId") Long attemptId);

    AttemptAnswer findByAttemptAndQuestion(@Param("attemptId") Long attemptId,
                                           @Param("questionId") Long questionId);

    int insertOrUpdate(AttemptAnswer answer);

    int review(@Param("id") Long id,
               @Param("attemptId") Long attemptId,
               @Param("correct") Integer correct,
               @Param("comment") String comment,
               @Param("reviewerId") Long reviewerId);
}
