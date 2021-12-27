package com.atguigu.gulimall.product.service.impl;

import com.atguigu.common.constant.ProductConstant;
import com.atguigu.gulimall.product.dao.AttrAttrgroupRelationDao;
import com.atguigu.gulimall.product.dao.AttrGroupDao;
import com.atguigu.gulimall.product.dao.CategoryDao;
import com.atguigu.gulimall.product.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gulimall.product.entity.AttrGroupEntity;
import com.atguigu.gulimall.product.entity.CategoryEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.vo.AttrGroupWithAttrVo;
import com.atguigu.gulimall.product.vo.AttrInfoVo;
import com.atguigu.gulimall.product.vo.AttrRespVo;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.AttrDao;
import com.atguigu.gulimall.product.entity.AttrEntity;
import com.atguigu.gulimall.product.service.AttrService;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;


@Service("attrService")
public class AttrServiceImpl extends ServiceImpl<AttrDao, AttrEntity> implements AttrService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;
    @Autowired
    private AttrGroupDao attrGroupDao;
    @Autowired
    @Resource
    private CategoryDao categoryDao;
    @Autowired
    private AttrDao attrDao;
    @Autowired
    private AttrAttrgroupRelationDao attrAttrgroupRelationDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {

        IPage<AttrEntity> page = this.page(
                new Query<AttrEntity>().getPage(params),
                new QueryWrapper<AttrEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 根据分组id找到关联的所有属性
     *
     * @param attrgroupId
     * @return
     */
    @Override
    public List<AttrEntity> getRelationAttr(Long attrgroupId) {

        List<AttrAttrgroupRelationEntity> entities = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrgroupId));
        List<Long> list = new ArrayList<>();
        for (AttrAttrgroupRelationEntity entity : entities) {
            list.add(entity.getAttrId());
        }
        if (list == null || list.size() < 0) {
            return null;
        }
        List<AttrEntity> attrEntities = this.baseMapper.selectBatchIds(list);
        return attrEntities;
    }

    @Override
    public PageUtils getBaseInfo(Map<String, Object> params, Long catelogId) {

        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>();
        if (catelogId != 0) {
            wrapper.eq("catelog_id", catelogId);
        }

        String key = (String) params.get("key");

        if (!StringUtils.isEmpty(key)) {
            wrapper.and(obj -> {
                obj.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);

        PageUtils pageUtils = new PageUtils(page);
        List<AttrEntity> records = page.getRecords();
        List<AttrRespVo> attrRespVos = records.stream().map(attrEntity -> {
            AttrRespVo attrRespVo = new AttrRespVo();
            BeanUtils.copyProperties(attrEntity, attrRespVo);
            AttrAttrgroupRelationEntity attr_id = relationDao.selectOne(new QueryWrapper<AttrAttrgroupRelationEntity>().
                    eq("attr_id", attrEntity.getAttrId()));
            if (attr_id != null) {
                String attrGroupName = attrGroupDao.selectById(attr_id.getAttrGroupId()).getAttrGroupName();
                attrRespVo.setGroupName(attrGroupName);
            }
            String name = categoryDao.selectById(attrEntity.getCatelogId()).getName();
            attrRespVo.setCatelogName(name);
            return attrRespVo;
        }).collect(Collectors.toList());

        pageUtils.setList(attrRespVos);
        return pageUtils;
    }

    @Override
    public AttrInfoVo getInfoById(Long attrId) {

        AttrEntity attrEntity = attrDao.selectById(attrId);
        AttrInfoVo attrInfoVo = new AttrInfoVo();
        BeanUtils.copyProperties(attrEntity, attrInfoVo);
        int attrGroupId = relationDao.deleteById(attrId);
        attrInfoVo.setAttrGroupId(attrGroupId);
        List<Long> list = new ArrayList<>();


        CategoryEntity categoryEntity = categoryDao.selectById(attrEntity.getCatelogId());
        List<Long> catelogIds = getCatelogIds(categoryEntity, list);
        Collections.reverse(catelogIds);
        Long[] longs = catelogIds.toArray(new Long[0]);
        attrInfoVo.setCatelogPath(longs);
        return attrInfoVo;
    }

    @Override
    public PageUtils attrWithRelation(Map<String, Object> params, Long catelogId) {


        List<AttrGroupEntity> attrGroupEntities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        List<AttrGroupWithAttrVo> list = new ArrayList<>();
        attrGroupEntities.stream().forEach(attrGroupEntity -> {
            AttrGroupWithAttrVo attrGroupWithAttrVo = new AttrGroupWithAttrVo();
            BeanUtils.copyProperties(attrGroupEntity, attrGroupWithAttrVo);
            List<AttrAttrgroupRelationEntity> attrgroupRelationEntities = attrAttrgroupRelationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", attrGroupEntity.getAttrGroupId()));
            for (AttrAttrgroupRelationEntity attrgroupRelationEntity : attrgroupRelationEntities) {
                List<AttrEntity> attrEntityList = attrDao.selectList(new QueryWrapper<AttrEntity>().eq("attr_id", attrgroupRelationEntity.getAttrId()));
                attrGroupWithAttrVo.setAttrs(attrEntityList);
            }
            list.add(attrGroupWithAttrVo);
        });

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params));
        PageUtils pageUtils = new PageUtils(page);
        pageUtils.setList(list);


        return pageUtils;
    }

    @Override
    public List<Long> selectSearchAttrs(List<Long> attrIds) {

        List<Long> searchAttrIds = this.baseMapper.selectSearchAttrIds(attrIds);

        return searchAttrIds;
    }

    private List<Long> getCatelogIds(CategoryEntity categoryEntity, List<Long> list) {

        list.add(categoryEntity.getCatId());
        if (categoryEntity.getParentCid() != 0) {
            getCatelogIds(categoryDao.selectById(categoryEntity.getParentCid()), list);
        }
        return list;

    }

    private List<String> getCatelogName(Long catelogId) {
        List<String> list = new ArrayList<>();
        CategoryEntity categoryEntity = categoryDao.selectById(catelogId);
        if (categoryEntity.getParentCid() != 0) {
            list.add(categoryEntity.getName());
            getCatelogName(categoryEntity.getParentCid());
        }
        return list;
    }

    @Override
    @Transactional
    public PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId) {

        //1、当前分组只能关联自己所属的分类里面的所有属性
        AttrGroupEntity attrGroupEntity = attrGroupDao.selectById(attrgroupId);
        //获取当前分类的id
        Long catelogId = attrGroupEntity.getCatelogId();
        //2、当前分组只能关联别的分组没有引用的属性
        //2.1）、当前分类下的其它分组
        List<AttrGroupEntity> entities = attrGroupDao.selectList(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catelogId));
        //获取到所有的attrGroupId
        List<Long> list = new ArrayList<>();
        for (AttrGroupEntity entity : entities) {
            list.add(entity.getAttrGroupId());
        }

        //2.2）、这些分组关联的属性
        List<AttrAttrgroupRelationEntity> groupId = relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().in("attr_group_id", list));
        List<Long> attrIds = groupId.stream().map(item -> {
            return item.getAttrId();
        }).collect(Collectors.toList());

        //2.3）、从当前分类的所有属性移除这些属性
        QueryWrapper<AttrEntity> wrapper = new QueryWrapper<AttrEntity>().eq("catelog_id", catelogId).eq("attr_type", ProductConstant.AttrEnum.ATTR_TYPE_BASE.getCode());
        if (attrIds != null || attrIds.size() > 0) {
            wrapper.notIn("attr_id", attrIds);
        }

        //判断是否有参数进行模糊查询
        String key = (String) params.get("key");
        if (!StringUtils.isEmpty(key)) {
            wrapper.and(obj -> {
                obj.eq("attr_id", key).or().like("attr_name", key);
            });
        }

        IPage<AttrEntity> page = this.page(new Query<AttrEntity>().getPage(params), wrapper);
        PageUtils pageUtils = new PageUtils(page);

        return pageUtils;
    }

}