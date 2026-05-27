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
import com.mall.user.DO.MallUser;
import com.mall.user.service.IMallUserService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 用户账号Controller
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/user")
public class MallUserController extends BaseController
{
    @Autowired
    private IMallUserService mallUserService;

    /**
     * 查询用户账号列表
     */
    @RequiresPermissions("mall-user:user:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUser mallUser)
    {
        startPage();
        List<MallUser> list = mallUserService.selectMallUserList(mallUser);
        return getDataTable(list);
    }

    /**
     * 导出用户账号列表
     */
    @RequiresPermissions("mall-user:user:export")
    @Log(title = "用户账号", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUser mallUser)
    {
        List<MallUser> list = mallUserService.selectMallUserList(mallUser);
        ExcelUtil<MallUser> util = new ExcelUtil<MallUser>(MallUser.class);
        util.exportExcel(response, list, "用户账号数据");
    }

    /**
     * 获取用户账号详细信息
     */
    @RequiresPermissions("mall-user:user:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserService.selectMallUserById(id));
    }

    /**
     * 新增用户账号
     */
    @RequiresPermissions("mall-user:user:add")
    @Log(title = "用户账号", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUser mallUser)
    {
        return toAjax(mallUserService.insertMallUser(mallUser));
    }

    /**
     * 修改用户账号
     */
    @RequiresPermissions("mall-user:user:edit")
    @Log(title = "用户账号", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUser mallUser)
    {
        return toAjax(mallUserService.updateMallUser(mallUser));
    }

    /**
     * 删除用户账号
     */
    @RequiresPermissions("mall-user:user:remove")
    @Log(title = "用户账号", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserService.deleteMallUserByIds(ids));
    }
}
