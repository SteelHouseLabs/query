/*
 * Copyright (c) 2013, Zenoss and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Zenoss or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.zenoss.app.metricservice.health;

import javax.ws.rs.WebApplicationException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.zenoss.app.metricservice.MetricServiceAppConfiguration;
import org.zenoss.app.metricservice.api.impl.ResourcePersistenceAPI;
import org.zenoss.dropwizardspring.annotations.HealthCheck;

@Configuration
@HealthCheck
public class ResourcePersistenceHealthCheck extends
        com.yammer.metrics.core.HealthCheck {
    @Autowired
    MetricServiceAppConfiguration config;

    @Autowired
    ResourcePersistenceAPI persistence;

    protected ResourcePersistenceHealthCheck() {
        super("Resource Persistence");
    }

    @Override
    protected Result check() throws Exception {
        try {
            String[] parts = config.getChartServiceConfig()
                    .getRedisConnection().split(":");
            if (parts.length == 1) {
                persistence.connect("UNKNOWN", parts[0]);
            } else if (parts.length == 2) {
                persistence.connect("UNKNOWN", parts[0],
                        Integer.parseInt(parts[1]));
            } else {
                return Result.unhealthy("Bad connection string");
            }

            persistence.ping();
            return Result.healthy();
        } catch (WebApplicationException wae) {
            return Result.unhealthy(wae);            
        } catch (Throwable t) {
            return Result.unhealthy(String.format("%s : %s", t.getClass()
                    .getName(), t.getMessage()));
        }
    }
}
