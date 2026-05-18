package com.mall.user.controller;

import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.ruoyi.common.security.annotation.RequiresPermissions;
import com.mall.user.domain.MallUserAddress;
import com.mall.user.service.IMallUserAddressService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 地址簿Controller
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/address")
public class MallUserAddressController extends BaseController
{
    @Autowired
    private IMallUserAddressService mallUserAddressService;

    /**
     * 查询地址簿列表
     */
    @RequiresPermissions("mall-user:address:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserAddress mallUserAddress)
    {
        startPage();
        List<MallUserAddress> list = mallUserAddressService.selectMallUserAddressList(mallUserAddress);
        return getDataTable(list);
    }

    /**
     * 导出地址簿列表
     */
    @RequiresPermissions("mall-user:address:export")
    @Log(title = "地址簿", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserAddress mallUserAddress)
    {
        List<MallUserAddress> list = mallUserAddressService.selectMallUserAddressList(mallUserAddress);
        ExcelUtil<MallUserAddress> util = new ExcelUtil<MallUserAddress>(MallUserAddress.class);
        util.exportExcel(response, list, "地址簿数据");
    }

    /**
     * 获取地址簿详细信息
     */
    @RequiresPermissions("mall-user:address:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserAddressService.selectMallUserAddressById(id));
    }

    /**
     * 新增地址簿
     */
    @RequiresPermissions("mall-user:address:add")
    @Log(title = "地址簿", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserAddress mallUserAddress)
    {
        return toAjax(mallUserAddressService.insertMallUserAddress(mallUserAddress));
    }

    /**
     * 修改地址簿
     */
    @RequiresPermissions("mall-user:address:edit")
    @Log(title = "地址簿", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserAddress mallUserAddress)
    {
        return toAjax(mallUserAddressService.updateMallUserAddress(mallUserAddress));
    }

    /**
     * 删除地址簿
     */
    @RequiresPermissions("mall-user:address:remove")
    @Log(title = "地址簿", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserAddressService.deleteMallUserAddressByIds(ids));
    }
}
