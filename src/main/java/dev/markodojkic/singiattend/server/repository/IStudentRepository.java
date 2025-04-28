package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Student;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

public interface IStudentRepository extends MongoRepository<Student, String> {
    @Query(value="{'index': {$regex : ?0, $options: 'i'}}")
    Optional<Student> getByIndex(String index);
    @Aggregation(pipeline={"{ '$match': { '_id': ObjectId(?0) } }",
            "{ $lookup: {from:'Studies',localField:'study_id',foreignField:'_id',as:'study'} }",
            "{ $unwind: {path:'$study',preserveNullAndEmptyArrays:true} }"})
    Optional<Student> getAggregatedById(String id);
    @Aggregation(pipeline={"{$lookup: {from:'Studies',localField:'study_id',foreignField:'_id',as:'study'}}",
            "{ $unwind: {path:'$study',preserveNullAndEmptyArrays:true} }"})
    List<Student> findAllAggregated();
    @Aggregation(pipeline={"{ '$match': { 'study_id': ObjectId(?0) } }",
            "{ '$match': { 'year':  eq(?1) } }",
            "{ $lookup: {from:'Studies',localField:'study_id',foreignField:'_id',as:'study'}}",
            "{ $unwind: {path:'$study',preserveNullAndEmptyArrays:true} }"})
    List<Student> findByStudyIdAndYear(String studyId, int year);
}