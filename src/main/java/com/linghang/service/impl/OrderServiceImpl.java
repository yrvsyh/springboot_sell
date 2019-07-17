package com.linghang.service.impl;

import com.linghang.converter.OrderMasterToOrderDTOConverter;
import com.linghang.dataobject.OrderDetail;
import com.linghang.dataobject.OrderMaster;
import com.linghang.dataobject.ProductInfo;
import com.linghang.dto.CartDTO;
import com.linghang.dto.OrderDTO;
import com.linghang.enums.OrderStatusEnum;
import com.linghang.enums.PayStatusEnum;
import com.linghang.enums.ResultEnum;
import com.linghang.exception.SellException;
import com.linghang.repository.OrderDetailRepository;
import com.linghang.repository.OrderMasterRepository;
import com.linghang.service.OrderService;
import com.linghang.service.ProductService;
import com.linghang.service.WebSocket;
import com.linghang.utils.KeyUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderDetailRepository orderDetailRepository;

    @Autowired
    private OrderMasterRepository orderMasterRepository;

    @Autowired
    private WebSocket webSocket;

    @Override
    @Transactional
    public OrderDTO creat(OrderDTO orderDTO) {

        String orderId = KeyUtil.genUniqueKey();
        BigDecimal orderAmount = new BigDecimal(0);

        //1. 查询商品（数量，价格）
        for (OrderDetail orderDetail : orderDTO.getOrderDetailList()) {
            ProductInfo productInfo = productService.findOne(orderDetail.getProductId());
            if (productInfo == null) {
                throw new SellException(ResultEnum.PRODUCT_NOT_EXIST);
            }

            //2. 计算总价
            orderAmount = productInfo.getProductPrice()
                    .multiply(new BigDecimal(orderDetail.getProductQuantity()))
                    .add(orderAmount);

            // 订单详情入库
            orderDetail.setOrderId(orderId);
            orderDetail.setDetailId(KeyUtil.genUniqueKey());
            orderDetail.setProductName(productInfo.getProductName());
            BeanUtils.copyProperties(productInfo, orderDetail);
            orderDetailRepository.save(orderDetail);
        }

        //3. 写入订单数据库
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderId(orderId);
        BeanUtils.copyProperties(orderDTO, orderMaster);
        orderMaster.setOrderAmount(orderAmount);
        orderMaster.setOrderStatus(OrderStatusEnum.NEW.getCode());
        orderMaster.setPayStatus(PayStatusEnum.WAIT.getCode());
        orderMasterRepository.save(orderMaster);

        //4. 扣库存
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                .map(e -> new CartDTO(e.getProductId(), e.getProductQuantity()))
                .collect(Collectors.toList());
        productService.decreaseStock(cartDTOList);

        //发送websocket消息
        webSocket.sendMessage(orderDTO.getOrderId());

        return orderDTO;
    }

    @Override
    public OrderDTO findOne(String orderId) {
        OrderDTO orderDTO = new OrderDTO();
        OrderMaster orderMaster = new OrderMaster();
        orderMaster.setOrderStatus(null);
        orderMaster.setPayStatus(null);
        orderMaster.setOrderId(orderId);
        Example<OrderMaster> example = Example.of(orderMaster);
        orderMaster = orderMasterRepository.findOne(example).orElse(null);
        if (orderMaster == null) {
            throw new SellException(ResultEnum.ORDER_NOT_EXIST);
        }
        List<OrderDetail> orderDetailList = orderDetailRepository.findByOrderId(orderMaster.getOrderId());
        if (CollectionUtils.isEmpty(orderDetailList)) {
            throw new SellException(ResultEnum.ORDERDETAIL_NOT_EXIST);
        }
        BeanUtils.copyProperties(orderMaster, orderDTO);
        orderDTO.setOrderDetailList(orderDetailList);
        return orderDTO;
    }

    @Override
    public Page<OrderDTO> findList(String buyerOpenId, Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findByBuyerOpenid(buyerOpenId, pageable);
        List<OrderDTO> orderDTOList = OrderMasterToOrderDTOConverter.convert(orderMasterPage.getContent());
        return new PageImpl<OrderDTO>(orderDTOList, pageable, orderMasterPage.getTotalElements());
    }

    @Override
    public Page<OrderDTO> findList(Pageable pageable) {
        Page<OrderMaster> orderMasterPage = orderMasterRepository.findAll(pageable);
        List<OrderDTO> orderDTOList = OrderMasterToOrderDTOConverter.convert(orderMasterPage.getContent());
        return new PageImpl<OrderDTO>(orderDTOList, pageable, orderMasterPage.getTotalElements());
    }

    @Override
    @Transactional
    public OrderDTO cancel(OrderDTO orderDTO) {

        // 判断订单状态
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())) {
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        // 修改订单状态
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderStatus(OrderStatusEnum.CANCEL.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }

        // 返回库存
        if (CollectionUtils.isEmpty(orderDTO.getOrderDetailList())) {
            throw new SellException(ResultEnum.ORDER_DETAIL_EMPTY);
        }
        List<CartDTO> cartDTOList = orderDTO.getOrderDetailList().stream()
                .map(e -> new CartDTO(e.getProductId(), e.getProductQuantity()))
                .collect(Collectors.toList());
        productService.increaseStock(cartDTOList);

        // 如果已支付，需要退款
        if (orderDTO.getPayStatus().equals(PayStatusEnum.SUCCESS.getCode())) {
            //TODO 退款处理
        }

        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO finish(OrderDTO orderDTO) {

        // 判断订单状态
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())) {
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        //修改订单状态
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setOrderStatus(OrderStatusEnum.FINISHED.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        return orderDTO;
    }

    @Override
    @Transactional
    public OrderDTO paid(OrderDTO orderDTO) {

        // 判断订单状态
        if (!orderDTO.getOrderStatus().equals(OrderStatusEnum.NEW.getCode())) {
            throw new SellException(ResultEnum.ORDER_STATUS_ERROR);
        }

        // 判断支付状态
        if (!orderDTO.getPayStatus().equals(PayStatusEnum.WAIT.getCode())) {
            throw new SellException(ResultEnum.ORDER_PAY_STATUS_ERROR);
        }

        // 修改支付状态
        OrderMaster orderMaster = new OrderMaster();
        orderDTO.setPayStatus(PayStatusEnum.SUCCESS.getCode());
        BeanUtils.copyProperties(orderDTO, orderMaster);
        OrderMaster updateResult = orderMasterRepository.save(orderMaster);
        if (updateResult == null) {
            throw new SellException(ResultEnum.ORDER_UPDATE_FAIL);
        }
        return orderDTO;
    }
}
