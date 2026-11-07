package com.sqlpractice.mapper;

import com.sqlpractice.entity.Attempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AttemptMapper {

    int insert(Attempt attempt);

    Attempt findById(@Param("id") Long id);

    List<Attempt> findByUserId(@Param("userId") Long userId);

    List<Attempt> listByStatus(@Param("status") String status, @Param("limit") int limit);

    List<Attempt> listAll(@Param("limit") int limit);

    int submit(@Param("id") Long id, @Param("userId") Long userId);

    int completeReview(@Param("id") Long id,
                       @Param("reviewerId") Long reviewerId,
                       @Param("overallComment") String overallComment,
                       @Param("score") Integer score);
}
