package modules;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.JFException;
import com.dukascopy.api.Period;
import com.dukascopy.api.drawings.IChartObjectFactory;
import com.dukascopy.api.drawings.IRectangleChartObject;
import model.TimePeriod;
import model.TimePeriodWithPrice;

import java.awt.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class KillZone {

  private ArrayList<TimePeriodWithPrice> historicalKillZones  = new ArrayList<>();

  private TimePeriod killZone;

  private Period period;

  private final int numOfBarsBack;

  public static TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");

  public static int NY_OPEN_HOUR = 8;

  public static int NY_KILLZONE_DURATION = 4 * 60 * 60 * 1000;

  public KillZone(final Period period, final List<IBar> barList, final int numOfBarsBack) throws JFException {
    this.period = period;
    this.numOfBarsBack = numOfBarsBack;
    update(barList);
  }

  public void update(final List<IBar> barList) throws JFException {
    historicalKillZones.clear();
    Calendar lastHistoricalKillZoneCalendar;
    Calendar gmtTime = Calendar.getInstance(GMT_TIMEZONE);
    gmtTime.setTimeInMillis(barList.get(barList.size() - 1).getTime());
    gmtTime.set(Calendar.HOUR_OF_DAY, NY_OPEN_HOUR);
    gmtTime.set(Calendar.MINUTE, 0);
    gmtTime.set(Calendar.SECOND, 0);
    gmtTime.set(Calendar.MILLISECOND, 0);

    int numOfDaysBack = calculateNumberOfDays(period, numOfBarsBack);
    lastHistoricalKillZoneCalendar = (Calendar) gmtTime.clone();
    for (int i = 0; i < numOfDaysBack; i++) {
      historicalKillZones.add(new TimePeriodWithPrice(gmtTime.getTimeInMillis(), NY_KILLZONE_DURATION, barList));
      gmtTime.add(Calendar.DAY_OF_MONTH, -1);
    }

    Calendar cal = (Calendar) lastHistoricalKillZoneCalendar.clone();
    cal.setTimeZone(GMT_TIMEZONE);
    killZone = new TimePeriod(cal.getTimeInMillis(), NY_KILLZONE_DURATION);
  }

  public void draw(IChart chart) throws JFException {
    IChartObjectFactory factory = chart.getChartObjectFactory();
    for (TimePeriodWithPrice timePeriod : historicalKillZones) {
      TimePeriodWithPrice timePeriodWithPrice = (TimePeriodWithPrice) timePeriod;
      IRectangleChartObject rectangle = factory.createRectangle("LO_KILL_ZONE" + timePeriodWithPrice.getStartTime(),
              timePeriodWithPrice.getStartTime(), timePeriodWithPrice.getHigh(), timePeriodWithPrice.getEndTime(), timePeriodWithPrice.getLow());
      rectangle.setFillColor(Color.LIGHT_GRAY);
      chart.add(rectangle);
    }
    IRectangleChartObject rectangle = factory.createRectangle("LO_KILL_ZONE" + killZone.getStartTime(),
            killZone.getStartTime(), 10, killZone.getEndTime(), 0);
    rectangle.setFillColor(Color.LIGHT_GRAY);
    chart.add(rectangle);
  }

  private static int calculateNumberOfDays(Period period, int numOfBarsBack) {
    int numOfDaysBack = 0;
    if (period == Period.ONE_MIN) {
      numOfDaysBack = numOfBarsBack / 24 / 60;
    } else if (period == Period.FIVE_MINS) {
      numOfDaysBack = numOfBarsBack / 24 / 12;
    } else if (period == Period.FIFTEEN_MINS) {
      numOfDaysBack = numOfBarsBack / 4;
    } else if (period == Period.ONE_HOUR) {
      numOfDaysBack = numOfBarsBack / 24;
    } else if (period == Period.DAILY) {
      numOfDaysBack = numOfBarsBack;
    }
    return numOfDaysBack;
  }

  public boolean isInKillZone(long currentTime) {
    if(currentTime >= killZone.getStartTime() && currentTime <= killZone.getEndTime()) {
      return true;
    }
    else {
      return false;
    }
  }

}

