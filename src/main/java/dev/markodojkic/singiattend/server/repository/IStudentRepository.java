package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Student;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface IStudentRepository extends MongoRepository<Student, String> {
    @Query(value="{'index': {$regex : ?0, $options: 'i'}}")
    Student getByIndex(String index);
    @Aggregation(pipeline={"{$lookup: {from:'Studies',localField:'study_id',foreignField:'_id',as:'study'}}","{ '$match': { '_id': ObjectId(?0) } }"})
    Student find(String id);
    @Aggregation(pipeline={"{$lookup: {from:'Studies',localField:'study_id',foreignField:'_id',as:'study'}}"})
    List<Student> findAll();
}
