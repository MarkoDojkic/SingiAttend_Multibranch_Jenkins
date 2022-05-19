package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.AttendanceSubobjectInstance;
import dev.markodojkic.singiattend.server.entity.CourseDataSubjectInstance;
import dev.markodojkic.singiattend.server.entity.Subject;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Date;

@EnableMongoRepositories
public interface ISubjectRepository extends MongoRepository<Subject, String> {
    @Aggregation(pipeline={"{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}","{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}","{$match: {enroled_students: {$eq: ?0}} }"})
    AggregationResults<AttendanceSubobjectInstance> getSubobjectdata(String id);
    @Aggregation(pipeline={"{ $match : { 'enroled_students' : { $eq: ?0 } } } \n",
            "{ $match : { 'last_lecture_at' : { $gte: ?1, $lte: ?2 } } } \n",
            "{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}} \n",
            "{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}} \n",
            "{$project: {'professor_id':0,'assistant_id':0,'date':0,'enroled_students':0, '_class':0}}"})
    AggregationResults<CourseDataSubjectInstance> getCourseDataByLectures(String studentId, Date from, Date to);
    @Aggregation(pipeline={"{ $match : { 'enroled_students' : { $eq: ?0 } } } \n",
            "{ $match : { 'last_exercise_at' : { $gte: ?1, $lte: ?2 } } } \n",
            "{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}} \n",
            "{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}} \n",
            "{$project: {'professor_id':0,'assistant_id':0,'date':0,'enroled_students':0,'_class':0}}"})
    AggregationResults<CourseDataSubjectInstance> getCourseDataByExercises(String studentId, Date from, Date to);
    @Aggregation(pipeline={"{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}","{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}"})
    AggregationResults<Subject> findAllAggregation();
}
