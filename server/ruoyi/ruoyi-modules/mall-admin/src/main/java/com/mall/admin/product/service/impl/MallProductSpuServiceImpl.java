package com.mall.admin.product.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import com.ruoyi.common.core.utils.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import com.mall.admin.product.domain.MallProductSku;
import com.mall.admin.product.mapper.MallProductSpuMapper;
import com.mall.admin.product.domain.MallProductSpu;
import com.mall.admin.product.service.IMallProductSpuService;
import com.mall.api.feign.RemoteProductService;
import lombok.extern.slf4j.Slf4j;

/**
 * SPU 管理Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-19
 */
@Slf4j
@Service
public class MallProductSpuServiceImpl implements IMallProductSpuService
{
    @Autowired
    private MallProductSpuMapper mallProductSpuMapper;

    /** mall-product 内部契约：商品变更后触发搜索索引同步 */
    @Autowired
    private RemoteProductService remoteProductService;

    /**
     * 查询SPU 管理
     *
     * @param id SPU 管理主键
     * @return SPU 管理
     */
    @Override
    public MallProductSpu selectMallProductSpuById(String id)
    {
        return mallProductSpuMapper.selectMallProductSpuById(id);
    }

    /**
     * 查询SPU 管理列表
     *
     * @param mallProductSpu SPU 管理
     * @return SPU 管理
     */
    @Override
    public List<MallProductSpu> selectMallProductSpuList(MallProductSpu mallProductSpu)
    {
        return mallProductSpuMapper.selectMallProductSpuList(mallProductSpu);
    }

    /**
     * 新增SPU 管理
     *
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    @Transactional
    @Override
    public int insertMallProductSpu(MallProductSpu mallProductSpu)
    {
        mallProductSpu.setCreateTime(DateUtils.getNowDate());
        int rows = mallProductSpuMapper.insertMallProductSpu(mallProductSpu);
        insertMallProductSku(mallProductSpu);
        syncSearchIndex(mallProductSpu.getId(), "UPSERT");
        return rows;
    }

    /**
     * 修改SPU 管理
     *
     * @param mallProductSpu SPU 管理
     * @return 结果
     */
    @Transactional
    @Override
    public int updateMallProductSpu(MallProductSpu mallProductSpu)
    {
        mallProductSpu.setUpdateTime(DateUtils.getNowDate());
        mallProductSpuMapper.deleteMallProductSkuBySpuId(mallProductSpu.getId());
        insertMallProductSku(mallProductSpu);
        int rows = mallProductSpuMapper.updateMallProductSpu(mallProductSpu);
        syncSearchIndex(mallProductSpu.getId(), "UPSERT");
        return rows;
    }

    /**
     * 批量删除SPU 管理
     *
     * @param ids 需要删除的SPU 管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallProductSpuByIds(String[] ids)
    {
        mallProductSpuMapper.deleteMallProductSkuBySpuIds(ids);
        int rows = mallProductSpuMapper.deleteMallProductSpuByIds(ids);
        for (String id : ids)
        {
            syncSearchIndex(id, "DELETE");
        }
        return rows;
    }

    /**
     * 删除SPU 管理信息
     *
     * @param id SPU 管理主键
     * @return 结果
     */
    @Transactional
    @Override
    public int deleteMallProductSpuById(String id)
    {
        mallProductSpuMapper.deleteMallProductSkuBySpuId(id);
        int rows = mallProductSpuMapper.deleteMallProductSpuById(id);
        syncSearchIndex(id, "DELETE");
        return rows;
    }

    /**
     * 触发搜索索引同步（尽力而为）
     *
     * <p>ES 故障不能阻断管理端的商品维护：失败只记日志——mall-product 的实时同步
     * 在 Feign 失败时会写 Outbox，由补偿任务兜底。</p>
     *
     * @param spuId     SPU ID
     * @param operation UPSERT（新增/更新）或 DELETE
     */
    private void syncSearchIndex(String spuId, String operation)
    {
        if (StringUtils.isEmpty(spuId))
        {
            return;
        }
        try
        {
            remoteProductService.syncSearchIndex(Long.valueOf(spuId), operation);
        }
        catch (Exception e)
        {
            log.error("触发搜索索引同步失败，将由 Outbox 补偿兜底: spuId={}, operation={}",
                    spuId, operation, e);
        }
    }

    /**
     * 新增SKU 管理信息
     *
     * @param mallProductSpu SPU 管理对象
     */
    public void insertMallProductSku(MallProductSpu mallProductSpu)
    {
        List<MallProductSku> mallProductSkuList = mallProductSpu.getMallProductSkuList();
        String id = mallProductSpu.getId();
        if (StringUtils.isNotNull(mallProductSkuList))
        {
            List<MallProductSku> list = new ArrayList<MallProductSku>();
            for (MallProductSku mallProductSku : mallProductSkuList)
            {
                mallProductSku.setSpuId(id);
                list.add(mallProductSku);
            }
            if (list.size() > 0)
            {
                mallProductSpuMapper.batchMallProductSku(list);
            }
        }
    }
}
