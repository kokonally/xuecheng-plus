package com.xuecheng.orders.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xuecheng.base.exception.XueChengPlusException;
import com.xuecheng.base.utils.IdWorkerUtils;
import com.xuecheng.base.utils.QRCodeUtil;
import com.xuecheng.messagesdk.model.po.MqMessage;
import com.xuecheng.messagesdk.service.MqMessageService;
import com.xuecheng.orders.config.PayNotifyConfig;
import com.xuecheng.orders.mapper.XcOrdersGoodsMapper;
import com.xuecheng.orders.mapper.XcOrdersMapper;
import com.xuecheng.orders.mapper.XcPayRecordMapper;
import com.xuecheng.orders.model.dto.AddOrderDto;
import com.xuecheng.orders.model.dto.PayRecordDto;
import com.xuecheng.orders.model.dto.PayStatusDto;
import com.xuecheng.orders.model.po.XcOrders;
import com.xuecheng.orders.model.po.XcOrdersGoods;
import com.xuecheng.orders.model.po.XcPayRecord;
import com.xuecheng.orders.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
@Slf4j
@Service
public class OrderServiceImpl implements OrderService {
    
    @Autowired
    private XcOrdersMapper xcOrdersMapper;

    @Autowired
    private XcOrdersGoodsMapper xcOrdersGoodsMapper;

    @Autowired
    private XcPayRecordMapper xcPayRecordMapper;

    @Value("${pay.qrcodeurl}")
    private String qrcodeurl;

    @Value("${pay.alipay.APP_ID}")
    String APP_ID;

    @Value("${pay.alipay.APP_PRIVATE_KEY}")
    String APP_PRIVATE_KEY;

    @Value("${pay.alipay.ALIPAY_PUBLIC_KEY}")
    String ALIPAY_PUBLIC_KEY;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private MqMessageService mqMessageService;

    @Transactional
    @Override
    public PayRecordDto createOrder(String userId, AddOrderDto addOrderDto) {
        //1.生成订单并插入到数据库  订单表 订单明细表
        XcOrders xcOrders = this.saveXcOrders(userId, addOrderDto);

        //2.插入支付记录
        XcPayRecord payRecord = this.createPayRecord(xcOrders);

        //3.生成二维码
        Long payNo = payRecord.getPayNo();
        QRCodeUtil qrCodeUtil = new QRCodeUtil();
        String url = String.format(qrcodeurl, payNo);  //支付二维码的url
        String qrCode = null;
        try {
            qrCode = qrCodeUtil.createQRCode(url, 300, 300);
        } catch (IOException e) {
            XueChengPlusException.cast("生成二维码出错");
        }
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        payRecordDto.setQrcode(qrCode);
        return payRecordDto;
    }

    @Override
    public XcPayRecord getPayRecordByPayno(String payNo) {
        LambdaQueryWrapper<XcPayRecord> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcPayRecord::getPayNo, payNo);
        return xcPayRecordMapper.selectOne(queryWrapper);
    }

    @Override
    public PayRecordDto queryPayResult(String payNo) {
        //1.调用支付宝接口获取支付结果
        PayStatusDto payStatusDto = this.queryPayResultFromAlipay(payNo);

        //2.更新支付表和订单表的支付状态
        OrderService proxy = (OrderService) AopContext.currentProxy();
        proxy.saveAliPayStatus(payStatusDto);
        //返回最新的支付记录
        XcPayRecord payRecord = this.getPayRecordByPayno(payNo);
        PayRecordDto payRecordDto = new PayRecordDto();
        BeanUtils.copyProperties(payRecord, payRecordDto);
        return payRecordDto;
    }

    @Override
    public PayStatusDto queryPayResultFromAlipay(String payNo) {
        AlipayClient alipayClient = new DefaultAlipayClient("https://openapi-sandbox.dl.alipaydev.com/gateway.do",
                APP_ID,APP_PRIVATE_KEY,"json","UTF-8",ALIPAY_PUBLIC_KEY,"RSA2");
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        JSONObject bizContent = new JSONObject();
        bizContent.put("out_trade_no", payNo);
        //bizContent.put("trade_no", "2014112611001004680073956707");
        request.setBizContent(bizContent.toString());
        AlipayTradeQueryResponse response = null;
        String body = null;  //支付宝返回的信息
        try {
            response = alipayClient.execute(request);
            body = response.getBody();
        } catch (AlipayApiException e) {
            XueChengPlusException.cast("请求支付宝查询结果异常");
        }

        //解析支付结果
        PayStatusDto payStatusDto = new PayStatusDto();
        Map bodyMap = JSON.parseObject(body, Map.class);
        Map<String, String>responeMap = (Map<String, String>) bodyMap.get("alipay_trade_query_response");
        payStatusDto.setOut_trade_no(payNo);
        payStatusDto.setTrade_no(responeMap.get("trade_no"));  //支付宝的交易号
        payStatusDto.setTrade_status(responeMap.get("trade_status"));  //支付状态
        payStatusDto.setApp_id(APP_ID);
        payStatusDto.setTotal_amount(responeMap.get("total_amount"));  //总金额
        return payStatusDto;
    }


    @Transactional
    @Override
    public void saveAliPayStatus(PayStatusDto payStatusDto) {
        String trade_status = payStatusDto.getTrade_status();  //支付宝支付状态
        String payNo = payStatusDto.getOut_trade_no();  //支付记录号

        //1.查询数据库 判断是否已经完成支付
        XcPayRecord payRecord = this.getPayRecordByPayno(payNo);
        if (payRecord == null) {
            XueChengPlusException.cast("找不到相关的支付记录");
        }
        String statusFromDB = payRecord.getStatus();  //支付状态

        Long orderId = payRecord.getOrderId();
        XcOrders order = xcOrdersMapper.selectById(orderId);
        if (order == null) {
            XueChengPlusException.cast("订单不存在");
        }
        if ("601002".equals(statusFromDB)) {
            //已经支付过了
            return;
        }

        if (!"TRADE_SUCCESS".equals(trade_status)) {
            //支付宝返回的信息，为未支付
            XueChengPlusException.cast("您未支付");
        }

        //2.保存到订单表
        order.setStatus("600002");
        int updateOrderrs = xcOrdersMapper.updateById(order);
        if (updateOrderrs <= 0) {
            XueChengPlusException.cast("订单状态更新失败");
        }

        //3.保存到支付表中
        payRecord.setStatus("601002");
        payRecord.setOutPayNo(payStatusDto.getTrade_no());  //支付宝的订单号
        payRecord.setOutPayChannel("Alipay");
        payRecord.setPaySuccessTime(LocalDateTime.now());
        int updatePayRecord = xcPayRecordMapper.updateById(payRecord);
        if (updatePayRecord <= 0) {
            XueChengPlusException.cast("支付状态更新失败");
        }

        //将消息写到数据库
        MqMessage mqMessage = mqMessageService.addMessage(PayNotifyConfig.MESSAGE_TYPE, order.getOutBusinessId(), order.getOrderType(), null);

        //发送消息
        this.notifyPayResult(mqMessage);
    }

    @Override
    public void notifyPayResult(MqMessage message) {

        String jsonString = JSON.toJSONString(message);
        Message rabbitMessage = MessageBuilder.withBody(jsonString.getBytes(StandardCharsets.UTF_8))
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)  //消息持久化
                .build();
        Long messageId = message.getId();
        CorrelationData correlationData = new CorrelationData(messageId.toString());  //回调方法
        correlationData.getFuture().addCallback(result -> {
            if (result.isAck()) {
                //消息成功投递到交换机
                log.info("消息发送成功:{}", jsonString);
                //删除数据库存的消息
                mqMessageService.completed(messageId);
            }else {
                //消息发送成功但是没有成功投递到交换机
                log.error("消息投送到交换机失败, 交换机:{}, 队列:{}, 消息内容:{}",
                        PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,
                        PayNotifyConfig.PAYNOTIFY_QUEUE,
                        jsonString);
            }
        }, ex -> {
           //发送失败
            log.error("消息发送失败, 交换机:{}, 队列:{}, 消息内容:{}",
                    PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT,
                    PayNotifyConfig.PAYNOTIFY_QUEUE,
                    jsonString);
        });
        rabbitTemplate.convertAndSend(PayNotifyConfig.PAYNOTIFY_EXCHANGE_FANOUT, "", rabbitMessage, correlationData);
    }

    /**
     * 插入支付记录表
     * @param orders 订单信息
     * @return XcPayRecord
     */
    public XcPayRecord createPayRecord(XcOrders orders) {
        //此订单存在就不添加支付记录
        Long orderId = orders.getId();  //订单id
        XcOrders xcOrders = xcOrdersMapper.selectById(orderId);
        if (xcOrders == null) {
            XueChengPlusException.cast("订单不存在");
        }
        String status = xcOrders.getStatus();  //订单的状态

        //订单已支付 不允许添加记录
        if ("601002".equals(status)) {
            //支付成功
            XueChengPlusException.cast("此订单已支付");
        }

        XcPayRecord xcPayRecord = new PayRecordDto();
        xcPayRecord.setPayNo(IdWorkerUtils.getInstance().nextId());  //支付记录号 将来传给支付宝
        xcPayRecord.setOrderId(orderId);
        xcPayRecord.setOrderName(orders.getOrderName());
        xcPayRecord.setTotalPrice(orders.getTotalPrice());
        xcPayRecord.setCurrency("CNY");
        xcPayRecord.setCreateDate(LocalDateTime.now());
        xcPayRecord.setStatus("601001");  //未支付
        xcPayRecord.setUserId(xcOrders.getUserId());
        int insert = xcPayRecordMapper.insert(xcPayRecord);
        if (insert <= 0) {
            XueChengPlusException.cast("插入支付记录失败");
        }
        return xcPayRecord;
    }

    /**
     * 保存订单信息
     * @param userId 用户id
     * @param addOrderDto 订单参数
     * @return XcOrders
     */
    public XcOrders saveXcOrders(String userId, AddOrderDto addOrderDto) {
        //插入订单表 订单明细表
        //幂等性判断
        String chooseCourseId = addOrderDto.getOutBusinessId();
        XcOrders xcOrders = this.getOrderByBusinessId(chooseCourseId);
        if (xcOrders != null) {
            //订单已经存在
            return xcOrders;
        }

        //创建订单
        xcOrders = new XcOrders();
        xcOrders.setId(IdWorkerUtils.getInstance().nextId());  //雪花算法生成订单号
        xcOrders.setTotalPrice(addOrderDto.getTotalPrice());
        xcOrders.setCreateDate(LocalDateTime.now());
        xcOrders.setStatus("600001");  //未支付
        xcOrders.setUserId(userId);
        xcOrders.setOrderType("60201");  //订单类型
        xcOrders.setOrderName(addOrderDto.getOrderName());
        xcOrders.setOrderDescrip(addOrderDto.getOrderDescrip());
        xcOrders.setOrderDetail(addOrderDto.getOrderDetail());
        xcOrders.setOutBusinessId(chooseCourseId);
        int insert = xcOrdersMapper.insert(xcOrders);
        if (insert <= 0) {
            XueChengPlusException.cast("添加订单失败");
        }

        //插入订单明细
        String orderDetailJson = addOrderDto.getOrderDetail();
        Long orderId = xcOrders.getId();
        List<XcOrdersGoods> xcOrdersGoods = JSON.parseArray(orderDetailJson, XcOrdersGoods.class);
        xcOrdersGoods.forEach(item -> {
            item.setOrderId(orderId);
            xcOrdersGoodsMapper.insert(item);
        });

        return xcOrders;
    }

    /**
     * 根据外部业务id查询订单信息
     * @param businessId 选课id
     * @return
     */
    public XcOrders getOrderByBusinessId(String businessId) {
        LambdaQueryWrapper<XcOrders> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(XcOrders::getOutBusinessId, businessId);
        return xcOrdersMapper.selectOne(queryWrapper);
    }
}
