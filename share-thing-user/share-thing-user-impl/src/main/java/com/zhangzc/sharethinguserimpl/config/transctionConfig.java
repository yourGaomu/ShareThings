//package com.zhangzc.sharethinguserimpl.config;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.datasource.DataSourceTransactionManager;
//import org.springframework.transaction.PlatformTransactionManager;
//import org.springframework.transaction.support.TransactionTemplate;
//
//import javax.sql.DataSource;
//
//@Configuration
//public class transctionConfig {
//    // 配置事务管理器（必须：基于 Druid 数据源）
//    @Bean
//    public PlatformTransactionManager transactionManager(DataSource dataSource) {
//        return new DataSourceTransactionManager(dataSource);
//    }
//
//    // 显式声明 TransactionTemplate Bean（兜底：确保容器中存在该 Bean）
//    @Bean
//    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
//        return new TransactionTemplate(transactionManager);
//    }
//}
