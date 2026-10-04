package com.blog.controller;

import com.blog.dto.GuestPostRequest;
import com.blog.model.GuestPost;
import com.blog.service.GuestPostService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/guest-posts")
@CrossOrigin(origins = "http://localhost:3000")
public class GuestPostController {

    private final GuestPostService guestPostService;

    public GuestPostController(GuestPostService guestPostService) {
        this.guestPostService = guestPostService;
    }

    /**
     * Submit a guest post, optionally with an attached document.
     * Requirements: 4.1, 4.3, 4.5, 4.6
     */
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<GuestPost> submitGuestPost(
            @RequestPart("request") GuestPostRequest request,
            @RequestPart(value = "document", required = false) MultipartFile document) {

        GuestPost savedGuestPost = guestPostService.submit(request, document);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedGuestPost);
    }
}
