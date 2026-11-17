package com.negocio.analitica.model;

/** System user stored in the PostgreSQL users table. */
public class Usuario {
    private Long id;
    private String username;
    private String passwordHash;
    private String fullName;
    private boolean active = true;

    public Usuario() { }

    public Usuario(Long id, String username, String passwordHash,
                   String fullName, boolean active) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.active = active;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
