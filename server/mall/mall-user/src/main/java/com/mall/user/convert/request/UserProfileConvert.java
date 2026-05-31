package com.mall.user.convert.request;

import com.mall.user.DO.MallUserDO;
import com.mall.user.DTO.request.UpdateProfileDTO;

import java.time.LocalDateTime;

/**
 * 用户资料 Request → DO 静态转换器
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class UserProfileConvert {

    private UserProfileConvert() {
    }

    /**
     * 将请求中非 null 字段覆盖写入用户 DO（partial update）
     *
     * @param request 修改请求
     * @param user    目标用户 DO
     */
    public static void merge(UpdateProfileDTO request, MallUserDO user) {
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }
        if (request.getBirthday() != null) {
            LocalDateTime parsed = parseBirthday(request.getBirthday());
            if (parsed != null) {
                user.setBirthday(parsed);
            }
        }
    }

    /**
     * 将生日字符串解析为 LocalDateTime
     *
     * @param birthday 生日字符串，格式 yyyy-MM-dd
     * @return 解析后的 LocalDateTime，解析失败返回 null
     */
    private static LocalDateTime parseBirthday(String birthday) {
        try {
            return LocalDateTime.parse(birthday + "T00:00:00");
        } catch (Exception e) {
            return null;
        }
    }
}
