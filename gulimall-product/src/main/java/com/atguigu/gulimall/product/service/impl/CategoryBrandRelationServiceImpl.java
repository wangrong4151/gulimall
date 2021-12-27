package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.dao.BrandDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.vo.BrandsVo;
import com.atguigu.gulimall.product.vo.CatelogVo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryBrandRelationDao;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;


@Service("categoryBrandRelationService")
public class CategoryBrandRelationServiceImpl extends ServiceImpl<CategoryBrandRelationDao, CategoryBrandRelationEntity> implements CategoryBrandRelationService {

    @Autowired
    private CategoryBrandRelationDao categoryBrandRelationDao;
    @Autowired
    private BrandDao brandDao;
    @Autowired
    private CategoryDao categoryDao;
    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private CategoryBrandRelationDao relationDao;
    @Autowired
    private BrandService brandService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryBrandRelationEntity> page = this.page(
                new Query<CategoryBrandRelationEntity>().getPage(params),
                new QueryWrapper<CategoryBrandRelationEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<BrandsVo> brandsList(Map<String, Object> params) {

        Long catId = Long.parseLong((String) params.get("catId"));
        List<BrandEntity> vos = categoryBrandRelationService.getBrandsByCatId(catId);

        List<BrandsVo> collect = vos.stream().map(item -> {
            BrandsVo brandVo = new BrandsVo();
            brandVo.setBrandId(item.getBrandId());
            brandVo.setBrandName(item.getName());
            return brandVo;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<CatelogVo> withCatelog(Map<String, Object> params) {

        String brandId = (String) params.get("brandId");
        if (StringUtils.isEmpty(brandId)) {
            throw new RuntimeException("brandId不能为空");
        }
        List<CategoryBrandRelationEntity> brandRelationEntities = categoryBrandRelationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("brand_id", brandId));
        List<CatelogVo> collect = brandRelationEntities.stream().map(brandRelationEntity -> {
            CategoryEntity categoryEntity = categoryDao.selectById(brandRelationEntity.getCatelogId());
            CatelogVo catelogVo = new CatelogVo();
            catelogVo.setCatelogId(categoryEntity.getCatId());
            catelogVo.setCatelogName(categoryEntity.getName());
            return catelogVo;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public List<BrandEntity> getBrandsByCatId(Long catId) {

        List<CategoryBrandRelationEntity> catelogId = relationDao.selectList(new QueryWrapper<CategoryBrandRelationEntity>().eq("catelog_id", catId));

        List<BrandEntity> collect = catelogId.stream().map(item -> {
            Long brandId = item.getBrandId();
            //查询品牌的详情
            BrandEntity byId = brandService.getById(brandId);
            return byId;
        }).collect(Collectors.toList());

        return collect;
    }

    @Override
    public void updateCategory(Long catId, String name) {
        // 这里可以按照上面的方法来，但是这里使用了另外一种方式
        this.baseMapper.updateCategory(catId, name);
    }

}