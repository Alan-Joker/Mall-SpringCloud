package com.leyou.uoload.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class LeyouCorsfiguration {

    @Bean
    public CorsFilter corsFilter(){
        //初始化cors配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许跨域得域名,如果要携带cookie，不能写*，*代表所有域名都可以跨域访问
        corsConfiguration.addAllowedOrigin("http://manage.leyou.com");
        corsConfiguration.setAllowCredentials(true);
        //*代表所有的请求方法,Get,Post,Put,Delete..
        corsConfiguration.addAllowedMethod("*");
        //允许携带任何头信息
        corsConfiguration.addAllowedHeader("*");
        //初始化cors配置源对象
        UrlBasedCorsConfigurationSource corsConfigurationSource=new UrlBasedCorsConfigurationSource();
        //设置过滤参数
        corsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        //返回corsFilter实例。参数：cors配置源对象
        return new CorsFilter(corsConfigurationSource);
    }
}
