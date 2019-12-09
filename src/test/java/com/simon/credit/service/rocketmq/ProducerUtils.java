package com.simon.credit.service.rocketmq;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.producer.DefaultMQProducer;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

public class ProducerUtils {

	public static void main(String[] args) {
		DefaultMQProducer producer = new DefaultMQProducer("test_producer");
		producer.setNamesrvAddr("192.168.183.128:9876");
		try {
			producer.start();
			try {
				String bodyStr = "hello world";
				Message msg = new Message("topic_test", bodyStr.getBytes("UTF-8"));
				SendResult sendResult = producer.send(msg);
				System.out.println(JSON.toJSONString(sendResult));
				System.out.println("id:" + sendResult.getMsgId() + " status:" + sendResult.getSendStatus());
			} catch (Exception e) {
				e.printStackTrace();
				Thread.sleep(2000);
			} finally {
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			producer.shutdown();
		}
	}

}