package com.simon.credit.service.rocketmq;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQConsumer extends DefaultMQPushConsumer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MQConsumer.class.getName());

    public MQConsumer(final String consumerGroup, final String topic, final String subExpression) {
        super(consumerGroup);

        try {
            super.subscribe(topic, subExpression);
        } catch (MQClientException e) {
            LOGGER.error("subcribe topic failure", e);
        }
    }

    @Override
    public void start() {
        try {
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

}