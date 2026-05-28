// 文件路径: src/main/java/com/aidrawing/backend/repository/DrawingRepository.java

package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.Drawing;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DrawingRepository extends JpaRepository<Drawing, String> {

    /**
     * Finds all Drawing entities for a user's personal history, sorted from newest to oldest.
     * * 查找某个用户的所有个人历史绘图，按创建日期降序排列。
     */
    List<Drawing> findAllByOrderByCreatedAtDesc();

    /**
     * Finds all Drawing entities that have been shared to the public gallery, sorted from newest to oldest.
     * 查找所有已分享到公共画廊的绘图，按创建日期降序排列。
     */
    @Query("SELECT d FROM Drawing d WHERE d.sharedToGallery = true ORDER BY d.createdAt DESC")
    List<Drawing> findBySharedToGalleryTrueOrderByCreatedAtDesc();

    /**
     * 查找所有已分享到公共画廊的绘图，包含作者信息，避免N+1查询问题
     */
    @Query("SELECT d FROM Drawing d LEFT JOIN FETCH d.user WHERE d.sharedToGallery = true ORDER BY d.createdAt DESC")
    List<Drawing> findBySharedToGalleryTrueWithUserOrderByCreatedAtDesc();

    @Query(value = "SELECT d FROM Drawing d LEFT JOIN FETCH d.user WHERE d.sharedToGallery = true ORDER BY d.createdAt DESC",
           countQuery = "SELECT COUNT(d) FROM Drawing d WHERE d.sharedToGallery = true")
    Page<Drawing> findBySharedToGalleryTrueWithUserOrderByCreatedAtDesc(Pageable pageable);

    Long countBySharedToGalleryTrue();

    // --- 个人中心相关查询方法 ---

    /**
     * 分页查询指定用户的所有作品
     */
    Page<Drawing> findByUserId(String userId, Pageable pageable);

    /**
     * 统计指定用户的总作品数
     */
    Long countByUserId(String userId);

    /**
     * 统计指定用户的已分享作品数
     */
    Long countByUserIdAndSharedToGallery(String userId, Boolean sharedToGallery);

    /**
     * 获取指定用户最新的作品（用于统计最后活跃时间）
     */
    Optional<Drawing> findTopByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 获取指定用户最近的N张作品
     */
    List<Drawing> findTop10ByUserIdOrderByCreatedAtDesc(String userId);

    /**
     * 分页查询指定用户的公开作品（已分享到画廊的作品）
     */
    Page<Drawing> findByUserIdAndSharedToGalleryOrderByCreatedAtDesc(String userId, Boolean sharedToGallery, Pageable pageable);
}

