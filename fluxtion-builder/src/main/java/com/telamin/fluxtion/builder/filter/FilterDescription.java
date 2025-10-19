/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.filter;

import com.telamin.fluxtion.runtime.event.Event;
import lombok.ToString;

import java.io.ObjectStreamException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Customises the generated source files to provide user controlled
 * {@link FilterDescription}'s. A user can provide logic to control comment and
 * variable names for filters in the generated code. The intention is to make
 * the generated SEP easier to understand reducing cost to rectify errors.<p>
 * <p>
 * {@link FilterDescriptionProducer} are registered as producers of descriptions.
 *
 * @author Greg Higgins
 */
@ToString
public class FilterDescription implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    public static final FilterDescription NO_FILTER = FilterDescription.buildNullFilter("NO_FILTER");
    public static final FilterDescription INVERSE_FILTER = FilterDescription.buildNullFilter("INVERSE_FILTER");
    public static final FilterDescription DEFAULT_FILTER = FilterDescription.buildNullFilter("DEFAULT");

    /**
     * Value used by the SEP to determine which decision branch to navigate. If
     * integer filtering is used.
     */
    public final int value;

    /**
     * Value used by the SEP to determine which decision branch to navigate. If
     * String filtering is used
     */
    public final String stringValue;

    private final String nullId;

    /**
     * boolean value indicating String or integer based filtering.
     */
    public final boolean isIntFilter;

    /**
     * Indicates presence of filtering, false value means match all values.
     */
    public final boolean isFiltered;

    /**
     * the event class for this filter.
     */
    private transient Class<? extends Event> eventClass;

    private String eventClassName;

    /**
     * Human readable comment to be associated with this filter in the generated
     * code of the SEP. Depending upon the target language this value may be
     * mutated to suit the target language rules.
     */
    public String comment;

    /**
     * User suggested identifier for this filter in the generated SEP code.
     * Depending upon the target language this value may be mutated to suit the
     * relevant rules.
     */
    public String variableName;

    private transient Method exportFunction;

    // Serializable copy of export function signature for code generation equivalence after serialization
    public String exportFunctionSignature;

    public FilterDescription(String eventClass) {
        this.value = 0;
        this.eventClass = null;
        this.stringValue = "";
        this.isIntFilter = true;
        this.isFiltered = false;
        this.nullId = "";
        this.eventClassName = eventClass;
    }

    public static FilterDescription build(Object input) {
        FilterDescription result = DEFAULT_FILTER;
        if (input instanceof Event) {
            Event event = (Event) input;
            if (event.filterId() != Integer.MAX_VALUE) {
                result = new FilterDescription(event.getClass(), event.filterId());
            } else if (event.filterString() != null && !event.filterString().isEmpty()) {
                result = new FilterDescription(event.getClass(), event.filterString());
            } else {
                result = new FilterDescription(event.getClass());
            }
        }
        return result;
    }

    public static FilterDescription buildNullFilter(String nullValue) {
        return new FilterDescription(null, "", 0, nullValue, false, true);
    }

    public FilterDescription(Class<? extends Event> eventClass) {
        this.value = 0;
        this.eventClass = eventClass;
        this.stringValue = "";
        this.isIntFilter = true;
        this.isFiltered = false;
        this.nullId = "";
        this.eventClassName = eventClass == null ? null : eventClass.getName();
    }

    public FilterDescription(Class<? extends Event> eventClass, int value) {
        this.value = value;
        this.eventClass = eventClass;
        this.stringValue = "";
        this.isIntFilter = true;
        this.isFiltered = true;
        this.nullId = "";
        this.eventClassName = eventClass == null ? null : eventClass.getName();
    }

    public FilterDescription(Class<? extends Event> eventClass, String value) {
        this.stringValue = value;
        this.eventClass = eventClass;
        this.isIntFilter = false;
        this.isFiltered = true;
        this.value = 0;
        this.nullId = "";
        this.eventClassName = eventClass == null ? null : eventClass.getName();
    }

    public FilterDescription(
            Class<? extends Event> eventClass,
            String stringValue,
            int value,
            String nullId,
            boolean isIntFilter,
            boolean isFiltered
    ) {
        this.value = value;
        this.stringValue = stringValue;
        this.nullId = nullId;
        this.isIntFilter = isIntFilter;
        this.isFiltered = isFiltered;
        this.eventClass = eventClass;
        this.eventClassName = eventClass == null ? null : eventClass.getName();
    }

    public FilterDescription changeClass(Class<? extends Event> newClass) {
        FilterDescription fd = new FilterDescription(newClass, stringValue);
        if (!isFiltered) {
            fd = new FilterDescription(newClass);
        } else if (isIntFilter) {
            fd = new FilterDescription(newClass, value);
        } else if (this == NO_FILTER) {
            return NO_FILTER;
        } else if (this == INVERSE_FILTER) {
            return INVERSE_FILTER;
        } else if (this == DEFAULT_FILTER) {
            return DEFAULT_FILTER;
        }
        return fd;
    }

    public int getValue() {
        return value;
    }

    public String getStringValue() {
        return stringValue;
    }

    public String getNullId() {
        return nullId;
    }

    public boolean isIntFilter() {
        return isIntFilter;
    }

    public boolean isFiltered() {
        return isFiltered;
    }

    public Class<? extends Event> getEventClass() {
        return eventClass;
    }

    public String getEventClassName() {
        return eventClassName;
    }

    public String getComment() {
        return comment;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setEventClass(Class<? extends Event> eventClass) {
        this.eventClass = eventClass;
        this.eventClassName = eventClass == null ? null : eventClass.getName();
    }

    public void setExportFunction(Method exportFunction) {
        this.exportFunction = exportFunction;
        this.exportFunctionSignature = exportFunction == null ? null : exportFunction.toGenericString();
    }

    public Method getExportFunction() {
        return exportFunction;
    }

    public String getExportFunctionSignature() {
        return exportFunctionSignature;
    }

    private Object readResolve() throws ObjectStreamException {
        if ("DEFAULT".equals(nullId)) {
            return DEFAULT_FILTER;
        }
        if ("NO_FILTER".equals(nullId)) {
            return NO_FILTER;
        }
        if ("INVERSE_FILTER".equals(nullId)) {
            return INVERSE_FILTER;
        }
        return this;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        if (isIntFilter) {
            hash = 89 * hash + this.value;
        } else {
            hash = 89 * hash + Objects.hashCode(this.stringValue);
        }
        hash = 89 * hash + (this.isIntFilter ? 1 : 0);
        hash = 89 * hash + Objects.hashCode(this.eventClass);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FilterDescription other = (FilterDescription) obj;
        if (isIntFilter && this.value != other.value) {
            return false;
        }
        if (!isIntFilter && !Objects.equals(this.stringValue, other.stringValue)) {
            return false;
        }
        if (this.isIntFilter != other.isIntFilter) {
            return false;
        }
        if (!Objects.equals(this.nullId, other.nullId)) {
            return false;
        }
        return Objects.equals(this.eventClassName, other.eventClassName);
    }
}