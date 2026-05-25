package com.mall.auth.service;

public interface SmsService {

    void send(String phone, String code);
}
