package com.mall.user.convert.response;

import com.mall.common.enums.user.GenderEnum;
import com.mall.user.DO.MallPointsAccountDO;
import com.mall.user.DO.MallUserDO;
import com.mall.user.DO.MallUserMemberDO;
import com.mall.user.DO.MallUserMemberLevelDO;
import com.mall.user.VO.UserProfileVO;

import java.time.format.DateTimeFormatter;

/**
 * 用户 DO → VO 静态转换器（含聚合多表数据）
 *
 * @author JH-Mall
 * @date 2026/05/29
 */
public class UserConvert {

    private static final DateTimeFormatter BIRTHDAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private UserConvert() {
    }

    /**
     * 聚合用户多表数据为用户资料 VO
     *
     * <p>组装用户基本信息、会员等级、积分账户等数据，字段类型转换和脱敏在此完成</p>
     *
     * @param user        用户 DO
     * @param maskedPhone 脱敏后的手机号
     * @param member      会员 DO，可为 null
     * @param level       会员等级 DO，可为 null
     * @param account     积分账户 DO，可为 null
     * @return 用户资料 VO
     */
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
