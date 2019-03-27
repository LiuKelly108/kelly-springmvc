package com.kelly.demo.mvc.v3;

import com.kelly.demo.mvc.annotation.KYRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class Handler {

    private Pattern pattern; //url的正则
    private Object controller;
    private Method method;
    protected Map<String,Integer> paramIndexMapping; //参数的顺序

    public Handler(Pattern pattern, Object controller, Method method) {
        this.pattern = pattern;
        this.controller = controller;
        this.method = method;

        //根据方法的参数设置参数的顺序
        paramIndexMapping = new HashMap<>();
        putParamIndexMapping(method);
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Object getController() {
        return controller;
    }

    public Method getMethod() {
        return method;
    }

    public Map<String, Integer> getParamIndexMapping() {
        return paramIndexMapping;
    }

    private void putParamIndexMapping(Method method){

        //提取方法中加了注解的参数
        Annotation[] [] pa = method.getParameterAnnotations();
        for (int i = 0; i < pa.length ; i ++) {
            for(Annotation a : pa[i]){
                if(a instanceof KYRequestParam){
                    String paramName = ((KYRequestParam) a).value();
                    if(!"".equals(paramName.trim())){
                        paramIndexMapping.put(paramName, i);
                    }
                }
            }
        }

        //提取方法中的request和response参数
        Class<?> [] paramsTypes = method.getParameterTypes();
        for (int i = 0; i < paramsTypes.length ; i ++) {
            Class<?> type = paramsTypes[i];
            if(type == HttpServletRequest.class ||
                    type == HttpServletResponse.class){
                paramIndexMapping.put(type.getName(),i);
            }
        }
    }
}


