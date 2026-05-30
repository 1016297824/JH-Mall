package com.mall.product.convert.response;

import com.mall.product.DO.MallCategoryDO;
import com.mall.product.VO.CategoryVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryConvertTest {

    @Test
    void toCategoryVOShouldConvertFields() {
        MallCategoryDO categoryDO = new MallCategoryDO();
        categoryDO.setId(1L);
        categoryDO.setParentId(0L);
        categoryDO.setName("手机数码");
        categoryDO.setLevel(1);
        categoryDO.setIcon("/icon.png");
        categoryDO.setSortOrder(1);
        categoryDO.setPath("/1");

        CategoryVO vo = CategoryConvert.toCategoryVO(categoryDO);

        assertThat(vo.getCategoryId()).isEqualTo("1");
        assertThat(vo.getParentId()).isEqualTo("0");
        assertThat(vo.getName()).isEqualTo("手机数码");
        assertThat(vo.getLevel()).isEqualTo(1);
        assertThat(vo.getIcon()).isEqualTo("/icon.png");
        assertThat(vo.getSortOrder()).isEqualTo(1);
        assertThat(vo.getPath()).isEqualTo("/1");
        assertThat(vo.getChildren()).isEmpty();
    }

    @Test
    void buildTreeShouldBuildThreeLevelTree() {
        MallCategoryDO root = categoryDO(1L, 0L, "手机数码", 1, "/1");
        MallCategoryDO l2 = categoryDO(2L, 1L, "手机通讯", 2, "/1/2");
        MallCategoryDO l3 = categoryDO(3L, 2L, "智能手机", 3, "/1/2/3");
        List<MallCategoryDO> list = Arrays.asList(root, l2, l3);

        List<CategoryVO> tree = CategoryConvert.buildTree(list);

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("手机数码");
        assertThat(tree.get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getName()).isEqualTo("手机通讯");
        assertThat(tree.get(0).getChildren().get(0).getChildren()).hasSize(1);
        assertThat(tree.get(0).getChildren().get(0).getChildren().get(0).getName()).isEqualTo("智能手机");
    }

    private MallCategoryDO categoryDO(Long id, Long parentId, String name, Integer level, String path) {
        MallCategoryDO d = new MallCategoryDO();
        d.setId(id);
        d.setParentId(parentId);
        d.setName(name);
        d.setLevel(level);
        d.setIcon("/icon.png");
        d.setSortOrder(1);
        d.setPath(path);
        return d;
    }
}
