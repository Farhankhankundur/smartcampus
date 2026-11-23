package com.campus.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/** Interface: anything that can be booked can describe itself. */
interface Bookable {
    String getDescription();
}

/** Abstract parent; Classroom, Lab and Equipment override getDescription() (runtime polymorphism). */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
    @JsonSubTypes.Type(value = Classroom.class, name = "Classroom"),
    @JsonSubTypes.Type(value = Lab.class, name = "Lab"),
    @JsonSubTypes.Type(value = Equipment.class, name = "Equipment")})
public abstract class Resource extends BaseEntity implements Bookable {
    protected String name;
    protected String type;
    protected int capacity;

    protected Resource(String type) { this.type = type; }

    public String getName() { return name; }
    public String getType() { return type; }
    public int getCapacity() { return capacity; }

    /** Factory used by the DAO: picks the right subclass from the stored type. */
    public static Resource of(int id, String name, String type, int capacity) {
        Resource r = switch (type) {
            case "Lab" -> new Lab();
            case "Equipment" -> new Equipment();
            default -> new Classroom();
        };
        r.id = id; r.name = name; r.capacity = capacity;
        return r;
    }

    @Override public void validate() {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("Resource name is required.");
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be greater than 0.");
    }
}

class Classroom extends Resource {
    Classroom() { super("Classroom"); }
    @Override public String getDescription() { return "Classroom with " + capacity + " seats"; }
}

class Lab extends Resource {
    Lab() { super("Lab"); }
    @Override public String getDescription() { return "Laboratory with " + capacity + " workstations"; }
}

class Equipment extends Resource {
    Equipment() { super("Equipment"); }
    @Override public String getDescription() { return capacity + " unit(s) of equipment"; }
}
