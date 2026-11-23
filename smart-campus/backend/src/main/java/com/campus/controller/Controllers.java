package com.campus.controller;

import com.campus.dao.BaseDao;
import com.campus.dao.Daos.*;
import com.campus.model.*;
import com.campus.service.AuthService;
import com.campus.service.BookingService;
import com.campus.service.LibraryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.*;

public class Controllers {

    /** Generic CRUD controller: write it once, reuse for every entity. */
    public abstract static class Crud<T extends BaseEntity> {
        private final BaseDao<T> dao;
        private final ObjectMapper mapper;
        protected Crud(BaseDao<T> dao, ObjectMapper mapper) { this.dao = dao; this.mapper = mapper; }

        @GetMapping public List<T> all() { return dao.findAll(); }
        @GetMapping("/{id}") public T one(@PathVariable int id) { return dao.findById(id); }
        @PostMapping public T create(@RequestBody T t) { t.validate(); return dao.create(t); }
        @PutMapping("/{id}") public T update(@PathVariable int id, @RequestBody T t) { t.validate(); return dao.update(id, t); }
        @DeleteMapping("/{id}") public void delete(@PathVariable int id) { dao.delete(id); }

        /** Streams + lambdas: keep records where any field contains the search text. */
        @GetMapping("/search")
        public List<T> search(@RequestParam String q) {
            String key = q.toLowerCase();
            return dao.findAll().stream()
                    .filter(t -> {
                        Map<?, ?> fields = mapper.convertValue(t, Map.class);
                        return fields.values().stream().anyMatch(v -> String.valueOf(v).toLowerCase().contains(key));
                    })
                    .collect(Collectors.toList());
        }
    }

    @RestController @RequestMapping("/api/students")
    public static class StudentApi extends Crud<Student> {
        public StudentApi(StudentDao d, ObjectMapper m) { super(d, m); }
    }

    @RestController @RequestMapping("/api/faculty")
    public static class FacultyApi extends Crud<Faculty> {
        public FacultyApi(FacultyDao d, ObjectMapper m) { super(d, m); }
    }

    @RestController @RequestMapping("/api/courses")
    public static class CourseApi extends Crud<Course> {
        public CourseApi(CourseDao d, ObjectMapper m) { super(d, m); }
    }

    @RestController @RequestMapping("/api/resources")
    public static class ResourceApi extends Crud<Resource> {
        public ResourceApi(ResourceDao d, ObjectMapper m) { super(d, m); }
    }

    @RestController @RequestMapping("/api/events")
    public static class EventApi extends Crud<CampusEvent> {
        public EventApi(EventDao d, ObjectMapper m) { super(d, m); }
    }

    @RestController @RequestMapping("/api/books")
    public static class BookApi extends Crud<Book> {
        public BookApi(BookDao d, ObjectMapper m) { super(d, m); }
    }

    @RestController @RequestMapping("/api/library")
    public static class LibraryApi {
        private final LibraryService library;
        public LibraryApi(LibraryService library) { this.library = library; }

        /** Administrators see every loan; students see only their own. */
        @GetMapping("/loans")
        public List<Loan> loans(@RequestAttribute("user") AppUser u) {
            return library.all(u.isAdmin() ? null : u.getFullName());
        }

        @PostMapping("/loans")
        public Loan issue(@RequestAttribute("user") AppUser u, @RequestBody Map<String, Object> body) {
            if (body.get("bookId") == null) throw new IllegalArgumentException("Choose a book.");
            int bookId = ((Number) body.get("bookId")).intValue();
            int days = body.get("days") == null ? 14 : Math.max(1, Math.min(60, ((Number) body.get("days")).intValue()));
            String member = u.getFullName();
            Object asked = body.get("member");
            if (u.isAdmin() && asked != null && !String.valueOf(asked).isBlank()) member = String.valueOf(asked).trim();
            return library.issue(bookId, member, days);
        }

        @PostMapping("/loans/{id}/return")
        public void giveBack(@RequestAttribute("user") AppUser u, @PathVariable int id) {
            library.giveBack(id, u.isAdmin() ? null : u.getFullName());
        }
    }

    @RestController @RequestMapping("/api/auth")
    public static class AuthApi {
        private final AuthService auth;
        public AuthApi(AuthService auth) { this.auth = auth; }

        @PostMapping("/login")
        public Map<String, Object> login(@RequestBody Map<String, String> b) {
            String token = auth.login(b.get("username"), b.get("password"));
            return Map.of("token", token, "user", auth.userForToken(token));
        }

        /** Anyone can create a STUDENT account. Administrator accounts cannot be created here. */
        @PostMapping("/register")
        public Map<String, Object> register(@RequestBody Map<String, String> b) {
            auth.register(b.get("username"), b.get("password"), b.get("fullName"), "STUDENT");
            String token = auth.login(b.get("username"), b.get("password"));
            return Map.of("token", token, "user", auth.userForToken(token));
        }

        @GetMapping("/me")
        public AppUser me(@RequestAttribute("user") AppUser u) { return u; }

        @PostMapping("/logout")
        public void logout(@RequestHeader(value = "Authorization", required = false) String header) {
            if (header != null && header.startsWith("Bearer ")) auth.logout(header.substring(7));
        }
    }

    @RestController @RequestMapping("/api/bookings")
    public static class BookingApi {
        private final BookingService service;
        public BookingApi(BookingService service) { this.service = service; }

        @GetMapping public List<Booking> all() { return service.all(); }
        @PostMapping public Booking book(@RequestBody Booking b) { return service.book(b); }
        @DeleteMapping("/{id}") public void cancel(@PathVariable int id) { service.cancel(id); }
    }

    @RestController @RequestMapping("/api/stats")
    public static class StatsApi {
        private final StudentDao students; private final FacultyDao faculty; private final CourseDao courses;
        private final ResourceDao resources; private final EventDao events; private final BookingService bookings;
        private final BookDao books; private final LibraryService library;

        public StatsApi(StudentDao s, FacultyDao f, CourseDao c, ResourceDao r, EventDao e, BookingService b, BookDao bk, LibraryService lib) {
            students = s; faculty = f; courses = c; resources = r; events = e; bookings = b; books = bk; library = lib;
        }

        @GetMapping
        public Map<String, Object> stats() {
            List<Student> st = students.findAll();
            List<Resource> rs = resources.findAll();
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("students", st.size());
            m.put("faculty", faculty.findAll().size());
            m.put("courses", courses.findAll().size());
            m.put("resources", rs.size());
            m.put("events", events.findAll().size());
            m.put("bookings", bookings.all().size());
            m.put("books", books.findAll().size());
            m.put("loans", library.activeCount());
            m.put("studentsByDepartment", st.stream().collect(
                    Collectors.groupingBy(x -> String.valueOf(x.getDepartment()), TreeMap::new, Collectors.counting())));
            m.put("resourcesByType", rs.stream().collect(
                    Collectors.groupingBy(Resource::getType, TreeMap::new, Collectors.counting())));
            return m;
        }
    }
}
