package dev.markodojkic.singiattend.server.service;

import dev.markodojkic.singiattend.server.entity.*;
import dev.markodojkic.singiattend.server.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ServerService implements IServerService {
    //TODO: FIX WHEN _class is added to mongodb on insert/update
    //TODO: FIX WHEN date is updated to be in same format as before
    //TODO: REFACTOR ANDROID APP, iOS APP IS FIXED
    @Autowired
    private IExerciseRepository exerciseRepository;
    @Autowired
    private ILectureRepository lectureRepository;
    @Autowired
    private IStaffRepository staffRepository;
    @Autowired
    private IStudentRepository studentRepository;
    @Autowired
    private IStudyRepository studyRepository;
    @Autowired
    private ISubjectRepository subjectRepository;

    @Override
    public Staff addNewStaffMember(Staff newStaff, boolean isUpdate) {
        if(!isUpdate) newStaff.setPassword_hash(this.encryptPassword(newStaff.getPassword_hash()));
        return this.staffRepository.save(newStaff);
    }

    @Override
    public Staff updateStaffMemberById(String id, Staff newStaffData) {
        Staff staff = staffRepository.findById(id).isPresent() ? staffRepository.findById(id).get() : null;
        if(staff == null) return null;
        if(newStaffData.getEmail() != null && !newStaffData.getEmail().isBlank()) staff.setEmail(newStaffData.getEmail());
        if(newStaffData.getName_surname() != null && !newStaffData.getName_surname().isBlank()) staff.setName_surname(newStaffData.getName_surname());
        if(newStaffData.getRole() != null && !newStaffData.getRole().isBlank()) staff.setRole(newStaffData.getRole());
        if(newStaffData.getPassword_hash() != null && !newStaffData.getPassword_hash().isBlank()) staff.setPassword_hash(this.encryptPassword(newStaffData.getPassword_hash()));
        return this.addNewStaffMember(staff, true);
    }

    @Override
    public boolean checkPassword(String id, String plainPassword) {
        return this.encryptPassword(plainPassword).equals(this.staffRepository.findById(id).get().getPassword_hash());
    }

    @Override
    public Student addNewStudent(Student newStudent, boolean isUpdate) {
        if(!isUpdate) newStudent.setPassword_hash(this.encryptPassword(newStudent.getPassword_hash()));
        return this.studentRepository.save(newStudent);
    }

    @Override
    public Student updateStudentById(String id, Student newStudentData) {
        Student student = studentRepository.findById(id).isPresent() ? studentRepository.findById(id).get() : null;
        if(student == null) return null;
        if(newStudentData.getYear() != null && newStudentData.getYear().isEmpty()) student.setYear(newStudentData.getYear());
        if(newStudentData.getEmail() != null && !newStudentData.getEmail().isEmpty()) student.setEmail(newStudentData.getEmail());
        if(newStudentData.getName_surname() != null && !newStudentData.getName_surname().isEmpty()) student.setName_surname(newStudentData.getName_surname());
        if(newStudentData.getIndex() != null && !newStudentData.getIndex().isEmpty()) student.setIndex(newStudentData.getIndex());
        if(newStudentData.getPassword_hash() != null && !newStudentData.getPassword_hash().isEmpty()) student.setPassword_hash(this.encryptPassword(newStudentData.getPassword_hash()));
        if(!newStudentData.getStudyId().isBlank()) student.setStudyId(newStudentData.getStudyId());
        return this.addNewStudent(student, true);
    }

    @Override
    public String checkPasswordStudent(String index, String plainPassword) {
        final String index_ = index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6);
        Optional<Student> optionalStudent = this.studentRepository.findAll().stream().filter(s -> s.getIndex().equals(index_)).findAny();
        if(optionalStudent.isEmpty()) return "UNKNOWN";
        String id = optionalStudent.get().getId();
        return this.encryptPassword(plainPassword).equals(this.studentRepository.findById(id).get().getPassword_hash()) ? "VALID" : "INVALID";
    }

    @Override
    public String getNameSurnameStudent(String index) {
        final String index_ = index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6);
        return this.studentRepository.getByIndex(index_) == null || this.studentRepository.getByIndex(index_).getName_surname().isEmpty() ? "-???-" : studentRepository.getByIndex(index_).getName_surname();
    }

    @Override
    public List<CourseDataInstance> getCourseData(String index) throws ParseException {
        final String index_ = index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6);
        List<CourseDataInstance> output = new ArrayList<>();

        for(CourseDataLecture lecture: this.subjectRepository.getCourseDataLectures(this.studentRepository.getByIndex(index_).getId(), System.currentTimeMillis() - 1000 * 60 * 60 * 2, System.currentTimeMillis()).getMappedResults()){
            String time = (lecture.getLast_lecture_at().split("T")[1]).split("\\+")[0];
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            Date d = df.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.MINUTE, 45);
            String newTime = df.format(cal.getTime());
            output.add(new CourseDataInstance(lecture.getId(),lecture.getProfessor().get(0).getName_surname(),lecture.getTitle() + "-предавања", lecture.getTitle_english() + "-lecture", time, newTime));
        }

        for(CourseDataLecture exercise: this.subjectRepository.getCourseDataExercises(this.studentRepository.getByIndex(index_).getId(), System.currentTimeMillis() - 1000 * 60 * 60 * 2, System.currentTimeMillis()).getMappedResults()){
            String ns = "";
            if(exercise.getAssistant().size() == 0) ns = exercise.getProfessor().get(0).getName_surname();
            else ns = exercise.getAssistant().get(0).getName_surname();
            String time = (exercise.getLast_lecture_at().split("T")[1]).split("\\+")[0];
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            Date d = df.parse(time);
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.add(Calendar.MINUTE, 45);
            String newTime = df.format(cal.getTime());
            output.add(new CourseDataInstance(exercise.getId(),ns,exercise.getTitle() + "-вежбе", exercise.getTitle_english() + "-practice", time, newTime));
        }

        return output;
    }

    @Override
    public String recordAttendance(String subjectId, String index, boolean isExercise) {
        final String index_ = index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6);

        if(!isExercise){
            Lecture lecture = this.lectureRepository.getLast(subjectId);
            var attendedStudents = lecture.getAttended_students();
            if(attendedStudents.contains(this.studentRepository.getByIndex(index_).getId())) return "ALREADY RECORDED ATTENDANCE";
            attendedStudents.add(this.studentRepository.getByIndex(index_).getId());
            lecture.setAttended_students(attendedStudents);
            this.lectureRepository.save(lecture);
            return "SUCCESSFULLY RECORDED ATTENDANCE";
        } else {
            Exercise exercise = this.exerciseRepository.getLast(subjectId);
            var attendedStudents = exercise.getAttended_students();
            if(attendedStudents.contains(this.studentRepository.getByIndex(index_).getId())) return "ALERADY RECORDED ATTENDANCE";
            attendedStudents.add(this.studentRepository.getByIndex(index_).getId());
            exercise.setAttended_students(attendedStudents);
            this.exerciseRepository.save(exercise);
            return "SUCCESSFULLY RECORDED ATTENDANCE";
        }
    }

    @Override
    public boolean checkPasswordAdmin(String plainPassword) {
        return this.encryptPassword(plainPassword).equals("1cfMqJGcxQ/L9LJSAk3bjk0KmDOlRLU+U2dge6iFlTY=");
    }

    @Override
    public List<AttendanceDataInstance> getAttendanceData(String index) {
        List<AttendanceDataInstance> output = new ArrayList<>();
        final String index_ = index.substring(0, index.length() - 6) + "/" + index.substring(index.length() - 6);

        AggregationResults<AttendanceSubobjectInstance> attendanceSubobjectInstances = this.subjectRepository.getSubobjectdata(this.studentRepository.getByIndex(index_).getId());
        for (AttendanceSubobjectInstance attendanceSubobjectInstance:attendanceSubobjectInstances) {
            attendanceSubobjectInstance.setNameT(attendanceSubobjectInstance.getProfessor().get(0).getName_surname());
            attendanceSubobjectInstance.setNameA(attendanceSubobjectInstance.getAssistant().size() == 0 ? "" : attendanceSubobjectInstance.getAssistant().get(0).getName_surname());
            attendanceSubobjectInstance.setProfessor(null);
            attendanceSubobjectInstance.setAssistant(null);
            int al = this.getAttendedLectures(attendanceSubobjectInstance.getSubjectId(), index_);
            int ap = this.getAttendedPractices(attendanceSubobjectInstance.getSubjectId(), index_);
            int tl = this.getTotalLectures(attendanceSubobjectInstance.getSubjectId());
            int tp = this.getTotalPractices(attendanceSubobjectInstance.getSubjectId());
            output.add(new AttendanceDataInstance(attendanceSubobjectInstance, al, ap, tl, tp));
        }
        return output;
    }

    @Override
    public List<Subject> getSubjectsByProfessorId(String professorId) {
        return this.getAllSubjects().stream().filter(s -> s.getProfessorId().equals(professorId)).collect(Collectors.toList());
    }

    @Override
    public List<Subject> getSubjectsByAssistantId(String assistantId) {
        return this.getAllSubjects().stream().filter(s -> s.getAssistantId() != null && s.getAssistantId().equals(assistantId)).collect(Collectors.toList());
    }

    @Override
    public List<Staff> getAllStaff() {
        return this.staffRepository.findAll();
    }

    @Override
    public List<Study> getAllStudies() {
        return this.studyRepository.findAll();
    }

    @Override
    public List<Student> getAllStudents() {
        return this.studentRepository.findAll();
    }

    @Override
    public List<Subject> getAllSubjects() {
        return this.subjectRepository.findAllAggregation().getMappedResults();
    }

    @Override
    public Subject addNewSubject(Subject newSubject) {
        //int is added as NumberInt, and _class property is added for all classes (can be ignored)
        return this.subjectRepository.save(newSubject);
    }

    @Override
    public int totalStudentsBySubjectId(String subjectId) {
        return this.subjectRepository.findById(subjectId).isPresent() ? this.subjectRepository.findById(subjectId).get().getEnroled_students().size() : -1;
    }

    @Override
    public List<Student> getAllStudentsBySY(String studyId, int year) {
        return this.getAllStudents().stream().filter(s -> s.getStudyId().equals(studyId) && s.getYear().equals(String.valueOf(year))).collect(Collectors.toList());
    }

    @Override
    public Subject getSubjectById(String subjectId) {
        List<Subject> matchingSubjects = this.getAllSubjects().stream().filter(s -> s.getId().equals(subjectId)).collect(Collectors.toList());
        return matchingSubjects.isEmpty() ? null : matchingSubjects.get(0);
    }

    @Override
    public Staff getStaffMemberById(String id) {
        List<Staff> matchingStaff = this.getAllStaff().stream().filter(s -> s.getId().equals(id)).collect(Collectors.toList());
        return matchingStaff.isEmpty() ? null : matchingStaff.get(0);
    }

    @Override
    public boolean checkIfStaffHasSubjectAssigned(String id, boolean isAssistant) {
        if(isAssistant) return this.getSubjectsByAssistantId(id).isEmpty();
        else return this.getSubjectsByProfessorId(id).isEmpty();
    }

    @Override
    public void deleteStaff(String staffId, boolean isAssistant) {
        if(isAssistant){
            this.getSubjectsByAssistantId(staffId).forEach(s -> {
                s.setAssistantId("-1");
                this.updateSubjectBySubjectId(s, s.getId());
            });
        } else {
            this.getSubjectsByProfessorId(staffId).forEach(s -> {
                this.subjectRepository.deleteById(s.getId());
            });
        }

        this.staffRepository.deleteById(staffId);
    }

    @Override
    public void deleteStudent(String studentId) {
        this.studentRepository.deleteById(studentId);
    }

    @Override
    public List<Student> allStudentBySubjectId(String subjectId) {
        List<Student> output = new ArrayList<>();
        for(String st: this.getSubjectById(subjectId).getEnroled_students()){
            output.add(this.studentRepository.find(st));
        }
        return output;
    }

    @Override
    public boolean subjectIsInactiveById(String subjectId) {
        return this.getSubjectById(subjectId).getIsInactive().equals("1");
    }

    @Override
    public List<Exercise> allExercisesBySubjectId(String subjectId) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss'+0000'");
        return this.exerciseRepository.findAll().stream().filter(e -> e.getSubject_id().equals(subjectId)).sorted((l1, l2) -> {
            try {
                return sdf.parse(l1.getEnded_at().split("T")[0]).after(sdf.parse(l2.getEnded_at().split("T")[0])) ? 1 : -1;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }).collect(Collectors.toList());
    }

    @Override
    public Exercise getLastExercise(String subjectId) {
        return this.exerciseRepository.getLast(subjectId);
    }

    @Override
    public void startNewLecture(String subjectId, String begin, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        begin = sdf.format(new Date()) + "T" + begin + ":00+0000";
        end = sdf.format(new Date()) + "T" + end + ":00+0000";
        this.lectureRepository.save(new Lecture(subjectId, begin, end, new ArrayList<>()));
        Subject subject = this.subjectRepository.findById(subjectId).get();
        subject.setLastLectureAt(begin);
        this.updateSubjectBySubjectId(subject, subjectId);
    }

    @Override
    public void startNewExercise(String subjectId, String begin, String end) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
        begin = sdf.format(new Date()) + "T" + begin + ":00+0000";
        end = sdf.format(new Date()) + "T" + end + ":00+0000";
        this.exerciseRepository.save(new Exercise(subjectId, begin, end, new ArrayList<>()));
        Subject subject = this.subjectRepository.findById(subjectId).get();
        subject.setLastExerciseAt(begin);
        this.updateSubjectBySubjectId(subject, subjectId);
    }

    @Override
    public List<Staff> getAllAsistants() {
        return this.getAllStaff().stream().filter(s -> s.getRole().equals("assistant")).collect(Collectors.toList());
    }

    @Override
    public void startNewSubjectYear(String subjectId) {
        this.lectureRepository.deleteAll(this.lectureRepository.findAll().stream().filter(l -> l.getSubject_id().equals(subjectId)).collect(Collectors.toList()));
        this.exerciseRepository.deleteAll(this.exerciseRepository.findAll().stream().filter(e -> e.getSubject_id().equals(subjectId)).collect(Collectors.toList()));
        Subject subject = this.getSubjectById(subjectId);
        subject.setIsInactive("0");
        this.updateSubjectBySubjectId(subject, subjectId);
    }

    @Override
    public void endCurrentSubjectYear(String subjectId) {
        Subject subject = this.getSubjectById(subjectId);
        subject.setIsInactive("1");
        this.updateSubjectBySubjectId(subject, subjectId);
    }

    @Override
    public Subject updateSubjectBySubjectId(Subject newSubjectData, String subjectId) {
        Subject subject = this.getSubjectById(subjectId);
        if(newSubjectData.getTitle() != null) subject.setTitle(newSubjectData.getTitle());
        if(newSubjectData.getTitle_english() != null) subject.setTitle_english(newSubjectData.getTitle_english());
        if(newSubjectData.getProfessorId() != null) subject.setProfessorId(newSubjectData.getProfessorId());
        if(newSubjectData.getAssistantId() != null) subject.setAssistantId(newSubjectData.getAssistantId().equals("-1") ? null : newSubjectData.getAssistantId());
        if(newSubjectData.getLastLectureAt() != null) subject.setLastLectureAt(newSubjectData.getLastLectureAt());
        if(newSubjectData.getLastExerciseAt() != null) subject.setLastExerciseAt(newSubjectData.getLastExerciseAt());
        if(newSubjectData.getEnroled_students() != null) subject.setTitle(newSubjectData.getTitle());
        if(newSubjectData.getIsInactive() != null) subject.setIsInactive(newSubjectData.getIsInactive());

        return this.subjectRepository.save(subject);
    }

    @Override
    public List<Lecture> allLecturesBySubjectId(String subjectId) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss'+0000'"); //Example of data: 28.11.2021T02:29:52+0000
        return this.lectureRepository.findAll().stream().filter(l -> l.getSubject_id().equals(subjectId)).sorted((l1, l2) -> {
            try {
                return sdf.parse(l1.getEnded_at()).after(sdf.parse(l2.getEnded_at())) ? 1 : -1;
            } catch (ParseException e) {
                e.printStackTrace();
            }
            return 0;
        }).collect(Collectors.toList());
    }

    @Override
    public Lecture getLastLecture(String subjectId) {
        return this.lectureRepository.getLast(subjectId);
    }

    @Override
    public String getStaffNameAndRole(String staffId) {
        return this.getStaffMemberById(staffId).getName_surname() + ":" + this.getStaffMemberById(staffId).getRole();
    }

    @Override
    public Lecture getLecture(String lectureId) {
        return this.lectureRepository.findById(lectureId).get();
    }

    @Override
    public Exercise getExercise(String exerciseId) {
        return this.exerciseRepository.findById(exerciseId).get();
    }

    private int getTotalPractices(String subjectId) {
        return this.exerciseRepository.getAllBySubject(subjectId);
    }

    private int getTotalLectures(String subjectId) {
        return this.lectureRepository.getAllBySubject(subjectId);
    }

    private int getAttendedPractices(String subjectId, String index) {
        return this.exerciseRepository.getAllAttended(subjectId, this.studentRepository.getByIndex(index).getId());
    }

    private int getAttendedLectures(String subjectId, String index) {
        return this.lectureRepository.getAllAttended(subjectId, this.studentRepository.getByIndex(index).getId());
    }

    private String encryptPassword(String plain) {
        try {
            byte[] decodedKey = Base64.getDecoder().decode("8p6pyFULk8j3DV/yHJaGzw==");
            SecretKey secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(1, secretKey);
            return Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            return "";
        }
    }
}
