package com.simon.credit.service.rocketmq;

import org.apache.rocketmq.client.producer.SendResult;

public interface IMessageCallback {

	void callback(SendResult result);

}