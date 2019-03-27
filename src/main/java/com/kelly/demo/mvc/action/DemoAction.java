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
 * 请求入口
 */
@KYController
@KYRequestMapping("/demo")
public class DemoAction {

    @KYAutowired("demoService") private IDemoService  demoService ;

    /**
     * 查询方法
     * @param req
     * @param res
     * @param name
     */
    @KYRequestMapping("/query")
    public  void  query(HttpServletRequest req, HttpServletResponse res,
                       @KYRequestParam(value="name")String name){

        String result =  demoService.get(name);

        try {
            res.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @KYRequestMapping("/add")
    public  void  add(HttpServletResponse req,HttpServletResponse res,
                      @KYRequestParam("a") Integer a, @KYRequestParam("b") Integer b){
       String result = demoService.add(a,b);
       try{
           res.getWriter().write(result);
       }catch (Exception e){
           e.printStackTrace();
       }
    }
}
