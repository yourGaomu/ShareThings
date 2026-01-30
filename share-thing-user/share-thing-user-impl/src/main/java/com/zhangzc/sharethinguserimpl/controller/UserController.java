package com.zhangzc.sharethinguserimpl.controller;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.globalcontextspringbootstart.utils.EncodeUtil;
import com.zhangzc.sharethingscommon.enums.ResponseCodeEnum;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserIdRequest;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserRightsDTO;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserSearchDTO;
import com.zhangzc.sharethinguserimpl.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/bbs/user/")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    private final UserService userService;
    private final EncodeUtil encodeUtil;

    @PostMapping("getCurrentUserRights")
    public com.zhangzc.sharethingscommon.utils.R<UserRightsDTO> getCurrentUserRights() {
        UserRightsDTO result = userService.getCurrentUserRights();
        return R.ok(result);
    }

    @PostMapping("/updateLikeState")
    public R<String> likeArticle(@RequestBody Map<String,String> articleId) {
        Object o = GlobalContext.get();
        if (o == null) {
            throw new BusinessException(ResponseCodeEnum.USER_NOT_FOUND);
        }
        String userId = (String) o;
        userService.likeArticle(articleId.get("articleId"),userId);
        return R.ok("用户点赞文章成功");
    }

    @PostMapping("getHotAuthorsList")
    public R<PageResponse<UserForumDTO>> getHotAuthorsList(UserSearchDTO userSearchDTO) {
        PageResponse<UserForumDTO> result = userService.getHotAuthorsList(userSearchDTO);
        return R.ok(result);
    }


    @PostMapping("getUserInfo")
    public R<UserForumDTO> getUserInfo(@RequestBody UserIdRequest userIdRequest) {
        // 检查请求参数
        if (userIdRequest == null || userIdRequest.getUserId() == null || userIdRequest.getUserId().isEmpty()) {
            throw new IllegalArgumentException("加密的userId不能为空");
        }

        String userid;
        try {
            userid = encodeUtil.decryptRaw(userIdRequest.getUserId());
        } catch (Exception e) {
            log.info("携带的是未加密的用户Id");
            userid = userIdRequest.getUserId();
        }

        if (userid == null) {
            throw new RuntimeException("用户不存在或userId已过期");
        }

        UserForumDTO result = userService.getUserInfo(userid);
        return R.ok(result);
    }

    @PostMapping("/getUserActive")
    public R<Boolean> getUserActive(@RequestBody UserIdRequest userIdRequest) {
        return R.ok(true);
    }
}
