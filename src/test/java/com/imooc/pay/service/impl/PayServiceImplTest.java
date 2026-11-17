package com.imooc.pay.service.impl;

import com.imooc.pay.PayApplicationTests;
import org.junit.Test;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

public class PayServiceImplTest extends PayApplicationTests {

    @Autowired
    private PayServiceImpl payServiceImpl;
    @Autowired
    private AmqpTemplate amqpTemplate;

    @Test
    public void create() {
        // 金额由服务端按 orderNo 反查，这里只传订单号（需要库里有该订单）
        payServiceImpl.create("1731000000000001");
    }

    @Test
    public void  sendMQMsg(){
        amqpTemplate.convertAndSend("payNotify","hello");
    }
}