package com.sidelined.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// The app's own record of a Supabase user: Supabase knows how to sign them in, this knows their role.
@Entity
@Table(name = "profiles")
public class Profile {

    // The Supabase user's id: the "sub" claim of their token. Not generated here.
    @Id
    private String id;

    // @Enumerated adds a database check allowing only USER and ADMIN, so a typo made while
    // promoting someone in the Table Editor is rejected there instead of breaking their requests.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Role role = Role.USER;

    // For JPA.
    protected Profile() {
    }

    public Profile(String id) {
        this.id = id;
    }

    public Profile(String id, Role role) {
        this.id = id;
        this.role = role;
    }

    public String getId() { return id; }
    public Role getRole() { return role; }
}
