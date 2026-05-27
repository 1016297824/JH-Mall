package com.mall.marketing.controller;

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
import com.mall.marketing.DO.MallMarketingPromotion;
import com.mall.marketing.service.IMallMarketingPromotionService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 活动管理Controller
 *
 * @author ruoyi
 * @date 2026-05-21
 */
@RestController
@RequestMapping("/promotion")
public class MallMarketingPromotionController extends BaseController
{
    @Autowired
    private IMallMarketingPromotionService mallMarketingPromotionService;

    /**
     * 查询活动管理列表
     */
    @RequiresPermissions("mall-marketing:promotion:list")
    @GetMapping("/list")
    public TableDataInfo list(MallMarketingPromotion mallMarketingPromotion)
    {
        startPage();
        List<MallMarketingPromotion> list = mallMarketingPromotionService.selectMallMarketingPromotionList(mallMarketingPromotion);
        return getDataTable(list);
    }

    /**
     * 导出活动管理列表
     */
    @RequiresPermissions("mall-marketing:promotion:export")
    @Log(title = "活动管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallMarketingPromotion mallMarketingPromotion)
    {
        List<MallMarketingPromotion> list = mallMarketingPromotionService.selectMallMarketingPromotionList(mallMarketingPromotion);
        ExcelUtil<MallMarketingPromotion> util = new ExcelUtil<MallMarketingPromotion>(MallMarketingPromotion.class);
        util.exportExcel(response, list, "活动管理数据");
    }

    /**
     * 获取活动管理详细信息
     */
    @RequiresPermissions("mall-marketing:promotion:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallMarketingPromotionService.selectMallMarketingPromotionById(id));
    }

    /**
     * 新增活动管理
     */
    @RequiresPermissions("mall-marketing:promotion:add")
    @Log(title = "活动管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallMarketingPromotion mallMarketingPromotion)
    {
        return toAjax(mallMarketingPromotionService.insertMallMarketingPromotion(mallMarketingPromotion));
    }

    /**
     * 修改活动管理
     */
    @RequiresPermissions("mall-marketing:promotion:edit")
    @Log(title = "活动管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallMarketingPromotion mallMarketingPromotion)
    {
        return toAjax(mallMarketingPromotionService.updateMallMarketingPromotion(mallMarketingPromotion));
    }

    /**
     * 删除活动管理
     */
    @RequiresPermissions("mall-marketing:promotion:remove")
    @Log(title = "活动管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallMarketingPromotionService.deleteMallMarketingPromotionByIds(ids));
    }
}
