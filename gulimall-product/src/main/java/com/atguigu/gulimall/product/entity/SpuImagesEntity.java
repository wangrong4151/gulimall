package com.atguigu.gulimall.product.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * spu图片
 *
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-05-10 20:46:36
 */
@Data
@TableName("pms_spu_images")
public class SpuImagesEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId
    private Long id;
    /**
     * spu_id
     */
    private Long spuId;
    /**
     * 图片名
     */
    private String imgName;
    /**
     * 图片地址
     */
    private String imgUrl;
    /**
     * 顺序
     */
    private Integer imgSort;
    /**
     * 是否默认图
     */
    private Integer defaultImg;

}
