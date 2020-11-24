package com.leyou.controller;

import com.leyou.service.GoodService;
import com.leyou.service.GoodsHtmlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/item")
public class GoodsController {

    @Autowired
    private GoodService goodService;

    @Autowired
    private GoodsHtmlService goodsHtmlService;
    /**
     * 跳转到商品详情页面
     * @param model
     * @param id
     * @return
     */
    @GetMapping("/{id}.html")
    public String toItemPage(Model model, @PathVariable("id")Long id){

        Map<String, Object> map = this.goodService.loadData(id);
        model.addAllAttributes(map);

        // 页面静态化
        this.goodsHtmlService.asyncExcute(id);
        return "item";
    }
}