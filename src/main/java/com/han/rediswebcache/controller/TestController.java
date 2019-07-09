package com.han.rediswebcache.controller;

import com.han.rediswebcache.redis.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/rediscache")
public class TestController {
    @Autowired
    RedisUtil redisUtil;

    @GetMapping("/page")
    public String test() {

        return "error-page";
    }

    @PostMapping("/json")
    @ResponseBody
    public Test post() {
        return new Test("test");
    }


}
