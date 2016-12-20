package com.ccentral4j;

class Counter {
  private int last;
  private int current;
  private long ts;

  public Counter() {
    last = 0;
    current = 0;
    ts = System.currentTimeMillis();
  }

  private void refresh() {
    while (ts + 60_000 < System.currentTimeMillis()) {
      last = current;
      current = 0;
      ts = ts + 60_000;
    }
  }

  public void increment(int amount) {
    refresh();
    current += amount;
  }

  public int getValue() {
    refresh();
    return last;
  }
}
