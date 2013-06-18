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
package org.zenoss.app.query.api.query.impl;

import java.lang.reflect.Constructor;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.zenoss.app.annotations.API;
import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricSpecification;
import org.zenoss.app.query.api.PerformanceMetricQueryAPI;
import org.zenoss.app.query.api.PerformanceQueryResponse;

import com.google.common.base.Optional;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
@API
public class PerformanceMetricQueryAPIImpl implements PerformanceMetricQueryAPI {
	@Autowired
	QueryAppConfiguration config;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.zenoss.app.query.api.PerformanceMetricQueryAPI#query(java.lang.String
	 * , com.google.common.base.Optional, com.google.common.base.Optional,
	 * com.google.common.base.Optional, com.google.common.base.Optional,
	 * com.google.common.base.Optional)
	 */
	@Override
	public PerformanceQueryResponse query(Optional<String> id,
			Optional<String> startTime, Optional<String> endTime,
			Optional<String> tz, Optional<Boolean> exactTimeWindow,
			List<MetricSpecification> queries) {
		try {
			Class<?> clazz = Class.forName(config
					.getPerformanceMetricQueryConfig().getProvider());
			Constructor<?> constructor = clazz
					.getConstructor(QueryAppConfiguration.class);
			PerformanceMetricQueryAPI api = (PerformanceMetricQueryAPI) constructor
					.newInstance(config);
			return api.query(id, startTime, endTime, tz, exactTimeWindow,
					queries);
		} catch (Throwable t) {
			t.printStackTrace();
		}
		return null;
	}
}