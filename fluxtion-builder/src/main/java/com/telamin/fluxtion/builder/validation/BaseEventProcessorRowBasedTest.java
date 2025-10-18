/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */

package com.telamin.fluxtion.builder.validation;

import com.fluxtion.extension.csvcompiler.RowMarshaller;
import com.telamin.fluxtion.runtime.CloneableDataFlow;
import lombok.SneakyThrows;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.extensions.compactnotation.CompactConstructor;
import org.yaml.snakeyaml.extensions.compactnotation.PackageCompactConstructor;

import java.io.Reader;
import java.util.function.BiPredicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A base class users can extend to drive validation tests against an {@link CloneableDataFlow} from a stream of
 * {@link TestRowValidationRecord}'s
 *
 * <pre>
 *
 * &#64;Test
 * public void parseAndFailCsvTest() {
 *     try {
 *         validateSinkOutput(generateSampleParser(), "doubledOutput", ParseValidationRow.class,
 *                 "intIn,answerString\n" +
 *                         "2,doubled:4\n" +
 *                         "5,This will fail\n" +
 *                         "2,doubled:4");
 *         fail("Expected exception was not thrown");
 *     } catch (Throwable e) {
 *         assertNotNull(e);
 *     }
 * }
 *
 * private static CloneableDataFlow generateSampleParser() {
 *     return Fluxtion.interpret(c -> EventFlow.subscribe(String.class)
 *             .mapToInt(Integer::parseInt)
 *             .map(i -> i * 2)
 *             .mapToObj(i -> "doubled:" + i)
 *             .sink("doubledOutput"));
 * }
 *
 * &#64;CsvMarshaller(trim = true, acceptPartials = true)
 * public static class ParseValidationRow implements Supplier &lt;TestRowValidationRecord&lt;String&gt;&gt; {
 *     private String intIn;
 *     private String answerString;
 *
 *     public String getIntIn() {
 *         return intIn;
 *     }
 *
 *     public void setIntIn(String intIn) {
 *         this.intIn = intIn;
 *     }
 *
 *     public String getAnswerString() {
 *         return answerString;
 *     }
 *
 *     public void setAnswerString(String answerString) {
 *         this.answerString = answerString;
 *     }
 *
 *     public TestRowValidationRecord get() {
 *         return new TestRowValidationRecord(-1, intIn, answerString);
 *     }
 * }
 * </pre>
 */
public class BaseEventProcessorRowBasedTest {

    protected boolean useSyntheticTime = false;

    protected void useSyntheticTime() {
        useSyntheticTime = true;
    }

    protected void useWallClockTime() {
        useSyntheticTime = false;
    }

    /**
     * Validates using a {@link com.fluxtion.extension.csvcompiler.annotations.CsvMarshaller} annotated java bean as
     * the source of {@link TestRowValidationRecord} stream. The validation is an equality test
     *
     * @param processor          The {@link CloneableDataFlow} under test
     * @param sinkId             The id of sink in the test CloneableDataFlow that results are published to
     * @param validationRowClass The class that is annotated with a {@link com.fluxtion.extension.csvcompiler.annotations.CsvMarshaller} annotation
     * @param data               String data driving the test
     * @param <O>                Expected result data type
     * @param <S>                supplier of TestRowValidationRecord
     */
    protected <O, S extends Supplier<TestRowValidationRecord<O>>> void validateSinkOutput(
            CloneableDataFlow processor,
            String sinkId,
            Class<S> validationRowClass,
            String data) {
        validateSinkOutput(processor, sinkId, validationRowClass, null, data);
    }

    /**
     * Validates using a {@link com.fluxtion.extension.csvcompiler.annotations.CsvMarshaller} annotated java bean as
     * the source of {@link TestRowValidationRecord} stream. The validation is a user supplied {@link BiPredicate}
     *
     * @param processor          The {@link CloneableDataFlow} under test
     * @param sinkId             The id of sink in the test CloneableDataFlow that results are published to
     * @param validationRowClass The class that is annotated with a {@link com.fluxtion.extension.csvcompiler.annotations.CsvMarshaller} annotation
     * @param validator          validation predicate
     * @param data               String data driving the test
     * @param <O>                Expected result data type
     * @param <S>                supplier of TestRowValidationRecord
     */
    protected <O, S extends Supplier<TestRowValidationRecord<O>>> void validateSinkOutput(
            CloneableDataFlow processor,
            String sinkId,
            Class<S> validationRowClass,
            BiPredicate<O, O> validator,
            String data) {
        Stream<S> streamIn = RowMarshaller.load(validationRowClass).stream(data);
        new SinkValidatorDriver<>(processor, sinkId, streamIn, validator)
                .useSyntheticTime(useSyntheticTime)
                .validate();
    }

    protected <O, S extends Supplier<TestRowValidationRecord<O>>> void validateSinkOutput(
            CloneableDataFlow processor,
            String sinkId,
            Stream<S> streamIn) {
        validateSinkOutput(processor, sinkId, streamIn, null);
    }

    protected <O, S extends Supplier<TestRowValidationRecord<O>>> void validateSinkOutput(
            CloneableDataFlow processor,
            String sinkId,
            Stream<S> streamIn,
            BiPredicate<O, O> validator) {
        new SinkValidatorDriver<>(processor, sinkId, streamIn, validator)
                .useSyntheticTime(useSyntheticTime)
                .validate();
    }

    /**
     * Validates an {@link CloneableDataFlow} against a {@link Stream<TestRowValidationRecord>} source using a user supplied
     * BiPredicate.
     *
     * @param processor The {@link CloneableDataFlow} under test
     * @param streamIn  The stream of {@link TestRowValidationRecord} records to validate with
     * @param validator The user supplied validation test
     * @param <O>       Expected result data type
     * @param <S>       supplier of TestRowValidationRecord
     */
    protected <O, S extends Supplier<TestRowValidationRecord<O>>> void validateEventProcessor(
            CloneableDataFlow processor,
            Stream<S> streamIn,
            BiPredicate<CloneableDataFlow, O> validator) {
        new EventProcessorValidatorDriver<>(processor, streamIn, validator)
                .useSyntheticTime(useSyntheticTime)
                .validate();
    }

    /**
     * Converts a set of yaml docs to a stream of type T. Supply a package prefix to reduce the yaml size:
     * <pre>
     *  String data = """
     *         MyData(in=in_1, out=out_1)
     *         ---
     *         MyData(in=in_2, out=out_2)
     *         """;
     *  yamlToStream(data, "com.somecompany.somedepartment.someteam.withareallylongname")
     *      .forEach(System.out::println);
     * </pre>
     */
    @SneakyThrows
    public static <T> Stream<T> yamlToStream(String doc, String packagePrefix) {
        Yaml yaml = new Yaml(new PackageCompactConstructor(packagePrefix));
        Stream<Object> stream = StreamSupport.stream(yaml.loadAll(doc).spliterator(), false);
        @SuppressWarnings("unchecked") Stream<T> castStream = (Stream<T>) stream;
        return castStream;
    }

    public static <T> Stream<T> yamlToStream(String doc, Class<T> targetClass) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setTagInspector(tag -> true);
        Yaml yaml = new Yaml(new Constructor(targetClass, loaderOptions));
        Stream<Object> stream = StreamSupport.stream(yaml.loadAll(doc).spliterator(), false);
        @SuppressWarnings("unchecked") Stream<T> castStream = (Stream<T>) stream;
        return castStream;
    }

    public static <T> Stream<T> yamlToStream(Reader reader, Class<T> targetClass) {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setTagInspector(tag -> true);
        Yaml yaml = new Yaml(new Constructor(targetClass, loaderOptions));
        Stream<Object> stream = StreamSupport.stream(yaml.loadAll(reader).spliterator(), false);
        @SuppressWarnings("unchecked") Stream<T> castStream = (Stream<T>) stream;
        return castStream;
    }

    public static <T> Stream<T> yamlToStream(String doc) {
        Yaml yaml = new Yaml(new CompactConstructor());
        Stream<Object> stream = StreamSupport.stream(yaml.loadAll(doc).spliterator(), false);
        @SuppressWarnings("unchecked") Stream<T> castStream = (Stream<T>) stream;
        return castStream;
    }

}
