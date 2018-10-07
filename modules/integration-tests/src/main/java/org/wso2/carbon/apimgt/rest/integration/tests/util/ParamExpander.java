/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.wso2.carbon.apimgt.rest.integration.tests.util;

import feign.Param;

import java.text.DateFormat;
import java.util.Date;

/**
 * Param Expander to convert {@link Date} to RFC3339
 */
public class ParamExpander implements Param.Expander {

  private static final DateFormat dateformat = new RFC3339DateFormat();

  @Override
  public String expand(Object value) {
    if (value instanceof Date) {
      return dateformat.format(value);
    }
    return value.toString();
  }
}
