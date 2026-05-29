package com.mall.user.convert.response;

import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.VO.GrowthVO;
import com.mall.user.VO.MemberLevelVO;
import com.mall.user.VO.MembershipVO;

import java.util.List;

public class MemberConvert {

    private MemberConvert() {
    }

    public static MemberLevelVO toMemberLevelVO(MallUserMemberLevelDO level) {
        MemberLevelVO vo = new MemberLevelVO();
        if (level != null) {
            vo.setLevelName(level.getLevelName());
            vo.setIcon(level.getIcon());
            vo.setLevelValue(level.getLevelValue());
        }
        return vo;
    }

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
