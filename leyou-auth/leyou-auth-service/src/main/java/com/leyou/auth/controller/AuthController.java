package com.leyou.auth.controller;

import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.entity.UserInfo;
import com.leyou.auth.service.AuthService;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.common.utils.CookieUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author Alan_
 * @create 2020/10/9 21:45
 */
@Controller
@EnableConfigurationProperties(JwtProperties.class)
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties properties;

    /**
     * 登录
     * @param username
     * @param password
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/accredit")
    public ResponseEntity<Void> accredit(@RequestParam("username")String username,
                                         @RequestParam("password")String password, HttpServletRequest request, HttpServletResponse response){
        String token=this.authService.accredit(username,password);
        if(StringUtils.isBlank(token)){
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        CookieUtils.setCookie(request,response,this.properties.getCookieName(),token,this.properties.getExpire()*60);

        return ResponseEntity.ok(null);
    }

    /**
     * 返回用户信息
     * @param token
     * @param request
     * @param response
     * @return
     */
    @GetMapping("/verify")
    public ResponseEntity<UserInfo> verifyUser(@CookieValue("LY_TOKEN")String token,
                                               HttpServletRequest request,HttpServletResponse response){
        //从token中获取信息
        try {
            UserInfo userInfo = JwtUtils.getInfoFromToken(token, this.properties.getPublicKey());

            //解析成功刷新cookie过期时间
            JwtUtils.generateToken(userInfo,properties.getPrivateKey(),properties.getExpire());
            //更新cookie中的token
            CookieUtils.setCookie(request,response,properties.getCookieName(),token,properties.getExpire());
            //解析成功返回用户信息
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //失败响应500
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
