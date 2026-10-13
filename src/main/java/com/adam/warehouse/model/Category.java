package com.adam.warehouse.model;

public class Category {

    private int categoryId;

    private String name;

    private String description;

    private boolean active;

    public Category() {
        this.active = true;
    }

    public Category(int categoryId,
                    String name,
                    String description,
                    boolean active) {

        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.active = active;
    }

    public Category(String name,
                    String description) {

        this.name = name;
        this.description = description;
        this.active = true;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? "" : name.trim();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean hasDescription() {
        return !description.isBlank();
    }

    @Override
    public String toString() {
        return name;
    }

}