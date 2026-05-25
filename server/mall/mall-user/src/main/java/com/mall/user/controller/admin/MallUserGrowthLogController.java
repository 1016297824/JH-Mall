package com.mall.user.controller.admin;

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
import com.mall.user.domain.MallUserGrowthLog;
import com.mall.user.service.IMallUserGrowthLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 成长值流水Controller
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/growth_log")
public class MallUserGrowthLogController extends BaseController
{
    @Autowired
    private IMallUserGrowthLogService mallUserGrowthLogService;

    /**
     * 查询成长值流水列表
     */
    @RequiresPermissions("mall-user:growth_log:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserGrowthLog mallUserGrowthLog)
    {
        startPage();
        List<MallUserGrowthLog> list = mallUserGrowthLogService.selectMallUserGrowthLogList(mallUserGrowthLog);
        return getDataTable(list);
    }

    /**
     * 导出成长值流水列表
     */
    @RequiresPermissions("mall-user:growth_log:export")
    @Log(title = "成长值流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserGrowthLog mallUserGrowthLog)
    {
        List<MallUserGrowthLog> list = mallUserGrowthLogService.selectMallUserGrowthLogList(mallUserGrowthLog);
        ExcelUtil<MallUserGrowthLog> util = new ExcelUtil<MallUserGrowthLog>(MallUserGrowthLog.class);
        util.exportExcel(response, list, "成长值流水数据");
    }

    /**
     * 获取成长值流水详细信息
     */
    @RequiresPermissions("mall-user:growth_log:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserGrowthLogService.selectMallUserGrowthLogById(id));
    }

    /**
     * 新增成长值流水
     */
    @RequiresPermissions("mall-user:growth_log:add")
    @Log(title = "成长值流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserGrowthLog mallUserGrowthLog)
    {
        return toAjax(mallUserGrowthLogService.insertMallUserGrowthLog(mallUserGrowthLog));
    }

    /**
     * 修改成长值流水
     */
    @RequiresPermissions("mall-user:growth_log:edit")
    @Log(title = "成长值流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserGrowthLog mallUserGrowthLog)
    {
        return toAjax(mallUserGrowthLogService.updateMallUserGrowthLog(mallUserGrowthLog));
    }

    /**
     * 删除成长值流水
     */
    @RequiresPermissions("mall-user:growth_log:remove")
    @Log(title = "成长值流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserGrowthLogService.deleteMallUserGrowthLogByIds(ids));
    }
}
