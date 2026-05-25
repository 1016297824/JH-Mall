package com.mall.payment.controller.admin;

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
import com.mall.payment.domain.MallPayment;
import com.mall.payment.service.IMallPaymentService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 支付单Controller
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/payment")
public class MallPaymentController extends BaseController
{
    @Autowired
    private IMallPaymentService mallPaymentService;

    /**
     * 查询支付单列表
     */
    @RequiresPermissions("mall-payment:payment:list")
    @GetMapping("/list")
    public TableDataInfo list(MallPayment mallPayment)
    {
        startPage();
        List<MallPayment> list = mallPaymentService.selectMallPaymentList(mallPayment);
        return getDataTable(list);
    }

    /**
     * 导出支付单列表
     */
    @RequiresPermissions("mall-payment:payment:export")
    @Log(title = "支付单", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallPayment mallPayment)
    {
        List<MallPayment> list = mallPaymentService.selectMallPaymentList(mallPayment);
        ExcelUtil<MallPayment> util = new ExcelUtil<MallPayment>(MallPayment.class);
        util.exportExcel(response, list, "支付单数据");
    }

    /**
     * 获取支付单详细信息
     */
    @RequiresPermissions("mall-payment:payment:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallPaymentService.selectMallPaymentById(id));
    }

    /**
     * 新增支付单
     */
    @RequiresPermissions("mall-payment:payment:add")
    @Log(title = "支付单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallPayment mallPayment)
    {
        return toAjax(mallPaymentService.insertMallPayment(mallPayment));
    }

    /**
     * 修改支付单
     */
    @RequiresPermissions("mall-payment:payment:edit")
    @Log(title = "支付单", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallPayment mallPayment)
    {
        return toAjax(mallPaymentService.updateMallPayment(mallPayment));
    }

    /**
     * 删除支付单
     */
    @RequiresPermissions("mall-payment:payment:remove")
    @Log(title = "支付单", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallPaymentService.deleteMallPaymentByIds(ids));
    }
}
