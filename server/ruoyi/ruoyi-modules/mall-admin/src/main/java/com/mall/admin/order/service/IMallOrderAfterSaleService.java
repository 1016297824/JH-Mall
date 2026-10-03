package com.mall.admin.order.service;

import java.util.List;
import com.mall.admin.order.domain.MallOrderAfterSale;

/**
 * 售后管理Service接口
 *
 * @author ruoyi
 * @date 2026-05-19
 */
public interface IMallOrderAfterSaleService
{
    /**
     * 查询售后管理
     *
     * @param id 售后管理主键
     * @return 售后管理
     */
    public MallOrderAfterSale selectMallOrderAfterSaleById(String id);

    /**
     * 查询售后管理列表
     *
     * @param mallOrderAfterSale 售后管理
     * @return 售后管理集合
     */
    public List<MallOrderAfterSale> selectMallOrderAfterSaleList(MallOrderAfterSale mallOrderAfterSale);

    /**
     * 新增售后管理
     *
     * @param mallOrderAfterSale 售后管理
     * @return 结果
     */
    public int insertMallOrderAfterSale(MallOrderAfterSale mallOrderAfterSale);

    /**
     * 修改售后管理
     *
     * @param mallOrderAfterSale 售后管理
     * @return 结果
     */
    public int updateMallOrderAfterSale(MallOrderAfterSale mallOrderAfterSale);

    /**
     * 批量删除售后管理
     *
     * @param ids 需要删除的售后管理主键集合
     * @return 结果
     */
    public int deleteMallOrderAfterSaleByIds(String[] ids);

    /**
     * 删除售后管理信息
     *
     * @param id 售后管理主键
     * @return 结果
     */
    public int deleteMallOrderAfterSaleById(String id);

    /**
     * 审核通过：调 mall-order 发起退款
     *
     * <p><b>必须走 Feign 而非本模块 Mapper 直改状态</b>：售后单状态机与退款发起都在 mall-order，
     * 直改库会绕过状态校验且不会触发退款。</p>
     *
     * @param id     售后单主键
     * @param remark 审核意见
     */
    public void approveAfterSale(String id, String remark);

    /**
     * 审核驳回
     *
     * @param id     售后单主键
     * @param remark 驳回原因
     */
    public void rejectAfterSale(String id, String remark);
}
