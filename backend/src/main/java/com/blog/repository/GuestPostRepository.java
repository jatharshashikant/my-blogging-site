package com.blog.repository;

import com.blog.model.GuestPost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuestPostRepository extends JpaRepository<GuestPost, Long> {
    List<GuestPost> findByStatusOrderBySubmittedAtDesc(String status);
}
