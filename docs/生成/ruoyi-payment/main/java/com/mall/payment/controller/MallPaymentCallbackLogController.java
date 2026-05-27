package com.mall.payment.controller;

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
import com.mall.payment.DO.MallPaymentCallbackLog;
import com.mall.payment.service.IMallPaymentCallbackLogService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 回调日志Controller
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/log")
public class MallPaymentCallbackLogController extends BaseController
{
    @Autowired
    private IMallPaymentCallbackLogService mallPaymentCallbackLogService;

    /**
     * 查询回调日志列表
     */
    @RequiresPermissions("mall-payment:log:list")
    @GetMapping("/list")
    public TableDataInfo list(MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        startPage();
        List<MallPaymentCallbackLog> list = mallPaymentCallbackLogService.selectMallPaymentCallbackLogList(mallPaymentCallbackLog);
        return getDataTable(list);
    }

    /**
     * 导出回调日志列表
     */
    @RequiresPermissions("mall-payment:log:export")
    @Log(title = "回调日志", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        List<MallPaymentCallbackLog> list = mallPaymentCallbackLogService.selectMallPaymentCallbackLogList(mallPaymentCallbackLog);
        ExcelUtil<MallPaymentCallbackLog> util = new ExcelUtil<MallPaymentCallbackLog>(MallPaymentCallbackLog.class);
        util.exportExcel(response, list, "回调日志数据");
    }

    /**
     * 获取回调日志详细信息
     */
    @RequiresPermissions("mall-payment:log:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallPaymentCallbackLogService.selectMallPaymentCallbackLogById(id));
    }

    /**
     * 新增回调日志
     */
    @RequiresPermissions("mall-payment:log:add")
    @Log(title = "回调日志", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        return toAjax(mallPaymentCallbackLogService.insertMallPaymentCallbackLog(mallPaymentCallbackLog));
    }

    /**
     * 修改回调日志
     */
    @RequiresPermissions("mall-payment:log:edit")
    @Log(title = "回调日志", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallPaymentCallbackLog mallPaymentCallbackLog)
    {
        return toAjax(mallPaymentCallbackLogService.updateMallPaymentCallbackLog(mallPaymentCallbackLog));
    }

    /**
     * 删除回调日志
     */
    @RequiresPermissions("mall-payment:log:remove")
    @Log(title = "回调日志", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallPaymentCallbackLogService.deleteMallPaymentCallbackLogByIds(ids));
    }
}
