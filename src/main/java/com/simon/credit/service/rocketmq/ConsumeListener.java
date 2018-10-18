package com.simon.credit.service.rocketmq;

import com.alibaba.rocketmq.common.message.Message;

public interface ConsumeListener {

	/**
	 * 消费MQ消息
	 * @param message 消息体内容
	 * @return true: 
	 * 				1 consume success, 
	 * 				2 consume failure, do not need retry later(reconsume later.)<br>
	 *         false: 
	 *         		consume failure, need to reconsume later(MQ will pull the message in seconds)
	 */
	boolean consume(Message message);

}
