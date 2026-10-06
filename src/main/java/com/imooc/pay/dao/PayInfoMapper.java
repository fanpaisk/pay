package com.imooc.pay.dao;

import com.imooc.pay.pojo.PayInfo;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;

/// pay表的对象，由generate生成的三个之一（mappers下xml存sql语句，dao层下sql语句调用方法，pojo下的表配成对象的类），
/// 作为mybatis能连接数据库的组件之一
//由于启动类下@MapperScan(basePackages = "com.imooc.pay.dao")都不用写@Mapper了
public interface PayInfoMapper {
    int deleteByPrimaryKey(Integer id);

    int insert(PayInfo record);

    int insertSelective(PayInfo record);

    PayInfo selectByPrimaryKey(Integer id);

    int updateByPrimaryKeySelective(PayInfo record);

    int updateByPrimaryKey(PayInfo record);
    /// 手动写一个方法，用来对数据库金额校验
    PayInfo selectByOrderNo(Long id);


}