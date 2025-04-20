package dev.markodojkic.singiattend.server.repository;

import dev.markodojkic.singiattend.server.entity.ClassInstance;

import java.util.List;

public interface IClassInstanceRepository {
    void save(String collection, ClassInstance classInstance);
    void clean(String subjectId);
    ClassInstance getLastBySubjectId(String collection, String subjectId);
    List<ClassInstance> getAllBySubjectId(String collection, String subjectId);
    int getAllAttendedBySubjectIdAndStudentIdCount(String collection, String subjectId, String studentId);
    int getAllBySubjectIdCount(String collection, String subjectId);
    ClassInstance findById(String collection, String id);
}
