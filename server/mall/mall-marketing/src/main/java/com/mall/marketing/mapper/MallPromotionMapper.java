package com.mall.marketing.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.marketing.DO.MallPromotionDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 促销活动 Mapper
 *
 * @author JH-Mall
 * @date 2026/10/02
 */
@Mapper
public interface MallPromotionMapper extends BaseMapper<MallPromotionDO> {

    /** 活动状态：进行中，与 {@code PromotionStatusEnum.ACTIVE} 保持一致 */
    int STATUS_ACTIVE = 1;

    /**
     * 查询当前进行中的活动
     *
     * <p>条件：状态为进行中 + 当前时间落在 {@code [start_time, end_time)} 内，
     * 按 {@code sort_order} 升序（越小越优先）。</p>
     *
     * @param now   当前时间
     * @param limit 单次返回上限
     * @return 进行中的活动列表
     */
    default List<MallPromotionDO> selectActive(LocalDateTime now, int limit) {
        return selectList(new LambdaQueryWrapper<MallPromotionDO>()
                .eq(MallPromotionDO::getPromotionStatus, STATUS_ACTIVE)
                .le(MallPromotionDO::getStartTime, now)
                .gt(MallPromotionDO::getEndTime, now)
                .eq(MallPromotionDO::getIsDeleted, 0)
                .orderByAsc(MallPromotionDO::getSortOrder)
                .orderByAsc(MallPromotionDO::getId)
                .last("LIMIT " + limit));
    }
}
