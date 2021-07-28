package com.atguigu.gulimall.ware.dao;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品库存
 * 
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-07-23 17:20:08
 */
@Mapper
public interface WareSkuDao extends BaseMapper<WareSkuEntity> {

    Long getSkuStock(Long item);
}
