package com.eypa.app.model.user;

public class RegisterRequest {
    private String username;
    private String email;
    private String password;
    private String code;

    public RegisterRequest(String username, String email, String password, String code) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.code = code;
    }

    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getCode() { return code; }
}
