package com.mall.auth.service;

public interface AuthService {

    void register(String phone, String password);

    String loginByPassword(String phone, String password);

    String loginBySms(String phone, String smsCode);

    String refresh(String refreshToken);

    void logout(String accessToken);

    void resetPassword(String phone, String newPassword);

    void changePassword(String userId, String oldPassword, String newPassword);
}
