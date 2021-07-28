package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.vo.BrandsVo;
import com.atguigu.gulimall.product.vo.CatelogVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;

import java.util.List;
import java.util.Map;

/**
 * 品牌分类关联
 *
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-05-10 20:46:36
 */
public interface CategoryBrandRelationService extends IService<CategoryBrandRelationEntity> {

    PageUtils queryPage(Map<String, Object> params);


    List<BrandsVo> brandsList(Map<String, Object> params);

    List<CatelogVo> withCatelog(Map<String, Object> params);
    List<BrandEntity> getBrandsByCatId(Long catId);
}

