package edu.buffalo.cse.phonelab.json;

/**
 * Any class that implements this interface has a toJSONObject function.
 *
 * {@hide}
 */
public interface JSONable{
    public abstract StrictJSONObject toJSONObject();
}
