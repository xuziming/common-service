package com.simon.credit.service.rocketmq;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.alibaba.rocketmq.common.message.Message;

public class TodoConsumerListener implements ConsumeListener {

	@Override
	public boolean consume(Message message) {
		String msg = new String(message.getBody());

		DateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E");
		String dt = dateformat.format(new Date());
		System.out.println("from consumer test access time:" + dt + " message content:" + msg);

		return true;
	}

}
