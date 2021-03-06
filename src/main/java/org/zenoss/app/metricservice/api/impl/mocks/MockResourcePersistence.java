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
package org.zenoss.app.metricservice.api.impl.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.zenoss.app.metricservice.api.impl.ResourcePersistenceAPI;
import org.zenoss.app.metricservice.api.impl.Utils;
import org.zenoss.app.metricservice.api.model.Chart;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;

@Configuration
@Profile({ "dev" })
public class MockResourcePersistence implements ResourcePersistenceAPI {
    private boolean connected = false;
    private List<String> list = new ArrayList<String>();
    private Map<String, String> idHash = new HashMap<String, String>();

    @Override
    public void connect(String prefix, String host) {
        connect(prefix, host, -1);
    }

    @Override
    public void connect(String prefix, String host, int port) {
        connected = true;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public void disconnect() {
        connected = false;
        list.clear();
        idHash.clear();
    }

    @Override
    public void ping() {
        // no op
    }

    @Override
    public String getResourceById(String id) {
        return idHash.get(id);
    }

    @Override
    public String getResourceByName(String name) {
        // Walk the list of charts and if we find one with the attribute
        // name and value specified
        ObjectMapper om = new ObjectMapper();
        ObjectReader reader = om.reader(Chart.class);
        Chart chart = null;

        for (String content : idHash.values()) {
            try {
                chart = reader.readValue(content);
                if (name.equals(chart.getName())) {
                    return content;
                }
            } catch (Throwable t) {
                throw new WebApplicationException(Utils.getErrorResponse(null,
                        500, "unable to read chart from persistence",
                        "persistence"));
            }
        }
        return null;
    }

    @Override
    public boolean delete(String id) {
        if (idHash.containsKey(id)) {
            idHash.remove(id);
            list.remove(id);
            return true;
        }
        return false;
    }

    @Override
    public boolean exists(String id) {
        return idHash.containsKey(id);
    }

    @Override
    public boolean add(String id, String content) {
        idHash.put(id, content);
        list.add(id);
        return true;
    }

    @Override
    public boolean update(String id, String content) {
        idHash.put(id, content);
        return true;
    }

    @Override
    public List<String> range(int start, int end) {
        if (start < 0 || end < 0) {
            return new ArrayList<String>();
        }
        if (end + 1 > list.size()) {
            return list.subList(start, list.size());
        }
        return list.subList(start, end + 1);
    }

    @Override
    public long count() {
        return list.size();
    }
}
