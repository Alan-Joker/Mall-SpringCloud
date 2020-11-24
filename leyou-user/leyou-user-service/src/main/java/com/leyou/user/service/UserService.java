package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.userMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

    @Autowired
    private userMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key_prefix="user:verify";

    /**
     * 校验数据是否可用
     * @param data
     * @param type
     * @return
     */
    public Boolean checkUser(String data, Integer type) {

        User record = new User();
        switch (type) {
            case 1:
                record.setUsername(data);
                break;
            case 2:
                record.setPhone(data);
                break;
            default:
                return null;
        }
        return this.userMapper.selectCount(record) == 0;
    }

    /**
     * 发送验证码
     * @param phone
     */
    public void sendVerifyCode(String phone) {
        if(StringUtils.isBlank(phone)){
            return;
        }
        //生成验证码
        String code = NumberUtils.generateCode(6);
        System.out.println(code);
        //发送消息到rabbitmq
        Map<String,String> msg=new HashMap<>();
        msg.put("phone",phone);
        msg.put("code",code);
        amqpTemplate.convertAndSend("LEYOU.SMS.EXCHANGE","verifycode.sms",msg);
        //把验证码保存到redis中
        this.redisTemplate.opsForValue().set(key_prefix+phone,code,5, TimeUnit.MINUTES);
    }

    public void register(User user, String code) {
        //查询redis中的验证码
        String redisCode = redisTemplate.opsForValue().get(key_prefix + user.getPhone());

        //校验验证码
        if(!StringUtils.equals(code,redisCode)){
            return;
        }
        //生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        //加盐加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(),salt));
        //新增用户
        user.setId(null);
        user.setCreated(new Date());
        userMapper.insertSelective(user);

        //从redis中删除验证码
        redisTemplate.delete(key_prefix + user.getPhone());
    }

    public User queryUser(String username, String password) {

        User record=new User();
        record.setUsername(username);
        User user=this.userMapper.selectOne(record);
        //校验用户名
        if(user==null){
            return null;
        }
        //校验密码
        if(!user.getPassword().equals(CodecUtils.md5Hex(password,user.getSalt()))){
            return null;
        }
        //用户名密码正确
        return user;

    }
}
