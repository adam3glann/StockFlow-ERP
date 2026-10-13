package com.adam.warehouse.model;

import com.adam.warehouse.enums.UserRole;

import java.time.LocalDateTime;

public class Employee {

    private int employeeId;

    private String firstName;

    private String lastName;

    private String username;

    private String password;

    private UserRole role;

    private String phone;

    private String email;

    private boolean active;

    private LocalDateTime createdAt;
    private String notes;

    public Employee() {
    }

    public Employee(int employeeId,
                    String firstName,
                    String lastName,
                    String username,
                    String password,
                    UserRole role,
                    String phone,
                    String email,
                    String notes,
                    boolean active,
                    LocalDateTime createdAt) {

        this.employeeId = employeeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.username = username;
        this.password = password;
        this.role = role;
        this.phone = phone;
        this.email = email;
        this.notes = notes;
        this.active = active;
        this.createdAt = createdAt;
    }

    public int getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(int employeeId) {
        this.employeeId = employeeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName == null ? "" : firstName.trim();
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName == null ? "" : lastName.trim();
    }

    public String getFullName() {

        return (firstName + " " + lastName).trim();

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? "" : username.trim();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public String getRoleName() {
        return role == null ? "" : role.toString();
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone == null ? "" : phone.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? "" : email.trim();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public String getNotes() {
        return notes;
    }
    public void setNotes(String notes) {
        this.notes = notes == null ? "" : notes.trim();
    }
    @Override
    public String toString() {

        return getFullName();

    }

}
