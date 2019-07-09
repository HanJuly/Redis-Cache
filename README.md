# Redis-Cache
> 以下均为本人理解，欢迎大佬指出错误

>  github地址：[redis缓存网页和json数据](https://github.com/HanJuly/Redis-Cache)

## 缓存的原理
- 1.首先使用redis实现一个计数器（排序set）记录网站的链接被点击的次数
- 2.链接点击的次数超过一定的阈值后，保存到一个Hash中
- 3 .当第二次再使用这个链接时，从redis中读取结果
>  缓存的数据包括网页和json数据

## java层面需要的组件
1.一个fitler用于实现计数器和缓存的功能
```
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse resp = (HttpServletResponse) servletResponse;
        String path = httpServletRequest.getServletPath();
        LOGGER.info("Recevice request path:" + path);

        try {
            //从缓存中取
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
                //当缓存中不存在，并且连接的使用次数超过阈值就进行缓存
                CacheResponseWrapper cacheResponseWrapper = new CacheResponseWrapper(resp);
                filterChain.doFilter(servletRequest, cacheResponseWrapper);
                jsonResult = cacheResponseWrapper.getBody();
                pageResult = cacheResponseWrapper.getBufferd();

                Map<String, String> cache = new HashMap<>();
                if (!Strings.isEmpty(jsonResult) && isJSONValid2(jsonResult)) {
                    //当结果是JSONResult
                    cache.put(path, jsonResult);
                    redisUtil.hmset(JSON_CACHE, cache);
                    resp.setContentType("application/json; charset=utf-8");
                    LOGGER.info("Redis cache json successfuly from path {}", path);
                } else {
                    //当结果是网页时
                    cache.put(path, pageResult);
                    redisUtil.hmset(WEB_CACHE, cache);

                    LOGGER.info("Redis cache page successfuly from path {}", path);
                }
                result = Strings.isEmpty(pageResult) ? jsonResult : pageResult;
                //将结果重新写入response，并提前结束避免调用 filterChain.doFilter(servletRequest, cacheResponseWrapper)报错
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

```
>    将结果重新写入response，并提前结束避免调用 filterChain.doFilter(servletRequest, cacheResponseWrapper)报错

2.CacheResponseWrapper extends HttpServletResponseWrapper 用于从response中获取请求的结果

```
public class CacheResponseWrapper extends HttpServletResponseWrapper {
    private CharArrayWriter bufferd;
    private PrintWriter printWriter;
    private ServletOutputStream outputStream;
    private ByteArrayOutputStream byteArrayOutputStream;

    public CacheResponseWrapper(HttpServletResponse response) {
        super(response);
        bufferd = new CharArrayWriter();
        printWriter = new PrintWriter(bufferd);
        byteArrayOutputStream = new ByteArrayOutputStream();
        outputStream = new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                byteArrayOutputStream.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {

            }
        };
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        return printWriter;
    }

    public String getBufferd() {
        //普通请求方式使用这个获取结果
        return bufferd.toString();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    public String getBody() {
        //json请求方式获取这个结果
        return new String(byteArrayOutputStream.toByteArray());
    }
}

```
3.操作redis的工具类
比如：
```
 public  boolean hmset(String key, Map<String, String> value) {
        boolean result = false;
        try {
            redisTemplate.opsForHash().putAll(key, value);
            result = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

```
