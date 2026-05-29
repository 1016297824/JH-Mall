package com.mall.user.convert;

import com.mall.user.DO.MallUserDO;
import com.mall.user.DTO.request.UpdateProfileRequest;
import com.mall.user.convert.request.UserProfileConvert;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class UserProfileRequestConvertTest {

    @Test
    void mergeShouldCopyNonNullFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");
        request.setAvatar("https://img.example.com/new.jpg");
        request.setGender(2);
        request.setBirthday("1995-06-15");
        request.setEmail("new@example.com");

        MallUserDO user = new MallUserDO();
        UserProfileConvert.merge(request, user);

        assertEquals("新昵称", user.getNickname());
        assertEquals("https://img.example.com/new.jpg", user.getAvatar());
        assertEquals(2, user.getGender());
        assertNotNull(user.getBirthday());
        assertEquals(1995, user.getBirthday().getYear());
        assertEquals(6, user.getBirthday().getMonthValue());
        assertEquals(15, user.getBirthday().getDayOfMonth());
        assertEquals("new@example.com", user.getEmail());
    }

    @Test
    void mergeShouldNotOverwriteNullFields() {
        MallUserDO user = new MallUserDO();
        user.setNickname("旧昵称");
        user.setAvatar("https://old.jpg");
        user.setGender(1);
        user.setEmail("old@example.com");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setNickname("新昵称");

        UserProfileConvert.merge(request, user);

        assertEquals("新昵称", user.getNickname());
        assertEquals("https://old.jpg", user.getAvatar());
        assertEquals(1, user.getGender());
        assertEquals("old@example.com", user.getEmail());
        assertNull(user.getBirthday());
    }

    @Test
    void mergeShouldHandleNullBirthdayString() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setBirthday("bad-date");

        MallUserDO user = new MallUserDO();
        user.setBirthday(LocalDateTime.of(2000, 1, 1, 0, 0));

        UserProfileConvert.merge(request, user);

        assertEquals(2000, user.getBirthday().getYear());
    }

    @Test
    void mergeShouldHandleEmptyRequest() {
        MallUserDO user = new MallUserDO();
        user.setNickname("旧昵称");

        UpdateProfileRequest request = new UpdateProfileRequest();
        UserProfileConvert.merge(request, user);

        assertEquals("旧昵称", user.getNickname());
    }
}
