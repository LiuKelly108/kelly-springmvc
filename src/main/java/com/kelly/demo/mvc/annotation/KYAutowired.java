package com.kelly.demo.mvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.FIELD}) //成员变量的注解类型
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KYAutowired  {

    String value() default "" ; //默认是""
}
