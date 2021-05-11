package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        List<CategoryEntity> entities = baseMapper.selectList(null);
        List<CategoryEntity> menu = new ArrayList<>();
        for (CategoryEntity entity : entities) {
            if (entity.getParentCid()==0){
                entity.setChildrens(getCategory(entity,entities));
                menu.add(entity);
            }
        }

        return menu;
    }

    @Override
    public R removeByIds(List<Long> ids) {
        //TOD0 检查当前菜单是否被其他地方引用
        List<CategoryBrandRelationEntity> list = categoryBrandRelationService.list(new QueryWrapper<CategoryBrandRelationEntity>().in("catelog_id", ids));
        if(list.size()>0){
            throw new RuntimeException("该菜单下还有其他属性，无法删除");
        }else{
            //逻辑删除
            baseMapper.deleteBatchIds(ids);
            return R.ok();
        }

    }

    @Override
    public Long[] findCateLogPath(Long catelogId) {
        List<Long> paths = new ArrayList<>();
        paths=findParentPath(catelogId,paths);
        // 收集的时候是顺序 前端是逆序显示的 所以用集合工具类给它逆序一下
        // 子父 转 父子
        Collections.reverse(paths);

        return paths.toArray(new Long[paths.size()]);
    }

    private List<Long> findParentPath(Long catelogId,List<Long> paths) {

        //收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

    private List<CategoryEntity> getCategory(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> list = new ArrayList<>();
        for (CategoryEntity categoryEntity : all) {
            if(categoryEntity.getCatId().equals(root.getParentCid())){
                categoryEntity.setChildrens(getCategory(categoryEntity,all));
                list.add(categoryEntity);
            }
        }
        return list;
    }

}