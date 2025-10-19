/*
 * Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com> - All Rights Reserved
 * This source code is protected under international copyright law.  All rights
 * reserved and protected by the copyright holders.
 * This file is confidential and only available to authorized individuals with the
 * permission of the copyright holders.  If you encounter this file and do not have
 * permission, please contact the copyright holders and delete this file.
 */
package com.telamin.fluxtion.builder.compile.generation.remote.common;

import java.io.Serializable;

public class RemoteGenerationResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String source;
    private String error;

    public static RemoteGenerationResponse ok(String source) {
        RemoteGenerationResponse r = new RemoteGenerationResponse();
        r.success = true;
        r.source = source;
        return r;
    }

    public static RemoteGenerationResponse error(String msg) {
        RemoteGenerationResponse r = new RemoteGenerationResponse();
        r.success = false;
        r.error = msg;
        return r;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
