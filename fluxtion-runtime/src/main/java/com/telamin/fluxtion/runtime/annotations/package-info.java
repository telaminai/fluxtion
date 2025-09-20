/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
/**
 * This package contains a set of annotations providing meta-data to the Fluxtion
 * static event compiler.<p>
 * <p>
 * Compiled user classes are inspected for annotations to generate meta-data.
 * Class
 * information, meta-data and configuration are all extracted by the Fluxtion
 * Static Event
 * Compiler (SEC). The SEC uses the combined information to generate a Static
 * Event
 * Processor (SEP)
 * .<p>
 * <p>
 * The annotations are only used at build time and are not used at runtime by
 * the
 * generated SEP.<p>
 * <p>
 * Annotations can be classified in the following categories:
 * <ul>
 * <li>Construction - used to bind nodes together in the execution graph
 * <li>Event propagation - determine how events are propagted to individual
 * nodes.
 * <li>Lifecycle - binding nodes to lifecycle phases in the SEP
 * <li>Generation config - Providing instructions to guide the Fluxtion Static
 * Event Compiler
 * </ul>
 * <p>
 * A SEP
 * processes event handling methods in two phases:
 * <ul>
 * <li>Event in phase - processes handler methods in topological order
 * <li>After event phase - processes handler methods in reverse topological
 * order
 * </ul>
 * <p>
 * The after event phase gives the node a chance to safely remove state after an event
 * cycle.
 */
package com.telamin.fluxtion.runtime.annotations;
