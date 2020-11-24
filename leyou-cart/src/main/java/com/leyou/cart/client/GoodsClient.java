package com.leyou.cart.client;

import com.leyou.item.api.GoodsApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Alan_
 * @create 2020/10/12 20:56
 */
@FeignClient("item-service")
public interface GoodsClient extends GoodsApi {
}
