package com.atguigu.gulimall.product.service;

import com.atguigu.gulimall.product.vo.AttrInfoVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.product.entity.AttrEntity;

import java.util.List;
import java.util.Map;

/**
 * 商品属性
 *
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-05-10 20:46:36
 */
public interface AttrService extends IService<AttrEntity> {

    PageUtils queryPage(Map<String, Object> params);

    PageUtils getNoRelationAttr(Map<String, Object> params, Long attrgroupId);

    List<AttrEntity> getRelationAttr(Long attrgroupId);

    PageUtils getBaseInfo(Map<String, Object> params, Long catelogId);

    AttrInfoVo getInfoById(Long attrId);

    PageUtils attrWithRelation(Map<String, Object> params, Long catelogId);

    List<Long> selectSearchAttrs(List<Long> attrIds);
}

