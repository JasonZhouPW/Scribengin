package com.neverwinterdp.scribengin.storage;

import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class StorageDescriptor extends HashMap<String, String> {
  public StorageDescriptor() { }
  
  public StorageDescriptor(String type) {
    setType(type);
  }
  
  public StorageDescriptor(String type, String location) {
    setType(type);
    setLocation(location);
  }
  
  @JsonIgnore
  public String getType() { return get("type"); }
  public void   setType(String type) { 
    put("type", type); 
  }
 
  @JsonIgnore
  public String getLocation() { return get("location"); }
  public void   setLocation(String location) { 
    put("location", location); 
  }
  
  public String attribute(String name) {
    return get(name);
  }
  
  public void attribute(String name, String value) {
    put(name, value);
  }
  
  public void attribute(String name, int value) {
    put(name, Integer.toString(value));
  }
  
  public int intAttribute(String name) {
    String value = get(name);
    if(value == null) return 0;
    return Integer.parseInt(value);
  }
}