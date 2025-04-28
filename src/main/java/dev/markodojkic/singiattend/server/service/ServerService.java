package dev.markodojkic.singiattend.server.service;

import dev.markodojkic.singiattend.server.entity.*;
import dev.markodojkic.singiattend.server.mapper.*;
import dev.markodojkic.singiattend.server.model.*;
import dev.markodojkic.singiattend.server.repository.*;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class ServerService implements IServerService {
    public static final String LECTURES = "Lectures";
    public static final String EXERCISES = "Exercises";
    private final ClassInstanceMapper classInstanceMapper;
    private final StaffMapper staffMapper;
    private final StudentMapper studentMapper;
    private final StudyMapper studyMapper;
    private final SubjectMapper subjectMapper;

    private final ClassInstanceRepository classInstanceRepository;
    private final IStaffRepository staffRepository;
    private final IStudentRepository studentRepository;
    private final IStudyRepository studyRepository;
    private final ISubjectRepository subjectRepository;

    @Autowired
    public ServerService(ClassInstanceMapper classInstanceMapper, StaffMapper staffMapper, StudentMapper studentMapper, StudyMapper studyMapper, SubjectMapper subjectMapper, ClassInstanceRepository classInstanceRepository, IStaffRepository staffRepository, IStudentRepository studentRepository, IStudyRepository studyRepository, ISubjectRepository subjectRepository) {
        this.classInstanceMapper = classInstanceMapper;
        this.staffMapper = staffMapper;
        this.studentMapper = studentMapper;
        this.studyMapper = studyMapper;
        this.subjectMapper = subjectMapper;
        this.classInstanceRepository = classInstanceRepository;
        this.staffRepository = staffRepository;
        this.studentRepository = studentRepository;
        this.studyRepository = studyRepository;
        this.subjectRepository = subjectRepository;
    }

    @Override
    public StaffDTO addNewStaffMember(StaffDTO newStaff) {
        Staff newStaffEntity = staffMapper.toEntity(newStaff);
        newStaffEntity.setPasswordHash(encryptPassword(newStaff.getPasswordHash()));
        return staffMapper.toDTO(staffRepository.save(newStaffEntity));
    }

    @Override
    public StaffDTO updateStaffMemberById(String staffId, StaffDTO newStaff) {
        Staff existingStaffEntity = staffRepository.findById(staffId).orElse(null);
        if(existingStaffEntity == null) return null;
        if(StringUtils.hasLength(newStaff.getEmail())) existingStaffEntity.setEmail(newStaff.getEmail());
        if(StringUtils.hasLength(newStaff.getNameSurname())) existingStaffEntity.setNameSurname(newStaff.getNameSurname());
        if(StringUtils.hasLength(newStaff.getRole())) existingStaffEntity.setRole(newStaff.getRole());
        if(StringUtils.hasLength(newStaff.getPasswordHash())) existingStaffEntity.setPasswordHash(encryptPassword(newStaff.getPasswordHash()));
        return staffMapper.toDTO(staffRepository.save(existingStaffEntity));
    }

    @Override
    public boolean checkPasswordStaff(String staffId, String plainPassword) {
        Optional<Staff> optionalStaffEntity = staffRepository.findById(staffId);
        return optionalStaffEntity.isPresent() && plainPassword.equals(decryptPassword(optionalStaffEntity.get().getPasswordHash()));
    }

    @Override
    public StudentDTO addNewStudent(StudentDTO newStudent) {
        Student newStudentEntity = studentMapper.toEntity(newStudent);
        newStudentEntity.setPasswordHash(encryptPassword(newStudent.getPasswordHash()));
        return studentMapper.toDTO(studentRepository.save(newStudentEntity));
    }

    @Override
    public StudentDTO updateStudentById(String studentId, StudentDTO newStudent) {
        Student existingStudentEntity = studentRepository.findById(studentId).orElse(null);
        if(existingStudentEntity == null) return null;
        if(StringUtils.hasLength(newStudent.getYear())) existingStudentEntity.setYear(newStudent.getYear());
        if(StringUtils.hasLength(newStudent.getEmail())) existingStudentEntity.setEmail(newStudent.getEmail());
        if(StringUtils.hasLength(newStudent.getNameSurname())) existingStudentEntity.setNameSurname(newStudent.getNameSurname());
        if(StringUtils.hasLength(newStudent.getIndex())) existingStudentEntity.setIndex(newStudent.getIndex());
        if(StringUtils.hasLength(newStudent.getPasswordHash())) existingStudentEntity.setPasswordHash(encryptPassword(newStudent.getPasswordHash()));
        if(StringUtils.hasLength(newStudent.getStudyId())) existingStudentEntity.setStudyId(newStudent.getStudyId());
        return studentMapper.toDTO(studentRepository.save(existingStudentEntity));
    }

    @Override
    public String checkPasswordStudent(String index, String plainPassword) {
        return studentRepository.getByIndex(index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6)).map(foundStudent -> plainPassword.equals(decryptPassword(foundStudent.getPasswordHash())) ? "VALID" : "INVALID").orElse("UNKNOWN");
    }

    @Override
    public String getNameSurnameStudent(String index) {
        return studentRepository.getByIndex(index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6)).map(Student::getNameSurname).orElse("-???-");
    }

    @Override
    public List<CourseDataInstance> getCourseData(String index) {
        final String studentId = studentRepository.getByIndex(index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6)).map(Student::getId).orElse("");
        List<CourseDataInstance> output = new ArrayList<>();
        final Date from = Date.from(Instant.now().minus(15, ChronoUnit.MINUTES));
        final Date to = Date.from(Instant.now());

        for(CourseDataSubjectInstance courseDataSubjectInstance: subjectRepository.getCourseDataByLectures(studentId, from, to).getMappedResults()){
            ClassInstanceDTO lecture = getLastClassInstanceBySubjectId(false, courseDataSubjectInstance.getId());
            output.add(new CourseDataInstance(lecture.getId(),courseDataSubjectInstance.getNameT(),courseDataSubjectInstance.getTitle() + "-предавања", courseDataSubjectInstance.getTitleEnglish() + "-lecture", lecture.getStartedAt().toString(), lecture.getEndedAt().toString()));
        }

        for(CourseDataSubjectInstance courseDataSubjectInstance: subjectRepository.getCourseDataByExercises(studentId, from, to).getMappedResults()){
            ClassInstanceDTO exercise = getLastClassInstanceBySubjectId(false, courseDataSubjectInstance.getId());
            output.add(new CourseDataInstance(exercise.getId(), (StringUtils.hasLength(courseDataSubjectInstance.getNameA()) ?  courseDataSubjectInstance.getNameA() : courseDataSubjectInstance.getNameT()),courseDataSubjectInstance.getTitle() + "-вежбе", courseDataSubjectInstance.getTitleEnglish() + "-practice", exercise.getStartedAt().toString(), exercise.getEndedAt().toString()));
        }

        return output;
    }

    @Override
    public String recordAttendance(String subjectId, String index, boolean isExercise) {
        final String studentId = studentRepository.getByIndex(index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6)).map(Student::getId).orElse("");

        ClassInstance classInstance = classInstanceRepository.getLastBySubjectId((isExercise ? EXERCISES : LECTURES), subjectId);
        var attendedStudents = classInstance.getAttendedStudents();
        if(attendedStudents.contains(studentId)) return "ALREADY RECORDED ATTENDANCE";
        attendedStudents.add(studentId);
        classInstance.setAttendedStudents(attendedStudents);
        classInstanceRepository.save((isExercise ? EXERCISES : LECTURES), classInstance);

        return "SUCCESSFULLY RECORDED ATTENDANCE";
    }

    @Override
    public List<AttendanceDataInstance> getAttendanceData(String index) {
        List<AttendanceDataInstance> output = new ArrayList<>();
        final String index_ = index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6);

        AggregationResults<AttendanceHelperInstance> attendanceHelperInstances = subjectRepository.getAttendanceHelperInstanceByStudentId(studentRepository.getByIndex(index_).map(Student::getId).orElse(""));
        for (AttendanceHelperInstance attendanceHelperInstance : attendanceHelperInstances) {
            int al = classInstanceRepository.getAllAttendedBySubjectIdAndStudentIdCount(LECTURES, attendanceHelperInstance.getSubjectId(), index_);
            int ap = classInstanceRepository.getAllAttendedBySubjectIdAndStudentIdCount(EXERCISES, attendanceHelperInstance.getSubjectId(), index_);
            int tl = classInstanceRepository.getAllBySubjectIdCount(LECTURES, attendanceHelperInstance.getSubjectId());
            int tp = classInstanceRepository.getAllBySubjectIdCount(EXERCISES, attendanceHelperInstance.getSubjectId());
            output.add(new AttendanceDataInstance(attendanceHelperInstance, al, ap, tl, tp));
        }
        return output;
    }

    @Override
    public List<AttendanceHelperInstance> getSubjectsByProfessorId(String professorId) {
        return subjectRepository.findAllAggregationByProfessorId(professorId).getMappedResults();
    }

    @Override
    public List<AttendanceHelperInstance> getSubjectsByAssistantId(String assistantId) {
        return subjectRepository.findAllAggregationByAssistantId(assistantId).getMappedResults();
    }

    @Override
    public List<StaffDTO> getAllStaff() {
        return staffMapper.toDTOList(staffRepository.findAll());
    }

    @Override
    public List<StudyDTO> getAllStudies() {
        return studyMapper.toDTOList(studyRepository.findAll());
    }

    @Override
    public List<StudentDTO> getAllStudents() {
        return studentMapper.toDTOList(studentRepository.findAllAggregated());
    }

    @Override
    public List<AttendanceHelperInstance> getAllSubjects() {
        return subjectRepository.findAllAggregation().getMappedResults();
    }

    @Override
    public SubjectDTO addNewSubject(SubjectDTO newSubject) {
        //int is added as NumberInt, and _class property is added for all classes (can be ignored)
        return subjectMapper.toDTO(subjectRepository.save(subjectMapper.toEntity(newSubject)));
    }

    @Override
    public int totalStudentsBySubjectId(String subjectId) {
        return subjectRepository.findById(subjectId).map(subject -> subject.getEnrolledStudentIds().size()).orElse(-1);
    }

    @Override
    public List<StudentDTO> getAllStudentsBySubjectIdAndAttendanceYear(String studyId, int attendanceYear) {
        return studentMapper.toDTOList(studentRepository.findByStudyIdAndYear(studyId, attendanceYear));
    }

    @Override
    public SubjectDTO getSubjectById(String subjectId) {
        return subjectMapper.toDTO(subjectRepository.findById(subjectId).orElse(null));
    }

    @Override
    public boolean checkIfStaffHasSubjectAssigned(String id) {
       return subjectRepository.existsByProfessorIdOrAssistantId(new ObjectId(id), new ObjectId(id));
    }

    @Override
    public void deleteStaff(String staffId, boolean isAssistant) {
        if(isAssistant){
            subjectRepository.findByAssistantId(new ObjectId(staffId)).forEach(subject -> {
               subject.setAssistantId(subject.getProfessorId());
               subjectRepository.save(subject);
            });
        } else getSubjectsByProfessorId(staffId).forEach(s -> subjectRepository.deleteById(s.getSubjectId()));

        staffRepository.deleteById(staffId);
    }

    @Override
    public void deleteStudent(String studentId) {
        studentRepository.deleteById(studentId);
    }

    @Override
    public List<StudentDTO> getAllStudentsBySubjectId(String subjectId) {
        return studentMapper.toDTOList(getSubjectById(subjectId).getEnrolledStudentIds().stream().map(enrolledStudentId -> studentRepository.getAggregatedById(enrolledStudentId).orElse(null)).filter(Objects::nonNull).toList());
    }

    @Override
    public boolean subjectIsInactiveById(String subjectId) {
        return getSubjectById(subjectId).getIsInactive() == null || getSubjectById(subjectId).getIsInactive();
    }

    public List<ClassInstanceDTO> getAllClassInstancesBySubjectId(boolean isExercise, String subjectId) {
        return classInstanceMapper.toDTOList(classInstanceRepository.getAllBySubjectId((isExercise ? EXERCISES : LECTURES), subjectId));
    }

    @Override
    public ClassInstanceDTO getLastClassInstanceBySubjectId(boolean isExercise, String subjectId) {
        return classInstanceMapper.toDTO(classInstanceRepository.getLastBySubjectId((isExercise ? EXERCISES : LECTURES), subjectId));
    }

    @Override
    public void startNewClassInstance(boolean isExercise, String subjectId, String begin, String end) {
        Subject subject = subjectRepository.findById(subjectId).orElse(null);
        if(subject == null) return;

        Date beginsAtUTC = Date.from(LocalDateTime.of(LocalDate.now(), LocalTime.parse(begin, DateTimeFormatter.ofPattern("HH:mm"))).atZone(ZoneId.of("Europe/Belgrade")).withZoneSameInstant(ZoneId.of("UTC")).toInstant());
        Date endsAtUTC = Date.from(LocalDateTime.of(LocalDate.now(), LocalTime.parse(end, DateTimeFormatter.ofPattern("HH:mm"))).atZone(ZoneId.of("Europe/Belgrade")).withZoneSameInstant(ZoneId.of("UTC")).toInstant());

        classInstanceRepository.save((isExercise ? EXERCISES : LECTURES), new ClassInstance(subjectId, beginsAtUTC, endsAtUTC));
        if(isExercise) subject.setLastExerciseAt(beginsAtUTC);
        else subject.setLastLectureAt(beginsAtUTC);
        updateSubjectBySubjectId(subjectMapper.toDTO(subject), subjectId);
    }

    @Override
    public List<StaffDTO> getAllAssistants() {
        Staff probe = new Staff();
        probe.setRole("assistant");
        return staffMapper.toDTOList(staffRepository.findAll(Example.of(probe, ExampleMatcher.matching().withStringMatcher(ExampleMatcher.StringMatcher.EXACT))));
    }

    @Override
    public void startNewSubjectYear(String subjectId) {
        classInstanceRepository.clean(subjectId);
        SubjectDTO subject = getSubjectById(subjectId);
        subject.setIsInactive(false);
        updateSubjectBySubjectId(subject, subjectId);
    }

    @Override
    public void endCurrentSubjectYear(String subjectId) {
        SubjectDTO subject = getSubjectById(subjectId);
        subject.setIsInactive(true);
        updateSubjectBySubjectId(subject, subjectId);
    }

    @Override
    public SubjectDTO updateSubjectBySubjectId(SubjectDTO newSubject, String subjectId) {
        Subject existingSubjectEntity = subjectRepository.findById(subjectId).orElse(null);
        if(existingSubjectEntity == null) return null;
        if(StringUtils.hasLength(newSubject.getTitle())) existingSubjectEntity.setTitle(newSubject.getTitle());
        if(StringUtils.hasLength(newSubject.getTitleEnglish())) existingSubjectEntity.setTitleEnglish(newSubject.getTitleEnglish());
        if(StringUtils.hasLength(newSubject.getProfessorId())) existingSubjectEntity.setProfessorId(newSubject.getProfessorId());
        if(StringUtils.hasLength(newSubject.getAssistantId())) existingSubjectEntity.setAssistantId(newSubject.getAssistantId().equals("-1") ? null : newSubject.getAssistantId());
        if(newSubject.getLastLectureAt() != null) existingSubjectEntity.setLastLectureAt(newSubject.getLastLectureAt());
        if(newSubject.getLastExerciseAt() != null) existingSubjectEntity.setLastExerciseAt(newSubject.getLastExerciseAt());
        if(newSubject.getEnrolledStudentIds() != null) existingSubjectEntity.setEnrolledStudentIds(newSubject.getEnrolledStudentIds());
        if(newSubject.getIsInactive() != null) existingSubjectEntity.setIsInactive(newSubject.getIsInactive());

        return subjectMapper.toDTO(subjectRepository.save(existingSubjectEntity));
    }

    @Override
    public String getStaffNameAndRole(String staffId) {
        return staffRepository.findById(staffId).map(foundStaffMember -> String.format("%s:%s", foundStaffMember.getNameSurname(), foundStaffMember.getRole())).orElse(null);
    }

    @Override
    public ClassInstanceDTO getClassInstance(boolean isExercise, String id) {
        return classInstanceMapper.toDTO(classInstanceRepository.findById((isExercise ? EXERCISES : LECTURES), id));
    }

    private String encryptPassword(String plain) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode("8p6pyFULk8j3DV/yHJaGzw==");
            SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

            byte[] iv = new byte[12];
            SecureRandom secureRandom = new SecureRandom();
            secureRandom.nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to cipher text
            byte[] encryptedWithIv = new byte[12 + cipherText.length];
            System.arraycopy(iv, 0, encryptedWithIv, 0, 12);
            System.arraycopy(cipherText, 0, encryptedWithIv, 12, cipherText.length);

            return Base64.getEncoder().encodeToString(encryptedWithIv);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                 NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            return "?";
        }
    }

    private String decryptPassword(String encrypted) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode("8p6pyFULk8j3DV/yHJaGzw==");
            SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");

            byte[] decoded = Base64.getDecoder().decode(encrypted);

            byte[] iv = Arrays.copyOfRange(decoded, 0, 12);
            byte[] cipherText = Arrays.copyOfRange(decoded, 12, decoded.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainText = cipher.doFinal(cipherText);

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (InvalidAlgorithmParameterException | NoSuchPaddingException | IllegalBlockSizeException |
                NoSuchAlgorithmException | BadPaddingException | InvalidKeyException e) {
            return null;
        }
    }

}
