package com.zhangzc.sharethingscommon.ExceptionHandle;


import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;


@RestControllerAdvice
@Slf4j
public class SaTokenExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public R businessException(BusinessException e) {
        String msg = e.getData() != null ? String.valueOf(e.getData()) : e.getMessage();
        log.error(msg);
        return R.error(e.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public R exception(Exception e) {
        log.error(e.getMessage());
        return R.error(String.valueOf(e.getMessage()), e.getMessage());
    }
}