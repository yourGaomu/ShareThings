package com.zhangzc.sharethinguserimpl.service.rpc;

import com.zhangzc.redisspringbootstart.utills.RedisUtil;
import com.zhangzc.sharethinguserapi.enums.RedisUserBuild;
import com.zhangzc.sharethinguserapi.pojo.dto.FsUserInfoDto;
import com.zhangzc.sharethinguserapi.rpc.userInfoSerach;
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserInfo;
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserLevel;
import com.zhangzc.sharethinguserimpl.service.FsUserInfoService;
import com.zhangzc.sharethinguserimpl.service.FsUserLevelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

@DubboService
@RequiredArgsConstructor
@Slf4j
public class userInfoRpc implements userInfoSerach {
    private final FsUserInfoService fsUserInfoService;
    private final FsUserLevelService fsUserLevelService;
    private final RedisUtil redisUtil;
    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Override
    public Map<String, FsUserInfoDto> getUserInfoByUserId(List<String> userId) {
        // 1. 参数校验
        if (userId == null || userId.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            // 2. 初始化
            List<String> needUserIDs2Info = new ArrayList<>();
            List<String> needUserIDs2Level = new ArrayList<>();
            // 3. 从Redis查询
            Map<String, FsUserInfo> userIDs2Info = getUserIDs2Info(userId, needUserIDs2Info);
            Map<String, FsUserLevel> userIDs2Level = getUserIDs2Level(userId, needUserIDs2Level);
            // 4. 异步查询数据库 (使用自定义线程池)
            CompletableFuture<List<FsUserInfo>> infoFuture = CompletableFuture.supplyAsync(() -> {
                if (needUserIDs2Info.isEmpty()) {
                    return Collections.emptyList();
                }
                List<FsUserInfo> list = fsUserInfoService.lambdaQuery()
                        .in(FsUserInfo::getUserId, needUserIDs2Info).list();
                // 保存到Redis
                if (!list.isEmpty()) {
                    Map<String, Object> collect = list.stream()
                            .collect(Collectors.toMap(FsUserInfo::getUserId, Function.identity()));
                    redisUtil.hmset(RedisUserBuild.userInfo, collect);
                }
                return list;
            }, threadPoolTaskExecutor);

            CompletableFuture<List<FsUserLevel>> levelFuture = CompletableFuture.supplyAsync(() -> {
                if (needUserIDs2Level.isEmpty()) {
                    return Collections.emptyList();
                }
                List<FsUserLevel> list = fsUserLevelService.lambdaQuery()
                        .in(FsUserLevel::getUserId, needUserIDs2Level).list();
                // 保存到Redis
                if (!list.isEmpty()) {
                    Map<String, Object> collect = list.stream()
                            .collect(Collectors.toMap(
                                    l -> String.valueOf(l.getUserId()),
                                    Function.identity()
                            ));
                    redisUtil.hmset(RedisUserBuild.userLevel, collect);
                }
                return list;
            }, threadPoolTaskExecutor);
            // 5. 等待异步任务完成
            CompletableFuture.allOf(infoFuture, levelFuture).join();
            // 6. 合并结果
            List<FsUserInfo> fetchedInfos = infoFuture.join();
            List<FsUserLevel> fetchedLevels = levelFuture.join();

            if (!fetchedInfos.isEmpty()) {
                Map<String, FsUserInfo> fetchedInfoMap = fetchedInfos.stream()
                        .collect(Collectors.toMap(FsUserInfo::getUserId, Function.identity(), (a, b) -> a));
                userIDs2Info.putAll(fetchedInfoMap);
            }

            if (!fetchedLevels.isEmpty()) {
                Map<String, FsUserLevel> fetchedLevelMap = fetchedLevels.stream()
                        .collect(Collectors.toMap(
                                l -> String.valueOf(l.getUserId()),
                                Function.identity(),
                                (a, b) -> a
                        ));
                userIDs2Level.putAll(fetchedLevelMap);
            }

            // 7. 构建DTO
            Map<String, FsUserInfoDto> result = new HashMap<>();
            for (String id : userId) {
                FsUserInfo info = userIDs2Info.get(id);
                FsUserLevel level = userIDs2Level.get(id);

                // 只有当至少有一个数据源有值时才创建DTO
                if (info != null || level != null) {
                    FsUserInfoDto dto = new FsUserInfoDto();
                    if (info != null) {
                        BeanUtils.copyProperties(info, dto);
                    }
                    if (level != null) {
                        BeanUtils.copyProperties(level, dto);
                    }
                    result.put(id, dto);
                }
            }

            return result;

        } catch (Exception e) {
            log.error("查询用户信息失败, userIds: {}", userId, e);
            // 降级: 直接查数据库
            return getUserInfoFromDB(userId);
        }
    }

    private Map<String, FsUserInfoDto> getUserInfoFromDB(List<String> userId) {
        Map<String, FsUserInfoDto> result = new HashMap<>();
        //从数据库里面直接查询
        CompletableFuture<Map<String, FsUserInfo>> mapCompletableFuture1 = CompletableFuture.supplyAsync(() -> fsUserInfoService.lambdaQuery()
                        .in(FsUserInfo::getUserId, userId)
                        .list().stream().collect(Collectors.toMap(FsUserInfo::getUserId, Function.identity()))
                , threadPoolTaskExecutor);

        CompletableFuture<Map<Long, FsUserLevel>> mapCompletableFuture = CompletableFuture.supplyAsync(() -> fsUserLevelService.lambdaQuery()
                        .in(FsUserLevel::getUserId, userId).list()
                        .stream().collect(Collectors.toMap(FsUserLevel::getUserId, Function.identity()))
                , threadPoolTaskExecutor);

        CompletableFuture.allOf(mapCompletableFuture1, mapCompletableFuture).join();
        //获取结果
        Map<String, FsUserInfo> stringFsUserInfoMap = mapCompletableFuture1.join();
        Map<Long, FsUserLevel> longFsUserLevelMap = mapCompletableFuture.join();
        //合并结果
        stringFsUserInfoMap.forEach((k, v) -> {
            FsUserInfoDto dto = new FsUserInfoDto();
            BeanUtils.copyProperties(v, dto);
            FsUserLevel fsUserLevel = longFsUserLevelMap.get(Long.parseLong(k));
            if (fsUserLevel != null) {
                //不要赋值id
                dto.setLevel(fsUserLevel.getLevel());
                dto.setPoints(fsUserLevel.getPoints());
            }
            result.put(k, dto);
        });
        //放入Redis
        CompletableFuture.runAsync(() -> {
            Map<String, Object> userInfoMap = new HashMap<>(stringFsUserInfoMap);
            Map<String, Object> userLevelMap = new HashMap<>();
            longFsUserLevelMap.forEach((k, v) -> userLevelMap.put(k.toString(), v));
            redisUtil.hmset(RedisUserBuild.userInfo, userInfoMap);
            redisUtil.hmset(RedisUserBuild.userLevel, userLevelMap);
        });
        return result;
    }

    // 优化后的辅助方法
    private Map<String, FsUserLevel> getUserIDs2Level(List<String> userId, List<String> needUserIDs2Level) {
        Map<String, FsUserLevel> userLevel = new HashMap<>();
        Map<Object, Object> hmget = redisUtil.hmget(RedisUserBuild.userLevel);

        userId.forEach(id -> {
            if (!hmget.containsKey(id)) {
                needUserIDs2Level.add(id);
            } else {
                Object obj = hmget.get(id);
                // 类型检查
                if (obj instanceof FsUserLevel) {
                    userLevel.put(id, (FsUserLevel) obj);
                } else {
                    // Redis数据异常，重新查询
                    needUserIDs2Level.add(id);
                }
            }
        });
        return userLevel;
    }

    private Map<String, FsUserInfo> getUserIDs2Info(List<String> userId, List<String> needUserIDs2Info) {
        Map<String, FsUserInfo> userInfo = new HashMap<>();
        Map<Object, Object> target = redisUtil.hmget(RedisUserBuild.userInfo);

        userId.forEach(id -> {
            if (!target.containsKey(id)) {
                needUserIDs2Info.add(id);
            } else {
                Object obj = target.get(id);
                // 类型检查
                if (obj instanceof FsUserInfo) {
                    userInfo.put(id, (FsUserInfo) obj);
                } else {
                    // Redis数据异常，重新查询
                    needUserIDs2Info.add(id);
                }
            }
        });
        return userInfo;
    }





    /*
    @Override
    public Map<String, FsUserInfoDto> getUserInfoByUserId(List<String> userId) {
        //从redis里面查询
        List<String> needUserIDs2Info = new ArrayList<>();
        List<String> needUserIDs2Level = new ArrayList<>();
        Map<String, FsUserInfo> userIDs2Info = getUserIDs2Info(userId, needUserIDs2Info, needUserIDs2Level);
        Map<String, FsUserLevel> userIDs2Level = getUserIDs2Level(userId, needUserIDs2Info, needUserIDs2Level);
        CompletableFuture<List<FsUserInfo>> listCompletableUserInfoFuture = CompletableFuture.supplyAsync(() -> {
            //去查询数据
            if (needUserIDs2Info.isEmpty()) {
                return null;
            }
            List<FsUserInfo> list = fsUserInfoService.lambdaQuery().in(FsUserInfo::getUserId, needUserIDs2Info).list();
            //保存到redis里面
            Map<String, Object> collect = list.stream()
                    .collect(Collectors.toMap(FsUserInfo::getUserId, Function.identity()));
            redisUtil.hmset(RedisUserBuild.userInfo, collect);
            return list;
        });

        CompletableFuture<List<FsUserLevel>> listCompletableUserLevelFuture = CompletableFuture.supplyAsync(() -> {
            if (needUserIDs2Level.isEmpty()) {
                return null;
            }
            //去查询数据
            List<FsUserLevel> list = fsUserLevelService.lambdaQuery().in(FsUserLevel::getUserId, needUserIDs2Level).list();
            //保存到redis里面
            Map<Long, Object> collect = list.stream()
                    .collect(Collectors.toMap(FsUserLevel::getUserId, Function.identity()));
            Map<String, Object> redisneed = new HashMap<>();
            collect.forEach((k, v) -> {
                String string = k.toString();
                redisneed.put(string, v);
            });
            redisUtil.hmset(RedisUserBuild.userLevel, redisneed);
            return list;
        });
        CompletableFuture.allOf(listCompletableUserInfoFuture, listCompletableUserLevelFuture).join();
        // 安全获取结果（避免 null）
        List<FsUserInfo> fetchedInfos = Optional.ofNullable(listCompletableUserInfoFuture.join()).orElse(Collections.emptyList());
        List<FsUserLevel> fetchedLevels = Optional.ofNullable(listCompletableUserLevelFuture.join()).orElse(Collections.emptyList());
        // 将远程查询到的数据转换为以 String user_id 为键的 Map
        // 合并到从 Redis 读取到的 map（覆盖已有键）
        if (!fetchedLevels.isEmpty()) {
            Map<String, FsUserLevel> fetchedLevelMap = fetchedLevels.stream()
                    .collect(Collectors.toMap(l -> String.valueOf(l.getUserId()), Function.identity(), (a, b) -> a));

            userIDs2Level.putAll(fetchedLevelMap);
        }
        if (!needUserIDs2Info.isEmpty()) {
            Map<String, FsUserInfo> fetchedInfoMap = fetchedInfos.stream()
                    .collect(Collectors.toMap(FsUserInfo::getUserId, Function.identity(), (a, b) -> a));
            userIDs2Info.putAll(fetchedInfoMap);
        }
        // 根据入参 userId 列表构建返回的 DTO 映射（保持原顺序/存在性）
        Map<String, FsUserInfoDto> result = new HashMap<>();
        for (String id : userId) {
            FsUserInfoDto dto = new FsUserInfoDto();
            FsUserInfo info = userIDs2Info.get(id);
            if (info != null) {
                BeanUtils.copyProperties(info, dto);
            }
            FsUserLevel level = userIDs2Level.get(id);
            if (level != null) {
                BeanUtils.copyProperties(level, dto);
            }
            result.put(id, dto);
        }

        return result;
    }

    private Map<String, FsUserLevel> getUserIDs2Level(List<String> userId, List<String> needUserIDs2Info, List<String> needUserIDs2Level) {
        Map<String, FsUserLevel> userLevel = new HashMap<>();
        Map<Object, Object> hmget = redisUtil.hmget(RedisUserBuild.userLevel);
        userId.forEach(id -> {
            if (!hmget.containsKey(id)) {
                needUserIDs2Level.add(id);
            } else {
                userLevel.put(id, (FsUserLevel) hmget.get(id));
            }
        });
        return userLevel;
    }

    private Map<String, FsUserInfo> getUserIDs2Info(List<String> userId, List<String> needUserIDs2Info, List<String> needUserIDs2Level) {
        Map<String, FsUserInfo> userInfo = new HashMap<>();
        Map<Object, Object> target = redisUtil.hmget(RedisUserBuild.userInfo);
        userId.forEach(id -> {
            if (!target.containsKey(id)) {
                needUserIDs2Info.add(id);
            } else {
                userInfo.put(id, (FsUserInfo) target.get(id));
            }
        });


        return userInfo;
    }*/


}
