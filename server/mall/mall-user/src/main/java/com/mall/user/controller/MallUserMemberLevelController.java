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
import com.mall.user.domain.MallUserMemberLevel;
import com.mall.user.service.IMallUserMemberLevelService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 会员等级定义Controller
 * 
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/level")
public class MallUserMemberLevelController extends BaseController
{
    @Autowired
    private IMallUserMemberLevelService mallUserMemberLevelService;

    /**
     * 查询会员等级定义列表
     */
    @RequiresPermissions("mall-user:level:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserMemberLevel mallUserMemberLevel)
    {
        startPage();
        List<MallUserMemberLevel> list = mallUserMemberLevelService.selectMallUserMemberLevelList(mallUserMemberLevel);
        return getDataTable(list);
    }

    /**
     * 导出会员等级定义列表
     */
    @RequiresPermissions("mall-user:level:export")
    @Log(title = "会员等级定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserMemberLevel mallUserMemberLevel)
    {
        List<MallUserMemberLevel> list = mallUserMemberLevelService.selectMallUserMemberLevelList(mallUserMemberLevel);
        ExcelUtil<MallUserMemberLevel> util = new ExcelUtil<MallUserMemberLevel>(MallUserMemberLevel.class);
        util.exportExcel(response, list, "会员等级定义数据");
    }

    /**
     * 获取会员等级定义详细信息
     */
    @RequiresPermissions("mall-user:level:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserMemberLevelService.selectMallUserMemberLevelById(id));
    }

    /**
     * 新增会员等级定义
     */
    @RequiresPermissions("mall-user:level:add")
    @Log(title = "会员等级定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserMemberLevel mallUserMemberLevel)
    {
        return toAjax(mallUserMemberLevelService.insertMallUserMemberLevel(mallUserMemberLevel));
    }

    /**
     * 修改会员等级定义
     */
    @RequiresPermissions("mall-user:level:edit")
    @Log(title = "会员等级定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserMemberLevel mallUserMemberLevel)
    {
        return toAjax(mallUserMemberLevelService.updateMallUserMemberLevel(mallUserMemberLevel));
    }

    /**
     * 删除会员等级定义
     */
    @RequiresPermissions("mall-user:level:remove")
    @Log(title = "会员等级定义", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserMemberLevelService.deleteMallUserMemberLevelByIds(ids));
    }
}
