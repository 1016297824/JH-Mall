package com.mall.order.controller;

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
import com.mall.order.domain.MallOrderCart;
import com.mall.order.service.IMallOrderCartService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 购物车Controller
 * 
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/cart")
public class MallOrderCartController extends BaseController
{
    @Autowired
    private IMallOrderCartService mallOrderCartService;

    /**
     * 查询购物车列表
     */
    @RequiresPermissions("mall-order:cart:list")
    @GetMapping("/list")
    public TableDataInfo list(MallOrderCart mallOrderCart)
    {
        startPage();
        List<MallOrderCart> list = mallOrderCartService.selectMallOrderCartList(mallOrderCart);
        return getDataTable(list);
    }

    /**
     * 导出购物车列表
     */
    @RequiresPermissions("mall-order:cart:export")
    @Log(title = "购物车", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallOrderCart mallOrderCart)
    {
        List<MallOrderCart> list = mallOrderCartService.selectMallOrderCartList(mallOrderCart);
        ExcelUtil<MallOrderCart> util = new ExcelUtil<MallOrderCart>(MallOrderCart.class);
        util.exportExcel(response, list, "购物车数据");
    }

    /**
     * 获取购物车详细信息
     */
    @RequiresPermissions("mall-order:cart:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallOrderCartService.selectMallOrderCartById(id));
    }

    /**
     * 新增购物车
     */
    @RequiresPermissions("mall-order:cart:add")
    @Log(title = "购物车", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallOrderCart mallOrderCart)
    {
        return toAjax(mallOrderCartService.insertMallOrderCart(mallOrderCart));
    }

    /**
     * 修改购物车
     */
    @RequiresPermissions("mall-order:cart:edit")
    @Log(title = "购物车", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallOrderCart mallOrderCart)
    {
        return toAjax(mallOrderCartService.updateMallOrderCart(mallOrderCart));
    }

    /**
     * 删除购物车
     */
    @RequiresPermissions("mall-order:cart:remove")
    @Log(title = "购物车", businessType = BusinessType.DELETE)
	@DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallOrderCartService.deleteMallOrderCartByIds(ids));
    }
}
