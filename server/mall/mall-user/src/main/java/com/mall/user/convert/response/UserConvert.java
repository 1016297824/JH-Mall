package com.mall.user.convert.response;

import com.mall.common.enums.user.GenderEnum;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.VO.UserProfileVO;

import java.time.format.DateTimeFormatter;

public class UserConvert {

    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private UserConvert() {
    }

    public static UserProfileVO toUserProfileVO(
            MallUserDO user,
            String maskedPhone,
            MallUserMemberDO member,
            MallUserMemberLevelDO level,
            MallPointsAccountDO account) {
        UserProfileVO vo = new UserProfileVO();

        vo.setUserId(String.valueOf(user.getId()));
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());

        GenderEnum genderEnum = GenderEnum.fromCode(user.getGender());
        vo.setGenderName(genderEnum != null ? genderEnum.getDescription() : "未知");

        if (user.getBirthday() != null) {
            vo.setBirthday(user.getBirthday().format(BIRTHDAY_FORMATTER));
        }

        vo.setPhone(maskedPhone);
        vo.setEmail(user.getEmail());

        if (member != null) {
            vo.setGrowth(member.getGrowth());
            vo.setTotalGrowth(member.getTotalGrowth());
        }

        if (level != null) {
            vo.setMembershipLevel(level.getLevelName());
            vo.setMembershipIcon(level.getIcon());
        }

        if (account != null) {
            vo.setPoints(account.getTotalPoints());
            vo.setAvailablePoints(account.getAvailablePoints());
        }

        return vo;
    }
}
