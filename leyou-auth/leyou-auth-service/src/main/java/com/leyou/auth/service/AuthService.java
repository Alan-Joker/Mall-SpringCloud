package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author Alan_
 * @create 2020/10/9 21:46
 */

@Service
public class AuthService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private UserClient userClient;
    //根据用户名和密码查询
    public String accredit(String username, String password) {

        //1.根据用户名和密码查询
        User user = userClient.queryUser(username, password);
        //2.判断user
        if(user==null){
            return null;
        }
        //jwtutils生成jwt类型的token

        try {
            UserInfo userInfo = new UserInfo();
            userInfo.setUsername(user.getUsername());
            userInfo.setId(user.getId());
           return JwtUtils.generateToken(userInfo,jwtProperties.getPrivateKey(),jwtProperties.getExpire());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }
}
