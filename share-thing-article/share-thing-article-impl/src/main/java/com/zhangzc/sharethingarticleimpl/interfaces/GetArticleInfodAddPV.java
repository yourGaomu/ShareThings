package com.zhangzc.sharethingarticleimpl.interfaces;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD}) // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时保留，允许AOP反射获取
public @interface GetArticleInfodAddPV {
    String value() default "";
}
