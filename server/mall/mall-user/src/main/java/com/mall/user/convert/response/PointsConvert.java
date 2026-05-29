package com.mall.user.convert.response;

import com.mall.common.enums.user.BizTypeEnum;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserPointsLogDO;
import com.mall.user.VO.PointsRecordVO;
import com.mall.user.VO.PointsVO;

import java.time.ZoneId;
import java.util.Date;

/**
 * 积分 DO ↔ VO 静态转换器
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class PointsConvert {

    private PointsConvert() {
    }

    /**
     * 将积分账户 DO 转换为积分余额 VO
     *
     * @param account 积分账户 DO
     * @return 积分余额 VO
     */
    public static PointsVO toPointsVO(MallPointsAccountDO account) {
        PointsVO vo = new PointsVO();
        vo.setTotalPoints(account.getTotalPoints());
        vo.setAvailablePoints(account.getAvailablePoints());
        vo.setUsedPoints(account.getUsedPoints());
        vo.setExpiredPoints(account.getExpiredPoints());
        return vo;
    }

    /**
     * 将积分流水 DO 转换为积分流水 VO
     *
     * @param logDO 积分流水 DO
     * @return 积分流水 VO
     */
    public static PointsRecordVO toPointsRecordVO(MallUserPointsLogDO logDO) {
        PointsRecordVO vo = new PointsRecordVO();
        vo.setId(logDO.getId());
        vo.setBizType(logDO.getBizType());
        BizTypeEnum bizTypeEnum = BizTypeEnum.fromCode(logDO.getBizType());
        vo.setBizTypeName(bizTypeEnum != null ? bizTypeEnum.getName() : logDO.getBizType());
        vo.setChangeType(logDO.getChangeType());
        vo.setPoints(logDO.getPoints());
        vo.setBeforePoints(logDO.getBeforePoints());
        vo.setAfterPoints(logDO.getAfterPoints());
        vo.setRemark(logDO.getRemark());
        if (logDO.getCreateTime() != null) {
            vo.setCreateTime(Date.from(logDO.getCreateTime().atZone(ZoneId.systemDefault()).toInstant()));
        }
        return vo;
    }
}
