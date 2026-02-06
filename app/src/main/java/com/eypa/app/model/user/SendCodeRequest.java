package com.eypa.app.model.user;

public class SendCodeRequest {
    private String email;
    private String type;

    public SendCodeRequest(String email, String type) {
        this.email = email;
        this.type = type;
    }

    public String getEmail() { return email; }
    public String getType() { return type; }
}
