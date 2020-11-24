package com.leyou.user.api;

import com.leyou.user.pojo.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * @Author Alan_
 * @create 2020/10/10 10:27
 */
public interface UserApi {

    @GetMapping("/query")
     User queryUser(@RequestParam("username") String username,
                    @RequestParam("password")String password);
}
