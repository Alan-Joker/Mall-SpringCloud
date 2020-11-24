package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.pojo.PageResult;
import com.leyou.item.bo.SpuBo;
import com.leyou.item.mapper.*;
import com.leyou.item.pojo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class GoodService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SpuDetailMapper spuDetailMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private StockMapper stockMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;
    /**
     * 根据条件查询查询spu
     * @param key
     * @param stable
     * @param page
     * @param rows
     * @return
     */
    public PageResult<SpuBo> querySpuByPage(String key, String stable, Integer page, Integer rows) {
        //添加查询条件
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(key)) {
            criteria.andLike("title", "%" + key + "%");
        }
        //添加上下架得过滤条件
        if(stable!=null){
            criteria.andEqualTo("saleable",stable);
        }
        //添加分页
        PageHelper.startPage(page,rows);
        //执行查询,获取spu集合
        List<Spu> spus = spuMapper.selectByExample(example);
        PageInfo<Spu> spuPageInfo = new PageInfo<>(spus);
        //spu集合转化成spubo集合
        List<SpuBo> spuBos = spus.stream().map(
                spu -> {
            SpuBo spuBo = new SpuBo();
            BeanUtils.copyProperties(spu, spuBo);
            //查询品牌名称
            Brand brand = brandMapper.selectByPrimaryKey(spu.getBrandId());
            spuBo.setBname(brand.getName());
            //查询分类名称
            List<String> names = categoryService.queryNameByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
                    //System.out.println(names);
            spuBo.setCname(StringUtils.join(names, "-"));
                    //System.out.println(spuBo);
            return spuBo;
        }
        ).collect(Collectors.toList());
        //返回PageResult<spuBo>

        return new PageResult<>(spuPageInfo.getTotal(),spuBos);
    }

    /**
     * 新增商品
     * @param spuBo
     */
    public void saveGoods(SpuBo spuBo) {
        //先新增spu
        spuBo.setId(null);//防止sql注入
        spuBo.setSaleable(true);
        spuBo.setValid(true);
        spuBo.setCreateTime(new Date());
        spuBo.setLastUpdateTime(spuBo.getCreateTime());
        spuMapper.insertSelective(spuBo);

        //再去新增spuDetail
        SpuDetail spuDetail = spuBo.getSpuDetail();
        spuDetail.setSpuId(spuBo.getId());
        spuDetailMapper.insertSelective(spuDetail);

        saveSkuAndStock(spuBo);
        sentMsg("insert",spuBo.getId());
    }

    private void sentMsg(String type,Long id) {
        try {
            this.amqpTemplate.convertAndSend("item."+type,id);
        } catch (AmqpException e) {
            e.printStackTrace();
        }
    }

    private void saveSkuAndStock(SpuBo spuBo) {
        spuBo.getSkus().forEach(sku -> {
            //新增sku
            sku.setId(null);
            sku.setSpuId(spuBo.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insertSelective(sku);
            //新增stock
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stockMapper.insertSelective(stock);
        });
    }

    /**
     * 根据spuId查询SpuDetail
     * @param spuId
     * @return
     */
    public SpuDetail querySpuDetailBySpuId(Long spuId) {

        return spuDetailMapper.selectByPrimaryKey(spuId);
    }


    /**
     * 根据spuId查询sku集合
     * @param spuId
     * @return
     */
    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku record = new Sku();
        record.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(record);

        skus.forEach(sku -> {
            Stock stock = stockMapper.selectByPrimaryKey(sku.getId());
            sku.setStock(stock.getStock());
        });

        return skus;
    }

    /**
     * 更新商品信息
     * @param spuBo
     */
    public void updataGoods(SpuBo spuBo) {
        //根据spuId查询要删除得sku
        Sku record = new Sku();
        record.setSpuId(spuBo.getId());
        List<Sku> skus = skuMapper.select(record);
        skus.forEach(sku -> {
            //先删除stock
            stockMapper.deleteByPrimaryKey(sku.getId());
        });

        //删除sku
        Sku sku = new Sku();
        sku.setSpuId(spuBo.getId());
        skuMapper.delete(sku);
        //新增sku和stock
        saveSkuAndStock(spuBo);
        //更新spu和spuDetail
        spuBo.setCreateTime(null);
        spuBo.setLastUpdateTime(new Date());
        spuBo.setValid(null);
        spuBo.setSaleable(null);
        spuMapper.updateByPrimaryKeySelective(spuBo);

        spuDetailMapper.updateByPrimaryKeySelective(spuBo.getSpuDetail());
        sentMsg("update",spuBo.getId());
    }

    public Spu querySpuById(Long spuId) {

        return this.spuMapper.selectByPrimaryKey(spuId);
    }

    public Sku querySkuById(Long id) {
        return skuMapper.selectByPrimaryKey(id);
    }
}
