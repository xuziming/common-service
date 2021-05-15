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
	private static final Logger LOGGER = LoggerFactory.getLogger(MQProducerImpl.class);

	public MQProducerImpl() {}

	public MQProducerImpl(String producerGroup) {
		super(producerGroup);
	}

	@Override
	public void start() {
		try {
			super.start();

		} catch (MQClientException e) {
			LOGGER.error("start producer error", e);
		}
	}

	@Override
	public SendResult sendMessage(Message msg) {
		try {
			return send(msg);
		} catch (Exception e) {
			LOGGER.error("send message error", e);
			return null;
		}
	}

	@Override
	public void sendAsyncMessage(Message msg, final IMessageCallback callback) {
		try {
			send(msg, new SendCallback() {
				@Override
				public void onSuccess(SendResult sendResult) {
					callback.callback(sendResult);
				}

				@Override
				public void onException(Throwable e) {
					LOGGER.error("async send message callback error", e);
				}
			});
		} catch (Exception e) {
			LOGGER.error("async send message error", e);
		}
	}

	@Override
	public void sendAsyncMessage(Message msg) {
		sendAsyncMessage(msg, result -> {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("async send message success! message:" + result.toString());
			}
		});
	}

}