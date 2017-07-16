package strategy;

import com.dukascopy.api.Configurable;
import com.dukascopy.api.Filter;
import com.dukascopy.api.IAccount;
import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IConsole;
import com.dukascopy.api.IContext;
import com.dukascopy.api.IEngine;
import com.dukascopy.api.IHistory;
import com.dukascopy.api.IMessage;
import com.dukascopy.api.IOrder;
import com.dukascopy.api.IStrategy;
import com.dukascopy.api.ITick;
import com.dukascopy.api.Instrument;
import com.dukascopy.api.JFException;
import com.dukascopy.api.OfferSide;
import com.dukascopy.api.Period;
import com.dukascopy.api.system.IClient;
import display.DataDisplayWindow;
import modules.KillZone;
import modules.PivotModule;

import java.util.List;

public class LondonOpenKillZoneStrategy implements IStrategy {

  private IContext context;
  private IHistory history;
  private IEngine engine;
  private IConsole console;

  @Configurable("Instrument")
  public Instrument instrument = Instrument.EURUSD;

  @Configurable("Period")
  public Period period = Period.ONE_HOUR;

  int CALCULATE_INDICATORS_FOR_BARS_BACK = 1000;

  int LIQUIDITY_SWIPE_VALID_TIME_HOURS = 5;

  int IGNORE_LIQUIDITY_WITHIN_PREVIOUS_BARS = 5;

  List<IBar> barList;

  PivotModule pivotModule = new PivotModule();

  KillZone londonOpenKillZone;

  boolean highLiquiditySwiped = false;

  boolean lowLiquiditySwiped = false;

  long highLiquiditySwipeTime = 0;

  long lowLiquiditySwipeTime = 0;

  int orderNumber = 0;

  long lastOrderTime = 0;

  DataDisplayWindow display;

  private IClient client;

  public LondonOpenKillZoneStrategy() throws JFException {
  }

  public void onStart(IContext context) throws JFException {
    this.context = context;
    this.history = context.getHistory();
    engine = context.getEngine();
    this.console = context.getConsole();

    console.getOut().println("Started: " + this.getClass().getName());
    IChart chart = context.getChart(this.instrument);
    if (chart == null) {
      context.getConsole().getErr().println("No chart opened for " + this.instrument);
      context.stop();
    }
    barList = getBarsToProcess(null, chart, CALCULATE_INDICATORS_FOR_BARS_BACK);
    londonOpenKillZone = new KillZone(period, barList, CALCULATE_INDICATORS_FOR_BARS_BACK);
    updateAndDrawIndicators(period, chart);
    display = new DataDisplayWindow();
  }

  public void onStop() throws JFException {
    console.getOut().println("Stopped: " + this.getClass().getName());
  }

  public void onMessage(IMessage message) throws JFException {
    switch (message.getType()) {
      case ORDER_SUBMIT_OK:
        console.getOut().println("Order opened: " + message.getOrder());
        break;
      case ORDER_SUBMIT_REJECTED:
        console.getOut().println("Order open failed: " + message.getOrder());
        break;
      case ORDER_FILL_OK:
        console.getOut().println("Order filled: " + message.getOrder());
        break;
      case ORDER_FILL_REJECTED:
        console.getOut().println("Order cancelled: " + message.getOrder());
        break;
    }
  }

  public void onAccount(IAccount account) throws JFException {

  }

  public void onBar(Instrument instrument, Period period, IBar askBar, IBar bidBar) throws JFException {
    if (!instrument.equals(this.instrument) || !period.equals(this.period)) {
      return;
    }
    IChart chart = context.getChart(this.instrument);
    if (chart == null) {
      context.getConsole().getErr().println("No chart opened for " + this.instrument);
      context.stop();
    }
    barList = getBarsToProcess(bidBar, chart, CALCULATE_INDICATORS_FOR_BARS_BACK);
    updateAndDrawIndicators(period, chart);
  }

  public void onTick(Instrument instrument, ITick tick) throws JFException {
    if (instrument != this.instrument) {
      return;
    }
    IChart chart = context.getChart(this.instrument);
    if (chart == null) {
      context.getConsole().getErr().println("No chart opened for " + this.instrument);
      context.stop();
    }

    checkIfLiquiditySwiped(tick);
    checkConditionsForEntry(tick);
  }

  private void checkConditionsForEntry(ITick tick) throws JFException {
    //if(tick.getTime() > lastOrderTime + 12 * 60 * 60 * 1000) {
    //  return;
    // }


    lastOrderTime = 0;
    if (positionsTotal(this.instrument) == 0 && londonOpenKillZone.isInKillZone(tick.getTime())) {
      displayData();
 /*     try {
        Thread.sleep(10);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }*/
      double currentPrice = tick.getBid();
      if (highLiquiditySwiped) {
        engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.SELL, 0.001, 0, 0, tick.getAsk()
            + instrument.getPipValue() * 10, tick.getAsk() - instrument.getPipValue() * 15);
        lastOrderTime = tick.getTime();
      }
      if (lowLiquiditySwiped) {
        engine.submitOrder(getLabel(instrument), instrument, IEngine.OrderCommand.BUY, 0.001, 0, 0, tick.getBid()
            - instrument.getPipValue() * 10, tick.getBid() + instrument.getPipValue() * 15);
        lastOrderTime = tick.getTime();
      }
    }
  }

  private void checkIfLiquiditySwiped(ITick tick) {
    double currentPrice = tick.getBid();
    if(pivotModule.isHighLiquiditySwiped(currentPrice)) {
      highLiquiditySwiped = true;
      highLiquiditySwipeTime = tick.getTime();
    }
    if(pivotModule.isLowLiquiditySwiped(currentPrice)) {
      lowLiquiditySwiped = true;
      lowLiquiditySwipeTime = tick.getTime();
    }
    if (highLiquiditySwipeTime != 0 && tick.getTime() >= highLiquiditySwipeTime + LIQUIDITY_SWIPE_VALID_TIME_HOURS * 60 * 60 * 1000) {
      highLiquiditySwipeTime = 0;
      highLiquiditySwiped = false;
    }
    if (lowLiquiditySwipeTime != 0 && tick.getTime() >= lowLiquiditySwipeTime + LIQUIDITY_SWIPE_VALID_TIME_HOURS * 60 * 60 * 1000) {
      lowLiquiditySwipeTime = 0;
      lowLiquiditySwiped = false;
    }
  }

  private void updateAndDrawIndicators(Period period, IChart chart) throws JFException {
    pivotModule.calculateAndDrawCombinedLiquidity(barList, CALCULATE_INDICATORS_FOR_BARS_BACK, chart);
    londonOpenKillZone.update(barList);
    londonOpenKillZone.draw(chart);
  }

  private List<IBar> getBarsToProcess(Object obj, IChart chart, int numOfBarsBack) throws JFException {
    List<IBar> bars;
    if (obj == null) {
      obj = history.getLastTick(chart.getInstrument());
    }
    if (obj instanceof IBar) {
      bars = history.getBars(this.instrument, chart.getSelectedPeriod(), OfferSide.BID, Filter.WEEKENDS, numOfBarsBack + 2, ((IBar) obj).getTime(), 0);
    } else {
      ITick tick = (ITick) obj;
      long prevBarTime = history.getPreviousBarStart(chart.getSelectedPeriod(), tick.getTime());
      bars = history.getBars(this.instrument, chart.getSelectedPeriod(), OfferSide.BID, Filter.WEEKENDS, numOfBarsBack + 1, prevBarTime, 1);
    }
    return bars;
  }

  //count open positions
  protected int positionsTotal(Instrument instrument) throws JFException {
    int counter = 0;
    for (IOrder order : engine.getOrders(instrument)) {
      if (order.getState() == IOrder.State.FILLED) {
        counter++;
      }
    }
    return counter;
  }

  protected String getLabel(Instrument instrument) {
    String label = instrument.name();
    label = label.substring(0, 2) + label.substring(3, 5);
    label = label + (orderNumber++);
    label = label.toLowerCase();
    return label;
  }

  private void displayData() {
    display.setHighLiquiditySwiped(highLiquiditySwiped);
    display.setLowLiquiditySwiped(lowLiquiditySwiped);
    display.setHighLiquiditySwipedTime(highLiquiditySwipeTime);
    display.setHighLiquiditySwipedTime(lowLiquiditySwipeTime);

    display.update();
  }

  /*private void displayData() {
    Chart chart = context.getChart(this.instrument);
    IChartObjectFactory factory = chart.getChartObjectFactory();
    ITextChartObject d = factory.createText("Test", Calendar.getInstance().getTimeInMillis(), );
    d.setText("Test", GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[0], 10);
    chart.add(d);
  }*/
}