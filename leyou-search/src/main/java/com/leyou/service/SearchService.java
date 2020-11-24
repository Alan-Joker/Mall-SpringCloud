package com.leyou.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.client.BrandClinet;
import com.leyou.client.CategoryClient;
import com.leyou.client.GoodsClient;
import com.leyou.client.SpecificationClient;
import com.leyou.pojo.SearchResult;
import com.leyou.repository.GoodsRepository;
import com.leyou.item.pojo.*;
import com.leyou.pojo.Goods;
import com.leyou.pojo.SearchRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private BrandClinet brandClinet;

    @Autowired
    private GoodsClient goodsClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository goodsRepository;

    private static final ObjectMapper MAPPER=new ObjectMapper();

    public Goods buildGoods(Spu spu) throws IOException {

        Goods goods = new Goods();

        //根据分类id查询分类名称
        List<String> names = categoryClient.queryNamesById(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));

        //根据品牌id查询品牌
        Brand brand = brandClinet.queryBrandById(spu.getBrandId());

        //根据spuId查询所有sku
        List<Sku> skus = goodsClient.querySkuBySpuId(spu.getId());
        //初始化一个价格集合，收集所有sku的价格
        List<Long> prices=new ArrayList<>();
        //手机sku的必要字段信息
        List<Map<String,Object>> skuMapList=new ArrayList<>();

        skus.forEach(sku -> {
            prices.add(sku.getPrice());

            Map<String,Object> map=new HashMap<>();
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            //获取sku中的图片，数据库中的图片，可能是多张，多张是以","分割，所以也以逗号来切割返回图片数组，获取第一张图片
            map.put("image",StringUtils.isBlank(sku.getImages())? "" :StringUtils.split(sku.getImages(),",")[0]);

            skuMapList.add(map);
        });

        //根据spu中的cid3查询出所有的搜索规格参数
        List<SpecParam> params = specificationClient.queryParmsByGid(null, spu.getCid3(), null, true);

        //根据spuId查询spuDetail
        SpuDetail spuDetail = goodsClient.querySpuDetailBySpuId(spu.getId());

        //把通用的规格参数值，进行反序列化
        Map<String,Object> geericSpecMap=MAPPER.readValue(spuDetail.getGenericSpec(),new TypeReference<Map<String,Object>>(){});
        //把特殊的规格参数值，进行反序列化
        Map<String,List<Object>> specialSpecMap=MAPPER.readValue(spuDetail.getSpecialSpec(),new TypeReference<Map<String,List<Object>>>(){});

        Map<String,Object> specs=new HashMap<>();
        params.forEach(param ->{
            //判断规格参数的类型是否是通用的规格参数
            if(param.getGeneric()){
                //通用类型参数，从geericSpecMap获取规格参数值
                String value = geericSpecMap.get(param.getId().toString()).toString();
                //判断是否是数值类型，如果是数值类型，应该返回一个区间
                if(param.getNumeric()){
                    value = chooseSegment(value, param);
                }
                specs.put(param.getName(),value);
            }else {
                //如果是特殊规格参数
                List<Object> value = specialSpecMap.get(param.getId().toString());
                specs.put(param.getName(),value);
            }
        });
        goods.setId(spu.getId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setBrandId(spu.getBrandId());
        goods.setCreateTime(spu.getCreateTime());
        goods.setSubTitle(spu.getSubTitle());
        //拼接All字段，需要分类名称和品牌名称
        goods.setAll(spu.getTitle() + " " + StringUtils.join(names," ") + " " + brand);
        //获取spu下的所有sku价格
        goods.setPrice(prices);
        //获取spu下的所有sku,并转化成json字符串
        goods.setSkus(MAPPER.writeValueAsString(skuMapList));
        System.out.println(goods.getSkus());
        //获取所有查询的规格参数{name:value}
        goods.setSpecs(specs);
        return goods;
    }

    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }

    public SearchResult search(SearchRequest request) {
        if(StringUtils.isBlank(request.getKey())){
            return null;
        }
        //自定义查询构建器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();

        //添加查询条件
       // QueryBuilder basicQuery = QueryBuilders.matchQuery("all", request.getKey()).operator(Operator.AND);
        BoolQueryBuilder basicQuery=buildBoolQueryBuilder(request);
        queryBuilder.withQuery(basicQuery);
        //添加分页,分页页码从0开始
        queryBuilder.withPageable(PageRequest.of(request.getPage()-1,request.getSize()));
        //添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","skus","subTitle"},null));
        //添加分类和品牌的聚合
        String categoryAggName="categories";
        String brandAggName="brands";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));

        //执行查询，获取结果集
        AggregatedPage<Goods> goodsPage =(AggregatedPage<Goods>) goodsRepository.search(queryBuilder.build());

        //获取聚集结果集并解析
        List<Map<String,Object>> categories=getCategoryAggResult(goodsPage.getAggregation(categoryAggName));
        List<Brand> brands=getBrandAggResult(goodsPage.getAggregation(brandAggName));

        //判断是否是一个分类，只有一个分类时才做规格参数的聚合
        List<Map<String,Object>> specs=null;
        if(!CollectionUtils.isEmpty(categories) && categories.size()==1){
            //对规格参数进行聚合
            specs= getParmAggResult((Long)categories.get(0).get("id"),basicQuery);
        }
        return new SearchResult(goodsPage.getTotalElements(),goodsPage.getTotalPages(),goodsPage.getContent(),categories,brands,specs);
    }

    /**
     * 构建布尔查询
     * @param request
     * @return
     */
    private BoolQueryBuilder buildBoolQueryBuilder(SearchRequest request) {
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //向布尔查询中添加基本查询条件
        boolQueryBuilder.must(QueryBuilders.matchQuery("all",request.getKey()).operator(Operator.AND));
        //添加过滤条件
        //获取用户选择的过滤信息
        Map<String, Object> filter = request.getFilter();
        for (Map.Entry<String, Object> entry : filter.entrySet()) {
            String key = entry.getKey();
            if(StringUtils.equals("品牌",key)){
                key="brandId";
            }else if(StringUtils.equals("分类",key)){
                key="cid3";
            }else {
                key="specs."+key+".keyword";
            }
            boolQueryBuilder.filter(QueryBuilders.termQuery(key,entry.getValue()));
        }
        return boolQueryBuilder;
    }

    /**
     * 根据查询条件聚合规格参数
     * @param cid
     * @param basicQuery
     * @return
     */
    private List<Map<String, Object>> getParmAggResult(Long cid, QueryBuilder basicQuery) {
        //自定义查询对象构建
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //添加基本查询条件
        queryBuilder.withQuery(basicQuery);

        //查询要聚合的规格参数
        List<SpecParam> params = this.specificationClient.queryParmsByGid(null, cid, null, true);
        //添加规格参数的聚合
        params.forEach(param->{
            queryBuilder.addAggregation(AggregationBuilders.terms(param.getName()).field("specs."+param.getName()+".keyword"));
        });
        //添加结果集过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{},null));
        //执行聚合查询,获取聚合结果集
        AggregatedPage goodsPage =(AggregatedPage) this.goodsRepository.search(queryBuilder.build());
        //解析聚合结果集,key-聚合名称(规格参数名) value-聚合对象
        List<Map<String, Object>> specs=new ArrayList<>();
        Map<String, Aggregation> aggregationMap = goodsPage.getAggregations().asMap();
        for (Map.Entry<String, Aggregation> entry : aggregationMap.entrySet()) {
            //初始化一个map {k:规格参数名, options:聚合的规格参数值}
            Map<String,Object> map=new HashMap<>();
            map.put("k",entry.getKey());
            //初始化一个options集合，收集桶中key
            List<String> options=new ArrayList<>();
            //获取聚合
            StringTerms terms = (StringTerms) entry.getValue();
            //获取桶集合
            terms.getBuckets().forEach(bucket -> {
                options.add(bucket.getKeyAsString());
            });
            map.put("options",options);
            specs.add(map);
        }
        return specs;
    }

    /**
     * 解析品牌的聚合结果集
     * @param aggregation
     * @return
     */
    private List<Brand> getBrandAggResult(Aggregation aggregation) {
        LongTerms terms=(LongTerms)aggregation;

       //获取聚合中的桶
       return terms.getBuckets().stream().map(bucket -> {
           return this.brandClinet.queryBrandById(bucket.getKeyAsNumber().longValue());
       }).collect(Collectors.toList());
    }

    /**
     * 解析分类的聚合结果集
     * @param aggregation
     * @return
     */
    private List<Map<String, Object>> getCategoryAggResult(Aggregation aggregation) {
        LongTerms terms=(LongTerms)aggregation;

        //获取桶的集合，转化成List<Map<String,Object>>
        return terms.getBuckets().stream().map(bucket -> {
            //初始化一个map
            Map<String,Object> map=new HashMap<>();
            //获取桶中的分类id(key)
            Long id = bucket.getKeyAsNumber().longValue();
            //根据分类id查询分类名称
            List<String> names = this.categoryClient.queryNamesById(Arrays.asList(id));
            map.put("id",id);
            map.put("name",names.get(0));
            return map;
        }).collect(Collectors.toList());
    }

    public void save(Long id) throws IOException {
        Spu spu = this.goodsClient.querySpuById(id);
        Goods goods = this.buildGoods(spu);
        this.goodsRepository.save(goods);
    }

    public void delete(Long id) {

        goodsRepository.deleteById(id);
    }
}
