package dev.markodojkic.singiattend.server.multitenant;

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.ClientSession;
import com.mongodb.client.MongoDatabase;
import lombok.extern.slf4j.Slf4j;
import org.bson.codecs.configuration.CodecRegistry;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.support.PersistenceExceptionTranslator;
import org.springframework.data.mongodb.MongoDatabaseFactory;

import java.util.Map;

@Slf4j
public class MultiTenantMongoDatabaseFactory implements MongoDatabaseFactory {

    private final Map<String, MongoDatabaseFactory> factories;

    public MultiTenantMongoDatabaseFactory(Map<String, MongoDatabaseFactory> factories) {
        this.factories = factories;
    }

    private MongoDatabaseFactory getCurrentFactory() {
        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || !factories.containsKey(tenantId)) {
            log.debug("Unknown or missing tenant ID: {}", tenantId);
            tenantId = "SingidunumBG";
        }
        log.debug("Using MongoDB factory for tenant: {}", tenantId);
        return factories.get(tenantId);
    }

    @Override
    public MongoDatabase getMongoDatabase() throws DataAccessException {
        return getCurrentFactory().getMongoDatabase();
    }

    @Override
    public MongoDatabase getMongoDatabase(String dbName) throws DataAccessException {
        // Optional: delegate with dbName if needed
        return getCurrentFactory().getMongoDatabase(dbName);
    }

    @Override
    public PersistenceExceptionTranslator getExceptionTranslator() {
        return getCurrentFactory().getExceptionTranslator();
    }

    @Override
    public CodecRegistry getCodecRegistry() {
        return getCurrentFactory().getCodecRegistry();
    }

    @Override
    public ClientSession getSession(ClientSessionOptions options) {
        return getCurrentFactory().getSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(ClientSessionOptions options) {
        return getCurrentFactory().withSession(options);
    }

    @Override
    public MongoDatabaseFactory withSession(ClientSession session) {
        return getCurrentFactory().withSession(session);
    }

    @Override
    public boolean isTransactionActive() {
        return getCurrentFactory().isTransactionActive();
    }
}
