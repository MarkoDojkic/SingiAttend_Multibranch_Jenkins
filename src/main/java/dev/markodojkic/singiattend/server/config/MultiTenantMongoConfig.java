package dev.markodojkic.singiattend.server.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.markodojkic.singiattend.server.multitenant.MultiTenantMongoDatabaseFactory;
import dev.markodojkic.singiattend.server.multitenant.TenantFilter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMongoRepositories(
        basePackages = "dev.markodojkic.singiattend.server.repository"
)
@Slf4j
public class MultiTenantMongoConfig {

    private final Map<String, MongoDatabaseFactory> tenantFactories = new HashMap<>();
    @Value("${mongoHost}")
    private String mongoHost;  // Inject from properties or env
    @Value("${tenants}")
    private String tenantJson;

    @PostConstruct
    public void init() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, String>> parsedTenants = mapper.readValue(tenantJson, new TypeReference<>() {});
            for (Map<String, String> tenant : parsedTenants) {
                String tenantId = tenant.get("tenantId");
                String dbName = tenant.get("db");
                tenantFactories.put(tenantId, createFactory(dbName));
                log.info("Registered tenant [{}] with DB [{}]", tenantId, dbName);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse tenant factories JSON", e);
        }
    }

    private MongoDatabaseFactory createFactory(String dbName) {
        String connectionUri = String.format("mongodb://%s/%s", mongoHost, dbName);
        return new SimpleMongoClientDatabaseFactory(connectionUri);
    }

    @Bean
    @Primary
    public MultiTenantMongoDatabaseFactory mongoDatabaseFactory() {
        return new MultiTenantMongoDatabaseFactory(tenantFactories);
    }

    @Bean(name = "mongoTemplate")
    @Primary
    public MongoTemplate mongoTemplate(MongoDatabaseFactory mongoDatabaseFactory) {
        return new MongoTemplate(mongoDatabaseFactory);
    }

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter filter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Ensure it's early
        return registration;
    }

    @Bean
    public MappingMongoConverter mappingMongoConverter(MongoMappingContext mongoMappingContext,
                                                       MultiTenantMongoDatabaseFactory multiTenantMongoDatabaseFactory) {
        DbRefResolver dbRefResolver = new DefaultDbRefResolver(multiTenantMongoDatabaseFactory);
        MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, mongoMappingContext);
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return converter;
    }
}
