package com.zhangzc.sharethinguserimpl.controller;

import com.zhangzc.globalcontextspringbootstart.context.GlobalContext;
import com.zhangzc.sharethingscommon.utils.R;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserForumDTO;
import com.zhangzc.sharethinguserimpl.pojo.vo.UserRightsDTO;
import com.zhangzc.sharethinguserimpl.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/bbs/user/")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("getCurrentUserRights")
    public com.zhangzc.sharethingscommon.utils.R<UserRightsDTO> getCurrentUserRights() {
        UserRightsDTO result = userService.getCurrentUserRights();
        return R.ok(result);
    }


    @GetMapping("getUserInfo")
    public R<UserForumDTO> getUserInfo(@RequestParam String userId) {
        UserForumDTO result = userService.getUserInfo(userId);
        return R.ok(result);
    }


}
