package com.zhangzc.sharethingsgateway.ExceptionHandle;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.SaTokenException;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.utils.R;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Component
@RestControllerAdvice
@Slf4j
public class SaTokenExceptionHandler {

    @ExceptionHandler(SaTokenException.class)
    public R getSaTokenException(SaTokenException e) {
        log.error(e.getMessage());
        return R.error(String.valueOf(e.getCode()), (String) e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public R businessException(BusinessException e) {
        log.error((String) e.getData());
        return R.error(e.getCode(), (String) e.getData());
    }

    @ExceptionHandler(Exception.class)
    public R exception(Exception e) {
        log.error(e.getMessage());
        return R.error(String.valueOf(e.getMessage()), e.getMessage());
    }
}