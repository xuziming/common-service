package com.simon.credit.service.url;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 短URL@Enable注解
 * @author xuziming 2021-05-12
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(ShortUrlConfiguration.class)
public @interface EnableShortUrl {
}