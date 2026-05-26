package com.mall.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * C 端用户模块配置属性（热更新）
 *
 * <p>对应 Nacos mall-user-dev.yml 中 mall.user.* 配置</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "mall.user")
public class MallUserConfigProperties {

    private final Address address = new Address();

    private final Profile profile = new Profile();

    private final Member member = new Member();

    private final Points points = new Points();

    public Address getAddress() {
        return address;
    }

    public Profile getProfile() {
        return profile;
    }

    public Member getMember() {
        return member;
    }

    public Points getPoints() {
        return points;
    }

    /** 地址配置 */
    public static class Address {

        /** 地址簿上限 */
        private int maxCount = 20;

        public int getMaxCount() {
            return maxCount;
        }

        public void setMaxCount(int maxCount) {
            this.maxCount = maxCount;
        }
    }

    /** 用户资料缓存配置 */
    public static class Profile {

        /** 用户资料缓存时间（秒） */
        private long cacheTtl = 600;

        public long getCacheTtl() {
            return cacheTtl;
        }

        public void setCacheTtl(long cacheTtl) {
            this.cacheTtl = cacheTtl;
        }
    }

    /** 会员配置 */
    public static class Member {

        /** 新用户默认会员等级 */
        private int defaultLevel = 1;

        public int getDefaultLevel() {
            return defaultLevel;
        }

        public void setDefaultLevel(int defaultLevel) {
            this.defaultLevel = defaultLevel;
        }
    }

    /** 积分配置 */
    public static class Points {

        /** 每日签到基础积分 */
        private int signinBase = 5;

        /** 连续签到上限积分 */
        private int signinConsecutive = 10;

        /** 评价奖励积分 */
        private int review = 10;

        /** 带图评价积分 */
        private int reviewWithPhoto = 20;

        public int getSigninBase() {
            return signinBase;
        }

        public void setSigninBase(int signinBase) {
            this.signinBase = signinBase;
        }

        public int getSigninConsecutive() {
            return signinConsecutive;
        }

        public void setSigninConsecutive(int signinConsecutive) {
            this.signinConsecutive = signinConsecutive;
        }

        public int getReview() {
            return review;
        }

        public void setReview(int review) {
            this.review = review;
        }

        public int getReviewWithPhoto() {
            return reviewWithPhoto;
        }

        public void setReviewWithPhoto(int reviewWithPhoto) {
            this.reviewWithPhoto = reviewWithPhoto;
        }
    }
}
