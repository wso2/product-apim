IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_DESTINATION_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_DESTINATION_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  destination varchar(100) NOT NULL DEFAULT '',
  total_request_count INTEGER DEFAULT NULL,
  hostName varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (api,version,apiPublisher,context,destination,hostName,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_FAULT_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_FAULT_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  consumerKey varchar(100) DEFAULT NULL,
  context varchar(100) NOT NULL DEFAULT '',
  total_fault_count INTEGER DEFAULT NULL,
  hostName varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (api,version,apiPublisher,context,hostName,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_REQUEST_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_REQUEST_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  api_version varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  consumerKey varchar(100) NOT NULL DEFAULT '',
  userId varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  max_request_time INTEGER DEFAULT NULL,
  total_request_count INTEGER DEFAULT NULL,
  hostName varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (api,api_version,version,apiPublisher,consumerKey,userId,context,hostName,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_Resource_USAGE_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_Resource_USAGE_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  consumerKey varchar(100) NOT NULL DEFAULT '',
  resourcePath varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  method varchar(100) NOT NULL DEFAULT '',
  total_request_count INTEGER DEFAULT NULL,
  hostName varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (api,version,apiPublisher,consumerKey,context,resourcePath,method,hostName,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_RESPONSE_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_RESPONSE_SUMMARY (
  api_version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  serviceTime INTEGER DEFAULT NULL,
  total_response_count INTEGER DEFAULT NULL,
  hostName varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (api_version,apiPublisher,context,hostName,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_VERSION_USAGE_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_VERSION_USAGE_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  total_request_count INTEGER DEFAULT NULL,
  hostName varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (api,version,apiPublisher,context,hostName,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_THROTTLED_OUT_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_THROTTLED_OUT_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  api_version varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  applicationName varchar(100) NOT NULL DEFAULT '',
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  week INTEGER DEFAULT NULL,
  time varchar(30) NOT NULL DEFAULT '',
  success_request_count INTEGER DEFAULT NULL,
  throttleout_count INTEGER DEFAULT NULL,
  PRIMARY KEY (api,api_version,context,apiPublisher,applicationName,tenantDomain,year,month,day,time)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_LAST_ACCESS_TIME_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_LAST_ACCESS_TIME_SUMMARY (
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) DEFAULT NULL,
  userId varchar(100) DEFAULT NULL,
  context varchar(100) DEFAULT NULL,
  max_request_time INTEGER DEFAULT NULL,
  PRIMARY KEY (tenantDomain,apiPublisher,api)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_EXECUTION_TME_DAY_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_EXECUTION_TME_DAY_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime INTEGER DEFAULT NULL,
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  time INTEGER,
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,mediationName,tenantDomain)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_EXECUTION_TIME_HOUR_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_EXECUTION_TIME_HOUR_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime INTEGER DEFAULT NULL,
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  hour INTEGER DEFAULT NULL,
  time INTEGER,
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,hour,mediationName,tenantDomain)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_EXECUTION_TIME_MINUTE_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_EXECUTION_TIME_MINUTE_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime INTEGER DEFAULT NULL,
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  hour INTEGER DEFAULT NULL,
  minutes INTEGER DEFAULT NULL,
  time INTEGER,
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,hour,minutes,mediationName,tenantDomain)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_EXECUTION_TIME_SECONDS_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_EXECUTION_TIME_SECONDS_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime INTEGER DEFAULT NULL,
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  hour INTEGER DEFAULT NULL,
  minutes INTEGER DEFAULT NULL,
  seconds INTEGER DEFAULT NULL,
  time INTEGER,
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,hour,minutes,seconds,mediationName,tenantDomain)
);

IF NOT  EXISTS (SELECT * FROM SYS.OBJECTS WHERE OBJECT_ID = OBJECT_ID(N'[DBO].[API_REQUEST_GEO_LOCATION_SUMMARY]') AND TYPE IN (N'U'))
CREATE TABLE API_REQUEST_GEO_LOCATION_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  total_request_count INTEGER DEFAULT NULL,
  year INTEGER DEFAULT NULL,
  month INTEGER DEFAULT NULL,
  day INTEGER DEFAULT NULL,
  requestTime INTEGER,
  country varchar(200) NOT NULL,
  city varchar(200) NOT NULL
  PRIMARY KEY (api,version,apiPublisher,year,month,day,tenantDomain,country,city)
);
