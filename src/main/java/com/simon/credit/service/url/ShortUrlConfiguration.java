package com.simon.credit.service.url;

import org.springframework.context.annotation.Bean;

/**
 * 短URL服务配置
 * @author xuziming 2021-05-12
 */
public class ShortUrlConfiguration {

    @Bean
    public ShortUrlGenerator shortUrlGenerator() {
        return new ShortUrlGenerator();
    }

}