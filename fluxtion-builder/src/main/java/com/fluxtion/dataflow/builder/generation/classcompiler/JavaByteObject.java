/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.classcompiler;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class JavaByteObject extends SimpleJavaFileObject {
    private final ByteArrayOutputStream outputStream;

    protected JavaByteObject(String name) throws URISyntaxException {
        super(URI.create("bytes:///" + name + name.replaceAll("\\.", "/")), Kind.CLASS);
        outputStream = new ByteArrayOutputStream();
    }

    //overriding this to provide our OutputStream to which the
    // bytecode can be written.
    @Override
    public OutputStream openOutputStream() throws IOException {
        return outputStream;
    }

    public byte[] getBytes() {
        return outputStream.toByteArray();
    }
}