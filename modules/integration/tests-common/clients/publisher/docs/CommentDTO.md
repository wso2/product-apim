

# CommentDTO

## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **String** |  |  [optional] [readonly]
**content** | **String** |  | 
**createdTime** | **String** |  |  [optional] [readonly]
**createdBy** | **String** |  |  [optional] [readonly]
**updatedTime** | **String** |  |  [optional] [readonly]
**category** | **String** |  |  [optional]
**parentCommentId** | **String** |  |  [optional]
**entryPoint** | [**EntryPointEnum**](#EntryPointEnum) |  |  [optional]
**commenterInfo** | [**CommenterInfoDTO**](CommenterInfoDTO.md) |  |  [optional]
**replies** | [**CommentListDTO**](CommentListDTO.md) |  |  [optional]



## Enum: EntryPointEnum

Name | Value
---- | -----
DEVPORTAL | &quot;devPortal&quot;
PUBLISHER | &quot;publisher&quot;



