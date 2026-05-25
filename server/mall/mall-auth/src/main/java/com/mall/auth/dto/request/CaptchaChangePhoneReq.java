package com.mall.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CaptchaChangePhoneReq {

    @NotBlank(message = "旧手机号不能为空")
    private String oldPhone;

    @NotBlank(message = "密码不能为空")
    private String password;

    @NotBlank(message = "新手机号不能为空")
    private String newPhone;

    public String getOldPhone() {
        return oldPhone;
    }

    public void setOldPhone(String oldPhone) {
        this.oldPhone = oldPhone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNewPhone() {
        return newPhone;
    }

    public void setNewPhone(String newPhone) {
        this.newPhone = newPhone;
    }
}
