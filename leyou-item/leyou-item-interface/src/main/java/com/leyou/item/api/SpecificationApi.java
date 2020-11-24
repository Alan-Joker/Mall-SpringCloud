package com.leyou.item.api;

import com.leyou.item.pojo.SpecGroup;
import com.leyou.item.pojo.SpecParam;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RequestMapping("/spec")
public interface SpecificationApi {


    /**
     * 根据条件查询规格参数
     *
     * @param gid
     * @return
     */
    @GetMapping("/params")
    List<SpecParam>queryParmsByGid(
            @RequestParam(value = "gid", required = false) Long gid,
            @RequestParam(value = "cid", required = false) Long cid,
            @RequestParam(value = "generic", required = false) Boolean genenric,
            @RequestParam(value = "searching", required = false) Boolean searching
    );

    /**
     * 查询规格参数
     * @param cid
     * @return
     */
    @GetMapping("/group/parm/{cid}")
     List<SpecGroup> queryGroupsWithParm(@PathVariable("cid")Long cid);
}
