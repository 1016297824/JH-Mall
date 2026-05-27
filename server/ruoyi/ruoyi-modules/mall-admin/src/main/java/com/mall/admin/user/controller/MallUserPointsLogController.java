package com.mall.admin.user.controller;

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
import com.mall.admin.user.domain.MallUserPointsLog;
import com.mall.admin.user.service.IMallUserPointsLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 积分流水Controller
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/points_log")
public class MallUserPointsLogController extends BaseController
{
    @Autowired
    private IMallUserPointsLogService mallUserPointsLogService;

    /**
     * 查询积分流水列表
     */
    @RequiresPermissions("mall-user:points_log:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserPointsLog mallUserPointsLog)
    {
        startPage();
        List<MallUserPointsLog> list = mallUserPointsLogService.selectMallUserPointsLogList(mallUserPointsLog);
        return getDataTable(list);
    }

    /**
     * 导出积分流水列表
     */
    @RequiresPermissions("mall-user:points_log:export")
    @Log(title = "积分流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserPointsLog mallUserPointsLog)
    {
        List<MallUserPointsLog> list = mallUserPointsLogService.selectMallUserPointsLogList(mallUserPointsLog);
        ExcelUtil<MallUserPointsLog> util = new ExcelUtil<MallUserPointsLog>(MallUserPointsLog.class);
        util.exportExcel(response, list, "积分流水数据");
    }

    /**
     * 获取积分流水详细信息
     */
    @RequiresPermissions("mall-user:points_log:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserPointsLogService.selectMallUserPointsLogById(id));
    }

    /**
     * 新增积分流水
     */
    @RequiresPermissions("mall-user:points_log:add")
    @Log(title = "积分流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserPointsLog mallUserPointsLog)
    {
        return toAjax(mallUserPointsLogService.insertMallUserPointsLog(mallUserPointsLog));
    }

    /**
     * 修改积分流水
     */
    @RequiresPermissions("mall-user:points_log:edit")
    @Log(title = "积分流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserPointsLog mallUserPointsLog)
    {
        return toAjax(mallUserPointsLogService.updateMallUserPointsLog(mallUserPointsLog));
    }

    /**
     * 删除积分流水
     */
    @RequiresPermissions("mall-user:points_log:remove")
    @Log(title = "积分流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserPointsLogService.deleteMallUserPointsLogByIds(ids));
    }
}
