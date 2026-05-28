package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.Follow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户关注关系数据访问层
 * User Follow Relationship Repository
 */
@Repository
public interface FollowRepository extends JpaRepository<Follow, String> {

    /**
     * 检查用户A是否关注了用户B
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 关注关系
     */
    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

    /**
     * 检查是否存在关注关系
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     * @return 是否存在关注关系
     */
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    /**
     * 获取用户的关注列表（用户关注的人）
     * @param followerId 关注者ID
     * @param pageable 分页参数
     * @return 关注列表
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC")
    Page<Follow> findFollowingsByFollowerId(@Param("followerId") String followerId, Pageable pageable);

    /**
     * 获取用户的粉丝列表（关注该用户的人）
     * @param followingId 被关注者ID
     * @param pageable 分页参数
     * @return 粉丝列表
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.follower WHERE f.following.id = :followingId ORDER BY f.createdAt DESC")
    Page<Follow> findFollowersByFollowingId(@Param("followingId") String followingId, Pageable pageable);

    /**
     * 统计用户关注的人数
     * @param followerId 关注者ID
     * @return 关注数
     */
    Long countByFollowerId(String followerId);

    /**
     * 统计用户的粉丝数
     * @param followingId 被关注者ID
     * @return 粉丝数
     */
    Long countByFollowingId(String followingId);

    /**
     * 取消关注
     * @param followerId 关注者ID
     * @param followingId 被关注者ID
     */
    @Modifying
    @Query("DELETE FROM Follow f WHERE f.follower.id = :followerId AND f.following.id = :followingId")
    void deleteByFollowerIdAndFollowingId(@Param("followerId") String followerId, @Param("followingId") String followingId);

    /**
     * 获取用户的最新关注列表（不分页，用于统计）
     * @param followerId 关注者ID
     * @param limit 限制数量
     * @return 最新关注列表
     */
    @Query("SELECT f FROM Follow f JOIN FETCH f.following WHERE f.follower.id = :followerId ORDER BY f.createdAt DESC")
    List<Follow> findTop10ByFollowerIdOrderByCreatedAtDesc(@Param("followerId") String followerId);
}
