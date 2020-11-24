package com.leyou.item.api;

import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GoodsApi {
    /**
     * 根据spuId查询SpuDetail
     *
     * @param spuId
     * @return
     */
    @GetMapping("/spu/detail/{spuId}")
    SpuDetail querySpuDetailBySpuId(@PathVariable("spuId") Long spuId);

    /**
     * 根据条件查询查询spu
     * @param key
     * @param stable
     * @param page
     * @param rows
     * @return
     */
    @GetMapping("/spu/page")
    PageResult<SpuBo> querySpuByPage(
            @RequestParam(value = "key",required = false)String key,
            @RequestParam(value = "stable",required = false)String stable,
            @RequestParam(value = "page",defaultValue = "1")Integer page,
            @RequestParam(value = "rows",defaultValue = "5")Integer rows
    );


    /**
     * 根据spuId查询sku集合
     * @param spuId
     * @return
     */
    @GetMapping("/sku/list")
    List<Sku>querySkuBySpuId(@RequestParam("id")Long spuId);

    /**
     * 根据id查询spu
     * @param spuId
     * @return
     */
    @GetMapping("/{id}")
     Spu querySpuById(@PathVariable("id")Long spuId);

    /**
     * 根据skuId查询sku
     * @param id
     * @return
     */
    @GetMapping("sku/{id}")
     Sku querySkuById(@PathVariable("id")Long id);
}
