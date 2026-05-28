package com.mall.user.controller;

import com.mall.user.service.IAddressService;
import com.mall.user.vo.AddressVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AddressController 单元测试
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private IAddressService addressBookService;

    @InjectMocks
    private AddressController controller;

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    private static final String X_USER_ID = "X-User-Id";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        objectMapper = new ObjectMapper();
    }

    private AddressVO buildMockAddress() {
        AddressVO vo = new AddressVO();
        vo.setAddressId("100");
        vo.setReceiverName("张三");
        vo.setReceiverPhone("13800138000");
        vo.setProvince("广东省");
        vo.setCity("深圳市");
        vo.setDistrict("南山区");
        vo.setDetailAddress("科技园路100号");
        vo.setZipCode("518000");
        vo.setIsDefault(true);
        vo.setLabel("公司");
        return vo;
    }

    @Test
    void list_shouldReturnAddressList() throws Exception {
        AddressVO addr1 = buildMockAddress();
        AddressVO addr2 = new AddressVO();
        addr2.setAddressId("101");
        addr2.setReceiverName("李四");
        addr2.setReceiverPhone("13900139000");
        addr2.setProvince("北京市");
        addr2.setCity("北京市");
        addr2.setDistrict("朝阳区");
        addr2.setDetailAddress("望京路200号");
        addr2.setIsDefault(false);
        List<AddressVO> addressList = Arrays.asList(addr1, addr2);
        when(addressBookService.listAddresses(1L)).thenReturn(addressList);

        mockMvc.perform(get("/api/user/addresses")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].receiverName").value("张三"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true))
                .andExpect(jsonPath("$.data[1].receiverName").value("李四"))
                .andExpect(jsonPath("$.data[1].isDefault").value(false));
    }

    @Test
    void add_shouldReturnNewAddress() throws Exception {
        AddressVO request = buildMockAddress();
        AddressVO mockVO = buildMockAddress();
        mockVO.setAddressId("200");
        when(addressBookService.addAddress(eq(1L), any(AddressVO.class))).thenReturn(mockVO);

        mockMvc.perform(post("/api/user/addresses")
                        .header(X_USER_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.addressId").value("200"))
                .andExpect(jsonPath("$.data.receiverName").value("张三"));
    }

    @Test
    void update_shouldReturnUpdatedAddress() throws Exception {
        AddressVO request = buildMockAddress();
        AddressVO mockVO = buildMockAddress();
        mockVO.setReceiverName("张三改");
        when(addressBookService.updateAddress(eq(1L), eq(100L), any(AddressVO.class))).thenReturn(mockVO);

        mockMvc.perform(put("/api/user/addresses/100")
                        .header(X_USER_ID, "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.receiverName").value("张三改"));
    }

    @Test
    void delete_shouldReturnSuccess() throws Exception {
        doNothing().when(addressBookService).deleteAddress(1L, 100L);

        mockMvc.perform(delete("/api/user/addresses/100")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk());
    }

    @Test
    void setDefault_shouldReturnSuccess() throws Exception {
        doNothing().when(addressBookService).setDefault(1L, 100L);

        mockMvc.perform(put("/api/user/addresses/100/default")
                        .header(X_USER_ID, "1"))
                .andExpect(status().isOk());
    }
}
