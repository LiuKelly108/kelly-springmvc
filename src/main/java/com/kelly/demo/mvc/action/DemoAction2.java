package com.kelly.demo.mvc.action;


import com.kelly.demo.mvc.annotation.KYAutowired;
import com.kelly.demo.mvc.annotation.KYController;
import com.kelly.demo.mvc.annotation.KYRequestMapping;
import com.kelly.demo.mvc.annotation.KYRequestParam;
import com.kelly.demo.mvc.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 *action
 */
@KYController
@KYRequestMapping("/demo2")
public class DemoAction2 {

    @KYAutowired("demoService") private IDemoService  demoService ;

    /**
     * 查询方法
     * @param req
     * @param res
     * @param name
     */
    @KYRequestMapping("/query")
    public  String  query(HttpServletRequest req, HttpServletResponse res,
                        @KYRequestParam(value="name")String name){

        return demoService.get(name);
    }

    @KYRequestMapping("/add")
    public  String  add(HttpServletRequest req,HttpServletResponse res,
                      @KYRequestParam("a") Integer a, @KYRequestParam("b") Integer b){
       return demoService.add(a,b);
    }
}
