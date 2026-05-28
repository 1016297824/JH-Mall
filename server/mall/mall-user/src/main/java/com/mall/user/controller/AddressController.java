package com.mall.user.controller;

import java.util.List;

import static com.mall.common.constant.HeaderConstants.X_USER_ID;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mall.common.DTO.MallResult;
import com.mall.user.service.IAddressService;
import com.mall.user.vo.AddressVO;

/**
 * C 端地址控制器
 *
 * @author JH-Mall
 * @date 2026/05/28
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AddressController {

    private final IAddressService addressService;

    /**
     * 查询地址列表
     *
     * @param request HTTP 请求
     * @return 地址列表
     */
    @GetMapping("/addresses")
    public MallResult<List<AddressVO>> list(HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(addressService.listAddresses(Long.parseLong(userId)));
    }

    /**
     * 新增地址
     *
     * @param addressVO 地址请求
     * @param request   HTTP 请求
     * @return 新增后的地址
     */
    @PostMapping("/addresses")
    public MallResult<AddressVO> add(@RequestBody AddressVO addressVO, HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(addressService.addAddress(Long.parseLong(userId), addressVO));
    }

    /**
     * 修改地址
     *
     * @param addressId 地址ID
     * @param addressVO 地址请求
     * @param request   HTTP 请求
     * @return 修改后的地址
     */
    @PutMapping("/addresses/{addressId}")
    public MallResult<AddressVO> update(@PathVariable Long addressId,
                                        @RequestBody AddressVO addressVO,
                                        HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        return MallResult.success(addressService.updateAddress(Long.parseLong(userId), addressId, addressVO));
    }

    /**
     * 删除地址
     *
     * @param addressId 地址ID
     * @param request   HTTP 请求
     * @return 空响应
     */
    @DeleteMapping("/addresses/{addressId}")
    public MallResult<Void> delete(@PathVariable Long addressId, HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        addressService.deleteAddress(Long.parseLong(userId), addressId);
        return MallResult.success(null);
    }

    /**
     * 设置默认地址
     *
     * @param addressId 地址ID
     * @param request   HTTP 请求
     * @return 空响应
     */
    @PutMapping("/addresses/{addressId}/default")
    public MallResult<Void> setDefault(@PathVariable Long addressId, HttpServletRequest request) {
        String userId = request.getHeader(X_USER_ID);
        addressService.setDefault(Long.parseLong(userId), addressId);
        return MallResult.success(null);
    }
}
