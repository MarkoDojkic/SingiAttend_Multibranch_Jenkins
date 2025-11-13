package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.ClassInstance;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ClassInstanceRepository implements IClassInstanceRepository {
    public static final String SUBJECT_ID = "subject_id";

    private final MongoTemplate mongoTemplate;

    public ClassInstanceRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void insert(ClassInstance classInstance, String collection) {
        mongoTemplate.insert(classInstance, collection);
    }

    @Override
    public void updateAttendedStudents(String collection, String id, List<String> newAttendedStudents) {
        Query query = new Query(Criteria.where("_id").is(id)); // or any unique field
        Update update = new Update();

        update.set("attended_students", newAttendedStudents);

        mongoTemplate.upsert(query, update, ClassInstance.class, collection);
    }

    @Override
    public ClassInstance findById(String collection, String id) {
        return mongoTemplate.findById(new ObjectId(id), ClassInstance.class, collection);
    }

    @Override
    public void clean(String subjectId) {
        Query query = Query.query(Criteria.where(SUBJECT_ID).is(subjectId));
        mongoTemplate.remove(query, ClassInstance.class, "Lectures");
        mongoTemplate.remove(query, ClassInstance.class, "Exercises");
    }

    @Override
    public ClassInstance getLastBySubjectId(String collection, String subjectId) {
        @SuppressWarnings("NullableProblems") TypedAggregation<ClassInstance> aggregation = Aggregation.newAggregation(ClassInstance.class,
                Aggregation.sort(Sort.Direction.DESC, "ended_at"),
                Aggregation.match(Criteria.where(SUBJECT_ID).is(subjectId)),
                Aggregation.limit(1));

        @SuppressWarnings("NullableProblems") AggregationResults<ClassInstance> results = mongoTemplate.aggregate(aggregation, collection, ClassInstance.class);
        return results.getUniqueMappedResult();
    }

    @Override
    public List<ClassInstance> getAllBySubjectId(String collection, String subjectId) {
        @SuppressWarnings("NullableProblems") TypedAggregation<ClassInstance> aggregation = Aggregation.newAggregation(ClassInstance.class,
                Aggregation.match(Criteria.where(SUBJECT_ID).is(subjectId)),
                Aggregation.sort(Sort.Direction.DESC, "ended_at"));

        @SuppressWarnings("NullableProblems") AggregationResults<ClassInstance> results = mongoTemplate.aggregate(aggregation, collection, ClassInstance.class);
        return results.getMappedResults();
    }

    @Override
    public int getAllAttendedBySubjectIdAndStudentIdCount(String collection, String subjectId, String studentId) {
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where(SUBJECT_ID).is(subjectId),
                Criteria.where("attended_students").regex(studentId, "i")
        ));
        return (int) mongoTemplate.count(query, ClassInstance.class, collection);
    }

    @Override
    public int getAllBySubjectIdCount(String collection, String subjectId) {
        Query query = Query.query(Criteria.where(SUBJECT_ID).is(subjectId));
        return (int) mongoTemplate.count(query, ClassInstance.class, collection);
    }
}
