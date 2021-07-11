package com.simon.credit.service.rocketmq.ioc;

import com.alibaba.fastjson.JSON;
import com.simon.credit.service.rocketmq.BaseTest;
import com.simon.credit.service.rocketmq.MQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

public class ProducerStartup extends BaseTest {

    @Resource
    private MQProducer producer;

    @Test
    public void sendMsg() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    Message msg = new Message("TopicTest", "hello java".getBytes());
                    SendResult result = producer.sendMessage(msg);
                    System.out.println(JSON.toJSONString(result));
                    System.out.println(result.getSendStatus());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }

        TimeUnit.SECONDS.sleep(Long.MAX_VALUE);
    }

}