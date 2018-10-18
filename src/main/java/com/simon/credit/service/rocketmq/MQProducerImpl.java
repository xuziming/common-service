package com.simon.credit.service.rocketmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendCallback;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

@Service
public class MQProducerImpl extends DefaultMQProducer implements MQProducer {
	private static final Logger logger = LoggerFactory.getLogger(MQProducerImpl.class);

	public MQProducerImpl() {}

	public MQProducerImpl(String producerGroup) {
		super(producerGroup);
	}

	public void start() {
		try {
			super.start();

		} catch (MQClientException e) {
			logger.error("start producer error", e);
		}
	}

	public SendResult sendMessage(Message msg) {
		try {
			return send(msg);
		} catch (Exception e) {
			logger.error("send message error", e);
			return null;
		}
	}

	public void sendAsyncMessage(Message msg, final IMessageCallback callback) {
		try {
			send(msg, new SendCallback() {
				@Override
				public void onSuccess(SendResult sendResult) {
					callback.callback(sendResult);
				}

				@Override
				public void onException(Throwable e) {
					logger.error("async send message callback error", e);
				}
			});
		} catch (Exception e) {
			logger.error("async send message error", e);
		}
	}

	@Override
	public void sendAsyncMessage(Message msg) {
		sendAsyncMessage(msg, new IMessageCallback() {
			@Override
			public void callback(SendResult result) {
				if (logger.isInfoEnabled()) {
					logger.info("async send message success! message:" + result.toString());
				}
			}
		});
	}

}
