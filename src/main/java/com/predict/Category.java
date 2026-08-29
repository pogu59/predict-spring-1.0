package com.predict;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 카테고리. 5개 고정값이며 애플리케이션에서 추가/삭제하지 않는다.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(name = "category_id")
    private Integer id;

    @Column(name = "name", nullable = false, unique = true, length = 30)
    private String name;

    protected Category() {
    }

    public Category(Integer id, String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
