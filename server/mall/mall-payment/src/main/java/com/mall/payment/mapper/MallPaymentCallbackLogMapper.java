package com.mall.payment.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mall.payment.DO.MallPaymentCallbackLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 支付回调记录 Mapper
 *
 * <p>回调日志「只增不改」，仅回填处理结果，因此只有 insert 与定向 update，
 * 不存在并发覆盖风险。</p>
 *
 * @author JH-Mall
 * @date 2026/10/03
 */
@Mapper
public interface MallPaymentCallbackLogMapper extends BaseMapper<MallPaymentCallbackLogDO> {

    /** 处理状态：待处理 */
    int PROCESS_PENDING = 0;

    /** 处理状态：处理成功 */
    int PROCESS_SUCCESS = 1;

    /** 处理状态：处理失败 */
    int PROCESS_FAILED = 2;

    /**
     * 按 nonce 查询回调记录
     *
     * <p>作为 Redis 防重放不可用时的<b>兜底</b>：
     * {@code uk_nonce} 唯一索引保证同一 nonce 只落一条。</p>
     *
     * @param nonce 回调防重放 nonce
     * @return 回调记录，不存在返回 null
     */
    default MallPaymentCallbackLogDO selectByNonce(String nonce) {
        return selectOne(new LambdaQueryWrapper<MallPaymentCallbackLogDO>()
                .eq(MallPaymentCallbackLogDO::getNonce, nonce)
                .eq(MallPaymentCallbackLogDO::getIsDeleted, 0)
                .last("LIMIT 1"));
    }

    /**
     * 回填回调处理结果
     *
     * @param id            回调记录主键
     * @param processStatus 处理状态，取值见本接口 PROCESS_* 常量
     * @param processResult 处理结果说明
     * @return 影响行数
     */
    @Update("UPDATE mall_payment_callback_log SET process_status = #{processStatus}, "
            + "process_result = #{processResult}, process_time = NOW(), update_time = NOW() "
            + "WHERE id = #{id}")
    int markProcessed(@Param("id") Long id,
                      @Param("processStatus") Integer processStatus,
                      @Param("processResult") String processResult);
}
