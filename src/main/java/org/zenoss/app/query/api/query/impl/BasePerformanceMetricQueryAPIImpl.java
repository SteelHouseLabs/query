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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.zenoss.app.query.QueryAppConfiguration;
import org.zenoss.app.query.api.MetricQuery;
import org.zenoss.app.query.api.PerformanceMetricQueryAPI;

import com.google.common.base.Optional;

/**
 * @author David Bainbridge <dbainbridge@zenoss.com>
 * 
 */
public abstract class BasePerformanceMetricQueryAPIImpl implements
		PerformanceMetricQueryAPI {
	protected static final String CLIENT_ID = "clientId";
	protected static final String SOURCE = "source";
	protected static final String START_TIME = "startTime";
	protected static final String END_TIME = "endTime";
	protected static final String TIME_ZONE = "timeZone";
	protected static final String RESULTS = "results";
	protected static final String DATAPOINTS = "datapoints";
	protected static final String AGGREGATOR = "aggregator";
	protected static final String RATE = "rate";
	protected static final String DOWNSAMPLE = "downsample";
	protected static final String METRIC = "metric";
	protected static final String EXACT_TIME_WINDOW = "exactTimeWindow";
	protected static final String TIMESTAMP = "timestamp";
	protected static final String SERIES = "series";
	protected static final String VALUE = "value";
	protected static final String TAGS = "tags";

	protected static final String NOT_SPECIFIED = "not-specified";
	protected static final String NOW = "now";

	protected abstract BufferedReader getReader(QueryAppConfiguration config,
			String id, String startTime, String endTime, String tz,
			Boolean exactTimeWindow, Boolean series, List<MetricQuery> queries)
			throws IOException;

	protected abstract QueryAppConfiguration getConfiguration();
	
	protected abstract String getSourceId();

	private class Worker implements StreamingOutput {

		private final String id;
		private final String startTime;
		private final String endTime;
		private final String tz;
		private final Boolean exactTimeWindow;
		private final Boolean series;
		private final List<MetricQuery> queries;

		public Worker(QueryAppConfiguration config, String id,
				String startTime, String endTime, String tz,
				Boolean exactTimeWindow, Boolean series,
				List<MetricQuery> queries) {
			this.id = id;
			this.startTime = startTime;
			this.endTime = endTime;
			this.tz = tz;
			this.exactTimeWindow = exactTimeWindow;
			this.series = series;
			this.queries = queries;
		}


		private void writeAsSeries(JsonWriter writer, BufferedReader reader)
				throws NumberFormatException, IOException {
			long t = -1;
			String line = null;
			long ts = 0;
			double val = 0;
			while ((line = reader.readLine()) != null) {
				String terms[] = line.split(" ", 4);

				// Check the timestamp and if we went backwards in time that
				// means that we are onto the next query.
				ts = Long.valueOf(terms[1]);
				if (t == -1 || ts < t) {
					if (t != -1) {
						writer.arrayE().objectE(true);
					}
					writer.objectS().value(METRIC, terms[0], true);

					// every entry in this series should have the same
					// tags, so output them once, by just using the tags
					// from the first entry
					if (terms.length > 3) {
						// The result has tags
						writer.objectS(TAGS);
						int eq = -1;
						for (String tag : terms[3].split(" ")) {
							// Bit of a hack here. We are using the fact
							// that only on the first trip through this loop
							// eq == -1 as an indicator that on before every
							// value except the first we need to add a ','
							if (eq != -1) {
								writer.write(',');
							}
							eq = tag.indexOf('=');
							writer.value(tag.substring(0, eq),
									tag.substring(eq + 1));
						}
						writer.objectE(true);
					}

					writer.arrayS(DATAPOINTS);
				} else {
					writer.write(',');
				}
				t = ts;
				val = Double.valueOf(terms[2]);
				writer.objectS().value(TIMESTAMP, ts, true)
						.value(VALUE, val, false).objectE();
			}
			writer.arrayE().objectE(); // end the last query
		}

		private void writeAsIs(JsonWriter writer, BufferedReader reader)
				throws IOException {
			String line = null;
			long ts = -1;
			double val = 0;
			while ((line = reader.readLine()) != null) {
				String terms[] = line.split(" ", 4);

				if (ts != -1) {
					writer.write(',');
				}

				// Check the timestamp and if we went backwards in time that
				// means that we are onto the next query.
				ts = Long.valueOf(terms[1]);
				val = Double.valueOf(terms[2]);
				writer.objectS().value(METRIC, terms[0], true)
						.value(TIMESTAMP, ts, true)
						.value(VALUE, val, terms.length > 3);

				if (terms.length > 3) {
					// The result has tags
					writer.objectS(TAGS);
					int eq = -1;
					for (String tag : terms[3].split(" ")) {
						// Bit of a hack here. We are using the fact that
						// only on the first trip through this loop eq == -1
						// as an indicator that on before every value except
						// the first we need to add a ','
						if (eq != -1) {
							writer.write(',');
						}
						eq = tag.indexOf('=');
						writer.value(tag.substring(0, eq),
								tag.substring(eq + 1));
					}
					writer.objectE();
				}
				writer.objectE();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.ws.rs.core.StreamingOutput#write(java.io.OutputStream)
		 */
		@Override
		public void write(OutputStream output) throws IOException,
				WebApplicationException {
			try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(
					output))) {

				BufferedReader reader = getReader(getConfiguration(), id, startTime,
						endTime, tz, exactTimeWindow, series, queries);
				// URL url = new URL(buildUrl());
				// BufferedReader reader = new BufferedReader(
				// new InputStreamReader(url.openStream()));

				writer.objectS().value(CLIENT_ID, id, true)
						.value(SOURCE, getSourceId(), true)
						.value(START_TIME, startTime, true)
						.value(END_TIME, endTime, true)
						.value(TIME_ZONE, tz, true)
						.value(EXACT_TIME_WINDOW, exactTimeWindow, true)
						.value(SERIES, series, true)
						.arrayS(RESULTS);

				if (series) {
					writeAsSeries(writer, reader);
				} else {
					writeAsIs(writer, reader);
				}

				writer.arrayE().objectE(); // end the whole thing
				writer.flush();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.zenoss.app.query.api.PerformanceMetricQueryAPI#query(com.google.common
	 * .base.Optional, com.google.common.base.Optional,
	 * com.google.common.base.Optional, com.google.common.base.Optional,
	 * com.google.common.base.Optional, java.util.List)
	 */
	@Override
	public Response query(Optional<String> id, Optional<String> startTime,
			Optional<String> endTime, Optional<String> tz,
			Optional<Boolean> exactTimeWindow, Optional<Boolean> series,
			List<MetricQuery> queries) {
		
		QueryAppConfiguration config = getConfiguration();

		return Response.ok(
				new Worker(config, id.or(NOT_SPECIFIED), startTime.or(config
						.getPerformanceMetricQueryConfig()
						.getDefaultStartTime()),
						endTime.or(config.getPerformanceMetricQueryConfig()
								.getDefaultEndTime()), tz.or(config
								.getPerformanceMetricQueryConfig()
								.getDefaultTimeZone()), exactTimeWindow
								.or(config.getPerformanceMetricQueryConfig()
										.getDefaultExactTimeWindow()), series
								.or(config.getPerformanceMetricQueryConfig()
										.getDefaultSeries()), queries)).build();
	}

}
