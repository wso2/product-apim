

# ThrottleLimitDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | [**TypeEnum**](#TypeEnum) | Type of the throttling limit. Allowed values are \&quot;REQUESTCOUNTLIMIT\&quot; and \&quot;BANDWIDTHLIMIT\&quot;. Please see schemas of \&quot;RequestCountLimit\&quot; and \&quot;BandwidthLimit\&quot; throttling limit types in Definitions section.  | 
**requestCount** | [**RequestCountLimitDTO**](RequestCountLimitDTO.md) |  |  [optional]
**bandwidth** | [**BandwidthLimitDTO**](BandwidthLimitDTO.md) |  |  [optional]
**eventCount** | [**EventCountLimitDTO**](EventCountLimitDTO.md) |  |  [optional]



## Enum: TypeEnum

Name | Value
---- | -----
REQUESTCOUNTLIMIT | &quot;REQUESTCOUNTLIMIT&quot;
BANDWIDTHLIMIT | &quot;BANDWIDTHLIMIT&quot;
EVENTCOUNTLIMIT | &quot;EVENTCOUNTLIMIT&quot;



