CREATE TABLE  API_DESTINATION_SUMMARY  (
   api  varchar(100) NOT NULL DEFAULT '',
   version  varchar(100) NOT NULL DEFAULT '',
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   context  varchar(100) NOT NULL DEFAULT '',
   destination  varchar(100) NOT NULL DEFAULT '',
   total_request_count  int DEFAULT NULL,
   hostName  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NULL,
   month  smallint DEFAULT NULL,
   day  smallint DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY ( api , version , apiPublisher , context , destination , hostName , time )
)/

CREATE TABLE  API_FAULT_SUMMARY  (
   api  varchar(100) NOT NULL DEFAULT '',
   version  varchar(100) NOT NULL DEFAULT '',	
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   consumerKey  varchar(100) DEFAULT NULL,
   context  varchar(100) NOT NULL DEFAULT '',
   total_fault_count  int DEFAULT NULL,
   hostName  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NULL,
   month  smallint DEFAULT NULL,
   day  smallint DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY ( api , version , apiPublisher , context , hostName , time )
)/

CREATE TABLE  API_REQUEST_SUMMARY  (
   api  varchar(100) NOT NULL DEFAULT '',
   api_version  varchar(100) NOT NULL DEFAULT '',
   version  varchar(100) NOT NULL DEFAULT '',
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   consumerKey  varchar(100) NOT NULL DEFAULT '',
   userId  varchar(100) NOT NULL DEFAULT '',
   context  varchar(100) NOT NULL DEFAULT '',
   max_request_time  bigint DEFAULT NULL,
   total_request_count  int DEFAULT NULL,
   hostName  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NULL,
   month  smallint DEFAULT NULL,
   day  smallint DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY ( api , api_version , version , apiPublisher , consumerKey , userId , context , hostName , time )
)/

CREATE TABLE  API_Resource_USAGE_SUMMARY  (
   api  varchar(100) NOT NULL DEFAULT '',
   version  varchar(100) NOT NULL DEFAULT '',
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   consumerKey  varchar(100) NOT NULL DEFAULT '',
   resourcePath  varchar(100) NOT NULL DEFAULT '',
   context  varchar(100) NOT NULL DEFAULT '',
   method  varchar(100) NOT NULL DEFAULT '',
   total_request_count  int DEFAULT NULL,
   hostName  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NULL,
   month  smallint DEFAULT NULL,
   day  smallint DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY ( api , version , apiPublisher , consumerKey , context , resourcePath , method , hostName , time )
)/

CREATE TABLE  API_RESPONSE_SUMMARY  (
   api_version  varchar(100) NOT NULL DEFAULT '',
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   context  varchar(100) NOT NULL DEFAULT '',
   serviceTime  int DEFAULT NULL,
   total_response_count  int DEFAULT NULL,
   hostName  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NULL,
   month  smallint DEFAULT NULL,
   day  smallint DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY ( api_version , apiPublisher , context , hostName , time )
)/

CREATE TABLE  API_VERSION_USAGE_SUMMARY  (
   api  varchar(100) NOT NULL DEFAULT '',
   version  varchar(100) NOT NULL DEFAULT '',
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   context  varchar(100) NOT NULL DEFAULT '',
   total_request_count  int DEFAULT NULL,
   hostName  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NULL,
   month  smallint DEFAULT NULL,
   day  smallint DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY ( api , version , apiPublisher , context , hostName , time )
)/

CREATE TABLE  API_THROTTLED_OUT_SUMMARY  (
   api  varchar(100) NOT NULL DEFAULT '',
   api_version  varchar(100) NOT NULL DEFAULT '',
   context  varchar(100) NOT NULL DEFAULT '',
   apiPublisher  varchar(100) NOT NULL DEFAULT '',
   applicationName  varchar(100) NOT NULL DEFAULT '',
   tenantDomain  varchar(100) NOT NULL DEFAULT '',
   year  smallint DEFAULT NOT NULL,
   month  smallint DEFAULT NOT NULL,
   day  smallint DEFAULT NOT NULL,
   week  int DEFAULT NULL,
   time  varchar(30) NOT NULL DEFAULT '',
   success_request_count  int DEFAULT NULL,
   throttleout_count  int DEFAULT NULL,
  PRIMARY KEY ( api , api_version , context , apiPublisher , applicationName , tenantDomain , year , month , day , time )
)/

CREATE TABLE IF NOT EXISTS API_LAST_ACCESS_TIME_SUMMARY (
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) DEFAULT NULL,
  userId varchar(100) DEFAULT NULL,
  context varchar(100) DEFAULT NULL,
  max_request_time bigint(20) DEFAULT NULL,
  PRIMARY KEY (tenantDomain,apiPublisher,api)
)/

CREATE TABLE IF NOT EXISTS API_EXECUTION_TME_DAY_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime int(11) DEFAULT NULL,
  year smallint(6) DEFAULT NULL,
  month smallint(6) DEFAULT NULL,
  day smallint(6) DEFAULT NULL,
  time bigint(20),
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,mediationName)
)/


CREATE TABLE IF NOT EXISTS API_EXECUTION_TIME_HOUR_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime int(11) DEFAULT NULL,
  year smallint(6) DEFAULT NULL,
  month smallint(6) DEFAULT NULL,
  day smallint(6) DEFAULT NULL,
  hour smallint(6) DEFAULT NULL,
  time bigint(20),
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,hour,mediationName)
)/


CREATE TABLE IF NOT EXISTS API_EXECUTION_TIME_MINUTE_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime int(11) DEFAULT NULL,
  year smallint(6) DEFAULT NULL,
  month smallint(6) DEFAULT NULL,
  day smallint(6) DEFAULT NULL,
  hour smallint(6) DEFAULT NULL,
  minutes smallint(6) DEFAULT NULL,
  time bigint(20),
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,hour,minutes,mediationName)
)/


CREATE TABLE IF NOT EXISTS API_EXECUTION_TIME_SECONDS_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  context varchar(100) NOT NULL DEFAULT '',
  mediationName varchar(100) NOT NULL DEFAULT '',
  executionTime int(11) DEFAULT NULL,
  year smallint(6) DEFAULT NULL,
  month smallint(6) DEFAULT NULL,
  day smallint(6) DEFAULT NULL,
  hour smallint(6) DEFAULT NULL,
  minutes smallint(6) DEFAULT NULL,
  seconds smallint(6) DEFAULT NULL,
  time bigint(20),
  PRIMARY KEY (api,version,apiPublisher,context,year,month,day,hour,minutes,seconds,mediationName)
)/
CREATE TABLE IF NOT EXISTS API_REQUEST_GEO_LOCATION_SUMMARY (
  api varchar(100) NOT NULL DEFAULT '',
  version varchar(100) NOT NULL DEFAULT '',
  apiPublisher varchar(100) NOT NULL DEFAULT '',
  tenantDomain varchar(100) NOT NULL DEFAULT '',
  total_request_count int(11) DEFAULT NULL,
  year smallint(6) DEFAULT NULL,
  month smallint(6) DEFAULT NULL,
  day smallint(6) DEFAULT NULL,
  requestTime bigint(20),
  country varchar(200) NOT NULL,
  city varchar(200) NOT NULL
  PRIMARY KEY (api,version,apiPublisher,year,month,day,tenantDomain,country,city)
)/
