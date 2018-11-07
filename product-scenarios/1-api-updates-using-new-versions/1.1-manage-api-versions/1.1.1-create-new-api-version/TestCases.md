# Test Case Template

### 1.1.1 Create new version from existing API

| Test Case ID| Test Case| Test Case Description| Status|
| ----------| --------| ----------| ------|
| 1.1.1.1| Create new version of API from existing API with mandatory values only| **Given**:Test environment is set properly. </br> **When**:A request sends to copy an existing API with new version. </br> **Then**:New version of API should be created.| Automated|
| | **[Negative Test Cases]**| | |
| 1.1.1.2| Create new version of API from existing API with same version| **Given**:Versioned API has to be created already. </br> **When**:A request sends to copy an existing API with same version. </br> **Then**:API should not be created, and a valid error message should be.| Automated|


