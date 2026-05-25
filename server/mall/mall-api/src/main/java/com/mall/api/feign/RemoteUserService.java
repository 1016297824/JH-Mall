package com.mall.api.feign;

import com.mall.common.dto.user.MallUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(contextId = "remoteUserService", value = "mall-user")
public interface RemoteUserService {

    @GetMapping("/inner/user/phone/{phone}")
    MallUserDTO findByPhone(@PathVariable("phone") String phone);

    @PostMapping("/inner/user/register")
    String register(@RequestBody RegisterRequest request);

    @PutMapping("/inner/user/{userId}/password")
    void updatePassword(@PathVariable("userId") String userId, @RequestBody PasswordUpdateRequest request);

    @PutMapping("/inner/user/{userId}/phone")
    void updatePhone(@PathVariable("userId") String userId, @RequestBody PhoneUpdateRequest request);

    @DeleteMapping("/inner/user/{userId}/account")
    void deactivateAccount(@PathVariable("userId") String userId);

    class RegisterRequest {

        private String phone;
        private String phoneHash;
        private String password;
        private String registerType;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getPhoneHash() {
            return phoneHash;
        }

        public void setPhoneHash(String phoneHash) {
            this.phoneHash = phoneHash;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRegisterType() {
            return registerType;
        }

        public void setRegisterType(String registerType) {
            this.registerType = registerType;
        }

    }

    class PasswordUpdateRequest {

        private String newPassword;

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }

    }

    class PhoneUpdateRequest {

        private String newPhone;
        private String newPhoneHash;

        public String getNewPhone() {
            return newPhone;
        }

        public void setNewPhone(String newPhone) {
            this.newPhone = newPhone;
        }

        public String getNewPhoneHash() {
            return newPhoneHash;
        }

        public void setNewPhoneHash(String newPhoneHash) {
            this.newPhoneHash = newPhoneHash;
        }

    }

}
