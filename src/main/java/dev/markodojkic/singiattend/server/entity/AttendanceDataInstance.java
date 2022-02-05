package dev.markodojkic.singiattend.server.entity;

public class AttendanceDataInstance {
    private AttendanceSubobjectInstance attendanceSubobjectInstance;
    private int attendedLectures, attendedPractices, totalLectures, totalPractices;

    public AttendanceDataInstance() { }

    public AttendanceDataInstance(AttendanceSubobjectInstance attendanceSubobjectInstance, int attendedLectures, int attendedPractices, int totalLectures, int totalPractices) {
        this.attendanceSubobjectInstance = attendanceSubobjectInstance;
        this.attendedLectures = attendedLectures;
        this.attendedPractices = attendedPractices;
        this.totalLectures = totalLectures;
        this.totalPractices = totalPractices;
    }

    public AttendanceSubobjectInstance getAttendanceSubobjectInstance() {
        return attendanceSubobjectInstance;
    }

    public void setAttendanceSubobjectInstance(AttendanceSubobjectInstance attendanceSubobjectInstance) {
        this.attendanceSubobjectInstance = attendanceSubobjectInstance;
    }

    public int getAttendedLectures() {
        return attendedLectures;
    }

    public void setAttendedLectures(int attendedLectures) {
        this.attendedLectures = attendedLectures;
    }

    public int getAttendedPractices() {
        return attendedPractices;
    }

    public void setAttendedPractices(int attendedPractices) {
        this.attendedPractices = attendedPractices;
    }

    public int getTotalLectures() {
        return totalLectures;
    }

    public void setTotalLectures(int totalLectures) {
        this.totalLectures = totalLectures;
    }

    public int getTotalPractices() {
        return totalPractices;
    }

    public void setTotalPractices(int totalPractices) {
        this.totalPractices = totalPractices;
    }
}
