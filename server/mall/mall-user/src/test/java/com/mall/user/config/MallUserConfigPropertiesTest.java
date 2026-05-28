package com.mall.user.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MallUserConfigProperties 单元测试
 *
 * @author system
 * @date 2026/05/28
 */
class MallUserConfigPropertiesTest {

    @Test
    void pointsDefaultsShouldBeCorrect() {
        MallUserConfigProperties.Points points = new MallUserConfigProperties.Points();

        assertEquals(5, points.getSigninBase());
        assertEquals(10, points.getSigninConsecutive());
        assertEquals(10, points.getReview());
        assertEquals(20, points.getReviewWithPhoto());
        assertEquals(1, points.getSigninConsecutiveBonus());
    }

    @Test
    void signinConsecutiveBonusGetterAndSetterShouldWork() {
        MallUserConfigProperties.Points points = new MallUserConfigProperties.Points();

        assertEquals(1, points.getSigninConsecutiveBonus());

        points.setSigninConsecutiveBonus(5);
        assertEquals(5, points.getSigninConsecutiveBonus());
    }

    @Test
    void pointsShouldBeAccessibleFromConfigProperties() {
        MallUserConfigProperties properties = new MallUserConfigProperties();
        MallUserConfigProperties.Points points = properties.getPoints();

        assertNotNull(points);
    }
}
