package com.models;

public class RolModel {

    private Long id;
    private String title;
    private String description;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescString() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
