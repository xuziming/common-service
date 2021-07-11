package com.simon.credit.service.rocketmq;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
			super.setNamesrvAddr("127.0.0.1:9876");
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