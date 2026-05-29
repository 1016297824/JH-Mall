package com.mall.user.convert.response;

import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.VO.GrowthVO;
import com.mall.user.VO.MemberLevelVO;
import com.mall.user.VO.MembershipVO;

import java.util.List;

/**
 * 会员 DO ↔ VO 静态转换器
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class MemberConvert {

    private MemberConvert() {
    }

    /**
     * 将会员等级 DO 转换为会员等级 VO
     *
     * @param level 会员等级 DO，可为 null
     * @return 会员等级 VO
     */
    public static MemberLevelVO toMemberLevelVO(MallUserMemberLevelDO level) {
        MemberLevelVO vo = new MemberLevelVO();
        if (level != null) {
            vo.setLevelName(level.getLevelName());
            vo.setIcon(level.getIcon());
            vo.setLevelValue(level.getLevelValue());
        }
        return vo;
    }

    /**
     * 将会员信息聚合为会员信息 VO
     *
     * @param member      会员 DO
     * @param currentLevel 当前等级 DO
     * @param nextLevel    下一等级 DO，已是最高级则为 null
     * @param benefits    权益名称列表
     * @return 会员信息 VO
     */
    public static MembershipVO toMembershipVO(MallUserMemberDO member, MallUserMemberLevelDO currentLevel,
                                              MallUserMemberLevelDO nextLevel, List<String> benefits) {
        MembershipVO vo = new MembershipVO();
        vo.setGrowth(member.getGrowth());
        vo.setTotalGrowth(member.getTotalGrowth());
        vo.setCurrentLevel(toMemberLevelVO(currentLevel));
        if (nextLevel != null) {
            vo.setNextLevel(toMemberLevelVO(nextLevel));
        }
        vo.setBenefits(benefits);
        return vo;
    }

    /**
     * 将成长值信息聚合为成长值 VO
     *
     * @param member         会员 DO
     * @param currentLevel   当前等级 DO
     * @param nextLevel      下一等级 DO，已是最高级则为 null
     * @param needGrowth     距离下一级所需成长值
     * @param progressPercent 等级进度百分比
     * @return 成长值 VO
     */
    public static GrowthVO toGrowthVO(MallUserMemberDO member, MallUserMemberLevelDO currentLevel,
                                      MallUserMemberLevelDO nextLevel, Integer needGrowth, Integer progressPercent) {
        GrowthVO vo = new GrowthVO();
        vo.setGrowth(member.getGrowth());
        vo.setTotalGrowth(member.getTotalGrowth());
        vo.setCurrentLevel(toMemberLevelVO(currentLevel));
        if (nextLevel != null) {
            vo.setNextLevel(toMemberLevelVO(nextLevel));
        }
        vo.setNeedGrowth(needGrowth);
        vo.setProgressPercent(progressPercent);
        return vo;
    }
}
