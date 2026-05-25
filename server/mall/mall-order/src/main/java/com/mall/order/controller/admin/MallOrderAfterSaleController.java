package com.mall.order.controller.admin;

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
import com.mall.order.domain.MallOrderAfterSale;
import com.mall.order.service.IMallOrderAfterSaleService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 售后管理Controller
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/after_sale")
public class MallOrderAfterSaleController extends BaseController
{
    @Autowired
    private IMallOrderAfterSaleService mallOrderAfterSaleService;

    /**
     * 查询售后管理列表
     */
    @RequiresPermissions("mall-order:after_sale:list")
    @GetMapping("/list")
    public TableDataInfo list(MallOrderAfterSale mallOrderAfterSale)
    {
        startPage();
        List<MallOrderAfterSale> list = mallOrderAfterSaleService.selectMallOrderAfterSaleList(mallOrderAfterSale);
        return getDataTable(list);
    }

    /**
     * 导出售后管理列表
     */
    @RequiresPermissions("mall-order:after_sale:export")
    @Log(title = "售后管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallOrderAfterSale mallOrderAfterSale)
    {
        List<MallOrderAfterSale> list = mallOrderAfterSaleService.selectMallOrderAfterSaleList(mallOrderAfterSale);
        ExcelUtil<MallOrderAfterSale> util = new ExcelUtil<MallOrderAfterSale>(MallOrderAfterSale.class);
        util.exportExcel(response, list, "售后管理数据");
    }

    /**
     * 获取售后管理详细信息
     */
    @RequiresPermissions("mall-order:after_sale:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallOrderAfterSaleService.selectMallOrderAfterSaleById(id));
    }

    /**
     * 新增售后管理
     */
    @RequiresPermissions("mall-order:after_sale:add")
    @Log(title = "售后管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallOrderAfterSale mallOrderAfterSale)
    {
        return toAjax(mallOrderAfterSaleService.insertMallOrderAfterSale(mallOrderAfterSale));
    }

    /**
     * 修改售后管理
     */
    @RequiresPermissions("mall-order:after_sale:edit")
    @Log(title = "售后管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallOrderAfterSale mallOrderAfterSale)
    {
        return toAjax(mallOrderAfterSaleService.updateMallOrderAfterSale(mallOrderAfterSale));
    }

    /**
     * 删除售后管理
     */
    @RequiresPermissions("mall-order:after_sale:remove")
    @Log(title = "售后管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallOrderAfterSaleService.deleteMallOrderAfterSaleByIds(ids));
    }
}
