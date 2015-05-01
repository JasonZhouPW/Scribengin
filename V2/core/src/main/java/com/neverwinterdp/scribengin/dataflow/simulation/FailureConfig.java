package com.neverwinterdp.scribengin.dataflow.simulation;

import com.neverwinterdp.registry.activity.Activity;
import com.neverwinterdp.registry.activity.ActivityStep;

public class FailureConfig {
  static public enum FailurePoint {Before, Middle, After }
  
  private String       activityType;
  private String       activityStepType;
  private FailurePoint failurePoint;
  
  public FailureConfig() {} 
  
  public FailureConfig(String activity, String step, FailurePoint failurePoint) {
    this.activityType     = activity;
    this.activityStepType = step;
    this.failurePoint     = failurePoint;
  }
  
  public String getActivityType() { return activityType; }
  public void setActivityType(String activityType) {
    this.activityType = activityType;
  }
  
  public String getActivityStepType() { return activityStepType; }
  public void setActivityStepType(String activityStepType) {
    this.activityStepType = activityStepType;
  }
  
  public FailurePoint getFailurePoint() { return failurePoint; }
  public void setFailurePoint(FailurePoint failurePoint) {
    this.failurePoint = failurePoint;
  }
  
  public boolean matches(Activity activity) {
    if(activityType == null) return false;  
    return activity.getType().equals(activityType) ;
  }
  
  public boolean matches(ActivityStep step) {
    if(activityStepType == null) return true ;
    return step.getType().equals(activityStepType) ;
  }
  
  public boolean matches(FailurePoint failurePoint) {
    if(failurePoint == null) return true ;
    return failurePoint == this.failurePoint ;
  }
}