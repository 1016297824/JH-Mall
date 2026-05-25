package com.mall.user.controller.api;

import com.mall.api.enums.UserStatusEnum;

import com.mall.api.dto.MallUserDTO;
import com.mall.api.feign.RemoteUserService;
import com.mall.user.domain.MallUser;
import com.mall.user.service.IMallUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/inner/user")
public class RemoteUserInnerController {

    @Autowired
    private IMallUserService mallUserService;

    @GetMapping("/phone/{phone}")
    public MallUserDTO findByPhone(@PathVariable("phone") String phone) {
        MallUser user = mallUserService.selectByPhone(phone);
        if (user == null) {
            return null;
        }
        return toDTO(user);
    }

    @PostMapping("/register")
    public String register(@RequestBody RemoteUserService.RegisterRequest request) {
        return mallUserService.registerByPhone(
                request.getPhone(),
                request.getPhoneHash(),
                request.getPassword()
        );
    }

    @PutMapping("/{userId}/password")
    public void updatePassword(
            @PathVariable("userId") String userId,
            @RequestBody RemoteUserService.PasswordUpdateRequest request) {
        mallUserService.updatePasswordById(userId, request.getNewPassword());
    }

    @PutMapping("/{userId}/phone")
    public void updatePhone(
            @PathVariable("userId") String userId,
            @RequestBody RemoteUserService.PhoneUpdateRequest request) {
        mallUserService.updatePhoneById(userId, request.getNewPhone(), request.getNewPhoneHash());
    }

    @DeleteMapping("/{userId}/account")
    public void deactivateAccount(@PathVariable("userId") String userId) {
        mallUserService.updateUserStatusById(userId, String.valueOf(UserStatusEnum.DELETED.getCode()));
    }

    private MallUserDTO toDTO(MallUser user) {
        MallUserDTO dto = new MallUserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setPassword(null);
        dto.setPrivacyAgreed(user.getIsPrivacyAgreed());
        return dto;
    }
}
