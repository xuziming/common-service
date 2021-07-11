package com.simon.credit.service.rocketmq.listener;

import com.simon.credit.service.rocketmq.ConsumeListener;
import org.apache.rocketmq.common.message.Message;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class XxxConsumerListener implements ConsumeListener {

    private static final ThreadLocal<DateFormat> DATE_FORMAT_TL = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss E"));

    @Override
    public boolean consume(Message message) {
        String msg = new String(message.getBody());
        String dt = DATE_FORMAT_TL.get().format(new Date());
        System.out.println("from consumer test access time:" + dt + " message content:" + msg);
        return true;
    }

}