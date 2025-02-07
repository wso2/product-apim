
# IPCondition

## Properties
Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**ipConditionType** | [**IpConditionTypeEnum**](#IpConditionTypeEnum) | Type of the IP condition. Allowed values are \&quot;IPRange\&quot; and \&quot;IPSpecific\&quot; |  [optional]
**specificIP** | **String** | Specific IP when \&quot;IPSpecific\&quot; is used as the ipConditionType |  [optional]
**startingIP** | **String** | Staring IP when \&quot;IPRange\&quot; is used as the ipConditionType |  [optional]
**endingIP** | **String** | Ending IP when \&quot;IPRange\&quot; is used as the ipConditionType |  [optional]


<a name="IpConditionTypeEnum"></a>
## Enum: IpConditionTypeEnum
Name | Value
---- | -----
IPRANGE | &quot;IPRange&quot;
IPSPECIFIC | &quot;IPSpecific&quot;



