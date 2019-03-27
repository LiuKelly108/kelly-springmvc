package com.kelly.demo.mvc.service;

import com.kelly.demo.mvc.annotation.KYRequestMapping;
import com.kelly.demo.mvc.annotation.KYService;

/**
 * 业务层
 */
@KYService("demoService")
public class DemoService implements IDemoService {

    public String get(String name) {
        String str = "my name is "+name ;
        return str;
    }

    @Override
    public String add(Integer a, Integer b) {
        Integer c = a+ b ;
        return a+"+"+b+"="+c;
    }
}
