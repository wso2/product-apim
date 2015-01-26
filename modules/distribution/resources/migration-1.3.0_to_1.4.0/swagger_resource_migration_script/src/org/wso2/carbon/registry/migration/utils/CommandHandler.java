/*
*  Copyright (c) WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.registry.migration.utils;

import java.util.HashMap;
import java.util.Map;

public class CommandHandler {

    private static final Map<String, String> inputs = new HashMap<String, String>();

    public static boolean setInputs(String[] arguments) {

        if (arguments.length == 0) {
            printMessage();
            return false;
        }
        if (arguments.length == 1 && arguments[0].equals("--help")) {
            printMessage();
            return false;
        }

        // now loop through the arguments list to capture the options
        for (int i = 0; i < arguments.length; i++) {
            if (arguments[i].equals("-h")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Hostname of the registry is missing");
                }
                inputs.put("-h", arguments[++i]);

            } else if (arguments[i].equals("-p")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Port of the registry is missing");
                }
                inputs.put("-p", arguments[++i]);

            } else if (arguments[i].equals("-u")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Username of the admin is missing");
                }
                inputs.put("-u", arguments[++i]);

            } else if (arguments[i].equals("-pw")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Password of the admin is missing");
                }
                inputs.put("-pw", arguments[++i]);
            }else if (arguments[i].equals("-dpw")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Password of the admin is missing");
                }
                inputs.put("-dpw", arguments[++i]);
            }else if (arguments[i].equals("-du")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Password of the database missing");
                }
                inputs.put("-du", arguments[++i]);
            }else if (arguments[i].equals("-durl")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Username of the database is missing");
                }
                inputs.put("-durl", arguments[++i]);
            } else if (arguments[i].equals("-dr")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Database is missing");
                }
                inputs.put("-dr", arguments[++i]);
            } else if (arguments[i].equals("-cr")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Context root of the service is missing");
                }
                inputs.put("-cr", arguments[++i]);
            } else if (arguments[i].equals("-gh")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Hostname of the API gateway is missing");
                }
                inputs.put("-gh", arguments[++i]);
            } else if (arguments[i].equals("-gp")) {
                if (arguments.length - 1 == i) {
                    throw new RuntimeException("Port of the API gateway is missing");
                }
                inputs.put("-gp", arguments[++i]);
            }
        }

        return true;
    }


    private static void printMessage() {
        System.out.println("Usage: migration-client <options>");
        System.out.println("Valid options are:");
        System.out.println("\t-h :\t(Required) The hostname/ip of the registry to login.");
        System.out.println("\t-p :\t(Required) The port of the registry to login.");
        System.out.println("\t-u :\t(Required) The user name of the registry login.");
        System.out.println("\t-pw:\t(Required) The password of the registry login.");
        System.out.println();
        System.out.println("Example to migrate a registry running on localhost on default values");
        System.out.println("\te.g: migration-client -h localhost -p 9443 -u admin -pw admin");
    }

    public static String getRegistryURL() {
        String contextRoot = inputs.get("-cr");

        if (contextRoot == null) {
            return "https://" + inputs.get("-h") + ":" + inputs.get("-p") + "/registry/";
        } else {
            return "https://" + inputs.get("-h") + ":" + inputs.get("-p") + "/" + contextRoot + "/registry/";
        }
    }

    public static String getHost() {
        return inputs.get("-h");
    }

    public static String getServiceURL() {
        String contextRoot = inputs.get("-cr");

        if (contextRoot == null) {
            return "https://" + inputs.get("-h") + ":" + inputs.get("-p") + "/services/";
        } else {
            return "https://" + inputs.get("-h") + ":" + inputs.get("-p") + "/" + contextRoot + "/services/";
        }
    }
    
    public static String getGatewayURL() {
    	int gatewayPort = Integer.parseInt(inputs.get("-gp"));
    	gatewayPort = 8280 + (gatewayPort - 9443);
        return "https://" + inputs.get("-gh") + ":" + gatewayPort + "/services/";
    }

    public static String getUsername() {
        return inputs.get("-u");
    }

    public static String getPassword() {
        return inputs.get("-pw");
    }

    public static String getDBUrl() {
        return inputs.get("-durl");
    }

    public static String getDBDriver() {
        return inputs.get("-dr");
    }

    public static String getDBPassword() {
        return inputs.get("-dpw");
    }

    public static String getDBUsername() {
        return inputs.get("-du");
    }
}

