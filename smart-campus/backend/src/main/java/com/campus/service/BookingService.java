package com.campus.service;

import com.campus.dao.Daos.ResourceDao;
import com.campus.exception.Errors;
import com.campus.model.Booking;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BookingService {
    private final JdbcTemplate jdbc;
    private final ResourceDao resources;

    public BookingService(JdbcTemplate jdbc, ResourceDao resources) {
        this.jdbc = jdbc;
        this.resources = resources;
    }

    /**
     * synchronized: many users (threads) may book at once, but only one runs the
     * check-then-insert at a time, so the same slot is never double-booked.
     * The UNIQUE(resource_id, slot) constraint is a second safety net in the database.
     */
    public synchronized Booking book(Booking b) {
        b.validate();
        resources.findById(b.getResourceId());   // throws NotFound if the resource does not exist
        Integer taken = jdbc.queryForObject(
                "SELECT COUNT(*) FROM bookings WHERE resource_id=? AND slot=?", Integer.class, b.getResourceId(), b.getSlot());
        if (taken != null && taken > 0)
            throw new Errors.Unavailable("That resource is already booked for this slot. Pick another time.");
        jdbc.update("INSERT INTO bookings(resource_id,booked_by,slot) VALUES (?,?,?)", b.getResourceId(), b.getBookedBy(), b.getSlot());
        return b;
    }

    public List<Booking> all() {
        return jdbc.query("SELECT * FROM bookings ORDER BY slot",
                (rs, i) -> new Booking(rs.getInt("id"), rs.getInt("resource_id"), rs.getString("booked_by"), rs.getString("slot")));
    }

    public void cancel(int id) {
        if (jdbc.update("DELETE FROM bookings WHERE id=?", id) == 0)
            throw new Errors.NotFound("Booking " + id + " not found.");
    }
}
