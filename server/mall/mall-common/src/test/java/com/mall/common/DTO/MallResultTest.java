package com.mall.common.DTO;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MallResultTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldCreateSuccessResult() {
        MallResult<String> result = MallResult.success("hello");
        assertEquals("00000", result.getErrorCode());
        assertEquals("操作成功", result.getErrorMessage());
        assertNull(result.getUserTip());
        assertEquals("hello", result.getData());
        assertNull(result.getRequestId());
    }

    @Test
    void shouldCreateSuccessResultWithNullData() {
        MallResult<String> result = MallResult.success(null);
        assertEquals("00000", result.getErrorCode());
        assertEquals("操作成功", result.getErrorMessage());
        assertNull(result.getData());
    }

    @Test
    void shouldCreateErrorResultWithoutUserTip() {
        MallResult<Void> result = MallResult.error("A0001", "验证码错误");
        assertEquals("A0001", result.getErrorCode());
        assertEquals("验证码错误", result.getErrorMessage());
        assertNull(result.getUserTip());
        assertNull(result.getData());
    }

    @Test
    void shouldCreateErrorResultWithUserTip() {
        MallResult<Void> result = MallResult.error("A0001", "验证码错误", "请重新输入验证码");
        assertEquals("A0001", result.getErrorCode());
        assertEquals("验证码错误", result.getErrorMessage());
        assertEquals("请重新输入验证码", result.getUserTip());
    }

    @Test
    void shouldSetAndGetRequestId() {
        MallResult<String> result = MallResult.success("data");
        result.setRequestId("req-123");
        assertEquals("req-123", result.getRequestId());
    }

    @Test
    void shouldSetAndGetAllFields() {
        MallResult<String> result = new MallResult<>();
        result.setErrorCode("E001");
        result.setErrorMessage("错误消息");
        result.setUserTip("提示");
        result.setData("数据");
        result.setRequestId("rid-1");

        assertEquals("E001", result.getErrorCode());
        assertEquals("错误消息", result.getErrorMessage());
        assertEquals("提示", result.getUserTip());
        assertEquals("数据", result.getData());
        assertEquals("rid-1", result.getRequestId());
    }

    @Test
    void shouldOmitNullFieldsInJson() throws Exception {
        MallResult<String> result = MallResult.success("data");
        String json = objectMapper.writeValueAsString(result);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.has("errorCode"));
        assertTrue(node.has("errorMessage"));
        assertTrue(node.has("data"));
        assertFalse(node.has("userTip"));
        assertFalse(node.has("requestId"));
    }

    @Test
    void shouldIncludeNonNullRequestIdInJson() throws Exception {
        MallResult<String> result = MallResult.success("data");
        result.setRequestId("req-123");
        String json = objectMapper.writeValueAsString(result);
        JsonNode node = objectMapper.readTree(json);

        assertTrue(node.has("requestId"));
        assertEquals("req-123", node.get("requestId").asText());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldDeserializeFromJson() throws Exception {
        String json = "{\"errorCode\":\"E001\",\"errorMessage\":\"失败\",\"data\":\"value\",\"requestId\":\"r1\"}";
        MallResult<String> result = objectMapper.readValue(json, MallResult.class);

        assertEquals("E001", result.getErrorCode());
        assertEquals("失败", result.getErrorMessage());
        assertEquals("value", result.getData());
        assertEquals("r1", result.getRequestId());
    }

}
