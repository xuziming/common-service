package com.simon.credit.service.rocketmq;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSON;
import com.alibaba.rocketmq.client.producer.SendResult;
import com.alibaba.rocketmq.common.message.Message;

public class ProducterTest extends BaseTest {

	@Autowired
	private MQProducer producer;

	@Test
	public void sendMsg() throws InterruptedException {
		for (int i = 0; i < 1; i++) {
			new Thread(new Runnable() {
				public void run() {
					try {
						Message msg = new Message("test_topic_todo", new String("hello java").getBytes());
						SendResult result = producer.sendMessage(msg);
						System.out.println(JSON.toJSONString(result));
						System.out.println(result.getSendStatus());
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}).start();
		}

		TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
	}

}
