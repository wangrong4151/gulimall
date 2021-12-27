package com.atguigu.gulimall.ware.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.SkuHasStockVo;
import com.atguigu.gulimall.ware.vo.FareVo;
import com.atguigu.gulimall.ware.vo.WareSkuLockVo;
//import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.ware.entity.WareSkuEntity;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;

import static com.atguigu.common.exception.BizCodeEnum.NO_STOCK_EXCEPTION;


/**
 * 商品库存
 *
 * @author wr
 * @email 1393224151@qq.com
 * @date 2021-07-23 17:20:08
 */
@RestController
@RequestMapping("/ware/sku")
public class WareSkuController {
    @Autowired
    private WareSkuService wareSkuService;

    /**
     * 锁定库存
     *
     * @param vo 库存解锁的场景
     *           1）、下订单成功，订单过期没有支付被系统自动取消或者被用户手动取消，都要解锁库存
     *           2）、下订单成功，库存锁定成功，接下来的业务调用失败，导致订单回滚。之前锁定的库存就要自动解锁
     *           3）、
     * @return
     */
    @PostMapping(value = "/lock/order")
    public R orderLockStock(@RequestBody WareSkuLockVo vo) {

        try {
            boolean lockStock = wareSkuService.orderLockStock(vo);
            return R.ok().setData(lockStock);
        } catch (NoStockException e) {
            return R.error(NO_STOCK_EXCEPTION.getCode(), NO_STOCK_EXCEPTION.getMessage());
        }
    }

    /**
     * 获取运费信息
     *
     * @return
     */
    @GetMapping(value = "/fare")
    public R getFare(@RequestParam("addrId") Long addrId) {

        FareVo fare = wareSkuService.getFare(addrId);

        return R.ok().setData(fare);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("ware:waresku:list")
    public R list(@RequestParam Map<String, Object> params) {
        PageUtils page = wareSkuService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("ware:waresku:info")
    public R info(@PathVariable("id") Long id) {
        WareSkuEntity wareSku = wareSkuService.getById(id);

        return R.ok().put("wareSku", wareSku);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("ware:waresku:save")
    public R save(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.save(wareSku);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("ware:waresku:update")
    public R update(@RequestBody WareSkuEntity wareSku) {
        wareSkuService.updateById(wareSku);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("ware:waresku:delete")
    public R delete(@RequestBody Long[] ids) {
        wareSkuService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }
// sku的规格参数相同，因此我们要将查询规格参数提前，只查询一次

    /**
     * 查询sku是否有库存
     * 返回skuId 和 stock库存量
     */
    @PostMapping("/hasStock")
    public R getSkuHasStock(@RequestBody List<Long> SkuIds) {
        List<SkuHasStockVo> vos = wareSkuService.getSkuHasStock(SkuIds);
        return R.ok().setData(vos);
    }

}
