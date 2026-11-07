package com.sqlpractice.mapper;

import com.sqlpractice.entity.AuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuditMapper {

    int insert(AuditLog log);

    List<AuditLog> list(@Param("offset") int offset, @Param("limit") int limit);

    int count();
}
