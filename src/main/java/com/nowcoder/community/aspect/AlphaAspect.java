package com.nowcoder.community.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

// @Component
// @Aspect
public class AlphaAspect {

    //括号里的表达式 描述哪些bean、哪些方法是要处理的目标
    //              *代表方法的返回值 com...service.*表示包下的所有类 *表示所有方法 (..)表示所有参数
    @Pointcut("execution(* com.nowcoder.community.service.*.*(..))")
    public void pointcut() {

    }

    //方法名随便取
    @Before("pointcut()")
    public void before() {
        System.out.println("before");
    }

    @After("pointcut()")
    public void after() {
        System.out.println("after");
    }

    //返回值以后
    @AfterReturning("pointcut()")
    public void afterReturning() {
        System.out.println("afterReturning");
    }

    //抛异常以后
    @AfterThrowing("pointcut()")
    public void afterThrowing() {
        System.out.println("afterThrowing");
    }

    //前后都织入
    //参数表示织入的部位
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("around before");
        Object obj = joinPoint.proceed(); //调用目标组件的方法
        System.out.println("around after");
        return obj;
    }

}
