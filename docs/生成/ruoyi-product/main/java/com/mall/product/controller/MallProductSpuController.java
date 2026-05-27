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
import com.mall.product.DO.MallProductSpu;
import com.mall.product.service.IMallProductSpuService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * SPU 管理Controller
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/spu")
public class MallProductSpuController extends BaseController
{
    @Autowired
    private IMallProductSpuService mallProductSpuService;

    /**
     * 查询SPU 管理列表
     */
    @RequiresPermissions("mall-product:spu:list")
    @GetMapping("/list")
    public TableDataInfo list(MallProductSpu mallProductSpu)
    {
        startPage();
        List<MallProductSpu> list = mallProductSpuService.selectMallProductSpuList(mallProductSpu);
        return getDataTable(list);
    }

    /**
     * 导出SPU 管理列表
     */
    @RequiresPermissions("mall-product:spu:export")
    @Log(title = "SPU 管理", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallProductSpu mallProductSpu)
    {
        List<MallProductSpu> list = mallProductSpuService.selectMallProductSpuList(mallProductSpu);
        ExcelUtil<MallProductSpu> util = new ExcelUtil<MallProductSpu>(MallProductSpu.class);
        util.exportExcel(response, list, "SPU 管理数据");
    }

    /**
     * 获取SPU 管理详细信息
     */
    @RequiresPermissions("mall-product:spu:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallProductSpuService.selectMallProductSpuById(id));
    }

    /**
     * 新增SPU 管理
     */
    @RequiresPermissions("mall-product:spu:add")
    @Log(title = "SPU 管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallProductSpu mallProductSpu)
    {
        return toAjax(mallProductSpuService.insertMallProductSpu(mallProductSpu));
    }

    /**
     * 修改SPU 管理
     */
    @RequiresPermissions("mall-product:spu:edit")
    @Log(title = "SPU 管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallProductSpu mallProductSpu)
    {
        return toAjax(mallProductSpuService.updateMallProductSpu(mallProductSpu));
    }

    /**
     * 删除SPU 管理
     */
    @RequiresPermissions("mall-product:spu:remove")
    @Log(title = "SPU 管理", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallProductSpuService.deleteMallProductSpuByIds(ids));
    }
}
