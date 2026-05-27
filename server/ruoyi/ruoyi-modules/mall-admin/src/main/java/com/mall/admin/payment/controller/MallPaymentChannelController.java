package com.mall.admin.payment.controller;

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
import com.mall.admin.payment.domain.MallPaymentChannel;
import com.mall.admin.payment.service.IMallPaymentChannelService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 支付渠道Controller
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/channel")
public class MallPaymentChannelController extends BaseController
{
    @Autowired
    private IMallPaymentChannelService mallPaymentChannelService;

    /**
     * 查询支付渠道列表
     */
    @RequiresPermissions("mall-payment:channel:list")
    @GetMapping("/list")
    public TableDataInfo list(MallPaymentChannel mallPaymentChannel)
    {
        startPage();
        List<MallPaymentChannel> list = mallPaymentChannelService.selectMallPaymentChannelList(mallPaymentChannel);
        return getDataTable(list);
    }

    /**
     * 导出支付渠道列表
     */
    @RequiresPermissions("mall-payment:channel:export")
    @Log(title = "支付渠道", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallPaymentChannel mallPaymentChannel)
    {
        List<MallPaymentChannel> list = mallPaymentChannelService.selectMallPaymentChannelList(mallPaymentChannel);
        ExcelUtil<MallPaymentChannel> util = new ExcelUtil<MallPaymentChannel>(MallPaymentChannel.class);
        util.exportExcel(response, list, "支付渠道数据");
    }

    /**
     * 获取支付渠道详细信息
     */
    @RequiresPermissions("mall-payment:channel:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallPaymentChannelService.selectMallPaymentChannelById(id));
    }

    /**
     * 新增支付渠道
     */
    @RequiresPermissions("mall-payment:channel:add")
    @Log(title = "支付渠道", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallPaymentChannel mallPaymentChannel)
    {
        return toAjax(mallPaymentChannelService.insertMallPaymentChannel(mallPaymentChannel));
    }

    /**
     * 修改支付渠道
     */
    @RequiresPermissions("mall-payment:channel:edit")
    @Log(title = "支付渠道", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallPaymentChannel mallPaymentChannel)
    {
        return toAjax(mallPaymentChannelService.updateMallPaymentChannel(mallPaymentChannel));
    }

    /**
     * 删除支付渠道
     */
    @RequiresPermissions("mall-payment:channel:remove")
    @Log(title = "支付渠道", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallPaymentChannelService.deleteMallPaymentChannelByIds(ids));
    }
}
