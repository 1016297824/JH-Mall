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
import com.mall.product.domain.MallProductCategory;
import com.mall.product.service.IMallProductCategoryService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;

/**
 * 商品类目Controller
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/category")
public class MallProductCategoryController extends BaseController
{
    @Autowired
    private IMallProductCategoryService mallProductCategoryService;

    /**
     * 查询商品类目列表
     */
    @RequiresPermissions("mall-product:category:list")
    @GetMapping("/list")
    public AjaxResult list(MallProductCategory mallProductCategory)
    {
        List<MallProductCategory> list = mallProductCategoryService.selectMallProductCategoryList(mallProductCategory);
        return success(list);
    }

    /**
     * 导出商品类目列表
     */
    @RequiresPermissions("mall-product:category:export")
    @Log(title = "商品类目", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallProductCategory mallProductCategory)
    {
        List<MallProductCategory> list = mallProductCategoryService.selectMallProductCategoryList(mallProductCategory);
        ExcelUtil<MallProductCategory> util = new ExcelUtil<MallProductCategory>(MallProductCategory.class);
        util.exportExcel(response, list, "商品类目数据");
    }

    /**
     * 获取商品类目详细信息
     */
    @RequiresPermissions("mall-product:category:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallProductCategoryService.selectMallProductCategoryById(id));
    }

    /**
     * 新增商品类目
     */
    @RequiresPermissions("mall-product:category:add")
    @Log(title = "商品类目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProductCategory mallProductCategory)
    {
        return toAjax(mallProductCategoryService.insertMallProductCategory(mallProductCategory));
    }

    /**
     * 修改商品类目
     */
    @RequiresPermissions("mall-product:category:edit")
    @Log(title = "商品类目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProductCategory mallProductCategory)
    {
        return toAjax(mallProductCategoryService.updateMallProductCategory(mallProductCategory));
    }

    /**
     * 删除商品类目
     */
    @RequiresPermissions("mall-product:category:remove")
    @Log(title = "商品类目", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallProductCategoryService.deleteMallProductCategoryByIds(ids));
    }
}
