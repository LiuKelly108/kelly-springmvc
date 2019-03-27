package com.kelly.demo.mvc.annotation;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface KYRequestParam {
    String value() default "";
}
