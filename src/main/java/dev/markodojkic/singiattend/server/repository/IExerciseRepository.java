package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Exercise;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface IExerciseRepository extends MongoRepository<Exercise, String> {
    @Aggregation(pipeline={"{ $addFields: { ended_at: { $toDate: '$ended_at' } } }", "{ $sort: { ended_at: -1 } }", "{ $match: { subject_id: ObjectId(?0) } }, { $limit: 1 }"})
    Exercise getLast(String subjectId);
    /*@Query(value="{'subject_id': {$regex : ?0, $options: 'i'}}")
    List<Exercise> getAllBySubject(String subjectId);*/
    @Query(value="{$and: [{'subject_id': ObjectId(?0)},{'attended_students': {$regex : ?1, $options: 'i'}}]}", count = true)
    int getAllAttended(String subjectId, String studentId);
    @Query(value = "{ subject_id: ObjectId(?0) }", count = true)
    int getAllBySubject(String subjectId);
}
