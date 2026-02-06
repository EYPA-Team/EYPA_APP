package com.eypa.app.model.user;

public class LoginResponse {
    private Object code;
    private String msg;
    private String message;
    private UserProfile data;

    public boolean isSuccess() {
        if (code instanceof Integer) {
            return (Integer) code == 200;
        } else if (code instanceof Double) {
            return ((Double) code).intValue() == 200;
        }
        return false;
    }

    public String getMessage() {
        return message != null ? message : msg;
    }

    public UserProfile getData() {
        return data;
    }
}
