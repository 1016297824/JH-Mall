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
import com.mall.order.domain.MallOrderAmount;
import com.mall.order.service.IMallOrderAmountService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 金额快照Controller
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@RestController
@RequestMapping("/amount")
public class MallOrderAmountController extends BaseController
{
    @Autowired
    private IMallOrderAmountService mallOrderAmountService;

    /**
     * 查询金额快照列表
     */
    @RequiresPermissions("mall-order:amount:list")
    @GetMapping("/list")
    public TableDataInfo list(MallOrderAmount mallOrderAmount)
    {
        startPage();
        List<MallOrderAmount> list = mallOrderAmountService.selectMallOrderAmountList(mallOrderAmount);
        return getDataTable(list);
    }

    /**
     * 导出金额快照列表
     */
    @RequiresPermissions("mall-order:amount:export")
    @Log(title = "金额快照", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallOrderAmount mallOrderAmount)
    {
        List<MallOrderAmount> list = mallOrderAmountService.selectMallOrderAmountList(mallOrderAmount);
        ExcelUtil<MallOrderAmount> util = new ExcelUtil<MallOrderAmount>(MallOrderAmount.class);
        util.exportExcel(response, list, "金额快照数据");
    }

    /**
     * 获取金额快照详细信息
     */
    @RequiresPermissions("mall-order:amount:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallOrderAmountService.selectMallOrderAmountById(id));
    }

    /**
     * 新增金额快照
     */
    @RequiresPermissions("mall-order:amount:add")
    @Log(title = "金额快照", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallOrderAmount mallOrderAmount)
    {
        return toAjax(mallOrderAmountService.insertMallOrderAmount(mallOrderAmount));
    }

    /**
     * 修改金额快照
     */
    @RequiresPermissions("mall-order:amount:edit")
    @Log(title = "金额快照", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallOrderAmount mallOrderAmount)
    {
        return toAjax(mallOrderAmountService.updateMallOrderAmount(mallOrderAmount));
    }

    /**
     * 删除金额快照
     */
    @RequiresPermissions("mall-order:amount:remove")
    @Log(title = "金额快照", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallOrderAmountService.deleteMallOrderAmountByIds(ids));
    }
}
