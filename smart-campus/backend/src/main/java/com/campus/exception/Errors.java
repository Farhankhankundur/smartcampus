package com.campus.exception;

import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Custom exceptions + one central handler that turns them into clean JSON errors. */
public class Errors {

    public static class NotFound extends RuntimeException {
        public NotFound(String msg) { super(msg); }
    }

    public static class Unavailable extends RuntimeException {
        public Unavailable(String msg) { super(msg); }
    }

    public static class Unauthorized extends RuntimeException {
        public Unauthorized(String msg) { super(msg); }
    }

    @RestControllerAdvice
    public static class Handler {
        @ExceptionHandler(NotFound.class)
        public ResponseEntity<Map<String, String>> notFound(NotFound e) { return reply(404, e.getMessage()); }

        @ExceptionHandler(Unavailable.class)
        public ResponseEntity<Map<String, String>> unavailable(Unavailable e) { return reply(409, e.getMessage()); }

        @ExceptionHandler(Unauthorized.class)
        public ResponseEntity<Map<String, String>> unauthorized(Unauthorized e) { return reply(401, e.getMessage()); }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException e) {
            return reply(409, "That record conflicts with existing data.");
        }

        @ExceptionHandler(RuntimeException.class)   // validation errors, bad JSON, etc.
        public ResponseEntity<Map<String, String>> badRequest(RuntimeException e) { return reply(400, e.getMessage()); }

        private ResponseEntity<Map<String, String>> reply(int status, String msg) {
            return ResponseEntity.status(status).body(Map.of("error", msg == null ? "Something went wrong." : msg));
        }
    }
}
