package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.Study;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IStudyRepository extends MongoRepository<Study, String> {
    /*@Aggregation("{ $match : { 'enroled_students' : { $eq: ?0 } } } }, {$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}},{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}])})")
    List<CourseDataInstance> getCourseData(String index);*/
}
