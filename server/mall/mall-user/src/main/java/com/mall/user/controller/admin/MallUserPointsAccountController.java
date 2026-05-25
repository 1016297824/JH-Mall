package com.mall.user.controller.admin;

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
import com.mall.user.domain.MallUserPointsAccount;
import com.mall.user.service.IMallUserPointsAccountService;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.page.TableDataInfo;

/**
 * 积分账户Controller
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@RestController
@RequestMapping("/account")
public class MallUserPointsAccountController extends BaseController
{
    @Autowired
    private IMallUserPointsAccountService mallUserPointsAccountService;

    /**
     * 查询积分账户列表
     */
    @RequiresPermissions("mall-user:account:list")
    @GetMapping("/list")
    public TableDataInfo list(MallUserPointsAccount mallUserPointsAccount)
    {
        startPage();
        List<MallUserPointsAccount> list = mallUserPointsAccountService.selectMallUserPointsAccountList(mallUserPointsAccount);
        return getDataTable(list);
    }

    /**
     * 导出积分账户列表
     */
    @RequiresPermissions("mall-user:account:export")
    @Log(title = "积分账户", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, MallUserPointsAccount mallUserPointsAccount)
    {
        List<MallUserPointsAccount> list = mallUserPointsAccountService.selectMallUserPointsAccountList(mallUserPointsAccount);
        ExcelUtil<MallUserPointsAccount> util = new ExcelUtil<MallUserPointsAccount>(MallUserPointsAccount.class);
        util.exportExcel(response, list, "积分账户数据");
    }

    /**
     * 获取积分账户详细信息
     */
    @RequiresPermissions("mall-user:account:query")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") String id)
    {
        return success(mallUserPointsAccountService.selectMallUserPointsAccountById(id));
    }

    /**
     * 新增积分账户
     */
    @RequiresPermissions("mall-user:account:add")
    @Log(title = "积分账户", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MallUserPointsAccount mallUserPointsAccount)
    {
        return toAjax(mallUserPointsAccountService.insertMallUserPointsAccount(mallUserPointsAccount));
    }

    /**
     * 修改积分账户
     */
    @RequiresPermissions("mall-user:account:edit")
    @Log(title = "积分账户", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody MallUserPointsAccount mallUserPointsAccount)
    {
        return toAjax(mallUserPointsAccountService.updateMallUserPointsAccount(mallUserPointsAccount));
    }

    /**
     * 删除积分账户
     */
    @RequiresPermissions("mall-user:account:remove")
    @Log(title = "积分账户", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable String[] ids)
    {
        return toAjax(mallUserPointsAccountService.deleteMallUserPointsAccountByIds(ids));
    }
}
