package com.mall.common.DTO.user.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MallUserDTOTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldSetAndGetAllFields() {
        MallUserDTO dto = new MallUserDTO();
        dto.setId("1");
        dto.setPhone("13800138000");
        dto.setPhoneHash("hash123");
        dto.setPassword("secret");
        dto.setNickname("张三");
        dto.setAvatar("http://example.com/avatar.png");
        dto.setEmail("test@example.com");
        dto.setEmailHash("emailhash");
        dto.setGender("M");
        dto.setUserStatus("normal");
        dto.setRegisterType("phone");
        dto.setRegisterIp("127.0.0.1");
        dto.setPrivacyAgreed("Y");

        assertEquals("1", dto.getId());
        assertEquals("13800138000", dto.getPhone());
        assertEquals("hash123", dto.getPhoneHash());
        assertEquals("secret", dto.getPassword());
        assertEquals("张三", dto.getNickname());
        assertEquals("http://example.com/avatar.png", dto.getAvatar());
        assertEquals("test@example.com", dto.getEmail());
        assertEquals("emailhash", dto.getEmailHash());
        assertEquals("M", dto.getGender());
        assertEquals("normal", dto.getUserStatus());
        assertEquals("phone", dto.getRegisterType());
        assertEquals("127.0.0.1", dto.getRegisterIp());
        assertEquals("Y", dto.getPrivacyAgreed());
    }

    @Test
    void shouldOmitPasswordInJsonSerialization() throws Exception {
        MallUserDTO dto = new MallUserDTO();
        dto.setPassword("secret123");
        String json = objectMapper.writeValueAsString(dto);

        assertFalse(json.contains("secret123"));
        assertFalse(json.contains("password"));
    }

    @Test
    void shouldDeserializePasswordFromJson() throws Exception {
        String json = "{\"password\":\"myPassword\"}";
        MallUserDTO dto = objectMapper.readValue(json, MallUserDTO.class);

        assertEquals("myPassword", dto.getPassword());
    }

    @Test
    void shouldHaveDefaultConstructor() {
        MallUserDTO dto = new MallUserDTO();
        assertNotNull(dto);
    }

}
