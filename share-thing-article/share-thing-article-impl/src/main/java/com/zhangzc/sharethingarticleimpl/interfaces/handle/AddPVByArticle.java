package com.zhangzc.sharethingarticleimpl.interfaces.handle;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.kafkaspringbootstart.utills.KafkaUtills;
import com.zhangzc.sharethingarticleimpl.consts.KafKaConst;
import com.zhangzc.sharethingscommon.enums.UserActionEnum;
import com.zhangzc.sharethingarticleimpl.interfaces.GetArticleInfodAddPV;
import com.zhangzc.sharethingarticleimpl.pojo.req.GetArticleInfoVo;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

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
        //String annotationValue = annotation.value();
        //获取入参的值
        Long annotationValue = getValue(joinPoint);
        if (annotationValue == null) {
            return joinPoint.proceed();
        }
        //发送消息PV+1
        kafkaUtills.sendMessage(KafKaConst.ADD_PV_TOPIC, annotationValue);
        //发送用户行为记录
        Map<String,Object> map = new HashMap<>();
        //那个用户发生了什么行为
        map.put(UserActionEnum.ARTICLE_READ.getActionName(),GlobalContext.get());
        kafkaUtills.sendMessage(KafKaConst.USER_BEHAVIOR_TOPIC, map);
        return joinPoint.proceed();
    }


    private Long getValue(ProceedingJoinPoint joinPoint) {
        // ========== 步骤2：获取方法入参（按需处理） ==========
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof GetArticleInfoVo) {
                    GetArticleInfoVo getArticleInfoVo = (GetArticleInfoVo) args[i];
                    System.out.println("处理文章PV统计，文章ID：" + getArticleInfoVo.getId());
                    return Long.valueOf(getArticleInfoVo.getId());
                }
            }
        }
        return null;
    }


}
