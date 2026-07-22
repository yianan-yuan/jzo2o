package com.jzo2o.orders.base.config;

import com.jzo2o.orders.base.properties.DispatchProperties;
import com.jzo2o.orders.base.properties.ExecutorProperties;
import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.SQLException;

@Configuration
@ComponentScan({"com.jzo2o.orders.base.service","com.jzo2o.orders.base.handler"})
@MapperScan("com.jzo2o.orders.base.mapper")
@Import({OrderStateMachine.class})
@EnableConfigurationProperties({DispatchProperties.class, ExecutorProperties.class})
public class AutoImportConfiguration {

    @Bean
    @Primary
    public DataSource shardingSphereDataSource(Environment environment) throws SQLException, IOException {
        String[] activeProfiles = environment.getActiveProfiles();
        String profile = activeProfiles.length > 0 ? activeProfiles[0] : "dev";
        ClassPathResource resource = new ClassPathResource("shardingsphere-jdbc-" + profile + ".yml");
        // 直接返回 ShardingSphere DataSource，不需要再包裹 DataSourceProxy
        // ShardingSphere 配置中已经设置了 providerType: Seata，内部已集成 Seata
        return YamlShardingSphereDataSourceFactory.createDataSource(resource.getFile());
    }
}
