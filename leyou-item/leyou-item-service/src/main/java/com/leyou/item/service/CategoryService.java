package com.leyou.item.service;

import com.leyou.item.mapper.CategoryMapper;
import com.leyou.item.pojo.Category;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 根据父节点查询子节点
     * @param pid
     * @return
     */
    public List<Category> queryCategoriesByPid(Long pid) {
        Category record=new Category();
        record.setParentId(pid);
        List<Category> select = categoryMapper.select(record);
        return select;
    }

    /**
     * 查询分类
     * @param ids
     * @return
     */
    public List<String> queryNameByIds(List<Long> ids){
        //System.out.println(ids);
        List<Category> categories = categoryMapper.selectByIdList(ids);

        return categories.stream().map(category ->category.getName()).collect(Collectors.toList());

    }
}
