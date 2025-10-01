# GroupBy join support
--- 

Fluxtion supports join operations for groupBy data flow nodes.

### Inner join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.innerJoin(schools, pupils)`

The value type of the joined GroupBy is a Tuple, the first value is the left join and the second value is the right join.
The utility static method in [Tuples]({{fluxtion_src_runtime}}/dataflow/helpers/Tuples.java)

`Tuples.mapTuple`

Is used to map the School, Pupil Tuple into a pretty print String.

See sample - [GroupByJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByJoinSample.java)

```java
public class GroupByJoinSample {
    public record Pupil(int year, String school, String name) {}
    public record School(String name) {}

    public static void main(String[] args) {
        var pupils = DataFlowBuilder.subscribe(Pupil.class).groupByToList(Pupil::school);
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);

        DataFlow processor = JoinFlowBuilder
                .innerJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));
        processor.onEvent(new Pupil(2013, "RGS", "Chelsea"));
        processor.onEvent(new Pupil(2013, "Belles", "Tamsin"));
        processor.onEvent(new Pupil(2013, "Belles", "Ayola"));
        processor.onEvent(new Pupil(2015, "Belles", "Sunita"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
{RGS=pupils[Bob]}
{RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin,Ayola], RGS=pupils[Bob,Ashkay,Chelsea]}
{Belles=pupils[Channing,Tamsin,Ayola,Sunita], RGS=pupils[Bob,Ashkay,Chelsea]}
```

### Left outer join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.leftJoin(schools, pupils)`

A default value of an empty collection is assigned to the pupil groupBy so the first school can join against a non-null
value.

See sample - [GroupByLeftOuterJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByLeftOuterJoinSample.java)

```java
public class GroupByLeftOuterJoinSample {
    public record Pupil(int year, String school, String name) {}

    public record School(String name) {}

    public static void main(String[] args) {
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);
        
        var pupils = DataFlowBuilder
                .subscribe(Pupil.class)
                .groupByToList(Pupil::school)
                .defaultValue(GroupBy.emptyCollection());

        DataFlow processor = JoinFlowBuilder
                .leftJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByLeftOuterJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));
        processor.onEvent(new Pupil(2015, "Belles", "Sunita"));

        System.out.println("left outer join\n");
        //left outer
        processor.onEvent(new School("Framling"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        pupils = pupils == null ? Collections.emptyList() : pupils;
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
{RGS=pupils[]}
{Belles=pupils[], RGS=pupils[]}
{Belles=pupils[], RGS=pupils[Bob]}
{Belles=pupils[], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing,Sunita], RGS=pupils[Bob,Ashkay]}
left outer join

{Belles=pupils[Channing,Sunita], RGS=pupils[Bob,Ashkay], Framling=pupils[]}
```

### right outer join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.rightJoin(schools, pupils)`

A default value of an empty collection is assigned to the pupil groupBy so the first school can join against a non-null
value.

See sample - [GroupByRightOuterJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByRightOuterJoinSample.java)

```java
public class GroupByRightOuterJoinSample {
    public record Pupil(int year, String school, String name) {}

    public record School(String name) {}

    public static void main(String[] args) {
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);
        
        var pupils = DataFlowBuilder
                .subscribe(Pupil.class)
                .groupByToList(Pupil::school);

        DataFlow processor = JoinFlowBuilder
                .rightJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByRightOuterJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));

        System.out.println("right outer join\n");
        //left outer
        processor.onEvent(new Pupil(2015, "Framling", "Sunita"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        pupils = pupils == null ? Collections.emptyList() : pupils;
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
{RGS=pupils[Bob]}
{RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
right outer join

{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay], Framling=pupils[Sunita]}
```


###Full outer join
Joins are create with the data flow node of a group by or using the [JoinFlowBuilder]({{fluxtion_src_compiler}}/builder/dataflow/JoinFlowBuilder.java)

`JoinFlowBuilder.outerJoin(schools, pupils)`

A default value of an empty collection is assigned to the pupil groupBy so the first school can join against a non-null
value.

See sample - [GroupByFullOuterJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/GroupByFullOuterJoinSample.java)

```java
public class GroupByFullOuterJoinSample {

    public record Pupil(int year, String school, String name) {}

    public record School(String name) {}

    public static void main(String[] args) {
        var schools = DataFlowBuilder.subscribe(School.class).groupBy(School::name);
        
        var pupils = DataFlowBuilder
                .subscribe(Pupil.class)
                .groupByToList(Pupil::school);

        DataFlow processor = JoinFlowBuilder
                .outerJoin(schools, pupils)
                .mapValues(Tuples.mapTuple(GroupByFullOuterJoinSample::prettyPrint))
                .map(GroupBy::toMap)
                .console()
                .build();

        //register some schools
        processor.onEvent(new School("RGS"));
        processor.onEvent(new School("Belles"));

        //register some pupils
        processor.onEvent(new Pupil(2015, "RGS", "Bob"));
        processor.onEvent(new Pupil(2013, "RGS", "Ashkay"));
        processor.onEvent(new Pupil(2013, "Belles", "Channing"));

        System.out.println("full outer join\n");
        //full outer
        processor.onEvent(new Pupil(2015, "Framling", "Sunita"));
        processor.onEvent(new School("St trinians"));
    }

    private static String prettyPrint(School schoolName, List<Pupil> pupils) {
        pupils = pupils == null ? Collections.emptyList() : pupils;
        return pupils.stream().map(Pupil::name).collect(Collectors.joining(",", "pupils[", "]"));
    }
}
```

Running the example code above logs to console

```console
07-May-24 21:31:33 [main] INFO GenerationContext - classloader:jdk.internal.loader.ClassLoaders$AppClassLoader@4e0e2f2a
{Belles=pupils[], RGS=pupils[Bob]}
{Belles=pupils[], RGS=pupils[Bob,Ashkay]}
{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay]}
full outer join

{Belles=pupils[Channing], RGS=pupils[Bob,Ashkay], Framling=pupils[Sunita]}
{Belles=pupils[Channing], St trinians=pupils[], RGS=pupils[Bob,Ashkay], Framling=pupils[Sunita]}
```

###Multi join or Co-group

Multi leg joins are supported with no limitation on the number of joins, The [MultiJoinBuilder]({{fluxtion_src_compiler}}/builder/dataflow/MultiJoinBuilder.java)
is used to construct a multi leg join with a builder style pattern

`MultiJoinBuilder.builder(Class<K> keyClass, Supplier<T> target`

Legs are joined on a common key class results are sent to target class. Each join is added from a flow and pushed into
the target class by specifying the consumer method on the target instance.

`[multijoinbuilder].addJoin(GroupByFlowBuilder<K2, B> flow, BiConsumer<T, B> setter)`

An optional join can be specified. The optional will be null in the target instance until a key match is found

`[multijoinbuilder].addOptionalJoin(GroupByFlowBuilder<K2, B> flow, BiConsumer<T, B> setter)`

The GroupBy data flow is created by calling

`[multijoinbuilder].dataFlow()`

The example joins four groupBy data flows for a person, using the String name as a key. When a matching join is found
individual item are set on MergedData instance. Dependents are an optional requirement for the join, so is not required
to publish a MergedData record to the flow. 

The MergedData instance is added to the GroupBy data flow keyed by name. The multi join data flow can be operated on 
as any normal flow, in this case we are mapping the value with a 
pretty printing function.

See sample - [MultiJoinSample.java]({{fluxtion_example_src}}/reference/src/main/java/com/telamin/fluxtion/example/reference/groupby/MultiJoinSample.java)

```java
public class MultiJoinSample {

    public static void main(String[] args) {
        var ageDataFlow = DataFlowBuilder.groupBy(Age::getName);
        var genderDataFlow = DataFlowBuilder.groupBy(Gender::getName);
        var nationalityDataFlow = DataFlowBuilder.groupBy(Nationality::getName);
        var dependentDataFlow = DataFlowBuilder.groupByToList(Dependent::getGuardianName);

        DataFlow processor = MultiJoinBuilder
                .builder(String.class, MergedData::new)
                .addJoin(ageDataFlow, MergedData::setAge)
                .addJoin(genderDataFlow, MergedData::setGender)
                .addJoin(nationalityDataFlow, MergedData::setNationality)
                .addOptionalJoin(dependentDataFlow, MergedData::setDependent)
                .dataFlow()
                .mapValues(MergedData::formattedString)
                .map(GroupBy::toMap)
                .console("multi join result : {}")
                .build();


        processor.onEvent(new Age("greg", 47));
        processor.onEvent(new Gender("greg", "male"));
        processor.onEvent(new Nationality("greg", "UK"));
        //update
        processor.onEvent(new Age("greg", 55));
        //new record
        processor.onEvent(new Age("tim", 47));
        processor.onEvent(new Gender("tim", "male"));
        processor.onEvent(new Nationality("tim", "UK"));

        processor.onEvent(new Dependent("greg", "ajay"));
        processor.onEvent(new Dependent("greg", "sammy"));

    }

    @Data
    public static class MergedData {
        private Age age;
        private Gender gender;
        private Nationality nationality;
        private List<Dependent> dependent;

        public String formattedString() {
            String dependentString = " no dependents";
            if (dependent != null) {
                dependentString = dependent.stream()
                        .map(Dependent::getDependentName)
                        .collect(Collectors.joining(", ", " guardian for: [", "]"));
            }
            return age.getAge() + " " + gender.getSex() + " " + nationality.getCountry() + dependentString;
        }
    }

    @Value
    public static class Age {
        String name;
        int age;
    }

    @Value
    public static class Gender {
        String name;
        String sex;
    }


    @Value
    public static class Nationality {
        String name;
        String country;
    }

    @Value
    public static class Dependent {
        String guardianName;
        String dependentName;
    }
}
```

Running the example code above logs to console

```console
multi join result : {greg=47 male UK no dependents}
multi join result : {greg=55 male UK no dependents}
multi join result : {tim=47 male UK no dependents, greg=55 male UK no dependents}
multi join result : {tim=47 male UK no dependents, greg=55 male UK guardian for: [ajay]}
multi join result : {tim=47 male UK no dependents, greg=55 male UK guardian for: [ajay, sammy]}
```
