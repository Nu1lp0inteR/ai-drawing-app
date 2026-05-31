package com.aidrawing.backend.repository;

import com.aidrawing.backend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.drawing.id = :drawingId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    Page<Comment> findTopLevelByDrawingId(@Param("drawingId") String drawingId, Pageable pageable);

    @Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.parentComment.id = :parentId ORDER BY c.createdAt ASC")
    Page<Comment> findRepliesByParentId(@Param("parentId") String parentId, Pageable pageable);

    Long countByDrawingId(String drawingId);

    @Query("SELECT c FROM Comment c JOIN FETCH c.drawing WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Comment> findByUserId(@Param("userId") String userId, Pageable pageable);

    Long countByUserId(String userId);

    @Modifying
    @Query("DELETE FROM Comment c WHERE c.drawing.id = :drawingId")
    void deleteByDrawingId(@Param("drawingId") String drawingId);
}
