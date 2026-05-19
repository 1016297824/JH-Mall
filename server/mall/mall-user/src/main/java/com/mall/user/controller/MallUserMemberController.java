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
import com.mall.user.domain.MallUserMember;
import com.mall.user.service.IMallUserMemberService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 用户会员信息Controller
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/member")
public class MallUserMemberController extends BaseController
{
    @Autowired
    private IMallUserMemberService mallUserMemberService;

    /**
     * 查询用户会员信息列表
     */
    @RequiresPermissions("mall-user:member:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserMember mallUserMember)
    {
        startPage();
        List<MallUserMember> list = mallUserMemberService.selectMallUserMemberList(mallUserMember);
        return getDataTable(list);
    }

    /**
     * 导出用户会员信息列表
     */
    @RequiresPermissions("mall-user:member:export")
    @Log(title = "用户会员信息", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserMember mallUserMember)
    {
        List<MallUserMember> list = mallUserMemberService.selectMallUserMemberList(mallUserMember);
        ExcelUtil<MallUserMember> util = new ExcelUtil<MallUserMember>(MallUserMember.class);
        util.exportExcel(response, list, "用户会员信息数据");
    }

    /**
     * 获取用户会员信息详细信息
     */
    @RequiresPermissions("mall-user:member:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserMemberService.selectMallUserMemberById(id));
    }

    /**
     * 新增用户会员信息
     */
    @RequiresPermissions("mall-user:member:add")
    @Log(title = "用户会员信息", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserMember mallUserMember)
    {
        return toAjax(mallUserMemberService.insertMallUserMember(mallUserMember));
    }

    /**
     * 修改用户会员信息
     */
    @RequiresPermissions("mall-user:member:edit")
    @Log(title = "用户会员信息", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserMember mallUserMember)
    {
        return toAjax(mallUserMemberService.updateMallUserMember(mallUserMember));
    }

    /**
     * 删除用户会员信息
     */
    @RequiresPermissions("mall-user:member:remove")
    @Log(title = "用户会员信息", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserMemberService.deleteMallUserMemberByIds(ids));
    }
}
