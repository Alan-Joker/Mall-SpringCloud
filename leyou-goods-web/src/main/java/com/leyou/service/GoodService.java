package com.leyou.service;

import com.leyou.client.BrandClinet;
import com.leyou.client.CategoryClient;
import com.leyou.client.GoodsClient;
import com.leyou.client.SpecificationClient;
import com.leyou.item.pojo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GoodService {

    @Autowired
    private BrandClinet brandClinet;
    @Autowired
    private CategoryClient categoryClient;
    @Autowired
    private GoodsClient goodsClient;
    @Autowired
    private SpecificationClient specificationClient;

    public Map<String,Object> loadData(Long spuId){
        Map<String,Object> model=new HashMap<>();

        //根据spuId查询spu
        Spu spu = this.goodsClient.querySpuById(spuId);
        model.put("spu",spu);
        //查询spuDetail
        SpuDetail spuDetail = this.goodsClient.querySpuDetailBySpuId(spuId);
        model.put("spuDetail",spuDetail);
        //查询分类
        List<Long> cids = Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3());
        List<String> names=this.categoryClient.queryNamesById(cids);
        //初始化一个分类map
        List<Map<String,Object>> categories=new ArrayList<>();
        for (int i = 0; i < cids.size(); i++) {
            Map<String,Object> map=new HashMap<>();
            map.put("id",cids.get(i));
            map.put("name", names.get(i));
            categories.add(map);
        }
        model.put("categories",categories);
        //查询品牌
        Brand brand = this.brandClinet.queryBrandById(spu.getBrandId());
        model.put("brand",brand);
        //查询skus
        List<Sku> skus = this.goodsClient.querySkuBySpuId(spuId);
        model.put("skus",skus);
        //查询规格参数组
        List<SpecGroup> groups = this.specificationClient.queryGroupsWithParm(spu.getCid3());
        model.put("groups",groups);
        //查询特殊的规格参数
        List<SpecParam> params = this.specificationClient.queryParmsByGid(null, spu.getCid3(), false, null);
        //初始化特殊规格参数map
        Map<Long,String> paramMap=new HashMap<>();
        params.forEach(parm->{
            paramMap.put(parm.getId(),parm.getName());
        });
        model.put("paramMap",paramMap);
        return model;
    }
}
