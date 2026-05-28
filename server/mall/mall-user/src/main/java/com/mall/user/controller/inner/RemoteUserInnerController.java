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

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户 DTO，不存在时返回 null
     */
    @GetMapping("/phone/{phone}")
    public MallUserDTO findByPhone(@PathVariable("phone") String phone) {
        MallUserDO user = mallUserService.selectByPhone(phone);
        if (user == null) {
            return null;
        }
        return toDTO(user);
    }

    /**
     * 注册新用户
     *
     * @param request 注册请求（含手机号、手机号哈希和密码）
     * @return 新用户 userId
     */
    @PostMapping("/register")
    public String register(@RequestBody RemoteUserService.RegisterRequest request) {
        return mallUserService.registerByPhone(
                request.getPhone(),
                request.getPhoneHash(),
                request.getPassword()
        );
    }

    /**
     * 修改密码
     *
     * @param userId  用户 ID
     * @param request 密码更新请求
     */
    @PutMapping("/{userId}/password")
    public void updatePassword(
            @PathVariable("userId") String userId,
            @RequestBody RemoteUserService.PasswordUpdateRequest request) {
        mallUserService.updatePasswordById(userId, request.getNewPassword());
    }

    /**
     * 修改手机号
     *
     * @param userId  用户 ID
     * @param request 手机号更新请求
     */
    @PutMapping("/{userId}/phone")
    public void updatePhone(
            @PathVariable("userId") String userId,
            @RequestBody RemoteUserService.PhoneUpdateRequest request) {
        mallUserService.updatePhoneById(userId, request.getNewPhone(), request.getNewPhoneHash());
    }

    /**
     * 注销用户账号
     *
     * @param userId 用户 ID
     */
    @DeleteMapping("/{userId}/account")
    public void deactivateAccount(@PathVariable("userId") String userId) {
        mallUserService.updateUserStatusById(userId, String.valueOf(UserStatusEnum.DELETED.getCode()));
    }

    // DO 转 DTO，脱敏移除密码
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
