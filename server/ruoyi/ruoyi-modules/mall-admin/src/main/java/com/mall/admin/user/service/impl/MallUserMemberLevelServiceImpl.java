package com.mall.admin.user.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.mall.admin.user.mapper.MallUserMemberLevelMapper;
import com.mall.admin.user.domain.MallUserMemberLevel;
import com.mall.admin.user.service.IMallUserMemberLevelService;

/**
 * 会员等级定义Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-18
 */
@Service
public class MallUserMemberLevelServiceImpl implements IMallUserMemberLevelService
{
    @Autowired
    private MallUserMemberLevelMapper mallUserMemberLevelMapper;

    /**
     * 查询会员等级定义
     *
     * @param id 会员等级定义主键
     * @return 会员等级定义
     */
    @Override
    public MallUserMemberLevel selectMallUserMemberLevelById(String id)
    {
        return mallUserMemberLevelMapper.selectMallUserMemberLevelById(id);
    }

    /**
     * 查询会员等级定义列表
     *
     * @param mallUserMemberLevel 会员等级定义
     * @return 会员等级定义
     */
    @Override
    public List<MallUserMemberLevel> selectMallUserMemberLevelList(MallUserMemberLevel mallUserMemberLevel)
    {
        return mallUserMemberLevelMapper.selectMallUserMemberLevelList(mallUserMemberLevel);
    }

    /**
     * 新增会员等级定义
     *
     * @param mallUserMemberLevel 会员等级定义
     * @return 结果
     */
    @Override
    public int insertMallUserMemberLevel(MallUserMemberLevel mallUserMemberLevel)
    {
        mallUserMemberLevel.setCreateTime(DateUtils.getNowDate());
        return mallUserMemberLevelMapper.insertMallUserMemberLevel(mallUserMemberLevel);
    }

    /**
     * 修改会员等级定义
     *
     * @param mallUserMemberLevel 会员等级定义
     * @return 结果
     */
    @Override
    public int updateMallUserMemberLevel(MallUserMemberLevel mallUserMemberLevel)
    {
        mallUserMemberLevel.setUpdateTime(DateUtils.getNowDate());
        return mallUserMemberLevelMapper.updateMallUserMemberLevel(mallUserMemberLevel);
    }

    /**
     * 批量删除会员等级定义
     *
     * @param ids 需要删除的会员等级定义主键
     * @return 结果
     */
    @Override
    public int deleteMallUserMemberLevelByIds(String[] ids)
    {
        return mallUserMemberLevelMapper.deleteMallUserMemberLevelByIds(ids);
    }

    /**
     * 删除会员等级定义信息
     *
     * @param id 会员等级定义主键
     * @return 结果
     */
    @Override
    public int deleteMallUserMemberLevelById(String id)
    {
        return mallUserMemberLevelMapper.deleteMallUserMemberLevelById(id);
    }
}
