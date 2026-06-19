package com.mall.product.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mall.common.DTO.PageResult;
import com.mall.common.DTO.product.SpuSearchDTO;
import com.mall.product.DO.MallBrandDO;
import com.mall.product.DO.MallCategoryDO;
import com.mall.product.DO.MallProductSkuDO;
import com.mall.product.DO.MallProductSpuDO;
import com.mall.product.VO.SpuDetailVO;
import com.mall.product.VO.SpuVO;
import com.mall.product.mapper.MallBrandMapper;
import com.mall.product.mapper.MallCategoryMapper;
import com.mall.product.mapper.MallProductSkuMapper;
import com.mall.product.mapper.MallProductSpuMapper;
import com.mall.product.service.IHotProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link SpuServiceImpl} 单元测试
 *
 * <p>覆盖分页查询、详情（含 UV 记录）、热点列表等核心场景</p>
 *
 * @author JH-Mall
 * @date 2026/06/01
 */
@ExtendWith(MockitoExtension.class)
class SpuServiceImplTest {

    @Mock
    private MallProductSpuMapper mallProductSpuMapper;

    @Mock
    private MallProductSkuMapper mallProductSkuMapper;

    @Mock
    private MallCategoryMapper mallCategoryMapper;

    @Mock
    private MallBrandMapper mallBrandMapper;

    /** 热点商品服务（用于验证 UV 记录调用） */
    @Mock
    private IHotProductService hotProductService;

    @InjectMocks
    private SpuServiceImpl spuService;

    @Test
    void pageShouldReturnSpuList() {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setSpuName("iPhone");
        spuDO.setPublishStatus(1);
        spuDO.setVerifyStatus(1);
        spuDO.setCategoryId(10L);
        spuDO.setBrandId(20L);
        spuDO.setPriceMin(699900L);
        spuDO.setPriceMax(899900L);
        spuDO.setSalesCount(100);

        Page<MallProductSpuDO> mockPage = new Page<>(1, 20, 1);
        mockPage.setRecords(List.of(spuDO));
        when(mallProductSpuMapper.selectPublishedPage(any(), any(), any(), any())).thenReturn(mockPage);

        PageResult<SpuVO> result = spuService.page(1, 20, null, null, null, null);

        assertThat(result.getTotal()).isEqualTo(1L);
        assertThat(result.getRows()).hasSize(1);
        assertThat(result.getRows().get(0).getSpuName()).isEqualTo("iPhone");
    }

    @Test
    void detailShouldReturnSpuWithSkus() {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setSpuName("iPhone");
        spuDO.setPublishStatus(1);
        spuDO.setVerifyStatus(1);
        spuDO.setCategoryId(10L);
        spuDO.setBrandId(20L);
        spuDO.setPriceMin(699900L);
        spuDO.setPriceMax(899900L);
        spuDO.setSalesCount(100);
        spuDO.setSpuDescription("<p>test</p>");

        MallProductSkuDO sku = new MallProductSkuDO();
        sku.setId(101L);
        sku.setSkuName("256GB");
        sku.setPrice(699900L);
        sku.setImage("/sku.jpg");

        when(mallProductSpuMapper.selectById(1L)).thenReturn(spuDO);
        when(mallProductSkuMapper.selectBySpuId(1L)).thenReturn(List.of(sku));

        SpuDetailVO detail = spuService.detail(1L);

        assertThat(detail.getSpuId()).isEqualTo("1");
        assertThat(detail.getSkus()).hasSize(1);
        assertThat(detail.getSkus().get(0).getSkuName()).isEqualTo("256GB");
    }

    @Test
    void detailShouldThrowWhenNotPublished() {
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setPublishStatus(0);
        spuDO.setVerifyStatus(1);
        when(mallProductSpuMapper.selectById(1L)).thenReturn(spuDO);

        assertThatThrownBy(() -> spuService.detail(1L))
                .isInstanceOf(com.mall.common.exception.BusinessException.class);
    }

    /**
     * 搜索索引重建专用分页查询：应返回含类目名、品牌名、SKU 规格拼接的富 DTO
     */
    @Test
    void pageForSearchRebuildShouldReturnSpuSearchDTOWithJoinedData() {
        // 构造 SPU DO
        MallProductSpuDO spuDO = new MallProductSpuDO();
        spuDO.setId(1L);
        spuDO.setSpuName("iPhone 15");
        spuDO.setSpuDescription("Apple 最新旗舰手机，A17 Pro 芯片");
        spuDO.setMainImage("/images/iphone15.jpg");
        spuDO.setPriceMin(699900L);
        spuDO.setPublishStatus(1);
        spuDO.setSalesCount(500);
        spuDO.setCategoryId(10L);
        spuDO.setBrandId(20L);
        spuDO.setCreateTime(java.time.LocalDateTime.of(2026, 1, 1, 10, 0));
        spuDO.setUpdateTime(java.time.LocalDateTime.of(2026, 6, 1, 12, 0));

        Page<MallProductSpuDO> mockPage = new Page<>(1, 10, 1);
        mockPage.setRecords(List.of(spuDO));
        when(mallProductSpuMapper.selectAllPage(any())).thenReturn(mockPage);

        // Mock 类目
        MallCategoryDO categoryDO = new MallCategoryDO();
        categoryDO.setId(10L);
        categoryDO.setName("手机");
        when(mallCategoryMapper.selectById(10L)).thenReturn(categoryDO);

        // Mock 品牌
        MallBrandDO brandDO = new MallBrandDO();
        brandDO.setId(20L);
        brandDO.setName("Apple");
        when(mallBrandMapper.selectById(20L)).thenReturn(brandDO);

        // Mock SKU 规格
        MallProductSkuDO sku1 = new MallProductSkuDO();
        sku1.setAttrsJson("[{\"k\":\"颜色\",\"v\":\"黑色\"},{\"k\":\"容量\",\"v\":\"128GB\"}]");
        MallProductSkuDO sku2 = new MallProductSkuDO();
        sku2.setAttrsJson("[{\"k\":\"颜色\",\"v\":\"白色\"},{\"k\":\"容量\",\"v\":\"256GB\"}]");
        when(mallProductSkuMapper.selectBySpuId(1L)).thenReturn(List.of(sku1, sku2));

        // 执行
        PageResult<SpuSearchDTO> result = spuService.pageForSearchRebuild(1, 10);

        // 验证
        assertThat(result).isNotNull();
        assertThat(result.getRows()).isNotEmpty();
        SpuSearchDTO first = result.getRows().get(0);
        assertThat(first.getSpuId()).isEqualTo(1L);
        assertThat(first.getSpuName()).isEqualTo("iPhone 15");
        assertThat(first.getCategoryId()).isEqualTo(10L);
        assertThat(first.getCategoryName()).isEqualTo("手机");
        assertThat(first.getBrandId()).isEqualTo(20L);
        assertThat(first.getBrandName()).isEqualTo("Apple");
        assertThat(first.getSpuSpecs()).isNotEmpty();
        assertThat(first.getPublishStatus()).isEqualTo(1);
        assertThat(first.getSalesCount()).isEqualTo(500);
        assertThat(first.getMainImage()).isEqualTo("/images/iphone15.jpg");
        assertThat(first.getPriceMin()).isEqualTo(699900L);
        assertThat(first.getSubTitle()).isEqualTo("Apple 最新旗舰手机，A17 Pro 芯片");
        assertThat(first.getCreateTime()).isNotNull();
        assertThat(first.getUpdateTime()).isNotNull();
    }
}
