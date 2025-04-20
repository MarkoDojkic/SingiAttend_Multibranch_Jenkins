package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.ClassInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClassInstanceRepository implements IClassInstanceRepository {
    public static final String SUBJECT_ID = "subject_id";

    private final MongoTemplate mongoTemplate;

    @Autowired
    public ClassInstanceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(String collection, ClassInstance classInstance) {
        mongoTemplate.insert(classInstance, collection);
    }

    @Override
    public ClassInstance findById(String collection, String id) {
        return mongoTemplate.findById(id, ClassInstance.class, collection);
    }

    @Override
    public void clean(String subjectId) {
        mongoTemplate.findAndRemove(new Query(Criteria.where(SUBJECT_ID).is(subjectId)), ClassInstance.class, "Lectures");
        mongoTemplate.findAndRemove(new Query(Criteria.where(SUBJECT_ID).is(subjectId)), ClassInstance.class, "Exercises");
    }

    @Override
    public ClassInstance getLastBySubjectId(String collection, String subjectId) {
        return mongoTemplate.aggregate(Aggregation.newAggregation(Aggregation.sort(Sort.Direction.DESC, "ended_at"),
                Aggregation.match(Criteria.where(SUBJECT_ID).is(subjectId)),
                Aggregation.limit(1)), collection, ClassInstance.class).getUniqueMappedResult();
    }

    @Override
    public List<ClassInstance> getAllBySubjectId(String collection, String subjectId) {
        return mongoTemplate.aggregate(Aggregation.newAggregation(Aggregation.match(Criteria.where(SUBJECT_ID).is(subjectId)),
                Aggregation.sort(Sort.Direction.DESC, "ended_at")), collection, ClassInstance.class).getMappedResults();
    }

    @Override
    public int getAllAttendedBySubjectIdAndStudentIdCount(String collection, String subjectId, String studentId) {
        return (int) mongoTemplate.count(new Query(new Criteria().andOperator(
                Criteria.where(SUBJECT_ID).is(subjectId),
                Criteria.where("attended_students").regex(studentId, "i")
        )), ClassInstance.class, collection);
    }


    @Override
    public int getAllBySubjectIdCount(String collection, String subjectId) {
        return (int) mongoTemplate.count(new Query(Criteria.where(SUBJECT_ID).is(subjectId)), ClassInstance.class, collection);
    }
}
