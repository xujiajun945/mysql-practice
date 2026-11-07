package com.sqlpractice.mapper;

import com.sqlpractice.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User findById(@Param("id") Long id);

    List<User> findAll();

    List<User> findByStatus(@Param("status") String status);

    int countByUsername(@Param("username") String username);

    int countByRoleId(@Param("roleId") Long roleId);

    int insert(User user);

    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("reason") String reason);

    int updateRole(@Param("id") Long id, @Param("roleId") Long roleId);

    int updateNickname(@Param("id") Long id, @Param("nickname") String nickname);

    int updatePassword(@Param("id") Long id, @Param("password") String password);

    int touchLogin(@Param("id") Long id);
}
