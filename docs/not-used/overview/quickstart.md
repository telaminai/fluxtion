---
title: 1 minute tutorial
parent: DataFlow
nav_order: 1
published: true
layout: default
---
# DataFlow developer quickstart
---

Quickstart tutorial to get developers up and running in 1 minute leveraging jbang. Calculates the average speed 
of s stream of cars grouped by manufacturer in a sliding window of 2 seconds with a 500 millisecond bucket size.

### 1.  Install jbang 
Open a new terminal or command shell to install jbang

Linux/OSX/Windows/AIX Bash:
{% highlight bash %} curl -Ls https://sh.jbang.dev | bash -s - app setup {% endhighlight %}
Windows Powershell:
{% highlight bash %} iex "& { $(iwr -useb https://ps.jbang.dev) } app setup" {% endhighlight %}
Close this terminal

### 2.  Copy the DataFlow java example into local file GroupByWindowExample.java
Open a new terminal or command shell to execute the example

Linux/OSX/Windows/AIX Bash:
{% highlight bash %} vi GroupByWindowExample.java {% endhighlight %}
Windows Powershell:
{% highlight bash %} notepad.exe GroupByWindowExample.java {% endhighlight %}

Copy the example into editor and save
{% highlight java %}
//REPOS repsy-fluxtion=https://repo.repsy.io/mvn/fluxtion/fluxtion-public
//DEPS com.fluxtion.dataflow:dataflow-builder:1.0.0
//COMPILE_OPTIONS -proc:full
//PREVIEW
//JAVA 21

import com.fluxtion.dataflow.builder.DataFlowBuilder;
import com.fluxtion.dataflow.runtime.DataFlow;
import com.fluxtion.dataflow.runtime.flowfunction.groupby.GroupBy;
import com.fluxtion.dataflow.runtime.flowfunction.helpers.Aggregates;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

record CarTracker(String make, double speed) { }
static String[] makes = new String[]{"BMW", "Ford", "Honda", "Jeep", "VW"};

//Calculates the average speed by manufacturer in a sliding window of 2 seconds with a 500 millisecond bucket size
public void main() {
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
{% endhighlight %}

### 3. Run the example with JBang
In the same terminal execute the example

{% highlight bash %}
jbang GroupByWindowExample.java
{% endhighlight %}

Console output: 

{% highlight bash %}
%> jbang GroupByWindowExample.java
[jbang] Resolving dependencies...
[jbang]    com.fluxtion.dataflow:dataflow-builder:1.0.0
[jbang] Dependencies resolved
[jbang] Building jar for GroupByWindowExample.java...
building DataFlow::avgSpeedByMake...
publishing events to DataFlow::avgSpeedByMake...

{VW=avgSpeed-92 km/h, Jeep=avgSpeed-70 km/h, Ford=avgSpeed-79 km/h, BMW=avgSpeed-42 km/h}
{Jeep=avgSpeed-70 km/h, BMW=avgSpeed-53 km/h}
{BMW=avgSpeed-54 km/h}
{VW=avgSpeed-68 km/h, BMW=avgSpeed-65 km/h}
{VW=avgSpeed-68 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-62 km/h}
{VW=avgSpeed-79 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-38 km/h}
{VW=avgSpeed-79 km/h, Jeep=avgSpeed-24 km/h, Ford=avgSpeed-15 km/h, BMW=avgSpeed-16 km/h}
{% endhighlight %}


# Description

**To be completed**