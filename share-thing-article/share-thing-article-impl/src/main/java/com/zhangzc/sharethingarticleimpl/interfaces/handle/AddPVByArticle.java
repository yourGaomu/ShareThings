package com.zhangzc.sharethingarticleimpl.interfaces.handle;

import com.zhangzc.kafkaspringbootstart.utills.KafkaUtills;
import com.zhangzc.sharethingarticleimpl.consts.KafKaConst;
import com.zhangzc.sharethingarticleimpl.interfaces.GetArticleInfodAddPV;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
public class AddPVByArticle {
    private final KafkaUtills kafkaUtills;

    @Pointcut("@annotation(com.zhangzc.sharethingarticleimpl.interfaces.GetArticleInfodAddPV)")
    public void pointcut() {
    }


    @Around("pointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取注解的值
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        // 获取方法上的注解实例
        GetArticleInfodAddPV annotation = method.getAnnotation(GetArticleInfodAddPV.class);
        // 获取注解的value值
        String annotationValue = annotation.value();
        //获取入参的值
        //getValue(joinPoint);
        kafkaUtills.sendMessage(KafKaConst.ADD_PV_TOPIC, annotationValue);
        return joinPoint.proceed();
    }


    private void getValue(ProceedingJoinPoint joinPoint) {
        // ========== 步骤2：获取方法入参（按需处理） ==========
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                // 示例：如果入参是文章ID，可在此处获取并处理PV统计
                if (args[i] instanceof Long) {
                    Long articleId = (Long) args[i];
                    System.out.println("处理文章PV统计，文章ID：" + articleId);
                    // TODO: 执行PV+1的业务逻辑（比如更新数据库/缓存）
                }
            }
        }
    }


}
