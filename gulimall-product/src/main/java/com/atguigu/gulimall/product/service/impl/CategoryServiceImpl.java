package com.atguigu.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.entity.CategoryBrandRelationEntity;
import com.atguigu.gulimall.product.service.CategoryBrandRelationService;
import com.atguigu.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jcajce.provider.asymmetric.ec.KeyFactorySpi;
import org.redisson.Redisson;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private RedissonClient redisson;
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
                entity.setChildren(getCategory(entity,entities));
                menu.add(entity);
            }
        }

        return menu;
    }
    private List<CategoryEntity> getCategory(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> list = new ArrayList<>();
        for (CategoryEntity categoryEntity : all) {
            if(categoryEntity.getParentCid().equals(root.getCatId())){
                categoryEntity.setChildren(getCategory(categoryEntity,all));
                list.add(categoryEntity);
            }
        }
        return list;
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
    //@CacheEvict(value = {"category"},allEntries = true)
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        RLock lock = redisson.getLock("catalogJson");
        lock.lock();
        try {
            this.updateById(category);
            if (!StringUtils.isEmpty(category.getName())) {
                categoryBrandRelationService.updateCategory(category.getCatId(), category.getName());

                // TODO 更新其他关联
            }
            stringRedisTemplate.delete("catalogJson");
        }catch (Exception e){
            e.getStackTrace();
        }finally {
            lock.unlock();
        }

    }
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String catalogJson = valueOperations.get("catalogJson");
        if (StringUtils.isEmpty(catalogJson)) {
            RLock lock = redisson.getLock("catalogJson");
            lock.lock();
            try {
                Map<String, List<Catelog2Vo>> dataBd = getDataBd();
                valueOperations.set("catalogJson",JSON.toJSONString(dataBd),1, TimeUnit.DAYS);
                return dataBd;
            }catch (Exception e){
                e.getStackTrace();
            }finally {
                lock.unlock();
            }

        }
        Map<String, List<Catelog2Vo>> map = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>(){});

        return map;
    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
    }

    private Map<String, List<Catelog2Vo>> getDataBd() {

        //再次判断缓存中是否有数据
        ValueOperations<String, String> valueOperations = stringRedisTemplate.opsForValue();
        String catalogJson = valueOperations.get("catalogJson");
        if (!StringUtils.isEmpty(catalogJson)) {
            Map<String, List<Catelog2Vo>> map = JSON.parseObject(catalogJson, new TypeReference<Map<String, List<Catelog2Vo>>>() {});
            return map;
        }


                /**
                 * 将数据库的多次查询变为一次
                 */
                List<CategoryEntity> selectList = this.baseMapper.selectList(null);

                //1、查出所有分类
                //1、1）查出所有一级分类
                List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

                //封装数据
                Map<String, List<Catelog2Vo>> parentCid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                    //1、每一个的一级分类,查到这个一级分类的二级分类
                    List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());

                    //2、封装上面的结果
                    List<Catelog2Vo> catelog2Vos = null;
                    if (categoryEntities != null) {
                        catelog2Vos = categoryEntities.stream().map(l2 -> {
                            Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName().toString());

                            //1、找当前二级分类的三级分类封装成vo
                            List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());

                            if (level3Catelog != null) {
                                List<Catelog2Vo.Category3Vo> category3Vos = level3Catelog.stream().map(l3 -> {
                                    //2、封装成指定格式
                                    Catelog2Vo.Category3Vo category3Vo = new Catelog2Vo.Category3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());

                                    return category3Vo;
                                }).collect(Collectors.toList());
                                catelog2Vo.setCatalog3List(category3Vos);
                            }

                            return catelog2Vo;
                        }).collect(Collectors.toList());
                    }

                    return catelog2Vos;
                }));

            String s = JSON.toJSONString(parentCid);
            valueOperations.set("catalogJson",s,1,TimeUnit.DAYS);
            return parentCid;
        }


    /**
     * 查询出父ID为 parent_cid的List集合
     */
    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList, Long parent_cid) {
        return selectList.stream().filter(item -> item.getParentCid() == parent_cid).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", level.getCatId()));
    }

}