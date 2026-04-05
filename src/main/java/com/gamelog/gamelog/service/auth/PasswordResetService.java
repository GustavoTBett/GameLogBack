package com.gamelog.gamelog.service.auth;

public interface PasswordResetService {

    void requestReset(String email);

    void resetPassword(String token, String newPassword);
}
