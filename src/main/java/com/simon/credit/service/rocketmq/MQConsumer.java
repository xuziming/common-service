package com.simon.credit.service.rocketmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.rocketmq.client.consumer.DefaultMQPushConsumer;
import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import com.alibaba.rocketmq.client.exception.MQClientException;
import com.alibaba.rocketmq.common.message.MessageExt;

public class MQConsumer extends DefaultMQPushConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQConsumer.class.getName());

    @Override
    public void start() {
        try {
            // super.setNamesrvAddr("127.0.0.1:9876");
            super.start();
        } catch (MQClientException e) {
            LOGGER.error("start consumer error", e);
        }
    }

	public void setConsumeListener(final ConsumeListener consumeListener) {
		super.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
			try {
				MessageExt msg = msgs.get(0);

				if (consumeListener.consume(msg)) {
					return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
				} else {
					return ConsumeConcurrentlyStatus.RECONSUME_LATER;
				}
			} catch (Throwable e) {
				LOGGER.error("consume message error", e);
				// in framework,by default ,if consume failure,
				// will reconsume later.but caller can decide whether need to reconsume
				return ConsumeConcurrentlyStatus.RECONSUME_LATER;
			}
		});
	}

    public MQConsumer(final String consumerGroup, final String topic, final String subExpression) {
        super(consumerGroup);

        try {
            super.subscribe(topic, subExpression);
        } catch (MQClientException e) {
            LOGGER.error("subcribe topic failure", e);
        }
    }

}
