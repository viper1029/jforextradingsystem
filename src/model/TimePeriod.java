package model;

public class TimePeriod {

  long startTime;

  long endTime;

  public TimePeriod(long startTime, int durationInMilliseconds) {
    this.startTime = startTime;
    this.endTime = startTime + durationInMilliseconds;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public void setStartTime(long startTime) {
    this.startTime = startTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }
}
