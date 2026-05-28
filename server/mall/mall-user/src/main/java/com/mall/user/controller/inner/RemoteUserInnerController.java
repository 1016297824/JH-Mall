package com.mall.user.controller.inner;

import com.mall.api.feign.RemoteUserService;
import com.mall.common.DTO.user.response.MallUserDTO;
import com.mall.common.enums.user.UserStatusEnum;
import com.mall.user.DO.MallUserDO;
import com.mall.user.service.IMallUserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

/**
 * C 端用户内部接口（供 RemoteUserService Feign 调用）
 *
 * <p>无鉴权，仅限服务间内网调用，禁止暴露到网关</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@RestController
@RequestMapping("/inner/user")
public class RemoteUserInnerController {

    private final IMallUserService mallUserService;

    public RemoteUserInnerController(IMallUserService mallUserService) {
        this.mallUserService = mallUserService;
    }

    @GetMapping("/phone/{phone}")
    public MallUserDTO findByPhone(@PathVariable("phone") String phone) {
        MallUserDO user = mallUserService.selectByPhone(phone);
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

    private MallUserDTO toDTO(MallUserDO user) {
        MallUserDTO dto = new MallUserDTO();
        BeanUtils.copyProperties(user, dto);
        dto.setId(String.valueOf(user.getId()));
        dto.setGender(String.valueOf(user.getGender()));
        dto.setUserStatus(String.valueOf(user.getUserStatus()));
        dto.setPassword(null);
        dto.setPrivacyAgreed(String.valueOf(user.getIsPrivacyAgreed()));
        return dto;
    }
}
