package modules;

import com.dukascopy.api.IBar;
import com.dukascopy.api.IChart;
import com.dukascopy.api.IChartObject;
import com.dukascopy.api.drawings.IChartObjectFactory;
import model.PricePoint;
import utils.CopyUtils;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PivotModule {

  List<PricePoint> lowPivots = new LinkedList<>();

  List<PricePoint> highPivots = new LinkedList<>();

  List<PricePoint> liquidityHighs;

  List<PricePoint> liquidityLows;

  List<PricePoint> combinedLiquidityHighs;

  List<PricePoint> combinedLiquidityLows;

  public void calculatePivots(List<IBar> barList, int numOfBarsBack) {
    lowPivots.clear();
    highPivots.clear();
    for (int i = 1; i < numOfBarsBack; i++) {
      if (barList.get(i - 1).getLow() > barList.get(i).getLow() && barList.get(i).getLow() < barList.get(i + 1).getLow()) {
        lowPivots.add(new PricePoint(i, barList.get(i)));
      }
      if (barList.get(i - 1).getHigh() < barList.get(i).getHigh() && barList.get(i).getHigh() > barList.get(i + 1).getHigh()) {
        highPivots.add(new PricePoint(i, barList.get(i)));
      }
    }
  }

  /*
  Process from the latest bar to the oldest bar and remove pivots in between a pivot hight and low
   */
  public void calculateSwingPoints(List<IBar> barList, int numOfBarsBack) {
    int lastLow = -1;
    int lastHigh = -1;
    liquidityHighs = CopyUtils.copyPricePointList(highPivots);
    liquidityLows = CopyUtils.copyPricePointList(lowPivots);

    ArrayList<PricePoint> highPivotsToRemove = new ArrayList<>();
    for (int i = liquidityHighs.size() - 1; i >= 0; i--) {
      if (lastHigh == -1 || barList.get(liquidityHighs.get(i).getIndex()).getHigh() > barList.get(lastHigh).getHigh()) {
        lastHigh = liquidityHighs.get(i).getIndex();
      } else {
        highPivotsToRemove.add(liquidityHighs.get(i));
      }
    }
    liquidityHighs.removeAll(highPivotsToRemove);

    ArrayList<PricePoint> lowPivotsToRemove = new ArrayList<>();
    for (int i = liquidityLows.size() - 1; i >= 0; i--) {
      if (lastLow == -1 || barList.get(liquidityLows.get(i).getIndex()).getLow() < barList.get(lastLow).getLow()) {
        lastLow = liquidityLows.get(i).getIndex();
      } else {
        lowPivotsToRemove.add(liquidityLows.get(i));
      }
    }
    liquidityLows.removeAll(lowPivotsToRemove);
  }

  public void combineCloseSwingPoints(List<IBar> barList, int numOfBarsBack) {
    combinedLiquidityHighs = CopyUtils.copyPricePointList(liquidityHighs);
    combinedLiquidityLows = CopyUtils.copyPricePointList(liquidityLows);
    IBar highestBar = barList.get(combinedLiquidityHighs.get(0).getIndex());
    IBar lowestBar = barList.get(combinedLiquidityLows.get(0).getIndex());
    double lowOfHighestBar = Math.min(highestBar.getOpen(), highestBar.getClose());
    double highOfLowestBar = Math.max(lowestBar.getOpen(), lowestBar.getClose());
    double lastHigh = highestBar.getHigh();
    double lastLow = lowestBar.getLow();

    LinkedList<PricePoint> highPivotsToRemove = new LinkedList<>();
    for (int i = 0; i < combinedLiquidityHighs.size(); i++) {
      IBar currentBar = barList.get(combinedLiquidityHighs.get(i).getIndex());
      if (currentBar.getHigh() > lowOfHighestBar && currentBar.getHigh() != lastHigh) {
        highPivotsToRemove.add(combinedLiquidityHighs.get(i));
      } else {
        IBar previousBar = barList.get(combinedLiquidityHighs.get(i).getIndex() - 1);
        lastHigh = currentBar.getHigh();
        lowOfHighestBar = Math.min(currentBar.getOpen(), currentBar.getClose());
        if (combinedLiquidityHighs.get(i).getIndex() > 0) {
          IBar nextBar = barList.get(combinedLiquidityHighs.get(i).getIndex() + 1);
          lowOfHighestBar = Math.max(lowOfHighestBar, Math.max(previousBar.getLow(), nextBar.getLow()));
          lowOfHighestBar = Math.max(lowOfHighestBar, Math.min(nextBar.getOpen(), nextBar.getClose()));
        }
        lowOfHighestBar = Math.max(lowOfHighestBar, Math.min(previousBar.getOpen(), previousBar.getClose()));
      }
    }
    combinedLiquidityHighs.removeAll(highPivotsToRemove);

    LinkedList<PricePoint> lowPivotsToRemove = new LinkedList<>();
    for (int i = 0; i < combinedLiquidityLows.size(); i++) {
      IBar currentBar = barList.get(combinedLiquidityLows.get(i).getIndex());
      if (currentBar.getLow() < highOfLowestBar && currentBar.getLow() != lastLow) {
        lowPivotsToRemove.add(combinedLiquidityLows.get(i));
      } else {
        IBar previousBar = barList.get(combinedLiquidityLows.get(i).getIndex() - 1);
        lastLow = currentBar.getHigh();
        highOfLowestBar = Math.max(currentBar.getOpen(), currentBar.getClose());
        if (combinedLiquidityLows.get(i).getIndex() > 0) {
          IBar nextBar = barList.get(combinedLiquidityLows.get(i).getIndex() + 1);
          highOfLowestBar = Math.min(highOfLowestBar, Math.min(previousBar.getHigh(), nextBar.getHigh()));
          highOfLowestBar = Math.min(highOfLowestBar, Math.max(nextBar.getOpen(), nextBar.getClose()));
        }
        highOfLowestBar = Math.min(highOfLowestBar, Math.max(previousBar.getOpen(), previousBar.getClose()));
      }
    }
    combinedLiquidityLows.removeAll(lowPivotsToRemove);
  }

  public void drawCombinedLiquidity(IChart chart, List<IBar> barList) {
    IChartObjectFactory factory = chart.getChartObjectFactory();
    //chart.removeAll();
    List<IChartObject> chartObjects = chart.getAll();
    for(IChartObject chartObject : chartObjects) {
      if(chartObject.getKey().matches("(?i)liquidity.*")) {
        chart.remove(chartObject);
      }
    }
    for (PricePoint low : combinedLiquidityLows) {
      chart.add(factory.createShortLine("liquidity_low_" + low, barList.get(low.getIndex()).getTime(), barList.get(low.getIndex()).getLow(), barList.get(barList.size() - 1).getTime(), barList.get(low.getIndex()).getLow()));
    }
    for (PricePoint high : combinedLiquidityHighs) {
      chart.add(factory.createShortLine("liquidity_high_" + high, barList.get(high.getIndex()).getTime(), barList.get(high.getIndex()).getHigh(), barList.get(barList.size() - 1).getTime(), barList.get(high.getIndex()).getHigh()));
    }
  }

  public void drawPivots(IChart chart, List<IBar> barList) {
    IChartObjectFactory factory = chart.getChartObjectFactory();
    chart.removeAll();
    for (PricePoint low : lowPivots) {
      chart.add(factory.createSignalUp(low + "low", barList.get(low.getIndex()).getTime(), barList.get(low.getIndex()).getLow()));
    }
    for (PricePoint high : highPivots) {
      chart.add(factory.createSignalDown(high + "high", barList.get(high.getIndex()).getTime(), barList.get(high.getIndex()).getHigh()));
    }
  }

  public void calculateAndDrawCombinedLiquidity(List<IBar> barList, int numOfBarsBack, IChart chart) {
    this.calculatePivots(barList, numOfBarsBack);
    this.calculateSwingPoints(barList, numOfBarsBack);
    this.combineCloseSwingPoints(barList, numOfBarsBack);
    this.drawCombinedLiquidity(chart, barList);
  }

  public boolean isHighLiquiditySwiped(double currentPrice) {
    boolean highLiquiditySwiped = false;
    for (PricePoint pricePoint : combinedLiquidityHighs) {
      if (currentPrice > pricePoint.getBar().getHigh()) {
        highLiquiditySwiped = true;
      }
    }
    return highLiquiditySwiped;
  }

  public boolean isLowLiquiditySwiped(double currentPrice) {
    boolean lowLiquiditySwiped = false;
    for (PricePoint pricePoint : combinedLiquidityLows) {
      if (currentPrice < pricePoint.getBar().getLow()) {
        lowLiquiditySwiped = true;
      }
    }
    return lowLiquiditySwiped;
  }

  public List<PricePoint> getCombinedLiquidityHighs() {
    return combinedLiquidityHighs;
  }

  public List<PricePoint> getCombinedLiquidityLows() {
    return combinedLiquidityLows;
  }
}
