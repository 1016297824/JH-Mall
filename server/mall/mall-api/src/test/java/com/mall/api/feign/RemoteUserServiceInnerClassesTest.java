package com.mall.api.feign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RemoteUserServiceInnerClassesTest {

    @Test
    void shouldSetAndGetRegisterRequest() {
        RemoteUserService.RegisterRequest request = new RemoteUserService.RegisterRequest();
        request.setPhone("13800138000");
        request.setPhoneHash("hash456");
        request.setPassword("pass789");
        request.setRegisterType("phone");

        assertEquals("13800138000", request.getPhone());
        assertEquals("hash456", request.getPhoneHash());
        assertEquals("pass789", request.getPassword());
        assertEquals("phone", request.getRegisterType());
    }

    @Test
    void shouldSetAndGetPasswordUpdateRequest() {
        RemoteUserService.PasswordUpdateRequest request = new RemoteUserService.PasswordUpdateRequest();
        request.setNewPassword("newPass123");

        assertEquals("newPass123", request.getNewPassword());
    }

    @Test
    void shouldSetAndGetPhoneUpdateRequest() {
        RemoteUserService.PhoneUpdateRequest request = new RemoteUserService.PhoneUpdateRequest();
        request.setNewPhone("13900139000");
        request.setNewPhoneHash("newHash789");

        assertEquals("13900139000", request.getNewPhone());
        assertEquals("newHash789", request.getNewPhoneHash());
    }

}
