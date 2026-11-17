package com.imooc.pay.dao;

import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

/**
 * 反查商城订单。
 *
 * <p>pay 与 mall 共用一个库，所以金额以 mall_order 为准，不信任 /pay/create 的调用方传参。
 */
public interface OrderMapper {

    /**
     * 只返回“待付款”（status=10）订单的应付金额。
     * 返回 null 表示订单不存在、已支付、已取消或已关闭。
     */
    BigDecimal selectPayableAmount(@Param("orderNo") Long orderNo);
}
