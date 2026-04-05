package com.gamelog.gamelog.service.auth;

public interface PasswordResetNotificationService {

    void sendResetLink(String email, String resetLink);
}
