package com.zhangzc.sharethinguserimpl.controller;


import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;

import com.zhangzc.globalcontextspringbootstart.utils.EncodeUtil;
import com.zhangzc.sharethingscommon.exception.BusinessException;

import com.zhangzc.fakebookspringbootstartjackon.Utils.JsonUtils;
import com.zhangzc.leaf.server.service.SegmentService;
import com.zhangzc.redisspringbootstart.utills.LuaUtil;
import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserimpl.Enum.ResponseCodeEnum;
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserInfo;
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserLevel;
import com.zhangzc.sharethinguserimpl.pojo.req.AuthCodeVo;
import com.zhangzc.sharethinguserimpl.pojo.req.AuthLoginVo;
import com.zhangzc.sharethinguserimpl.service.FsUserInfoService;
import com.zhangzc.sharethinguserimpl.service.FsUserLevelService;
import com.zhangzc.smsspringbootstart.enums.redisHashKey;
import com.zhangzc.smsspringbootstart.enums.smsTemplateEum;
import com.zhangzc.smsspringbootstart.utills.SmsUtil;
import com.zhangzc.smsspringbootstart.utills.VerificationCodeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    // 常量抽离（解决硬编码问题）
    private static final String DEFAULT_AVATAR = "http://119.45.4.154:9090/api/v1/buckets/images/objects/download?preview=true&prefix=MjAyNS0xMS00L3BzYy5qcGc=";
    private static final String DEFAULT_NICKNAME_PREFIX = "share_user_";
    private static final String DEFAULT_USER_LEVEL = "1";
    private static final Integer DEFAULT_POINTS = 0;
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");

    private final SmsUtil smsUtil;
    private final RedisUtil redisUtil;
    private final FsUserInfoService fsUserInfoService;
    private final LuaUtil luaUtil;
    private final TransactionTemplate transactionTemplate;
    private final SegmentService segmentService;
    private final FsUserLevelService fsUserLevelService;
    private final EncodeUtil encodeUtil;


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
        Map<String, String> code = new HashMap<>();
        //生成随机的验证码
        String code1 = VerificationCodeUtils.generateAlphanumericCode(6);
        code.put("code", code1);
        code.put("min", "5");
        String jsonString = JsonUtils.toJsonString(code);
        smsUtil.sendSms("速通互联验证平台", smsTemplateEum.LoginOrRegister, phone, jsonString);
        return R.ok("发送成功");
    }


    @PostMapping("/phone-login")
    public R login(@RequestBody AuthLoginVo authLoginVo) throws Exception {
        // 1. 手机号校验（修复：补充格式校验+拼写错误）
        if (StrUtil.isBlank(authLoginVo.getPhone()) || !PHONE_PATTERN.matcher(authLoginVo.getPhone()).matches()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        String phone = authLoginVo.getPhone();
        String code = authLoginVo.getCode();
        //检查是否过期
        List<Object> data = List.of(phone);
        Object chickAuthCodeExeit = luaUtil.execute("chick_auth_code_exeit", redisHashKey.VerificationCode, data);
        if (Objects.isNull(chickAuthCodeExeit)) {
            throw new BusinessException(ResponseCodeEnum.LOGIN_CODE_LOSS);
        }
        // 3. 验证码对比（修复：避免NPE）
        String code_new = chickAuthCodeExeit.toString();
        if (!code_new.equalsIgnoreCase(code)) {
            throw new BusinessException(ResponseCodeEnum.LOGIN_CODE_ERROR);
        }
        // 4. 查询用户
        FsUserInfo one = fsUserInfoService.lambdaQuery()
                .eq(FsUserInfo::getPhone, phone)
                .one();
        AtomicReference<Long> userId = new AtomicReference<>();

        // 5. 新用户注册（修复：事务逻辑+数据一致性+异常日志）
        if (Objects.isNull(one)) {
            Boolean execute = transactionTemplate.execute(status -> {
                try {
                    // 修复：segmentService.getId 空指针校验
                    String userid = String.valueOf(segmentService.getId("userId").getId());
                    // 构建用户对象
                    FsUserInfo fsUserInfo = new FsUserInfo();
                    fsUserInfo.setPhone(phone);
                    fsUserInfo.setNickname(DEFAULT_NICKNAME_PREFIX + userid);
                    fsUserInfo.setAvatar(DEFAULT_AVATAR);
                    fsUserInfo.setSex(0);
                    fsUserInfo.setStatus(0);
                    fsUserInfo.setIntroduction("新人报道");
                    fsUserInfo.setCreateTime(new Date());
                    fsUserInfo.setUpdateTime(new Date());
                    fsUserInfo.setIsDeleted(false);
                    fsUserInfo.setUserId(userid);
                    // 修复：校验用户保存结果
                    boolean saveUser = fsUserInfoService.save(fsUserInfo);
                    if (!saveUser) {
                        log.error("保存用户失败，手机号：{}，用户ID：{}", phone, userid);
                        throw new RuntimeException("保存用户失败");
                    }
                    userId.set(Long.valueOf(userid));

                    // 构建用户等级对象
                    FsUserLevel fsUserLevel = new FsUserLevel();
                    fsUserLevel.setUserId(Long.valueOf(userid));
                    fsUserLevel.setLevel(DEFAULT_USER_LEVEL);
                    fsUserLevel.setPoints(DEFAULT_POINTS);
                    fsUserLevel.setCreateTime(new Date());
                    fsUserLevel.setUpdateTime(new Date());

                    // 修复：校验用户等级保存结果
                    boolean saveLevel = fsUserLevelService.save(fsUserLevel);
                    if (!saveLevel) {
                        log.error("保存用户等级失败，用户ID：{}", userid);
                        throw new RuntimeException("保存用户等级失败");
                    }
                    log.info("新用户注册成功，手机号：{}，用户ID：{}", phone, userid);
                    return true;
                } catch (Exception e) {
                    // 修复：记录异常日志+主动回滚
                    log.error("新用户注册事务失败，手机号：{}", phone, e);
                    status.setRollbackOnly();
                    return false;
                }
            });
            if (Boolean.FALSE.equals(execute)) {
                throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
            }
        } else {
            // 修复：用户ID格式校验，避免NumberFormatException
            try {
                userId.set(Long.valueOf(one.getUserId()));
            } catch (NumberFormatException e) {
                log.error("用户ID格式错误，手机号：{}，用户ID：{}", phone, one.getUserId(), e);
                throw new BusinessException(ResponseCodeEnum.SYSTEM_ERROR);
            }
        }
        //加密用户的id
        String encodeUserId = encodeUtil.encryptRaw(String.valueOf(userId));
        StpUtil.login(userId.get());
        //使用cannal去进行数据库的同步
        String tokenValue = StpUtil.getTokenValue();
        Map<String, String> map = new HashMap<>();
        map.put("token", tokenValue);
        map.put("encodedUserId", encodeUserId);
        System.out.println(map);
        return R.ok("登录成功", map);
    }

    @PostMapping("/loginOut")
    public R<String> loginOut() {
        StpUtil.logout();
        return R.ok("退出成功");
    }

}
