package com.simon.credit.service.url;

import redis.clients.jedis.Jedis;

import javax.annotation.Resource;

/**
 * 短URL生成器
 * @author xuziming 2021-05-12
 */
public class ShortUrlGenerator {

    private static final String SHORT_URL_KEY     = "SHORT_URL_KEY";
    private static final String LOCALHOST         = "http://localhost:4444/";
    private static final String SHORT_LONG_PREFIX = "short_long_prefix_";
    private static final String CACHE_KEY_PREFIX  = "cache_key_prefix_";
    private static final int CACHE_SECONDS        = 1 * 60 * 60;// 一小时

    @Resource
    private String redisConfig;

    private Jedis jedis;

    public ShortUrlGenerator() {
        this.jedis = new Jedis(this.redisConfig);
    }

    public ShortUrlGenerator(String redisConfig) {
        this.redisConfig = redisConfig;
        this.jedis = new Jedis(this.redisConfig);
    }

    public String getShortURL(String longUrl, Decimal decimal) {
        // 查询缓存
        String cache = jedis.get(CACHE_KEY_PREFIX + longUrl);
        if (cache != null) {
            return LOCALHOST + toOtherBaseString(Long.valueOf(cache), decimal.x);
        }

        // 自增
        long serialNo = jedis.incr(SHORT_URL_KEY);
        // 在数据库中保存短-长URL的映射关系, 可以保存在MySQL中
        jedis.set(SHORT_LONG_PREFIX + serialNo, longUrl);
        // 写入缓存
        jedis.setex(CACHE_KEY_PREFIX + longUrl, CACHE_SECONDS, String.valueOf(serialNo));
        return LOCALHOST + toOtherBaseString(serialNo, decimal.x);
    }

    /** 在进制表示中的字符集合 */
    final static char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
                                  'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
                                  'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
                                  'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
                                  'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    /** 由10进制的数字转换到其它进制 */
    private String toOtherBaseString(long n, int base) {
        long num = 0;
        if (n < 0) {
            num = ((long) 2 * 0x7fffffff) + n + 2;
        } else {
            num = n;
        }
        char[] buf = new char[32];
        int charPos = 32;
        while ((num / base) > 0) {
            buf[--charPos] = digits[(int) (num % base)];
            num /= base;
        }
        buf[--charPos] = digits[(int) (num % base)];
        return new String(buf, charPos, (32 - charPos));
    }

    enum Decimal {
        D32(32),
        D64(64);

        int x;

        Decimal(int x) {
            this.x = x;
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < 100; i++) {
            System.out.println(new ShortUrlGenerator("localhost").getShortURL("www.baidu.com", Decimal.D32));
            System.out.println(new ShortUrlGenerator("localhost").getShortURL("www.baidu.com", Decimal.D64));
        }
    }

}