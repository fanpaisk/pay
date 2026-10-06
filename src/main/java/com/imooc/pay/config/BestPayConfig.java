package com.imooc.pay.config;

import com.lly835.bestpay.config.AliPayConfig;
import com.lly835.bestpay.config.WxPayConfig;
import com.lly835.bestpay.service.BestPayService;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/// 将重复使用的账户信息放入ioc容器里，供随时使用，增加复用性
@Component
public class BestPayConfig {
    @Autowired
    private WxAccountConfig wxAccountConfig;

    //    @Bean
//    public BestPayService bestPayService(){
//        WxPayConfig wxPayConfig = new WxPayConfig();
//        wxPayConfig.setAppId("********");
//        wxPayConfig.setMchId("********");
//        wxPayConfig.setMchKey("********");
//        wxPayConfig.setNotifyUrl("********");
//        wxPayConfig.setReturnUrl("********");
//
//        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
//        bestPayService.setWxPayConfig(wxPayConfig);
//        return bestPayService;
//    }
    @Bean
    public BestPayService bestPayService(WxPayConfig wxPayConfig) {
//    AliPayConfig aliPayConfig = new AliPayConfig();
//    aliPayConfig.setAppId(alipayAccountConfig.getAppId());
//    aliPayConfig.setPrivateKey(alipayAccountConfig.getPrivateKey());
//    aliPayConfig.setAliPayPublicKey(alipayAccountConfig.getPublicKey());
//    aliPayConfig.setNotifyUrl(alipayAccountConfig.getNotifyUrl());
//    aliPayConfig.setReturnUrl(alipayAccountConfig.getReturnUrl());

        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
        bestPayService.setWxPayConfig(wxPayConfig);
//    bestPayService.setAliPayConfig(aliPayConfig);
        return bestPayService;
    }

    @Bean
    public WxPayConfig wxPayConfig() {
        WxPayConfig wxPayConfig = new WxPayConfig();
        wxPayConfig.setAppId(wxAccountConfig.getAppId());
        wxPayConfig.setMchId(wxAccountConfig.getMchId());
        wxPayConfig.setMchKey(wxAccountConfig.getMchkey());
        wxPayConfig.setNotifyUrl(wxAccountConfig.getNotifyUrl());
        wxPayConfig.setReturnUrl(wxAccountConfig.getReturnUrl());
        return wxPayConfig;
    }
}
