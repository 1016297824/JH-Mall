package com.mall.user.controller.inner;

import com.mall.api.feign.RemoteUserService;
import com.mall.common.DTO.user.response.MallUserDTO;
import com.mall.common.enums.user.UserStatusEnum;
import com.mall.user.DO.MallUserDO;
import com.mall.user.schedule.PointsExpireTask;
import com.mall.user.service.IMallUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * C 端用户内部接口（供 RemoteUserService Feign 和 ruoyi-job 调用）
 *
 * <p>无鉴权，仅限服务间内网调用，禁止暴露到网关</p>
 *
 * @author JH-Mall
 * @date 2026/05/26
 */
@Slf4j
@RestController
@RequestMapping("/inner/user")
@RequiredArgsConstructor
public class RemoteUserInnerController {

    /** 用户服务 */
    private final IMallUserService mallUserService;

    /** 积分过期定时任务 */
    private final PointsExpireTask pointsExpireTask;

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户 DTO（脱敏后），不存在则返回 null
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
     * 手机号注册
     *
     * @param request 注册请求（含手机号、手机号哈希、密码）
     * @return 新用户 ID
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
     * @param request 密码更新请求（含新密码）
     */
    @PutMapping("/{userId}/password")
    public void updatePassword(
            @PathVariable("userId") String userId,
            @RequestBody RemoteUserService.PasswordUpdateRequest request) {
        mallUserService.updatePasswordById(userId, request.getNewPassword());
    }

    /**
     * 换绑手机号
     *
     * @param userId  用户 ID
     * @param request 换绑请求（含新手机号、新手机号哈希）
     */
    @PutMapping("/{userId}/phone")
    public void updatePhone(
            @PathVariable("userId") String userId,
            @RequestBody RemoteUserService.PhoneUpdateRequest request) {
        mallUserService.updatePhoneById(userId, request.getNewPhone(), request.getNewPhoneHash());
    }

    /**
     * 注销账号（软删除）
     *
     * @param userId 用户 ID
     */
    @DeleteMapping("/{userId}/account")
    public void deactivateAccount(@PathVariable("userId") String userId) {
        mallUserService.updateUserStatusById(userId, String.valueOf(UserStatusEnum.DELETED.getCode()));
    }

    /**
     * 年度积分清零（ruoyi-job 调此端点）
     *
     * @return 共清零的积分总数
     */
    @PostMapping("/points/expire")
    public int expirePoints() {
        return pointsExpireTask.execute();
    }

    /**
     * DO 转 DTO，脱敏移除密码
     *
     * @param user 用户 DO
     * @return 用户 DTO（密码置空）
     */
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
