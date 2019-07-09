package com.han.rediswebcache.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@PropertySource(value = "classpath:config.properties")
public class BaseConfig {
    @Value("${click.count}")
    private double clickCount;

    public double getClickCount() {
        return clickCount;
    }

    public void setClickCount(double clickCount) {
        this.clickCount = clickCount;
    }

}
