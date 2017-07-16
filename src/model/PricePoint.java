package model;

import com.dukascopy.api.IBar;

public class PricePoint {

  int index;

  IBar bar;

  public PricePoint(int index, IBar bar) {
    this.index = index;
    this.bar = bar;
  }

  public IBar getBar() {
    return bar;
  }

  public int getIndex() {
    return index;
  }

  @Override
  public boolean equals(Object v) {
    boolean retVal = false;
    if(v == null) return false;
    if(v == this) return true;
    if (v instanceof PricePoint){
      PricePoint that = (PricePoint) v;
      retVal = that.index == this.index;
    }
    return retVal;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 17 * hash * index;
    return hash;
  }
}
