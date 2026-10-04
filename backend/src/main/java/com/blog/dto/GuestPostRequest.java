package com.blog.dto;

import org.springframework.web.multipart.MultipartFile;

public class GuestPostRequest {

    private String title;
    private String body;
    private String authorName;
    private String category;

    public GuestPostRequest() {}

    public GuestPostRequest(String title, String body, String authorName, String category) {
        this.title = title;
        this.body = body;
        this.authorName = authorName;
        this.category = category;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
