package com.predict.controller.dto;

import com.predict.Category;

public record CategoryResponse(Integer id, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
