package com.telamin.fluxtion.builder.example;

import com.telamin.fluxtion.builder.DataFlowBuilder;
import com.telamin.fluxtion.runtime.DataFlow;
import com.telamin.fluxtion.runtime.flowfunction.groupby.GroupBy;
import com.telamin.fluxtion.runtime.flowfunction.helpers.Aggregates;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class GroupByExample {

    record CarTracker(String make, double speed) { }
    static String[] makes = new String[]{"BMW", "Ford", "Honda", "Jeep", "VW"};

    //Calculates the average speed by manufacturer in a sliding window of 2 seconds with a 500 millisecond bucket size
    public static void main(String[] args) {
        System.out.println("building DataFlow::avgSpeedByMake...");
        //build the DataFlow
        DataFlow avgSpeedByMake = DataFlowBuilder.subscribe(CarTracker.class)
                .groupBySliding(
                        CarTracker::make, //key
                        CarTracker::speed, //value
                        Aggregates.doubleAverageFactory(), //avg function per bucket
                        500, 4) //4 buckets 500 millis each
                .mapValues(v -> "avgSpeed-" + v.intValue() + " km/h")
                .map(GroupBy::toMap)
                .sink("average car speed")
                .build();

        //register an output sink with the DataFlow
        avgSpeedByMake.addSink("average car speed", System.out::println);

        //send data from an unbounded real-time feed to the DataFlow
        System.out.println("publishing events to DataFlow::avgSpeedByMake...\n");
        Random random = new Random();
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> avgSpeedByMake.onEvent(new CarTracker(makes[random.nextInt(makes.length)], random.nextDouble(100))),
                100, 400, TimeUnit.MILLISECONDS);
    }
}
