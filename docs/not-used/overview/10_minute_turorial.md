---
title: 10 minute tutorial
parent: DataFlow
nav_order: 40
published: true
layout: default
---
# DataFlow agentic 10 minute tutorial
---

A 10 minute tutorial demonstrating agentic DataFlow integration. Jbang is used to minimise setup for quick start,  
see [1 minute tutorial](quickstart) for jbang installation help. In this tutorial we cover:
- Listening to multiple event feeds each with a different type
- Grouping DataFlows into map structures
- Merging and joining DataFlows
- Applying map and filter functions
- Integrating user created stateful agents
- Controlling external systems with user agents

## Run from github
To run the example directly from github without installation uses jbang trusted remote execution. **You have to approve
remote code does not run automatically**
### 1. Open terminal window
### 2. Execute GitHub example with jbang:
{% highlight shell %}
jbang https://github.com/telaminai/dataflow-examples/blob/main/getstarted/src/main/java/com/fluxtion/dataflow/examples/frontpage/multijoin/MultiFeedJoinExample.java
{% endhighlight %}

Console output:

{% highlight bash %}
[jbang] Resolving dependencies...
[jbang]    com.fluxtion.dataflow:dataflow-builder:1.0.0
[jbang]    org.projectlombok:lombok:1.18.36
[jbang] Dependencies resolved
[jbang] Building jar for MultiFeedJoinExample.java...
Application started - wait four seconds for first machine readings

ALARM UPDATE 07:29:24.129
New alarms: ['server_GOOG@USA_EAST_1',  temp:'41.81', avgTemp:'49.99' SupportContactEvent[name=Jean, locationCode=USA_EAST_1, contactDetails=jean@fluxtion.com], 'server_TKM@USA_EAST_2',  temp:'13.02', avgTemp:'50.06' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com], 'server_MSFT@USA_EAST_2',  temp:'65.99', avgTemp:'50.04' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com]]
Alarms to clear[]
Current alarms[server_GOOG, server_TKM, server_MSFT]
------------------------------------

ALARM UPDATE 07:29:26.116
New alarms: []
Alarms to clear[server_MSFT]
Current alarms[server_GOOG, server_TKM]
------------------------------------

ALARM UPDATE 07:29:27.116
New alarms: ['server_MSFT@USA_EAST_2',  temp:'72.93', avgTemp:'50.01' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com]]
Alarms to clear[]
Current alarms[server_GOOG, server_TKM, server_MSFT]
------------------------------------

ALARM UPDATE 07:29:29.116
New alarms: []
Alarms to clear[server_MSFT]
Current alarms[server_GOOG, server_TKM]
------------------------------------

ALARM UPDATE 07:29:30.116
New alarms: ['server_MSFT@USA_EAST_2',  temp:'34.17', avgTemp:'50.05' SupportContactEvent[name=Tandy, locationCode=USA_EAST_2, contactDetails=tandy@fluxtion.com]]
Alarms to clear[]
Current alarms[server_GOOG, server_TKM, server_MSFT]
------------------------------------

{% endhighlight %}

## Develop locally

### 1.  Copy the DataFlow java example into local file MultiFeedJoinExample.java
Open a new terminal or command shell to execute the example

Linux/OSX/Windows/AIX Bash:
{% highlight bash %} vi MultiFeedJoinExample.java {% endhighlight %}
Windows Powershell:
{% highlight bash %} notepad.exe MultiFeedJoinExample.java {% endhighlight %}

Copy the example into editor and save
{% highlight java %}

public class MultiFeedJoinExample {
    public static void main(String[] args) {
        //stream of realtime machine temperatures grouped by machineId
        DataFlow currentMachineTemp = DataFlowBuilder.groupBy(
                MachineReadingEvent::id, MachineReadingEvent::temp);
        //create a stream of averaged machine sliding temps,
        //with a 4-second window and 1 second buckets grouped by machine id
        DataFlow avgMachineTemp = DataFlowBuilder.subscribe(MachineReadingEvent.class)
                .groupBySliding(
                        MachineReadingEvent::id,
                        MachineReadingEvent::temp,
                        DoubleAverageFlowFunction::new,
                        1000,
                        4);
        //join machine profiles with contacts and then with readings.
        //Publish alarms with stateful user function
        DataFlow tempMonitor = DataFlowBuilder.groupBy(MachineProfileEvent::id)
                .mapValues(MachineState::new)
                .mapBi(
                        DataFlowBuilder.groupBy(SupportContactEvent::locationCode),
                        Helpers::addContact)
                .innerJoin(currentMachineTemp, MachineState::setCurrentTemperature)
                .innerJoin(avgMachineTemp, MachineState::setAvgTemperature)
                .publishTriggerOverride(FixedRateTrigger.atMillis(1_000))
                .filterValues(MachineState::outsideOperatingTemp)
                .map(GroupBy::toMap)
                .map(new AlarmDeltaFilter()::updateActiveAlarms)
                .filter(AlarmDeltaFilter::isChanged)
                .sink("alarmPublisher")
                .build();

        runSimulation(tempMonitor);
    }

    private static void runSimulation(DataFlow tempMonitor) {
        //any java.util.Consumer can be used as sink
        tempMonitor.addSink("alarmPublisher", Helpers::prettyPrintAlarms);

        //set up machine locations
        tempMonitor.onEvent(new MachineProfileEvent("server_GOOG", LocationCode.USA_EAST_1, 70, 48));
        tempMonitor.onEvent(new MachineProfileEvent("server_AMZN", LocationCode.USA_EAST_1, 99.999, 65));
        tempMonitor.onEvent(new MachineProfileEvent("server_MSFT", LocationCode.USA_EAST_2,92, 49.99));
        tempMonitor.onEvent(new MachineProfileEvent("server_TKM", LocationCode.USA_EAST_2,102, 50.0001));

        //set up support contacts
        tempMonitor.onEvent(new SupportContactEvent("Jean", LocationCode.USA_EAST_1, "jean@fluxtion.com"));
        tempMonitor.onEvent(new SupportContactEvent("Tandy", LocationCode.USA_EAST_2, "tandy@fluxtion.com"));

        //Send random MachineReadingEvent using `DataFlow.onEvent` 
        Random random = new Random();
        final String[] MACHINE_IDS = new String[]{"server_GOOG", "server_AMZN", "server_MSFT", "server_TKM"};
        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
                    String machineId = MACHINE_IDS[random.nextInt(MACHINE_IDS.length)];
                    double temperatureReading = random.nextDouble() * 100;
                    tempMonitor.onEvent(new MachineReadingEvent(machineId, temperatureReading));
                },
                10_000, 1, TimeUnit.MICROSECONDS);

        System.out.println("Simulation started - wait four seconds for first machine readings\n");
    }
}

{% endhighlight %}

### 2. Run the example with JBang
In the same terminal execute the example

{% highlight bash %}
jbang MultiFeedJoinExample.java
{% endhighlight %}

# Description

**To be completed**