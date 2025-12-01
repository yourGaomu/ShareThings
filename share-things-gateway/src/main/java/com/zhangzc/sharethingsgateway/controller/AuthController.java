package com.zhangzc.sharethingsgateway.controller;






import cn.dev33.satoken.stp.StpUtil;
import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.leaf.server.service.SegmentService;
import com.zhangzc.redisspringbootstart.utills.LuaUtil;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethingsgateway.Enum.ResponseCodeEnum;
import com.zhangzc.sharethingsgateway.pojo.domain.FsUserInfo;
import com.zhangzc.sharethingsgateway.pojo.req.AuthCodeVo;
import com.zhangzc.sharethingsgateway.pojo.req.AuthLoginVo;
import com.zhangzc.sharethingsgateway.service.FsUserInfoService;
import com.zhangzc.smsspringbootstart.enums.redisHashKey;
import com.zhangzc.smsspringbootstart.enums.smsTemplateEum;
import com.zhangzc.smsspringbootstart.utills.SmsUtil;
import com.zhangzc.smsspringbootstart.utills.VerificationCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final SmsUtil smsUtil;
    private final RedisUtil redisUtil;
    private final FsUserInfoService fsUserInfoService;
    private final LuaUtil luaUtil;
    private final TransactionTemplate transactionTemplate;
    private final SegmentService segmentService;


    @PostMapping("/send-code")
    public R auth(@RequestBody AuthCodeVo authCodeVo) {
        String phone = authCodeVo.getPhone();
        if (phone == null || phone.length() != 11) {
            throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
        }
        //检查是否过期
        List<Object> data = List.of(phone);
        Object chickAuthCodeExeit = luaUtil.execute("chick_auth_code_exeit", redisHashKey.VerificationCode, data);
        if (!Objects.isNull(chickAuthCodeExeit)) {
            throw new BusinessException(ResponseCodeEnum.AUTH_CODE_EXIT);
        }
        Map<String,String> code = new HashMap<>();
        //生成随机的验证码
        String code1 = VerificationCodeUtils.generateAlphanumericCode(6);
        code.put("code", code1);
        code.put("min","5");
        String jsonString = JsonUtils.toJsonString(code);
        smsUtil.sendSms("速通互联验证平台", smsTemplateEum.LoginOrRegister,phone,jsonString);
        return R.ok("发送成功");
    }


    @PostMapping("/phone-login")
    public R login(@RequestBody AuthLoginVo authLoginVo) {
        if (authLoginVo.getPhone() == null || authLoginVo.getPhone().length() != 11) {
            throw new BusinessException(ResponseCodeEnum.LOGIN_PRAM_LOSS);
        }
        String phone = authLoginVo.getPhone();
        String code = authLoginVo.getCode();
        //从redis里面查询
        //检查是否过期
        List<Object> data = List.of(phone);
        Object chickAuthCodeExeit = luaUtil.execute("chick_auth_code_exeit", redisHashKey.VerificationCode, data);
        if (Objects.isNull(chickAuthCodeExeit)) {
            throw new BusinessException(ResponseCodeEnum.LOGIN_CODE_LOSS);
        }
        String code_new = chickAuthCodeExeit.toString();
        if (!code.equals(code_new)) {
            //验证码错误
            throw new BusinessException(ResponseCodeEnum.LOGIN_CODE_ERROR);
        }
        //查询用户
        FsUserInfo one = fsUserInfoService.lambdaQuery().eq(FsUserInfo::getPhone, phone).one();
        if (Objects.isNull(one)) {
            Boolean execute = transactionTemplate.execute(status -> {
                //构建初始化对象
                try{
                    FsUserInfo fsUserInfo = new FsUserInfo();
                    fsUserInfo.setPhone(phone);
                    fsUserInfo.setNickname("share_new_user");
                    fsUserInfo.setAvatar("http://119.45.4.154:9090/api/v1/buckets/images/objects/download?preview=true&prefix=MjAyNS0xMS00L3BzYy5qcGc=&version_id=null");
                    fsUserInfo.setSex(0);
                    fsUserInfo.setStatus(0);
                    fsUserInfo.setIntroduction("新人报道");
                    fsUserInfo.setCreate_time(new Date());
                    fsUserInfo.setUpdate_time(new Date());
                    fsUserInfo.setIs_deleted(false);
                    String userid = String.valueOf(segmentService.getId("user").getId());
                    fsUserInfo.setUser_id(userid);
                    fsUserInfoService.save(fsUserInfo);
                    one.setId(Long.valueOf(userid));
                    return true;
                }catch (Exception e){
                    status.setRollbackOnly();
                    return false;
                }
            });
            if (Boolean.FALSE.equals(execute)){
                throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
            }
        }
        StpUtil.login(one.getId());
        String tokenValue = StpUtil.getTokenValue();
        Map<String,String> map = new HashMap<>();
        map.put("token", tokenValue);
        return R.ok("登录成功",map);
    }
}
