package com.mall.marketing.controller.admin;

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
import com.mall.marketing.domain.MallMarketingCoupon;
import com.mall.marketing.service.IMallMarketingCouponService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 优惠券定义Controller
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/coupon")
public class MallMarketingCouponController extends BaseController
{
    @Autowired
    private IMallMarketingCouponService mallMarketingCouponService;

    /**
     * 查询优惠券定义列表
     */
    @RequiresPermissions("mall-marketing:coupon:list")
    @GetMapping("/list")
    public TableDataInfo list(MallMarketingCoupon mallMarketingCoupon)
    {
        startPage();
        List<MallMarketingCoupon> list = mallMarketingCouponService.selectMallMarketingCouponList(mallMarketingCoupon);
        return getDataTable(list);
    }

    /**
     * 导出优惠券定义列表
     */
    @RequiresPermissions("mall-marketing:coupon:export")
    @Log(title = "优惠券定义", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallMarketingCoupon mallMarketingCoupon)
    {
        List<MallMarketingCoupon> list = mallMarketingCouponService.selectMallMarketingCouponList(mallMarketingCoupon);
        ExcelUtil<MallMarketingCoupon> util = new ExcelUtil<MallMarketingCoupon>(MallMarketingCoupon.class);
        util.exportExcel(response, list, "优惠券定义数据");
    }

    /**
     * 获取优惠券定义详细信息
     */
    @RequiresPermissions("mall-marketing:coupon:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallMarketingCouponService.selectMallMarketingCouponById(id));
    }

    /**
     * 新增优惠券定义
     */
    @RequiresPermissions("mall-marketing:coupon:add")
    @Log(title = "优惠券定义", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallMarketingCoupon mallMarketingCoupon)
    {
        return toAjax(mallMarketingCouponService.insertMallMarketingCoupon(mallMarketingCoupon));
    }

    /**
     * 修改优惠券定义
     */
    @RequiresPermissions("mall-marketing:coupon:edit")
    @Log(title = "优惠券定义", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallMarketingCoupon mallMarketingCoupon)
    {
        return toAjax(mallMarketingCouponService.updateMallMarketingCoupon(mallMarketingCoupon));
    }

    /**
     * 删除优惠券定义
     */
    @RequiresPermissions("mall-marketing:coupon:remove")
    @Log(title = "优惠券定义", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallMarketingCouponService.deleteMallMarketingCouponByIds(ids));
    }
}
