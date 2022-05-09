package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.exception.CartExceptionHandler;
import com.atguigu.gulimall.cart.fegin.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.to.UserInfoTo;
import com.atguigu.gulimall.cart.vo.CartItemVo;
import com.atguigu.gulimall.cart.vo.CartVo;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.atguigu.common.constant.CarConstant.CART_PREFIX;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ProductFeignService productFeignService;
    @Autowired
    private ThreadPoolExecutor executor;

    @Override
    public List<CartItemVo> getUserCartItems() {

        List<CartItemVo> cartItemVoList = new ArrayList<>();
        //获取当前用户登录的信息
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        //如果用户未登录直接返回null
        if (userInfoTo.getUserId() == null) {
            return null;
        } else {
            //获取购物车项
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            //获取所有的
            List<CartItemVo> cartItems = getCartItems(cartKey);
            if (cartItems == null) {
                throw new CartExceptionHandler();
            }
            //筛选出选中的
           /* cartItemVoList = cartItems.stream()
                    .filter(items -> items.getCheck())
                    .map(item -> {
                        //更新为最新的价格（查询数据库）
                        BigDecimal price = productFeignService.getPrice(item.getSkuId());
                        item.setPrice(price);
                        return item;
                    }).collect(Collectors.toList());*/

            for (CartItemVo cartItem : cartItems) {
                if(cartItem.getCheck()){
                    BigDecimal price = productFeignService.getPrice(cartItem.getSkuId());
                    cartItem.setPrice(price);
                    cartItemVoList.add(cartItem);
                }
            }
        }

        return cartItemVoList;
    }

    @Override
    public CartVo getCart() throws ExecutionException, InterruptedException {
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        CartVo cartVo = new CartVo();
        // 临时购物车的key
        String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
        //是否登录
        if (userInfoTo.getUserId() != null) {
            // 已登录 对用户的购物车进行操作
            String cartKey = CART_PREFIX + userInfoTo.getUserId();
            // 1 如果临时购物车的数据没有进行合并
            List<CartItemVo> tempItems = getCartItems(tempCartKey);
            if (!CollectionUtils.isEmpty(tempItems)) {
                // 2 临时购物车有数据 则进行合并
                //log.info("\n[" + userInfoTo.getUsername() + "] 的购物车已合并");
                for (CartItemVo tempItem : tempItems) {
                    addToCart(tempItem.getSkuId(), tempItem.getCount());
                }
                //清除临时购物车的数据
                clearCartInfo(tempCartKey);
            }
            //3、获取登录后的购物车数据【包含合并过来的临时购物车的数据和登录后购物车的数据】
            List<CartItemVo> cartItems = getCartItems(cartKey);
            cartVo.setItems(cartItems);
        } else {
            // 没登录 获取临时购物车的所有购物项
            cartVo.setItems(getCartItems(tempCartKey));
        }

        return cartVo;
    }

    @Override
    public void clearCartInfo(String cartKey) {
        redisTemplate.delete(cartKey);
    }

    @Override
    public CartItemVo addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        // 获取当前用户的map
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        Object res = cartOps.get(skuId.toString());


        // 查看用户购物车里是否已经有了该sku项
        if (ObjectUtils.isEmpty(res)) {
            CartItemVo itemVo = new CartItemVo();
            CompletableFuture<Void> skuFuture = CompletableFuture.runAsync(() -> {

                // 1. 远程查询当前要添加的商品的信息
                R r = productFeignService.getInfo(skuId);
                if (r.getCode() == 0) {
                    SkuInfoVo skuInfo = r.getData("skuInfo", new TypeReference<SkuInfoVo>() {
                    });
                    if (skuInfo != null) {
                        // 2. 填充购物项
                        itemVo.setCheck(true);
                        itemVo.setCount(num);
                        itemVo.setImage(skuInfo.getSkuDefaultImg());
                        itemVo.setPrice(skuInfo.getPrice());
                        itemVo.setTitle(skuInfo.getSkuTitle());
                        itemVo.setSkuId(skuId);
                    }
                }
            }, executor);
            // 3. 远程查询sku销售属性，销售属性是个list
            CompletableFuture<Void> skuSaleFuture = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                if (!CollectionUtils.isEmpty(skuSaleAttrValues)) {
                    itemVo.setSkuAttrValues(skuSaleAttrValues);
                }
            }, executor);

            //等待任务完成
            CompletableFuture.allOf(skuFuture, skuSaleFuture).get();
            // sku放到用户购物车redis中
            cartOps.put(skuId.toString(), JSON.toJSON(itemVo));
            return itemVo;
        } else {
            CartItemVo cartItemVo = JSON.parseObject(res.toString(), CartItemVo.class);
            //购物车里已经有该sku了，数量+1即可
            cartItemVo.setCount(cartItemVo.getCount() + num);
            // 不太可能并发，无需加锁
            cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVo));
            return cartItemVo;
        }

    }

    @Override
    public CartItemVo getCartItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = cartOps.get(skuId.toString()).toString();
        CartItemVo cartItemVo = JSON.parseObject(o, CartItemVo.class);
        return cartItemVo;
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String o = cartOps.get(skuId.toString()).toString();
        CartItemVo cartItemVo = JSON.parseObject(o, CartItemVo.class);
        cartItemVo.setCount(num);
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItemVo));
    }

    @Override
    public void deleteIdCartInfo(Integer skuId) {

        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }

    @Override
    public void checkItem(Long skuId, Integer checked) {
        // 获取要选中的购物项 // 信息还是在原来的缓存中，更新即可
        CartItemVo cartItem = getCartItem(skuId);
        cartItem.setCheck(checked == 1 ? true : false);
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.put(skuId.toString(), JSON.toJSONString(cartItem));
    }

    /**
     * 获取购物车所有项
     */
    private List<CartItemVo> getCartItems(String cartKey) {
        BoundHashOperations operations = redisTemplate.boundHashOps(cartKey);
        // key不重要，拿到值即可
        List<Object> values = operations.values();
        if (!CollectionUtils.isEmpty(values)) {
            List<CartItemVo> collect = values.stream()
                    .map(obj -> JSON.parseObject(obj.toString(), CartItemVo.class))
                    .collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    /**
     * 获取到我们要操作的购物车
     *
     * @return
     */
    private BoundHashOperations<String, Object, Object> getCartOps() {

        // 1. 这里我们需要知道操作的是离线购物车还是在线购物车
        UserInfoTo userInfoTo = CartInterceptor.toThreadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            //gulimall:cart:1
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }
        BoundHashOperations<String, Object, Object> boundHashOperations = redisTemplate.boundHashOps(cartKey);
        return boundHashOperations;
    }


}
