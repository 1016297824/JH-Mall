package com.mall.auth.service;

import com.mall.auth.dto.response.TokenResponse;

public interface TokenService {

    TokenResponse issue(String userId);

    String verify(String accessToken);

    TokenResponse refresh(String refreshToken);

    void revoke(String accessToken);

    void revokeAll(String userId);
}
