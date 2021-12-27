package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.MemberResponseVo;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.order.constant.PayConstant;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.to.SpuInfoVo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import static com.atguigu.common.constant.CarConstant.CART_PREFIX;
import static com.atguigu.gulimall.order.constant.OrderConstant.USER_ORDER_TOKEN_PREFIX;

@Slf4j
@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    private ThreadLocal<OrderSubmitVo> submitVoThreadLocal = new ThreadLocal<>();
    @Autowired
    private MemberFeignService memberFeignService;

    @Autowired
    private CartFeignService cartFeignService;

    @Autowired
    private WmsFeignService wmsFeignService;

    @Autowired
    private ProductFeignService productFeignService;

    @Autowired
    private OrderItemService orderItemService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private BestPayService bestPayService;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        return this.baseMapper.selectOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id", memberResponseVo.getId()).orderByDesc("create_time"));

        List<OrderEntity> orderEntityList = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> orderItemEntities = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setOrderItemEntityList(orderItemEntities);
            return order;
        }).collect(Collectors.toList());
        page.setRecords(orderEntityList);
        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        // 获取用户，用用户信息获取购物车
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        // 封装订单
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        // 我们要从request里获取用户数据，但是其他线程是没有这个信息的，
        // 所以可以手动设置新线程里也能共享当前的request数据
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 1.远程查询所有的收获地址列表
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {

            // 因为异步线程需要新的线程，而新的线程里没有request数据，所以我们自己设置进去
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //1、远程查询所有的收获地址列表
            List<MemberAddressVo> address = memberFeignService.getAddress(memberResponseVo.getId());
            if (!CollectionUtils.isEmpty(address)) {
                orderConfirmVo.setMemberAddressVos(address);
            }
        }, threadPoolExecutor);

        // 2. 远程查询购物车服务，并得到每个购物项是否有库存
        CompletableFuture<Void> cartFuture = CompletableFuture.runAsync(() -> {
            //每一个线程都来共享之前的请求数据
            RequestContextHolder.setRequestAttributes(requestAttributes);
            //2、远程查询购物车所有选中的购物项
            //feign在远程调用之前要构造请求，调用很多的拦截器
            List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
            orderConfirmVo.setItems(currentCartItems);

        }, threadPoolExecutor).thenRunAsync(() -> {
            List<OrderItemVo> items = orderConfirmVo.getItems();
            //获取全部商品的id
            List<Long> collect = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            //远程查询商品库存信息
            R r = wmsFeignService.getSkuHasStock(collect);
            if (r.getCode() == 0) {
                List<SkuStockVo> skuStockVoList = r.getData("data", new TypeReference<List<SkuStockVo>>() {
                });
                if (!CollectionUtils.isEmpty(skuStockVoList)) {
                    Map<Long, Boolean> skuStockMap = skuStockVoList.stream().collect(Collectors.toMap(SkuStockVo::getSkuId, SkuStockVo::getHasStock));
                    orderConfirmVo.setStocks(skuStockMap);
                }
            }

        }, threadPoolExecutor);

        //3、查询用户积分
        Integer integration = memberResponseVo.getIntegration();

        orderConfirmVo.setIntegration(integration);

        //4、价格数据自动计算

        //TODO 5、防重令牌(防止表单重复提交)
        //为用户设置一个token，三十分钟过期时间（存在redis）
        String token = UUID.randomUUID().toString().replace("-", "");
        redisTemplate.opsForValue().set(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId(), token, 30, TimeUnit.MINUTES);

        orderConfirmVo.setOrderToken(token);

        CompletableFuture.allOf(addressFuture, cartFuture).get();
        return orderConfirmVo;
    }

    @Override
    @Transactional
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        // 当条线程共享这个对象
        submitVoThreadLocal.set(vo);
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        //去创建、下订单、验令牌、验价格、锁定库存...
        //获取当前用户登录的信息

        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();

        responseVo.setCode(0);
        //1、验证令牌是否合法【令牌的对比和删除必须保证原子性】
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        String orderToken = vo.getOrderToken();
        //通过lure脚本原子验证令牌和删除令牌
        Long result = redisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class),
                Arrays.asList(USER_ORDER_TOKEN_PREFIX + memberResponseVo.getId()), orderToken);

        if (result == 0) {
            //令牌验证失败
            responseVo.setCode(1);
            return responseVo;
        } else {
            //令牌验证成功
            //1、创建订单、订单项等信息
            OrderCreateTo order = createOrder();
            //2、验证价格
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                //金额对比
                //TODO 3、保存订单
                saveOrder(order);
                //4、库存锁定,只要有异常，回滚订单数据
                //订单号、所有订单项信息(skuId,skuNum,skuName)
                WareSkuLockVo lockVo = new WareSkuLockVo();
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                //获取出要锁定的商品数据信息
                List<OrderItemVo> collect = order.getOrderItems().stream().map((item) -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setLocks(collect);
                //TODO 调用远程锁定库存的方法
                //出现的问题：扣减库存成功了，但是由于网络原因超时，出现异常，导致订单事务回滚，库存事务不回滚(解决方案：seata)
                //为了保证高并发，不推荐使用seata，因为是加锁，并行化，提升不了效率,可以发消息给库存服务
                R r = wmsFeignService.orderLockStock(lockVo);
                if (r.getCode() == 0) {
                    //锁定成功
                    responseVo.setOrder(order.getOrder());
                    //TODO 订单创建成功，发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order", order.getOrder());
                    //删除购物车里的数据
                    redisTemplate.delete(CART_PREFIX + memberResponseVo.getId());
                    return responseVo;
                } else {
                    //锁定失败
                    String msg = (String) r.get("msg");
                    throw new NoStockException(msg);
                }
            } else {
                responseVo.setCode(2);
                return responseVo;
            }
        }

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String handlePayResult(PayAsyncVo asyncVo) {
        //保存交易流水信息
        PaymentInfoEntity paymentInfo = new PaymentInfoEntity();
        paymentInfo.setOrderSn(asyncVo.getOut_trade_no());
        paymentInfo.setAlipayTradeNo(asyncVo.getTrade_no());
        paymentInfo.setTotalAmount(new BigDecimal(asyncVo.getBuyer_pay_amount()));
        paymentInfo.setSubject(asyncVo.getBody());
        paymentInfo.setPaymentStatus(asyncVo.getTrade_status());
        paymentInfo.setCreateTime(new Date());
        paymentInfo.setCallbackTime(asyncVo.getNotify_time());
        //添加到数据库中
        this.paymentInfoService.save(paymentInfo);

        //修改订单状态
        //获取当前状态
        String tradeStatus = asyncVo.getTrade_status();

        if (tradeStatus.equals("TRADE_SUCCESS") || tradeStatus.equals("TRADE_FINISHED")) {
            //支付成功状态
            String orderSn = asyncVo.getOut_trade_no(); //获取订单号
            this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.ALIPAY);
        }

        return "success";
    }

    /**
     * 微信异步通知结果
     *
     * @param notifyData
     * @return
     */
    @Override
    public String asyncNotify(String notifyData) {

        //签名效验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("payResponse={}", payResponse);

        //2.金额效验（从数据库查订单）
        OrderEntity orderEntity = this.getOrderByOrderSn(payResponse.getOrderId());

        //如果查询出来的数据是null的话
        //比较严重(正常情况下是不会发生的)发出告警：钉钉、短信
        if (orderEntity == null) {
            //TODO 发出告警，钉钉，短信
            throw new RuntimeException("通过订单编号查询出来的结果是null");
        }

        //判断订单状态状态是否为已支付或者是已取消,如果不是订单状态不是已支付状态
        Integer status = orderEntity.getStatus();
        if (status.equals(OrderStatusEnum.PAYED.getCode()) || status.equals(OrderStatusEnum.CANCLED.getCode())) {
            throw new RuntimeException("该订单已失效,orderNo=" + payResponse.getOrderId());
        }

        /*//判断金额是否一致,Double类型比较大小，精度问题不好控制
        if (orderEntity.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0) {
            //TODO 告警
            throw new RuntimeException("异步通知中的金额和数据库里的不一致,orderNo=" + payResponse.getOrderId());
        }*/

        //3.修改订单支付状态
        //支付成功状态
        String orderSn = orderEntity.getOrderSn();
        this.updateOrderStatus(orderSn, OrderStatusEnum.PAYED.getCode(), PayConstant.WXPAY);

        //4.告诉微信不要再重复通知了
        return "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
    }


    /**
     * 修改订单状态
     *
     * @param orderSn
     * @param code
     */
    private void updateOrderStatus(String orderSn, Integer code, Integer payType) {

        this.baseMapper.updateOrderStatus(orderSn, code, payType);
    }


    /**
     * 保存订单所有数据
     *
     * @param orderCreateTo
     */
    private void saveOrder(OrderCreateTo orderCreateTo) {

        //获取订单信息
        OrderEntity order = orderCreateTo.getOrder();
        order.setModifyTime(new Date());
        order.setCreateTime(new Date());
        //保存订单
        this.baseMapper.insert(order);

        //获取订单项信息
        List<OrderItemEntity> orderItems = orderCreateTo.getOrderItems();
        //批量保存订单项数据
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder() {

        OrderCreateTo orderCreateTo = new OrderCreateTo();
        //1、生成订单号
        String orderSn = IdWorker.getTimeId();
        OrderEntity orderEntity = builderOrder(orderSn);
        //2、获取到所有的订单项
        List<OrderItemEntity> orderItemEntities = builderOrderItems(orderSn);
        //3、验价(计算价格、积分等信息)
        computePrice(orderEntity, orderItemEntities);

        orderCreateTo.setOrder(orderEntity);
        orderCreateTo.setOrderItems(orderItemEntities);

        return orderCreateTo;
    }

    /**
     * 计算价格的方法
     *
     * @param orderEntity
     * @param orderItemEntities
     */
    private void computePrice(OrderEntity orderEntity, List<OrderItemEntity> orderItemEntities) {

        //总价
        BigDecimal total = new BigDecimal("0.0");
        //优惠价
        BigDecimal coupon = new BigDecimal("0.0");
        BigDecimal intergration = new BigDecimal("0.0");
        BigDecimal promotion = new BigDecimal("0.0");

        //积分、成长值
        Integer integrationTotal = 0;
        Integer growthTotal = 0;

        //订单总额，叠加每一个订单项的总额信息
        for (OrderItemEntity orderItem : orderItemEntities) {
            //优惠价格信息
            coupon = coupon.add(orderItem.getCouponAmount());
            promotion = promotion.add(orderItem.getPromotionAmount());
            intergration = intergration.add(orderItem.getIntegrationAmount());

            //总价
            total = total.add(orderItem.getRealAmount());

            //积分信息和成长值信息
            integrationTotal += orderItem.getGiftIntegration();
            growthTotal += orderItem.getGiftGrowth();

        }
        //1、订单价格相关的
        orderEntity.setTotalAmount(total);
        //设置应付总额(总额+运费)
        orderEntity.setPayAmount(total.add(orderEntity.getFreightAmount()));
        orderEntity.setCouponAmount(coupon);
        orderEntity.setPromotionAmount(promotion);
        orderEntity.setIntegrationAmount(intergration);

        //设置积分成长值信息
        orderEntity.setIntegration(integrationTotal);
        orderEntity.setGrowth(growthTotal);

        //设置删除状态(0-未删除，1-已删除)
        orderEntity.setDeleteStatus(0);

    }

    /**
     * 构建订单数据
     *
     * @param orderSn
     * @return
     */
    private OrderEntity builderOrder(String orderSn) {
        //获取当前用户登录信息
        MemberResponseVo memberResponseVo = LoginUserInterceptor.loginUser.get();
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(memberResponseVo.getId());
        orderEntity.setOrderSn(orderSn);
        orderEntity.setMemberUsername(memberResponseVo.getUsername());

        OrderSubmitVo orderSubmitVo = submitVoThreadLocal.get();
        //远程获取收货地址和运费信息
        R r = wmsFeignService.getFare(orderSubmitVo.getAddrId());
        if (r.getCode() == 0) {
            FareVo data = r.getData("data", new TypeReference<FareVo>() {
            });
            if (data != null) {
                //获取到运费信息
                BigDecimal fare = data.getFare();
                orderEntity.setFreightAmount(fare);

                //获取到收货地址信息
                MemberAddressVo address = data.getAddress();
                //设置收货人信息
                orderEntity.setReceiverName(address.getName());
                orderEntity.setReceiverPhone(address.getPhone());
                orderEntity.setReceiverPostCode(address.getPostCode());
                orderEntity.setReceiverProvince(address.getProvince());
                orderEntity.setReceiverCity(address.getCity());
                orderEntity.setReceiverRegion(address.getRegion());
                orderEntity.setReceiverDetailAddress(address.getDetailAddress());

                //设置订单相关的状态信息
                orderEntity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
                orderEntity.setAutoConfirmDay(7);
                orderEntity.setConfirmStatus(0);
                return orderEntity;
            }
        }
        return null;
    }

    /**
     * 构建所有订单项数据
     *
     * @return
     */
    public List<OrderItemEntity> builderOrderItems(String orderSn) {

        List<OrderItemEntity> orderItemEntityList = new ArrayList<>();

        //最后确定每个购物项的价格
        List<OrderItemVo> currentCartItems = cartFeignService.getCurrentCartItems();
        if (currentCartItems != null && currentCartItems.size() > 0) {
            orderItemEntityList = currentCartItems.stream().map((items) -> {
                //构建订单项数据
                OrderItemEntity orderItemEntity = builderOrderItem(items);
                orderItemEntity.setOrderSn(orderSn);

                return orderItemEntity;
            }).collect(Collectors.toList());
        }

        return orderItemEntityList;
    }

    /**
     * 构建某一个订单项的数据
     *
     * @param items
     * @return
     */
    private OrderItemEntity builderOrderItem(OrderItemVo items) {

        OrderItemEntity orderItemEntity = new OrderItemEntity();

        //1、商品的spu信息
        Long skuId = items.getSkuId();
        //获取spu的信息
        R spuInfo = productFeignService.getSpuInfoBySkuId(skuId);
        SpuInfoVo spuInfoData = spuInfo.getData("data", new TypeReference<SpuInfoVo>() {
        });
        orderItemEntity.setSpuId(spuInfoData.getId());
        orderItemEntity.setSpuName(spuInfoData.getSpuName());
        orderItemEntity.setSpuBrand(spuInfoData.getBrandName());
        orderItemEntity.setCategoryId(spuInfoData.getCatalogId());

        //2、商品的sku信息
        orderItemEntity.setSkuId(skuId);
        orderItemEntity.setSkuName(items.getTitle());
        orderItemEntity.setSkuPic(items.getImage());
        orderItemEntity.setSkuPrice(items.getPrice());
        orderItemEntity.setSkuQuantity(items.getCount());

        //使用StringUtils.collectionToDelimitedString将list集合转换为String
        String skuAttrValues = StringUtils.collectionToDelimitedString(items.getSkuAttrValues(), ";");
        orderItemEntity.setSkuAttrsVals(skuAttrValues);

        //3、商品的优惠信息

        //4、商品的积分信息
        orderItemEntity.setGiftGrowth(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());
        orderItemEntity.setGiftIntegration(items.getPrice().multiply(new BigDecimal(items.getCount())).intValue());

        //5、订单项的价格信息
        orderItemEntity.setPromotionAmount(BigDecimal.ZERO);
        orderItemEntity.setCouponAmount(BigDecimal.ZERO);
        orderItemEntity.setIntegrationAmount(BigDecimal.ZERO);

        //当前订单项的实际金额.总额 - 各种优惠价格
        //原来的价格
        BigDecimal origin = orderItemEntity.getSkuPrice().multiply(new BigDecimal(orderItemEntity.getSkuQuantity().toString()));
        //原价减去优惠价得到最终的价格
        BigDecimal subtract = origin.subtract(orderItemEntity.getCouponAmount())
                .subtract(orderItemEntity.getPromotionAmount())
                .subtract(orderItemEntity.getIntegrationAmount());
        orderItemEntity.setRealAmount(subtract);

        return orderItemEntity;
    }

}