package visualization;

import java.time.LocalDate;
import java.util.ArrayList;

public class KYS
{
    // values to pass here
    static LocalDate restrict_starting_date;
    static LocalDate restrict_ending_date;
    static ArrayList<LocalDate> restrict_banned_days;
    static int restrict_type_of_exam;
    static int restrict_break_duration;
    static boolean restrict_multiple_exams_allowed;

    // values made from passed values
    static ArrayList<LocalDate> restrict_allowed_days;

    // the big three
    static ArrayList<Exam_Info> exams_list;
    static ArrayList<Room_Info> rooms_list;
    static ArrayList<Course_Info> courses_list;

    // function that returns the allowed day list if given the limiters and not allowed days list
    static ArrayList<LocalDate> ExtractAllowedDays(LocalDate restrict_starting_date, LocalDate restrict_ending_date, ArrayList<LocalDate> restrict_banned_days)
    {
        LocalDate test_date = restrict_starting_date;

        while(!test_date.isAfter(restrict_ending_date))
        {
            if(!restrict_banned_days.contains(test_date))
            {
                restrict_allowed_days.add(test_date);
            }
            test_date = test_date.plusDays(1);
        }
        return restrict_banned_days;
    }

    // holds info for each course (ones that will have exams) and their exam parameters so we know exactly which exams to host
    public class Course_Info
    {
        String course_id;
        String course_name;
        String course_teacher;
        int course_type;
        int course_exam_length;

        // full constructor
        Course_Info(String course_id, String course_name, String course_teacher, int course_type, int course_exam_length)
        {
            this.course_id = course_id;
            this.course_name = course_name;
            this.course_teacher = course_teacher;
            this.course_type = course_type;
            this.course_exam_length = course_exam_length;
        }

        // empty constructor
        Course_Info(){}
    }

    // holds each classrooms schedule so we know when they are available and when they are not
    public class Room_Info
    {
        String room_id;
        String room_name;
        String room_effective_capacity;
        ArrayList<Exam_Schedule> room_exams_list;

        // full constructor
        Room_Info(String room_id, String room_name, String room_effective_capacity, ArrayList<Exam_Schedule> room_exams_list)
        {
            this.room_id = room_id;
            this.room_name = room_name;
            this.room_effective_capacity = room_effective_capacity;
            this.room_exams_list = room_exams_list;
        }

        // empty constructor
        Room_Info(){}
    }

    // class that holds everything we will need to print out the damn exam program
    public static class Exam_Info
    {
        String exam_course_teacher;
        String exam_course_name;
        String exam_course_id;
        LocalDate exam_date;
        int exam_time;
        int exam_duration;
        ArrayList<String> exam_roomlist;

        // full constructor
        Exam_Info(String exam_course_teacher, String exam_course_name, String exam_course_id, LocalDate exam_date, int exam_time, int exam_duration, ArrayList<String> exam_roomlist)
        {
            this.exam_course_teacher = exam_course_teacher;
            this.exam_course_name = exam_course_name;
            this.exam_course_id = exam_course_id;
            this.exam_date = exam_date;
            this.exam_time = exam_time;
            this.exam_duration = exam_duration;
            this.exam_roomlist = exam_roomlist;
        }

        // empty constructor
        Exam_Info(){}
    }

    // holds exam schedule of a single classroom
    public static class Exam_Schedule
    {
        LocalDate date;
        int starting_point;
        int length;
        String exam_course_id;

        // full constructor
        Exam_Schedule(LocalDate date, int starting_point, int length, String exam_course_id)
        {
            this.date = date;
            this.starting_point = starting_point;
            this.length = length;
            this.exam_course_id = exam_course_id;
        }

        // empty constructor
        Exam_Schedule(){}
    }
}