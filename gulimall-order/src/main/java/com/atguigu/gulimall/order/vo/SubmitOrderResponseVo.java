package com.atguigu.gulimall.order.vo;

import com.atguigu.gulimall.order.entity.OrderEntity;
import lombok.Data;

@Data
public class SubmitOrderResponseVo {

    // 该实体为order表的映射
    private OrderEntity order;

    /**
     * 错误状态码
     **/
    private Integer code;
}

