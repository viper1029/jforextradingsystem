package model;

import com.dukascopy.api.IBar;
import com.dukascopy.api.JFException;

import java.util.List;

public class TimePeriodWithPrice extends TimePeriod {

  double high = Double.MIN_VALUE;

  double low = Double.MAX_VALUE;

  public TimePeriodWithPrice(long startTime, int durationInMilliseconds, List<IBar> barList) throws JFException {
    super(startTime, durationInMilliseconds);
    for (IBar bar : barList) {
      boolean inRange = false;
      if (bar.getTime() == startTime) {
        inRange = true;
      } else if (bar.getTime() == getEndTime()) {
        break;
      }
      if (inRange) {
        if (bar.getHigh() > high) {
          high = bar.getHigh();
        }
        if (bar.getLow() < low) {
          low = bar.getLow();
        }
      }
    }
  }

  public double getHigh() {
    return high;
  }

  public double getLow() {
    return low;
  }
}
