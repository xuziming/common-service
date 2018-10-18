package com.simon.credit.service.redis;

public interface RedisCallback<T, E> {

	T callback(E paramE);

}
