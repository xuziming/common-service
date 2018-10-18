package com.simon.credit.service.rocketmq;

import com.alibaba.rocketmq.client.producer.SendResult;

public interface IMessageCallback {

	void callback(SendResult result);

}
