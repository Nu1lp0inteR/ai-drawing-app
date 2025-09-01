package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 用户数据访问层
 * User Data Access Layer
 * 
 * 提供用户相关的数据库操作
 * Provides user-related database operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, String> {

    /**
     * 根据用户名查找用户
     * Find user by username
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     * Find user by email
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据用户名或邮箱查找用户
     * Find user by username or email
     */
    Optional<User> findByUsernameOrEmail(String username, String email);

    /**
     * 检查用户名是否已存在
     * Check if username exists
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否已存在
     * Check if email exists  
     */
    boolean existsByEmail(String email);
}
