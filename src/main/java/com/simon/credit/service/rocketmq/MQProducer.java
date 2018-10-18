package com.simon.credit.service.rocketmq;

import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

public interface MQProducer {

	/**
	 * @param msg
	 * @return null if failure, else success. 
	 * success type Divided many type. you can see SendResult define
	 */
	SendResult sendMessage(Message msg);

	/**
	 * 异步发送消息,写完内存直接返回
	 * @param msg
	 * @param callback
	 */
	void sendAsyncMessage(Message msg, IMessageCallback callback);

	void sendAsyncMessage(Message msg);

}
