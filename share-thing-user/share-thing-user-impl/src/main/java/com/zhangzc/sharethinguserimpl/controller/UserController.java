package com.zhangzc.sharethinguserimpl.controller;

import com.zhangzc.globalcontextspringbootstart.utils.EncodeUtil;
import com.zhangzc.sharethingscommon.utils.PageResponse;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserIdRequest;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserRightsDTO;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserSearchDTO;
import com.zhangzc.sharethinguserimpl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bbs/user/")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final EncodeUtil encodeUtil;

    @PostMapping("getCurrentUserRights")
    public com.zhangzc.sharethingscommon.utils.R<UserRightsDTO> getCurrentUserRights() {
        UserRightsDTO result = userService.getCurrentUserRights();
        return R.ok(result);
    }


    @PostMapping("getHotAuthorsList")
    public R<PageResponse<UserForumDTO>> getHotAuthorsList(UserSearchDTO userSearchDTO) {
        PageResponse<UserForumDTO> result =   userService.getHotAuthorsList(userSearchDTO);
        return R.ok(result);
    }


    @PostMapping("getUserInfo")
    public R<UserForumDTO> getUserInfo(@RequestBody UserIdRequest userIdRequest) throws Exception {
        // 检查请求参数
        if (userIdRequest == null || userIdRequest.getUserId() == null || userIdRequest.getUserId().isEmpty()) {
            throw new IllegalArgumentException("加密的userId不能为空");
        }

        String userid = encodeUtil.decryptRaw(userIdRequest.getUserId());
        if (userid == null) {
            throw new RuntimeException("用户不存在或userId已过期");
        }
        
        UserForumDTO result = userService.getUserInfo(userid);
        return R.ok(result);
    }


}
