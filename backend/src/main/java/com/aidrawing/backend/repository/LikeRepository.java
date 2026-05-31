package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.Like;
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
 * 作品点赞数据访问层
 * Artwork Like Repository
 */
@Repository
public interface LikeRepository extends JpaRepository<Like, String> {

    /**
     * 检查用户是否点赞了某作品
     * @param userId 用户ID
     * @param drawingId 作品ID
     * @return 点赞关系
     */
    Optional<Like> findByUserIdAndDrawingId(String userId, String drawingId);

    /**
     * 检查是否存在点赞关系
     * @param userId 用户ID
     * @param drawingId 作品ID
     * @return 是否存在点赞关系
     */
    boolean existsByUserIdAndDrawingId(String userId, String drawingId);

    /**
     * 获取作品的点赞列表
     * @param drawingId 作品ID
     * @param pageable 分页参数
     * @return 点赞列表
     */
    @Query("SELECT l FROM Like l JOIN FETCH l.user WHERE l.drawing.id = :drawingId ORDER BY l.createdAt DESC")
    Page<Like> findLikesByDrawingId(@Param("drawingId") String drawingId, Pageable pageable);

    /**
     * 获取用户的点赞列表
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 用户点赞列表
     */
    @Query("SELECT l FROM Like l JOIN FETCH l.drawing WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    Page<Like> findLikesByUserId(@Param("userId") String userId, Pageable pageable);

    /**
     * 统计作品的点赞数
     * @param drawingId 作品ID
     * @return 点赞数
     */
    Long countByDrawingId(String drawingId);

    /**
     * 统计用户的点赞数（用户点赞了多少作品）
     * @param userId 用户ID
     * @return 点赞数
     */
    Long countByUserId(String userId);

    /**
     * 统计用户作品获得的点赞总数
     * @param userId 用户ID（作品所有者）
     * @return 获得的点赞总数
     */
    @Query("SELECT COUNT(l) FROM Like l WHERE l.drawing.user.id = :userId")
    Long countLikesReceivedByUserId(@Param("userId") String userId);

    /**
     * 取消点赞
     * @param userId 用户ID
     * @param drawingId 作品ID
     */
    @Modifying
    @Query("DELETE FROM Like l WHERE l.user.id = :userId AND l.drawing.id = :drawingId")
    void deleteByUserIdAndDrawingId(@Param("userId") String userId, @Param("drawingId") String drawingId);

    /**
     * 获取用户最新点赞的作品列表（不分页，用于统计）
     * @param userId 用户ID
     * @return 最新点赞列表
     */
    @Query("SELECT l FROM Like l JOIN FETCH l.drawing WHERE l.user.id = :userId ORDER BY l.createdAt DESC")
    List<Like> findTop10ByUserIdOrderByCreatedAtDesc(@Param("userId") String userId);

    /**
     * 批量获取多个作品的点赞数
     * @param drawingIds 作品ID列表
     * @return 点赞数映射 (drawingId -> likeCount)
     */
    @Query("SELECT l.drawing.id, COUNT(l) FROM Like l WHERE l.drawing.id IN :drawingIds GROUP BY l.drawing.id")
    List<Object[]> countLikesByDrawingIds(@Param("drawingIds") List<String> drawingIds);

    /**
     * 批量检查用户对多个作品的点赞状态
     * @param userId 用户ID
     * @param drawingIds 作品ID列表
     * @return 已点赞的作品ID列表
     */
    @Query("SELECT l.drawing.id FROM Like l WHERE l.user.id = :userId AND l.drawing.id IN :drawingIds")
    List<String> findLikedDrawingIdsByUserAndDrawingIds(@Param("userId") String userId, @Param("drawingIds") List<String> drawingIds);

    @Modifying
    @Query("DELETE FROM Like l WHERE l.drawing.id = :drawingId")
    void deleteByDrawingId(@Param("drawingId") String drawingId);
}
