package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.AttendanceSubobjectInstance;
import dev.markodojkic.singiattend.server.entity.CourseDataLecture;
import dev.markodojkic.singiattend.server.entity.Subject;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@EnableMongoRepositories
public interface ISubjectRepository extends MongoRepository<Subject, String> {
    @Aggregation(pipeline={"{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}","{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}","{$match: {enroled_students: {$eq: ?0}} }"})
    AggregationResults<AttendanceSubobjectInstance> getSubobjectdata(String id);
    @Aggregation(pipeline={"{  '$addFields': {  'date': \n" +
            "{ '$toLong':{  '$dateFromString': { 'dateString': {  $cond: { if: { $eq: [ '$last_lecture_at',''] }, then: '1-1-1970+0000', else: '$last_lecture_at' } } } } } } } \n",
            "{ $match : { 'enroled_students' : { $eq: ?0 } } } \n",
            "{ $match : { 'date' : { $gte: ?1, $lte: ?2 } } } \n",
            "{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}} \n",
            "{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}} \n",
            "{$project: {'professor_id':0,'assistant_id':0,'date':0,'enroled_students':0, '_class':0}}"})
    AggregationResults<CourseDataLecture> getCourseDataLectures(String studentId, long minimal, long maximal);
    @Aggregation(pipeline={"{  '$addFields': {  'date': \n" +
            "{ '$toLong':{  '$dateFromString': { 'dateString': {  $cond: { if: { $eq: [ '$last_exercise_at',''] }, then: '1-1-1970+0000', else: '$last_exercise_at' } } } } } } } \n",
            "{ $match : { 'enroled_students' : { $eq: ?0 } } } \n",
            "{ $match : { 'date' : { $gte: ?1, $lte: ?2 } } } \n",
            "{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}} \n",
            "{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}} \n",
            "{$project: {'professor_id':0,'assistant_id':0,'date':0,'enroled_students':0,'_class':0}}"})
    AggregationResults<CourseDataLecture> getCourseDataExercises(String studentId, long minimal, long maximal);
    @Aggregation(pipeline={"{$lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}","{$lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}"})
    AggregationResults<Subject> findAllAggregation();
}
