/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
/**
 * Contains classes and functions that are used to construct a Fluxtion Static
 * Event Processor (SEP). Builder functions are used to:
 * <ul>
 * <li>Describe the graph
 * <li>Add nodes
 * <li>Control access scope of nodes
 * <li>Name elements in the graph
 * <li>Factory management
 * <li>Injection points for extending build functions
 * </ul>
 *
 * <h2>Class space</h2>
 * The builder classes depends upon the api module, both modules are loaded by
 * Fluxtion generator at generation time. Building is a <b>compile time only</b>
 * operation. The builder module is not referenced by the generated SEP and
 * should not be distributed with the generated SEP.<p>
 * <p>
 * The goal is to reduce the dependencies of the final application to the
 * minimum possible. This will simplify integration, testing and deployment
 * reducing development costs. The generator and builder may have complex
 * dependencies but they are not required at runtime or in the application class
 * space.
 */
package com.telamin.fluxtion.builder;
