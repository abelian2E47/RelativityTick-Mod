package com.abelian;


public interface RebindableBlockEntityTickInvoker {

    boolean relativityTick$rebindNeeded();
    void relativityTick$markRebound();
    boolean relativityTick$isSleeping();
}
