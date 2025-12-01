package com.zhangzc.mongodbspringbootstart.utills;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MongoUtil {


    private final MongoTemplate mongoTemplate;
    
    public MongoOperations getMongoTemplate() {
        return mongoTemplate;
    }
    
    public <T> T insert(T objectToSave, String collectionName) {
        return mongoTemplate.insert(objectToSave, collectionName);
    }
    
    public <T> Collection<T> insert(Collection<? extends T> batchToSave, String collectionName) {
        return mongoTemplate.insert(batchToSave, collectionName);
    }
    
    public <T> T save(T objectToSave) {
        return mongoTemplate.save(objectToSave);
    }

    public <T> T save(T objectToSave, String collectionName) {
        return mongoTemplate.save(objectToSave, collectionName);
    }

    public <T> T findOne(Query query, Class<T> entityClass) {
        return mongoTemplate.findOne(query, entityClass);
    }
    
    public <T> T findOne(Query query, Class<T> entityClass, String collectionName) {
        return mongoTemplate.findOne(query, entityClass, collectionName);
    }

    public boolean exists(Query query, String collectionName) {
        return mongoTemplate.exists(query, collectionName);
    }

    public boolean exists(Query query, Class<?> entityClass, String collectionName) {
        return mongoTemplate.exists(query, entityClass);
    }
    
    public <T> List<T> find(Query query, Class<T> entityClass, String collectionName) {
        return mongoTemplate.find(query, entityClass, collectionName);
    }

    public long count(Query query, String collectionName) {
        return mongoTemplate.count(query, collectionName);
    }

    public UpdateResult updateMulti(Query query, UpdateDefinition update, Class<?> entityClass, String collectionName) {
        return mongoTemplate.updateMulti(query, update, entityClass, collectionName);
    }

    public DeleteResult remove(Object object, String collectionName) {
        return mongoTemplate.remove(object, collectionName);
    }
}
