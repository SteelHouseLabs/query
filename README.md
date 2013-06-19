Zenoss Central Query Service
=====
This project provides a HTTP/JSON based query capability for the information 
that Zenoss stores in its central repository.

The resources this service provides are:

  - GET /query/performance - returns the performance metrics that match the search criteria.

    This resource is loosely based on the OpenTSDB HTTP query API at [OpenTsdb](http://opentsdb.net/http-api.html#/q) and supports the following query parameters:

    - id = <_string_><br/>
      This ID is specified by the client and is not checked for uniqueness by the server, nor even used by the server.
    - start=<_date_ | _relative-date_ | _now_><br/>
      A date should be specified in the format "yyyy/MM/dd-HH:mm:ss-ZZZ"<br/>
      A relative date is of the format [-]#[ywdhms]-ago<br/>
      "now" translates to the current time when the request is made
    - end=<_date_ | _relative-date_ | _now_><br/>
      A date should be specified in the format "yyyy/MM/dd-HH:mm:ss-ZZZ"<br/>
      A relative date is of the format [-]#[ywdhms]-ago<br/>
      "now" translates to the current time when the request is made
    - exact=<_true_ | _false_>
    - series-<_true_ | _false_><br/>
      Determines is the results are grouped as individual series based on the results based on metric name and tag values not based on the number of queries specified.
    - query=<_AGG:[rate:][downsample:]metric[{tags}]_>

        AGG = min | max | sum | avg

        downsample = _like_ 10m-avg

        tags = name=tag-value

        tag-value = \* __or__ partial\* __or__ tag-value | tag-value 

        _Multiple "query" parameters can be specified in which case all query results will be generated from a single HTTP/JSON connection._

The results of the query will be a JSON object of the form:

        {
            "clientId" : "2342feo234",
            "source": "OpenTSDB",
            "startTime" : "5s-ago",
            "startTimeActual": "2013/06/19-17:00:00-+0000",
            "endTime" : "now",
            "endTimeActual": "2013/06/19-19:05:00-+0000",
            "exactTimeWindow": true,
            "series": false,
            "results" : [ 
                {
                    "metric" : "laLoadInt1",
                    "timestamp" : 1371512029,
                    "value" : 49,
                    "tags" : {
                        "tag1" : "value1",
                        "tag2" : "value2",
                        "tag3" : "value3"
                    }
                },
                {
                    "metric" : "laLoadInt1",
                    "timestamp" : 1371512044,
                    "value" : 52,
                    "tags" : {
                        "tag1" : "value1",
                        "tag2" : "value2",
                        "tag3" : "value3"
                    }
                }
            ]
        }

This response object contains the information which was given as part of the query as well as an array of results for each query. While some of the information in this response object may seem redundant it is not as each data point in the underling storage (OpenTSDB) may have a different set of tags.

Also note the __client id__ attribute in the response object. This is the value that was specified as the __id__ as the query parameter.