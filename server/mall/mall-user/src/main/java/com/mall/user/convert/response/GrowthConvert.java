package com.mall.user.convert.response;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.DO.MallUserGrowthLogDO;
import com.mall.user.VO.GrowthRecordVO;

import java.time.ZoneId;
import java.util.Date;

/**
 * 成长值流水 DO ↔ VO 静态转换器
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class GrowthConvert {

    private GrowthConvert() {
    }

    /**
     * 将成长值流水 DO 转换为成长值流水 VO
     *
     * @param logDO 成长值流水 DO
     * @return 成长值流水 VO
     */
    public static GrowthRecordVO toGrowthRecordVO(MallUserGrowthLogDO logDO) {
        GrowthRecordVO vo = new GrowthRecordVO();
        vo.setId(logDO.getId());
        vo.setBizType(logDO.getBizType());
        BizTypeEnum bizTypeEnum = BizTypeEnum.fromCode(logDO.getBizType());
        vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : logDO.getBizType());
        vo.setChangeType(logDO.getChangeType());
        vo.setGrowth(logDO.getGrowth());
        vo.setBeforeGrowth(logDO.getBeforeGrowth());
        vo.setAfterGrowth(logDO.getAfterGrowth());
        vo.setRemark(logDO.getRemark());
        if (logDO.getCreateTime() != null) {
            vo.setCreateTime(Date.from(logDO.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        return vo;
    }
}
