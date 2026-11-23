package com.campus.model;

/** Root of every stored object. */
public abstract class BaseEntity {
    protected int id;
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public void validate() { }   // subclasses override; throw IllegalArgumentException when invalid
}
