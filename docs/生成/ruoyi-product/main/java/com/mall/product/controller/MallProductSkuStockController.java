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
import com.mall.product.DO.MallProductSkuStock;
import com.mall.product.service.IMallProductSkuStockService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 库存管理Controller
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/stock")
public class MallProductSkuStockController extends BaseController
{
    @Autowired
    private IMallProductSkuStockService mallProductSkuStockService;

    /**
     * 查询库存管理列表
     */
    @RequiresPermissions("mall-product:stock:list")
    @GetMapping("/list")
    public TableDataInfo list(MallProductSkuStock mallProductSkuStock)
    {
        startPage();
        List<MallProductSkuStock> list = mallProductSkuStockService.selectMallProductSkuStockList(mallProductSkuStock);
        return getDataTable(list);
    }

    /**
     * 导出库存管理列表
     */
    @RequiresPermissions("mall-product:stock:export")
    @Log(title = "库存管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallProductSkuStock mallProductSkuStock)
    {
        List<MallProductSkuStock> list = mallProductSkuStockService.selectMallProductSkuStockList(mallProductSkuStock);
        ExcelUtil<MallProductSkuStock> util = new ExcelUtil<MallProductSkuStock>(MallProductSkuStock.class);
        util.exportExcel(response, list, "库存管理数据");
    }

    /**
     * 获取库存管理详细信息
     */
    @RequiresPermissions("mall-product:stock:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallProductSkuStockService.selectMallProductSkuStockById(id));
    }

    /**
     * 新增库存管理
     */
    @RequiresPermissions("mall-product:stock:add")
    @Log(title = "库存管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProductSkuStock mallProductSkuStock)
    {
        return toAjax(mallProductSkuStockService.insertMallProductSkuStock(mallProductSkuStock));
    }

    /**
     * 修改库存管理
     */
    @RequiresPermissions("mall-product:stock:edit")
    @Log(title = "库存管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProductSkuStock mallProductSkuStock)
    {
        return toAjax(mallProductSkuStockService.updateMallProductSkuStock(mallProductSkuStock));
    }

    /**
     * 删除库存管理
     */
    @RequiresPermissions("mall-product:stock:remove")
    @Log(title = "库存管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallProductSkuStockService.deleteMallProductSkuStockByIds(ids));
    }
}
