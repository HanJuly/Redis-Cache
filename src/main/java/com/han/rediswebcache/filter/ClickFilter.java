package com.han.rediswebcache.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.han.rediswebcache.config.BaseConfig;
import com.han.rediswebcache.redis.RedisUtil;
import org.apache.logging.log4j.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@WebFilter(filterName = "click", urlPatterns = {"/*"})
public class ClickFilter implements Filter {
    private static Logger LOGGER = LoggerFactory.getLogger(ClickFilter.class);
    private static final String WEB_CACHE = "web-cache";
    private static final String JSON_CACHE = "json-cache";
    private static final String CACHE_COUNT = "cache-count";

    @Autowired
    RedisUtil redisUtil;

    @Autowired
    BaseConfig baseConfig;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        String path = httpServletRequest.getServletPath();
        LOGGER.info("Recevice request path:" + path);

        try {
             String jsonResult = (String) redisUtil.hget(JSON_CACHE, path);
            String pageResult = (String) redisUtil.hget(WEB_CACHE, path);
            double sorce = redisUtil.zIncreament(CACHE_COUNT, path, 1);
            LOGGER.info("Redis increment successfuly.");
            String result = Strings.isEmpty(pageResult) ? jsonResult : pageResult;
            if (!Strings.isEmpty(jsonResult)) {
                resp.setContentType("application/json; charset=utf-8");
            } else if (!Strings.isEmpty(pageResult)) {
                resp.setContentType("text/html; charset=utf-8");
            }

            if (Strings.isEmpty(result) && sorce > baseConfig.getClickCount()) {
                CacheResponseWrapper cacheResponseWrapper = new CacheResponseWrapper(resp);
                filterChain.doFilter(servletRequest, cacheResponseWrapper);
                jsonResult = cacheResponseWrapper.getBody();
                pageResult = cacheResponseWrapper.getBufferd();

                Map<String, String> cache = new HashMap<>();
                if (!Strings.isEmpty(jsonResult) && isJSONValid2(jsonResult)) {
                    cache.put(path, jsonResult);
                    redisUtil.hmset(JSON_CACHE, cache);
                    resp.setContentType("application/json; charset=utf-8");
                    LOGGER.info("Redis cache json successfuly from path {}", path);
                } else {
                    cache.put(path, pageResult);
                    redisUtil.hmset(WEB_CACHE, cache);

                    LOGGER.info("Redis cache page successfuly from path {}", path);
                }
                result = Strings.isEmpty(pageResult) ? jsonResult : pageResult;
                resp.getWriter().write(result);
                return;
            } else {
                if(!Strings.isEmpty(result)){
                    LOGGER.info("Data from redis cache.");
                    resp.getWriter().write(result);
                    return;
                }else {
                    LOGGER.info("Path click count < 1");
                }
            }
        } catch (Exception e) {
            redisUtil.zadd(CACHE_COUNT, path, 1);
            LOGGER.error(e.getCause().toString());
            LOGGER.info("Redis add successfuly.");
        }
        filterChain.doFilter(servletRequest, resp);
    }

    @Override
    public void destroy() {

    }

    /**
     * Jackson library
     *
     * @param jsonInString
     * @return
     */
    private boolean isJSONValid2(String jsonInString) {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            mapper.readTree(jsonInString);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
