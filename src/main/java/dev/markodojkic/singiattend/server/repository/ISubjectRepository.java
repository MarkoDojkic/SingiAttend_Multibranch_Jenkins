package dev.markodojkic.singiattend.server.repository;

import com.mongodb.lang.Nullable;
import dev.markodojkic.singiattend.server.entity.AttendanceHelperInstance;
import dev.markodojkic.singiattend.server.entity.CourseDataSubjectInstance;
import dev.markodojkic.singiattend.server.entity.Subject;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

@SuppressWarnings("NullableProblems")
public interface ISubjectRepository extends MongoRepository<Subject, String> {
    @SuppressWarnings("NullableProblems")
    @Aggregation(pipeline={"{$match: {enrolled_study_ids: {$in:  ['?0'] } } }",
            "{ $lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}",
            "{ $lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}",
            "{ $unwind: { path: '$professor', preserveNullAndEmptyArrays: true } }",
            "{ $unwind: { path: '$assistant', preserveNullAndEmptyArrays: true } }",
            "{ $addFields: { nameT: '$professor.name_surname', nameA: '$assistant.name_surname' } }",
            "{ $project: { 'professor':0, 'assistant':0, 'professor_id':0,'assistant_id':0,'enrolled_study_ids':0, 'last_lecture_at':0, 'last_exercise_at':0 } }"
    })
    AggregationResults<AttendanceHelperInstance> getAttendanceHelperInstance(String enrolledStudyId);

    @SuppressWarnings("NullableProblems")
    @Aggregation(pipeline={"{$match: {enrolled_study_ids: {$in:  [?0] } } }",
            "{ $match : { 'last_lecture_at' : { $gte: ?1, $lte: ?2 } } }",
            "{ $lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}",
            "{ $lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}",
            "{ $unwind: { path: '$professor', preserveNullAndEmptyArrays: true } }",
            "{ $unwind: { path: '$assistant', preserveNullAndEmptyArrays: true } }",
            "{ $addFields: { nameT: '$professor.name_surname', nameA: '$assistant.name_surname' } }",
            "{ $project: { 'professor':0, 'assistant':0, 'professor_id':0,'assistant_id':0,'enrolled_study_ids':0 } }"
    })
    AggregationResults<CourseDataSubjectInstance> getCourseDataByLectures(String enrolledStudyId, Date from, Date to);

    @SuppressWarnings("NullableProblems")
    @Aggregation(pipeline={"{$match: {enrolled_study_ids: {$in:  [?0] } } }",
            "{ $match : { 'last_exercise_at' : { $gte: ?1, $lte: ?2 } } }",
            "{ $lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}",
            "{ $lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}",
            "{ $unwind: { path: '$professor', preserveNullAndEmptyArrays: true } }",
            "{ $unwind: { path: '$assistant', preserveNullAndEmptyArrays: true } }",
            "{ $addFields: { nameT: '$professor.name_surname', nameA: '$assistant.name_surname' } }",
            "{ $project: { 'professor':0, 'assistant':0, 'professor_id':0,'assistant_id':0,'enrolled_study_ids':0 } }"
    })
    AggregationResults<CourseDataSubjectInstance> getCourseDataByExercises(String enrolledStudyId, Date from, Date to);

    @SuppressWarnings("NullableProblems")
    @Aggregation(pipeline={"{ $lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}",
            "{ $lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}",
            "{ $unwind: { path: '$professor', preserveNullAndEmptyArrays: true } }",
            "{ $unwind: { path: '$assistant', preserveNullAndEmptyArrays: true } }",
            "{ $addFields: { nameT: '$professor.name_surname', nameA: '$assistant.name_surname' } }",
            "{ $project: { 'professor':0, 'assistant':0, 'professor_id':0,'assistant_id':0,'enrolled_study_ids':0 } }"})
    AggregationResults<AttendanceHelperInstance> findAllAggregation();

    @SuppressWarnings("NullableProblems")
    @Aggregation(pipeline={"{ $match: { 'professor_id': ObjectId(?0) } }",
            "{ $lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}",
            "{ $lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}",
            "{ $unwind: { path: '$professor', preserveNullAndEmptyArrays: true } }",
            "{ $unwind: { path: '$assistant', preserveNullAndEmptyArrays: true } }",
            "{ $addFields: { nameT: '$professor.name_surname', nameA: '$assistant.name_surname' } }",
            "{ $project: { 'professor':0, 'assistant':0, 'professor_id':0,'assistant_id':0,'enrolled_study_ids':0 } }"})
    AggregationResults<AttendanceHelperInstance> findAllAggregationByProfessorId(String professorId);

    @SuppressWarnings("NullableProblems")
    @Aggregation(pipeline={"{ $match: { 'assistant_id': ObjectId(?0) } }",
            "{ $lookup: {from:'Staff',localField:'professor_id',foreignField:'_id',as:'professor'}}",
            "{ $lookup: {from:'Staff',localField:'assistant_id',foreignField:'_id',as:'assistant'}}",
            "{ $unwind: { path: '$professor', preserveNullAndEmptyArrays: true } }",
            "{ $unwind: { path: '$assistant', preserveNullAndEmptyArrays: true } }",
            "{ $addFields: { nameT: '$professor.name_surname', nameA: '$assistant.name_surname' } }",
            "{ $project: { 'professor':0, 'assistant':0, 'professor_id':0,'assistant_id':0,'enrolled_study_ids':0 } }"})
    AggregationResults<AttendanceHelperInstance> findAllAggregationByAssistantId(String assistantId);

    @SuppressWarnings("SpringDataRepositoryMethodParametersInspection")
    boolean existsByProfessorIdOrAssistantId(ObjectId professorId, ObjectId assistantId);

    @SuppressWarnings("SpringDataRepositoryMethodParametersInspection")
    List<Subject> findByAssistantId(@Nullable ObjectId assistantId);
}
