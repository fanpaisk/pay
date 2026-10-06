package com.imooc.pay.service.impl;

import com.google.gson.Gson;
import com.imooc.pay.dao.PayInfoMapper;
import com.imooc.pay.pojo.PayInfo;
import com.imooc.pay.service.IPayService;
import com.lly835.bestpay.enums.BestPayTypeEnum;
import com.lly835.bestpay.enums.OrderStatusEnum;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.model.PayResponse;
import com.lly835.bestpay.service.BestPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
/// 不用创建log对象即可使用log方法的注释
@Slf4j
@Service
public class PayServiceImpl implements IPayService {
    private final static String QUEUE_PAY_NOTIFY= "payNotify";
    //加入数据库业务
    @Autowired
    private BestPayService bestPayService;
    /// 这个报错不影响，只是idea识别问题
    @Autowired
    private PayInfoMapper payInfoMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PayResponse create(String orderId, BigDecimal amount) {
//        /// 以下都是bestpay_sdk微信支付的框架
//        WxPayConfig wxPayConfig = new WxPayConfig();
//        wxPayConfig.setAppId("********");
//        wxPayConfig.setMchId("********");
//        wxPayConfig.setMchKey("********");
//        /// 目前改成了异步回调地址
//        wxPayConfig.setNotifyUrl("********");
//
//        BestPayServiceImpl bestPayService = new BestPayServiceImpl();
//        bestPayService.setWxPayConfig(wxPayConfig);

        /*
        *支付信息写入数据库
         *
         */
        PayInfo payInfo = new PayInfo(Long.parseLong(orderId),
                /// OrderStatusEnum是bestpaySDK自带的支付转态枚举类
                OrderStatusEnum.NOTPAY.name(),
                amount
                );
        payInfoMapper.insertSelective(payInfo);

        PayRequest request = new PayRequest();
        request.setOrderName("借来的猫-bestpay_sdk");
        request.setOrderId(orderId);
        request.setOrderAmount(amount.doubleValue());
        request.setPayTypeEnum(BestPayTypeEnum.WXPAY_NATIVE);


        PayResponse response = bestPayService.pay(request);
        /// log方法日志记录支付结果是否成功
        log.info("发起支付 response={}", response);
        return response;
    }

    /*
    异步通知处理
    @parm notifyData
     */
    @Override
    /// 这里用string是因为异步回调商家要返回收到通知的消息，这个消息是string
    public String asyncNotify(String notifyData) {
        //1.签名检验
        PayResponse payResponse = bestPayService.asyncNotify(notifyData);
        log.info("异步通知 Response={}", payResponse);


        //2.金额校验（从数据库查订单）
        //比较严重（正常情况下不会发生）发出告警：钉钉，短信
        /// payInfo为数据库内一条信息，这一行将微信传来的通知得到的orderid传入dao方法获得数据库信息
        PayInfo payInfo = payInfoMapper.selectByOrderNo(Long.parseLong(payResponse.getOrderId()));
        if (payInfo==null){
            //告警
            throw new RuntimeException("通过orderNo查询到的内容是null");
        }
        //如果订单支付状态不是“已支付”
        if (!payInfo.getPlatformStatus().equals(OrderStatusEnum.SUCCESS.name())) {
            /// Double类型比较大小，精度。1.00 1.0很麻烦
            if (payInfo.getPayAmount().compareTo(BigDecimal.valueOf(payResponse.getOrderAmount())) != 0) {
                //告警
                throw new RuntimeException("异步通知中的金额和数据库里的不一致，orderNo=" + payResponse.getOrderId() + ",<UNK>" + payResponse.getOrderId());
            }
            //3.修改订单支付状态
            payInfo.setPlatformStatus(OrderStatusEnum.SUCCESS.name());
            /// 为数据设流水号。payResponse.getOutTradeNo()是由微信产生的
            payInfo.setPlatformNumber(payResponse.getOutTradeNo());
            payInfoMapper.updateByPrimaryKeySelective(payInfo);
        }
        //TODO pay发送MQ消息，mall接受MQ消息,
        //TODO mq作用：处理异步，高并发场景
        //把payInfo对象转成json
        amqpTemplate.convertAndSend(QUEUE_PAY_NOTIFY, new Gson().toJson(payInfo));


        //4告诉微信不用通知了
        return "<xml>\n" +
                "  <return_code><![CDATA[SUCCESS]]></return_code>\n" +
                "  <return_msg><![CDATA[OK]]></return_msg>\n" +
                "</xml>";
    }

    @Override
    public PayInfo queryByOrderId(String orderId) {
        return payInfoMapper.selectByOrderNo(Long.parseLong(orderId));
    }

}
