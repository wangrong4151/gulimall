package com.atguigu.gulimall.product.service;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品三级分类
 *
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-05-10 20:46:36
 */
public interface CategoryService extends IService<CategoryEntity> {

    PageUtils queryPage(Map<String, Object> params);

    List<CategoryEntity> listWithTree();

    R removeByIds(List<Long> ids);

    Long[] findCateLogPath(Long catelogId);

    void updateCascade(CategoryEntity category);

    Map<String, List<Catelog2Vo>> getCatalogJson();

    List<CategoryEntity> getLevel1Categorys();

    IPage<CategoryEntity> lista(Integer currentPage, Integer pageSize);
}

