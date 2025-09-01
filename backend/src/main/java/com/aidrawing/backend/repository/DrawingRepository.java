// 文件路径: src/main/java/com/aidrawing/backend/repository/DrawingRepository.java

package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.Drawing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
}

