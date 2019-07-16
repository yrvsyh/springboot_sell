package com.linghang.service.impl;

import com.linghang.dto.OrderDTO;
import com.linghang.service.PayService;
import com.lly835.bestpay.model.PayRequest;
import com.lly835.bestpay.service.impl.BestPayServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PayServiceImpl implements PayService {

    @Autowired
    private BestPayServiceImpl bestPayService;

    @Override
    public void create(OrderDTO orderDTO) {

        //TODO
        PayRequest payRequest = new PayRequest();
        bestPayService.pay(payRequest);
    }
}
