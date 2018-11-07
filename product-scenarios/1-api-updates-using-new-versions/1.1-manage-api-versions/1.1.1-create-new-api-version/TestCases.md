# Test Case Template

### 1.1.1 Create new version from existing API

| TestCaseID| TestCase| Test steps| Status|
| ----------| --------| ----------| ------|
| 1.1.1.1| Create new version from exsiting API with mendatory values only| **Given**:Test environment is set properly. </br> **When**:A request sends to copy an existing API with new version. </br> **Then**:New version of API should be created.| Automated|
| 1.1.1.2| Create new version from exsiting API with same version| **Given**:Versioned API exists. </br> **When**:A request sends to copy an existing API with same version. </br> **Then**:API should not be created.| Automated|


