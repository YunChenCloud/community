package com.nowcoder.community.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD) //该注解运用在方法上
@Retention(RetentionPolicy.RUNTIME) //程序运行时有效
public @interface LoginRequired {
}
