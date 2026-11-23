package com.campus.service;

import com.campus.dao.Daos.*;
import com.campus.model.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Fills an empty database with sample data when the app starts. */
@Component
public class DataSeeder implements CommandLineRunner {
    private final StudentDao students;
    private final FacultyDao faculty;
    private final CourseDao courses;
    private final ResourceDao resources;
    private final EventDao events;
    private final BookDao books;
    private final BookingService bookings;
    private final LibraryService library;
    private final AuthService auth;

    public DataSeeder(StudentDao students, FacultyDao faculty, CourseDao courses, ResourceDao resources,
                      EventDao events, BookDao books, BookingService bookings, LibraryService library, AuthService auth) {
        this.students = students;
        this.faculty = faculty;
        this.courses = courses;
        this.resources = resources;
        this.events = events;
        this.books = books;
        this.bookings = bookings;
        this.library = library;
        this.auth = auth;
    }

    @Override
    public void run(String... args) {
        auth.seedDefaultUsers();     // admin / admin123 and student / student123
        seedCampus();
        seedLibrary();
    }

    private void seedCampus() {
        if (!students.findAll().isEmpty() || !faculty.findAll().isEmpty() || !resources.findAll().isEmpty()) return;

        Faculty f1 = faculty.create(new Faculty(0, "Dr. Ananya Rao", "ananya.rao@smartcampus.edu", "Computer Science"));
        Faculty f2 = faculty.create(new Faculty(0, "Prof. Suresh Kumar", "suresh.kumar@smartcampus.edu", "Information Science"));
        Faculty f3 = faculty.create(new Faculty(0, "Dr. Meera Nair", "meera.nair@smartcampus.edu", "Electronics"));
        Faculty f4 = faculty.create(new Faculty(0, "Prof. Vikram Shetty", "vikram.shetty@smartcampus.edu", "Mechanical"));

        students.create(new Student(0, "Rahul Sharma", "rahul.sharma@smartcampus.edu", "Computer Science", 3));
        students.create(new Student(0, "Priya Patel", "priya.patel@smartcampus.edu", "Computer Science", 3));
        students.create(new Student(0, "Arjun Reddy", "arjun.reddy@smartcampus.edu", "Information Science", 3));
        students.create(new Student(0, "Sneha Gowda", "sneha.gowda@smartcampus.edu", "Information Science", 5));
        students.create(new Student(0, "Mohammed Faiz", "mohammed.faiz@smartcampus.edu", "Electronics", 3));
        students.create(new Student(0, "Kavya Iyer", "kavya.iyer@smartcampus.edu", "Electronics", 5));
        students.create(new Student(0, "Rohan Desai", "rohan.desai@smartcampus.edu", "Mechanical", 3));
        students.create(new Student(0, "Ishita Singh", "ishita.singh@smartcampus.edu", "Computer Science", 7));

        courses.create(new Course(0, "UE25CS2303", "Object Oriented Programming with Practice", 4, f1.getId()));
        courses.create(new Course(0, "UE25CS2301", "Data Structures", 4, f2.getId()));
        courses.create(new Course(0, "UE25CS2302", "Database Management Systems", 3, f1.getId()));
        courses.create(new Course(0, "UE25EC2201", "Digital Electronics", 3, f3.getId()));
        courses.create(new Course(0, "UE25ME2101", "Engineering Mechanics", 3, f4.getId()));

        Resource r1 = resources.create(Resource.of(0, "Room A-101", "Classroom", 60));
        resources.create(Resource.of(0, "Room A-102", "Classroom", 60));
        resources.create(Resource.of(0, "Seminar Hall", "Classroom", 150));
        Resource r4 = resources.create(Resource.of(0, "Java Programming Lab", "Lab", 40));
        resources.create(Resource.of(0, "Computer Networks Lab", "Lab", 30));
        resources.create(Resource.of(0, "Electronics Lab", "Lab", 30));
        Resource r7 = resources.create(Resource.of(0, "Projector", "Equipment", 5));
        resources.create(Resource.of(0, "Laptops", "Equipment", 20));
        resources.create(Resource.of(0, "Wireless Microphone", "Equipment", 4));

        events.create(new CampusEvent(0, "Java Workshop", "2026-11-28", "Java Programming Lab"));
        events.create(new CampusEvent(0, "Guest Lecture on Artificial Intelligence", "2026-12-02", "Seminar Hall"));
        events.create(new CampusEvent(0, "Tech Fest 2026", "2026-12-05", "Main Auditorium"));
        events.create(new CampusEvent(0, "PBL Project Demonstration", "2026-12-10", "Room A-101"));
        events.create(new CampusEvent(0, "Annual Sports Day", "2026-12-15", "Sports Ground"));

        bookings.book(new Booking(0, r1.getId(), "Dr. Ananya Rao", "2026-11-20T10:00"));
        bookings.book(new Booking(0, r4.getId(), "Prof. Suresh Kumar", "2026-11-20T14:00"));
        bookings.book(new Booking(0, r7.getId(), "Rahul Sharma", "2026-11-21T11:00"));
    }

    private void seedLibrary() {
        if (!books.findAll().isEmpty()) return;

        Book b1 = books.create(new Book(0, "Head First Java", "Kathy Sierra and Bert Bates", "Programming", 4, 0));
        books.create(new Book(0, "Effective Java", "Joshua Bloch", "Programming", 3, 0));
        Book b3 = books.create(new Book(0, "Java: The Complete Reference", "Herbert Schildt", "Programming", 5, 0));
        books.create(new Book(0, "Clean Code", "Robert C. Martin", "Software Engineering", 2, 0));
        books.create(new Book(0, "Database System Concepts", "Silberschatz, Korth and Sudarshan", "Databases", 3, 0));
        books.create(new Book(0, "Introduction to Algorithms", "Cormen, Leiserson, Rivest and Stein", "Algorithms", 2, 0));
        books.create(new Book(0, "Operating System Concepts", "Silberschatz, Galvin and Gagne", "Computer Science", 3, 0));
        books.create(new Book(0, "Computer Networks", "Andrew S. Tanenbaum", "Computer Science", 3, 0));
        books.create(new Book(0, "Digital Design", "M. Morris Mano", "Electronics", 3, 0));
        books.create(new Book(0, "Engineering Mechanics", "Irving H. Shames", "Mechanical", 2, 0));

        library.issue(b1.getId(), "Rahul Sharma", 14);
        library.issue(b3.getId(), "Priya Patel", 7);
    }
}
