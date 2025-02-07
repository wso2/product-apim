
# ThrottleLimit

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**type** | [**TypeEnum**](#TypeEnum) | Type of the throttling limit. Allowed values are \&quot;RequestCountLimit\&quot; and \&quot;BandwidthLimit\&quot;. Please see schemas of each of those throttling limit types in Definitions section.  | 
**timeUnit** | **String** | Unit of the time. Allowed values are \&quot;sec\&quot;, \&quot;min\&quot;, \&quot;hour\&quot;, \&quot;day\&quot; | 
**unitTime** | **Integer** | Time limit that the throttling limit applies. | 


<a name="TypeEnum"></a>
## Enum: TypeEnum
Name | Value
---- | -----
REQUESTCOUNTLIMIT | &quot;RequestCountLimit&quot;
BANDWIDTHLIMIT | &quot;BandwidthLimit&quot;



