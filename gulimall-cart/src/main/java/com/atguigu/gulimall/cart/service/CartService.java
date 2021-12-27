package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;

import java.util.List;
import java.util.concurrent.ExecutionException;

public interface CartService {
    List<CartItemVo> getUserCartItems();

    CartVo getCart() throws ExecutionException, InterruptedException;


    void clearCartInfo(String cartKey);

    CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;

    CartItemVo getCartItem(Long skuId);

    void changeItemCount(Long skuId, Integer num);

    void deleteIdCartInfo(Integer skuId);

    void checkItem(Long skuId, Integer checked);
}
