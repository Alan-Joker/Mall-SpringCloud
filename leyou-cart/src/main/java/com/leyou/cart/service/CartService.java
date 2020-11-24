package com.leyou.cart.service;

import com.leyou.auth.entity.UserInfo;
import com.leyou.cart.client.GoodsClient;
import com.leyou.cart.entity.Cart;
import com.leyou.cart.interceptor.LoginInterceptor;
import com.leyou.common.utils.JsonUtils;
import com.leyou.item.pojo.Sku;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Alan_
 * @create 2020/10/12 20:35
 */
@Service
public class CartService {

    @Autowired
    private StringRedisTemplate template;

    @Autowired
    private GoodsClient goodsClient;

    static final String KEY_PREFIX = "leyou:cart:uid:";

    static final Logger logger = LoggerFactory.getLogger(CartService.class);

    public void addCart(Cart cart) {
        //获取登录用户
        UserInfo user = LoginInterceptor.getUserInfo();
        //先从redis中查询原先的商品信息
        //Redis的key
        String key = KEY_PREFIX + user.getId();
        //获取redis 的Hash操作对象
        BoundHashOperations<String, Object, Object> hashops = template.boundHashOps(key);
        //如果有，判断商品是否存在
        Boolean boo = hashops.hasKey(cart.getSkuId().toString());
        //新增加的商品数量
        Integer num = cart.getNum();
        if(boo){
            //存在,获取原来购物车信息
            String json = hashops.get(cart.getSkuId().toString()).toString();
            //将json字符串反序列化为json对象
             cart = JsonUtils.parse(json, Cart.class);
             //修改购物车数量
            cart.setNum(cart.getNum()+num);
        }else {
            //不存在,新增数据到购物车
            cart.setUserId(cart.getUserId());
            // 其它商品信息，需要查询商品服务
            Sku sku = this.goodsClient.querySkuById(cart.getSkuId());
            cart.setImage(StringUtils.isBlank(sku.getImages()) ? "" : StringUtils.split(sku.getImages(), ",")[0]);
            cart.setPrice(sku.getPrice());
            cart.setTitle(sku.getTitle());
            cart.setOwnSpec(sku.getOwnSpec());

        }
        // 将购物车数据写入redis
        hashops.put(cart.getSkuId().toString(), JsonUtils.serialize(cart));


    }

    /**
     * 查询购物车
     * @return
     */
    public List<Cart> queryCartList() {
        // 获取登录用户
        UserInfo user = LoginInterceptor.getUserInfo();

        // 判断是否存在购物车
        String key = KEY_PREFIX + user.getId();
        if(!this.template.hasKey(key)){
            // 不存在，直接返回
            return null;
        }
        BoundHashOperations<String, Object, Object> hashOps = this.template.boundHashOps(key);
        List<Object> carts = hashOps.values();
        // 判断是否有数据
        if(CollectionUtils.isEmpty(carts)){
            return null;
        }
        // 查询购物车数据
        return carts.stream().map(o -> JsonUtils.parse(o.toString(), Cart.class)).collect(Collectors.toList());
    }

    public void updataCarts(Cart cart) {
            // 获取登陆信息
            UserInfo userInfo = LoginInterceptor.getUserInfo();
            String key = KEY_PREFIX + userInfo.getId();
            // 获取hash操作对象
            BoundHashOperations<String, Object, Object> hashOperations = this.template.boundHashOps(key);
            // 获取购物车信息
            String cartJson = hashOperations.get(cart.getSkuId().toString()).toString();
            Cart cart1 = JsonUtils.parse(cartJson, Cart.class);
            // 更新数量
            cart1.setNum(cart.getNum());
            // 写入购物车
            hashOperations.put(cart.getSkuId().toString(), JsonUtils.serialize(cart1));
        }
}
