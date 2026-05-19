package com.mall.product.controller;

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
import com.mall.product.domain.MallProductSku;
import com.mall.product.service.IMallProductSkuService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * SKU 管理Controller
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/sku")
public class MallProductSkuController extends BaseController
{
    @Autowired
    private IMallProductSkuService mallProductSkuService;

    /**
     * 查询SKU 管理列表
     */
    @RequiresPermissions("mall-product:sku:list")
    @GetMapping("/list")
    public TableDataInfo list(MallProductSku mallProductSku)
    {
        startPage();
        List<MallProductSku> list = mallProductSkuService.selectMallProductSkuList(mallProductSku);
        return getDataTable(list);
    }

    /**
     * 导出SKU 管理列表
     */
    @RequiresPermissions("mall-product:sku:export")
    @Log(title = "SKU 管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallProductSku mallProductSku)
    {
        List<MallProductSku> list = mallProductSkuService.selectMallProductSkuList(mallProductSku);
        ExcelUtil<MallProductSku> util = new ExcelUtil<MallProductSku>(MallProductSku.class);
        util.exportExcel(response, list, "SKU 管理数据");
    }

    /**
     * 获取SKU 管理详细信息
     */
    @RequiresPermissions("mall-product:sku:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallProductSkuService.selectMallProductSkuById(id));
    }

    /**
     * 新增SKU 管理
     */
    @RequiresPermissions("mall-product:sku:add")
    @Log(title = "SKU 管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProductSku mallProductSku)
    {
        return toAjax(mallProductSkuService.insertMallProductSku(mallProductSku));
    }

    /**
     * 修改SKU 管理
     */
    @RequiresPermissions("mall-product:sku:edit")
    @Log(title = "SKU 管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProductSku mallProductSku)
    {
        return toAjax(mallProductSkuService.updateMallProductSku(mallProductSku));
    }

    /**
     * 删除SKU 管理
     */
    @RequiresPermissions("mall-product:sku:remove")
    @Log(title = "SKU 管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallProductSkuService.deleteMallProductSkuByIds(ids));
    }
}
