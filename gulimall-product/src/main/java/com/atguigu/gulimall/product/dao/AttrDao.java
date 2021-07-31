package com.atguigu.gulimall.product.dao;

import com.atguigu.gulimall.product.entity.AttrEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 商品属性
 * 
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-05-10 20:46:36
 */
@Mapper
public interface AttrDao extends BaseMapper<AttrEntity> {

    AttrEntity selectByCatelogId(Long catelogId);

    List<Long> selectSearchAttrIds(List<Long> attrIds);
}
