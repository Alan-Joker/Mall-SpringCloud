package com.leyou.test;

import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.pojo.Sku;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import tk.mybatis.mapper.entity.Example;

import java.util.List;

@SpringBootTest
@RunWith(SpringRunner.class)
public class test {

    @Autowired
    private SkuMapper mapper;

    Sku sku=new Sku();

    @Test
    public void test(){
//        Example example = new Example(Sku.class);
//        Example example1 = example.selectProperties("images");
//        List<Sku> skus = mapper.selectByExample(example1);
        List<Sku> skus = mapper.selectAll();
        //System.out.println(skus);

        for (Sku skus1 : skus) {
            String newStr = skus1.getImages().replaceAll("image.leyou.com", "39.106.140.83");
            skus1.setImages(newStr);
            mapper.updateByPrimaryKeySelective(skus1);
        }
        //System.out.println(skus);

    }
}
