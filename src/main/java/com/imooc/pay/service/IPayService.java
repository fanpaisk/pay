package com.imooc.pay.service;

import com.imooc.pay.pojo.PayInfo;
import com.lly835.bestpay.model.PayResponse;

public interface IPayService {
    /*
    创建/发起支付：金额由服务端按 orderNo 反查，不从调用方接收
     */
    PayResponse create(String orderId);
    /*
    异步通知处理
     */
    /// 这里用string是因为异步回调商家要返回收到通知的消息，这个消息是string
    String asyncNotify(String notifyData);
    /**
     * 查询支付记录(通过订单号)
     * @param orderId
     * @return
     */
    PayInfo queryByOrderId(String orderId);
}

