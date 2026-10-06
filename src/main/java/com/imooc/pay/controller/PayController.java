package com.imooc.pay.controller;

import com.imooc.pay.pojo.PayInfo;
import com.imooc.pay.service.impl.PayServiceImpl;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.model.PayResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/pay")
@Slf4j
public class PayController {
    @Autowired
    private PayServiceImpl payServiceImpl;
    @Autowired
    private WxPayConfig wxPayConfig;
    /// 创建订单方法
    @GetMapping("/create")
    public ModelAndView create(@RequestParam("orderId") String orderId,
                               @RequestParam("amount") BigDecimal amount) {

        PayResponse response = payServiceImpl.create(orderId, amount);

        Map map = new HashMap<>();
        map.put("codeUrl", response.getCodeUrl());
        map.put("orderId", orderId);
        map.put("returnUrl",wxPayConfig.getReturnUrl());
        return new ModelAndView("createForWxNative", map);
    }
    /// 异步通知回调方法
    @PostMapping("/notify")
    @ResponseBody
    /// @ResponseBody将 Java 对象自动序列化为 JSON（或 XML）格式，并写入到 HTTP 响应体（Response Body）中
    /// notifyData是微信传来给我们后端的参数，然后我这个方法是得到微信的支付信息再传给微信说不要再通知了
    /// 进一步解释：notifyData是pay发送的消息，是mall接收的消息
    public String asyncNotify(@RequestBody String notifyData) {
        return payServiceImpl.asyncNotify(notifyData);
    }

    @GetMapping("/queryByOrderId")
    @ResponseBody
    public PayInfo queryByOrderId(@RequestParam String orderId) {
        log.info("查询支付记录...");
        return payServiceImpl.queryByOrderId(orderId);
    }
}
