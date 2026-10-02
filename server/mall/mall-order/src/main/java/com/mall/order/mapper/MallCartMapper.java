package com.mall.order.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.order.DO.MallCartDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 购物车 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallCartMapper extends BaseMapper<MallCartDO> {

    /**
     * 查询用户全部购物车项
     *
     * @param userId 用户 ID
     * @return 购物车项列表（已过滤逻辑删除）
     */
    default List<MallCartDO> selectByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<MallCartDO>()
                .eq(MallCartDO::getUserId, userId)
                .eq(MallCartDO::getIsDeleted, 0)
                .orderByDesc(MallCartDO::getUpdateTime));
    }

    /**
     * 查询用户某个 SKU 的购物车项
     *
     * <p>用于加购时判断是合并数量还是新增行。</p>
     *
     * @param userId 用户 ID
     * @param skuId  SKU ID
     * @return 购物车项，不存在返回 null
     */
    default MallCartDO selectByUserIdAndSkuId(Long userId, Long skuId) {
        return selectOne(new LambdaQueryWrapper<MallCartDO>()
                .eq(MallCartDO::getUserId, userId)
                .eq(MallCartDO::getSkuId, skuId)
                .eq(MallCartDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 查询用户已选中的购物车项（下单只处理已选项）
     *
     * @param userId 用户 ID
     * @return 已选中的购物车项
     */
    default List<MallCartDO> selectSelectedByUserId(Long userId) {
        return selectList(new LambdaQueryWrapper<MallCartDO>()
                .eq(MallCartDO::getUserId, userId)
                .eq(MallCartDO::getIsSelected, 1)
                .eq(MallCartDO::getIsDeleted, 0));
    }

    /**
     * 更新数量
     *
     * @param id       购物车项 ID
     * @param quantity 新数量
     * @return 影响行数
     */
    @Update("UPDATE mall_order_cart SET quantity = #{quantity}, update_time = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateQuantity(@Param("id") Long id, @Param("quantity") Integer quantity);

    /**
     * 更新选中状态
     *
     * @param id        购物车项 ID
     * @param isSelected 是否选中，1=选中 0=未选
     * @return 影响行数
     */
    @Update("UPDATE mall_order_cart SET is_selected = #{isSelected}, update_time = NOW() " +
            "WHERE id = #{id} AND is_deleted = 0")
    int updateSelected(@Param("id") Long id, @Param("isSelected") Integer isSelected);

    /**
     * 物理删除单个购物车项
     *
     * <p>购物车无审计需求，设计文档 §4.3 要求物理删除。</p>
     *
     * <p>方法名刻意避开 {@code BaseMapper.deleteById}（逻辑删除），防止语义混淆。</p>
     *
     * @param id 购物车项 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM mall_order_cart WHERE id = #{id}")
    int hardDeleteById(@Param("id") Long id);

    /**
     * 物理删除用户全部购物车项（清空购物车）
     *
     * @param userId 用户 ID
     * @return 影响行数
     */
    @Delete("DELETE FROM mall_order_cart WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}