package com.mall.common.DTO.user.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 用户凭据 DTO 测试
 *
 * <p>与 {@link MallUserDTO} 相反：MallUserDTO 的 password 是 WRITE_ONLY（Feign 传输后必然丢失），
 * 而本 DTO 的 passwordHash 必须能被序列化，否则 mall-auth 拿不到密码哈希，密码比对永远失败。</p>
 *
 * @author JH-Mall
 * @date 2026/09/27
 */
class UserCredentialDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSerializePasswordHash() throws Exception {
        UserCredentialDTO dto = new UserCredentialDTO("1", "$2a$12$abcdefghijklmnopqrstuv");

        String json = objectMapper.writeValueAsString(dto);

        assertTrue(json.contains("passwordHash"), "凭据 DTO 必须序列化 passwordHash 字段");
        assertTrue(json.contains("$2a$12$abcdefghijklmnopqrstuv"), "密码哈希必须能通过 Feign 传输");
    }

    @Test
    void shouldDeserializePasswordHash() throws Exception {
        String json = "{\"userId\":\"1\",\"passwordHash\":\"$2a$12$hash\"}";

        UserCredentialDTO dto = objectMapper.readValue(json, UserCredentialDTO.class);

        assertEquals("1", dto.getUserId());
        assertEquals("$2a$12$hash", dto.getPasswordHash());
    }

    @Test
    void shouldMaskPasswordHashInToString() {
        UserCredentialDTO dto = new UserCredentialDTO("1", "$2a$12$secretHash");

        String text = dto.toString();

        assertFalse(text.contains("$2a$12$secretHash"), "toString 不得输出密码哈希明文");
        assertTrue(text.contains("***"), "应以掩码替代密码哈希");
    }

}
