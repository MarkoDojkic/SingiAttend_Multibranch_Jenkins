package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Student;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("NullableProblems")
@Repository
public interface IStudentRepository extends MongoRepository<Student, String> {
    @Query("{ 'index': { $regex: ?0, $options: 'i' } }")
    Optional<Student> getByIndex(String index);

    @Aggregation(pipeline = {
            "{ $match: { '_id': { $oid: ?0 } } }",
            "{ $lookup: { from: 'Studies', localField: 'study_id', foreignField: '_id', as: 'study' } }",
            "{ $unwind: { path: '$study', preserveNullAndEmptyArrays: true } }"
    })
    Optional<Student> getAggregatedById(String id);

    @Aggregation(pipeline = {
            "{ $lookup: { from: 'Studies', localField: 'study_id', foreignField: '_id', as: 'study' } }",
            "{ $unwind: { path: '$study', preserveNullAndEmptyArrays: true } }"
    })
    List<Student> findAllAggregated();

    @Aggregation(pipeline = {
            "{ $match: { 'study_id': { $oid: ?0 } } }",
            "{ $match: { 'year': '?1' } }",
            "{ $lookup: { from: 'Studies', localField: 'study_id', foreignField: '_id', as: 'study' } }",
            "{ $unwind: { path: '$study', preserveNullAndEmptyArrays: true } }"
    })
    List<Student> findByStudyIdAndYear(String studyId, int year);
}