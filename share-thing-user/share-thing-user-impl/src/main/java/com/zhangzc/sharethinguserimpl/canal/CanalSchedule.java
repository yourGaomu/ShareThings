package com.zhangzc.sharethinguserimpl.canal;


import cn.hutool.core.date.DateTime;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.common.collect.Maps;
import com.zhangzc.sharethingscommon.exception.BusinessException;
import com.zhangzc.sharethinguserimpl.Enum.ResponseCodeEnum;
import com.zhangzc.sharethinguserimpl.canal.vo.ValueAndFlagVo;
import com.zhangzc.sharethinguserimpl.pojo.domain.FsUserRole;
import com.zhangzc.sharethinguserimpl.service.FsUserRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@RequiredArgsConstructor
public class CanalSchedule implements Runnable {

    private final FsUserRoleService fsUserRoleService;
    private final CanalProperties canalProperties;
    private final CanalConnector canalConnector;
    private final TransactionTemplate transactionTemplate;


    @Override
    @Scheduled(fixedDelay = 100) // 每隔 100ms 被执行一次
    public void run() {
        // 初始化批次 ID，-1 表示未开始或未获取到数据
        long batchId = -1;
        try {
            // 从 canalConnector 获取批量消息，返回的数据量由 batchSize 控制，若不足，则拉取已有的
            Message message = canalConnector.getWithoutAck(canalProperties.getBatchSize());

            // 获取当前拉取消息的批次 ID
            batchId = message.getId();

            // 获取当前批次中的数据条数
            long size = message.getEntries().size();
            if (batchId == -1 || size == 0) {
                try {
                    // 拉取数据为空，休眠 1s, 防止频繁拉取
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    log.error("Canal 消费数据异常", e);
                }
            } else {
                // 如果当前批次有数据，处理这批次数据
                processEntry(message.getEntries());
            }
            // 对当前批次的消息进行 ack 确认，表示该批次的数据已经被成功消费
            canalConnector.ack(batchId);
        } catch (Exception e) {
            log.error("消费 Canal 批次数据异常", e);
            // 如果出现异常，需要进行数据回滚，以便重新消费这批次的数据
            canalConnector.rollback(batchId);
        }
    }

    /**
     * 处理这一批次数据
     *
     * @param enties
     */
    private void processEntry(List<CanalEntry.Entry> enties) throws Exception {
        // 循环处理批次数据
        for (CanalEntry.Entry entry : enties) {
            // 只处理 ROWDATA 行数据类型的 Entry，忽略事务等其他类型
            if (entry.getEntryType() == CanalEntry.EntryType.ROWDATA) {
                // 获取事件类型（如：INSERT、UPDATE、DELETE 等等）
                CanalEntry.EventType eventType = entry.getHeader().getEventType();
                // 获取数据库名称
                String database = entry.getHeader().getSchemaName();
                // 获取表名称
                String table = entry.getHeader().getTableName();

                // 过滤掉不是 share_all 数据库的数据
                if (!"share_all".equals(database)) {
                    log.debug("Ignore database: {}, table: {}", database, table);
                    continue;
                }

                // 解析出 RowChange 对象，包含 RowData 和事件相关信息
                CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());

                // 遍历所有行数据（RowData）
                for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                    log.info("本次执行的sql语句是#{}", rowChange.getSql());

                    // 获取行中所有列的最新值（AfterColumns）
                    List<CanalEntry.Column> columns = rowData.getAfterColumnsList();

                    // 将列数据解析为 Map，方便后续处理
                    Map<String, ValueAndFlagVo> columnMap = parseColumns2Map(columns);

                    log.info("EventType: {}, Database: {}, Table: {}, Columns: {}", eventType, database, table, columnMap);

                    // 处理事件
                    processEvent(columnMap, table, eventType);
                }
            }
        }
    }


    /**
     * 将列数据解析为 Map
     *
     * @param columns
     * @return
     */
    private Map<String, ValueAndFlagVo> parseColumns2Map(List<CanalEntry.Column> columns) {
        Map<String, ValueAndFlagVo> map = Maps.newHashMap();
        columns.forEach(column -> {
            if (Objects.isNull(column)) return;
            map.put(column.getName(), ValueAndFlagVo
                    .builder()
                    .value(column.getValue())
                    .updated(column.getUpdated())
                    .build());
        });
        return map;
    }

    /**
     * 处理事件
     *
     * @param columnMap
     * @param table
     * @param eventType
     */
    private void processEvent(Map<String, ValueAndFlagVo> columnMap, String table, CanalEntry.EventType eventType) throws Exception {
        switch (table) {
            //用户表事件
            case "fs_user_info" -> handleUserEvent(columnMap, eventType);
            default -> log.warn("Table: {} not support", table);
        }
    }

    private void handleUserEvent(Map<String, ValueAndFlagVo> columnMap, CanalEntry.EventType eventType) {
        if (eventType == CanalEntry.EventType.INSERT) {
            Boolean execute = transactionTemplate.execute(status -> {
                try {
                    //开启事物管理
                    log.info("用户表新增数据: {}", columnMap);
                    //开始给这个新用户配置一个普通的默认角色的权限
                    FsUserRole fsUserRole = new FsUserRole();
                    fsUserRole.setCreateUser(null);
                    fsUserRole.setUpdateUser(null);
                    fsUserRole.setUpdateTime(DateTime.now());
                    fsUserRole.setCreateTime(DateTime.now());
                    fsUserRole.setIsDeleted(0);
                    fsUserRole.setUserId(Long.valueOf(columnMap.get("user_id").getValue().toString()));
                    fsUserRole.setRoleId(1);
                    fsUserRoleService.save(fsUserRole);
                    return true;
                } catch (Exception e) {
                    log.error("处理用户表事件异常", e);
                    status.setRollbackOnly();
                    return false;
                }
            });

            if (Boolean.FALSE.equals(execute)) {
                //发生了异常
                throw new BusinessException(ResponseCodeEnum.CANAL_ERROR);
            }
        }
    }


}

