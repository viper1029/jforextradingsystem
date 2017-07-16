package utils;

import model.PricePoint;

import java.util.LinkedList;
import java.util.List;

public class CopyUtils {

  public static List<PricePoint> copyPricePointList(List<PricePoint> pricePoints) {
    List<PricePoint> copy = new LinkedList<>();
    for(PricePoint pricePoint : pricePoints) {
      copy.add(new PricePoint(pricePoint.getIndex(), pricePoint.getBar()));
    }
    return copy;
  }

}
