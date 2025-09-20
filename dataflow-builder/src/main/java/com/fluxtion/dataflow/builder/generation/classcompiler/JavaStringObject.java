/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: SSPL-3.0-only
 */

package com.fluxtion.dataflow.builder.generation.classcompiler;

import javax.tools.SimpleJavaFileObject;
import java.io.IOException;
import java.net.URI;

public class JavaStringObject extends SimpleJavaFileObject {
    private final String source;

    protected JavaStringObject(String name, String source) {
        super(URI.create("string:///" + name.replaceAll("\\.", "/") +
                Kind.SOURCE.extension), Kind.SOURCE);
        this.source = source;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
        return source;
    }
}