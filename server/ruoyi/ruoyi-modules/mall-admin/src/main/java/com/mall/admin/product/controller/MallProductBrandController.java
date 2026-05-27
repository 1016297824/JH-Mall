package com.mall.admin.product.controller;

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
import com.mall.admin.product.domain.MallProductBrand;
import com.mall.admin.product.service.IMallProductBrandService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 品牌管理Controller
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/brand")
public class MallProductBrandController extends BaseController
{
    @Autowired
    private IMallProductBrandService mallProductBrandService;

    /**
     * 查询品牌管理列表
     */
    @RequiresPermissions("mall-product:brand:list")
    @GetMapping("/list")
    public TableDataInfo list(MallProductBrand mallProductBrand)
    {
        startPage();
        List<MallProductBrand> list = mallProductBrandService.selectMallProductBrandList(mallProductBrand);
        return getDataTable(list);
    }

    /**
     * 导出品牌管理列表
     */
    @RequiresPermissions("mall-product:brand:export")
    @Log(title = "品牌管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallProductBrand mallProductBrand)
    {
        List<MallProductBrand> list = mallProductBrandService.selectMallProductBrandList(mallProductBrand);
        ExcelUtil<MallProductBrand> util = new ExcelUtil<MallProductBrand>(MallProductBrand.class);
        util.exportExcel(response, list, "品牌管理数据");
    }

    /**
     * 获取品牌管理详细信息
     */
    @RequiresPermissions("mall-product:brand:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallProductBrandService.selectMallProductBrandById(id));
    }

    /**
     * 新增品牌管理
     */
    @RequiresPermissions("mall-product:brand:add")
    @Log(title = "品牌管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProductBrand mallProductBrand)
    {
        return toAjax(mallProductBrandService.insertMallProductBrand(mallProductBrand));
    }

    /**
     * 修改品牌管理
     */
    @RequiresPermissions("mall-product:brand:edit")
    @Log(title = "品牌管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProductBrand mallProductBrand)
    {
        return toAjax(mallProductBrandService.updateMallProductBrand(mallProductBrand));
    }

    /**
     * 删除品牌管理
     */
    @RequiresPermissions("mall-product:brand:remove")
    @Log(title = "品牌管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallProductBrandService.deleteMallProductBrandByIds(ids));
    }
}
