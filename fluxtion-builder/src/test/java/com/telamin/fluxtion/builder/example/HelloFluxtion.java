package com.telamin.fluxtion.builder.example;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;

public class HelloFluxtion {
    public static void main(String[] args) {
        DataFlow dataFlow = DataFlowBuilder
                .subscribe(String.class)           // accept String events
                .map(String::toUpperCase)          // transform
                .console("msg:{}")
                .build();// print to console

        dataFlow.onEvent("hello");  // prints: msg:HELLO
        dataFlow.onEvent("world");  // prints: msg:WORLD
    }
}