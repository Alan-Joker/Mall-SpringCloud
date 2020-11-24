package com.leyou.auth.client;

import com.leyou.user.api.UserApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * @Author Alan_
 * @create 2020/10/10 10:47
 */
@FeignClient("user-service")
public interface UserClient extends UserApi {
}
