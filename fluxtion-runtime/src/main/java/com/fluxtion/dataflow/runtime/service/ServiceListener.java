/*
 * SPDX-File Copyright: © 2025.  Gregory Higgins <greg.higgins@v12technology.com>
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package com.fluxtion.dataflow.runtime.service;

import com.fluxtion.dataflow.runtime.annotations.feature.Preview;

/**
 * Export this service to receive notification callbacks when any services are registered or de-registered in the
 * container
 */

@Preview
public interface ServiceListener {

    void registerService(Service<?> service);

    void deRegisterService(Service<?> service);
}
