package com.eypa.app.model.user;

public class ResetPasswordRequest {
    private String email;
    private String password;
    private String code;

    public ResetPasswordRequest(String email, String password, String code) {
        this.email = email;
        this.password = password;
        this.code = code;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getCode() { return code; }
}
