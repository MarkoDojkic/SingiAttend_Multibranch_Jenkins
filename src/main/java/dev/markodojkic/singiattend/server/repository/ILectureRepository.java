package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Lecture;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface ILectureRepository extends MongoRepository<Lecture, String> {
    @Aggregation(pipeline={"{ $addFields: { ended_at: { $toDate: '$ended_at' } } }", "{ $sort: { ended_at: -1 } }", "{ $match: { subject_id: ObjectId(?0) } }", "{ $limit: 1 }"})
    Lecture getLast(String subjectId);
    /*@Query(value="{'subject_id': {$regex : ?0, $options: 'i'}}")
    List<Lecture> getAllBySubject(String subjectId);*/
    @Query(value = "{$and: [ {'subject_id': ObjectId(?0) }, {'attended_students': {$regex : ?1, $options: 'i'}}]}", count = true)
    int getAllAttended(String subjectId, String studentId);
    @Query(value = "{ subject_id: ObjectId(?0) }", count = true)
    int getAllBySubject(String subjectId);
}
