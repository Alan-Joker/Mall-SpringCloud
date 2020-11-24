package com.leyou.item.api;

import com.leyou.item.pojo.Brand;

import org.springframework.web.bind.annotation.*;


@RequestMapping("/brand")
public interface BrandApi {


    /**
     * 根据id查询品牌名称
     */
    @GetMapping("/{id}")
    Brand queryBrandById(@PathVariable("id")Long id);
}
