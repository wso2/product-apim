#! /bin/bash
# ----------------------------------------------------------------------------
#  Copyright 2023 WSO2, LLC. http://www.wso2.org
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

BC_FIPS_VERSION=1.0.2.5;
BCPKIX_FIPS_VERSION=1.0.7;

EXPECTED_BC_FIPS_CHECKSUM="704e65f7e4fe679e5ab2aa8a840f27f8ced4c522"
EXPECTED_BCPKIX_FIPS_CHECKSUM="fe07959721cfa2156be9722ba20fdfee2b5441b0"

# resolve links - $0 may be a softlink
PRG="$0"

while [ -h "$PRG" ]; do
  ls=`ls -ld "$PRG"`
  link=`expr "$ls" : '.*-> \(.*\)$'`
  if expr "$link" : '.*/.*' > /dev/null; then
    PRG="$link"
  else
    PRG=`dirname "$PRG"`/"$link"
  fi
done

# Get standard environment variables
PRGDIR=`dirname "$PRG"`

# Only set CARBON_HOME if not already set
[ -z "$CARBON_HOME" ] && CARBON_HOME=`cd "$PRGDIR/.." ; pwd`

ARGUMENT=$1;
api_publisher_bundles_info="$CARBON_HOME/repository/components/api-publisher-deprecated/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
api_devportal_bundles_info="$CARBON_HOME/repository/components/api-devportal-deprecated/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
api_key_manager_bundles_info="$CARBON_HOME/repository/components/api-key-manager-deprecated/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
default_bundles_info="$CARBON_HOME/repository/components/default/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
control_plane_bundles_info="$CARBON_HOME/repository/components/control-plane/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
traffic_manager_bundles_info="$CARBON_HOME/repository/components/traffic-manager/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";
gateway_worker_bundles_info="$CARBON_HOME/repository/components/gateway-worker/configuration/org.eclipse.equinox.simpleconfigurator/bundles.info";

homeDir="$HOME"
sever_restart_required=false

if [ "$ARGUMENT" = "DISABLE" ] || [ "$ARGUMENT" = "disable" ]; then
  if [ -f $CARBON_HOME/repository/components/lib/bc-fips*.jar ]; then
    sever_restart_required=true
    echo "Removing existing bc-fips jar from lib folder."
    rm rm $CARBON_HOME/repository/components/lib/bc-fips*.jar 2> /dev/null
    echo "Successfully removed bc-fips_$BC_FIPS_VERSION.jar from component/lib."
  fi
  if [ -f $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar ]; then
    sever_restart_required=true
    echo "Removing existing bcpkix-fips jar from lib folder."
    rm rm $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar 2> /dev/null
    echo "Successfully removed bcpkix-fips_$BCPKIX_FIPS_VERSION.jar  from component/lib."
  fi
  if [ -f $CARBON_HOME/repository/components/dropins/bc_fips*.jar ]; then
    sever_restart_required=true
    echo "Removing existing bc-fips jar from dropins folder."
    rm rm $CARBON_HOME/repository/components/dropins/bc_fips*.jar 2> /dev/null
    echo "Successfully removed bc-fips_$BC_FIPS_VERSION.jar from component/dropins."
  fi
  if [ -f $CARBON_HOME/repository/components/dropins/bcpkix_fips*.jar ]; then
      sever_restart_required=true
    echo "Removing existing bcpkix_fips jar from dropins folder."
    rm rm $CARBON_HOME/repository/components/dropins/bcpkix_fips*.jar 2> /dev/null
    echo "Successfully removed bcpkix_fips_$BCPKIX_FIPS_VERSION.jar from component/dropins."
  fi
  if [ ! -e $CARBON_HOME/repository/components/plugins/bcprov-jdk*.jar ]; then
      sever_restart_required=true
      if [ -e $homeDir/.wso2-bc/backup/bcprov-jdk*.jar ]; then
        location=$(find "$homeDir/.wso2-bc/backup/" -type f -name "bcprov-jdk*.jar" | head -1)
        bcprov_file_name=$(basename "$location")
        bcprov_jar_name=${bcprov_file_name%_*}
        bcprov_version=${bcprov_file_name#*_}
        bcprov_version=${bcprov_version%.jar}
        mv "$location" "$CARBON_HOME/repository/components/plugins"
        echo "Moved $bcprov_file_name from $homeDir/.wso2-bc/backup to components/plugins."
      else
        echo "Required bcprov jar is not available in $homeDir/.wso2-bc/backup. Download the jar from maven central repository."
      fi
  fi
  if [ ! -e $CARBON_HOME/repository/components/plugins/bcpkix-jdk*.jar ]; then
      sever_restart_required=true
      if [ -e $homeDir/.wso2-bc/backup/bcpkix-jdk*.jar ]; then
        location=$(find "$homeDir/.wso2-bc/backup/" -type f -name "bcpkix-jdk*.jar" | head -1)
        bcpkix_file_name=$(basename "$location")
        bcpkix_jar_name=${bcpkix_file_name%_*}
        bcpkix_version=${bcpkix_file_name#*_}
        bcpkix_version=${bcpkix_version%.jar}
        mv "$location" "$CARBON_HOME/repository/components/plugins"
        echo "Moved $bcpkix_file_name from $homeDir/.wso2-bc/backup to components/plugins."
      else
        echo "Required bcpkix jar is not available in $homeDir/.wso2-bc/backup. Download the jar from maven central repository."
      fi
  fi

  bcprov_text="$bcprov_jar_name,$bcprov_version,../plugins/$bcprov_file_name,4,true";
  bcpkix_text="$bcpkix_jar_name,$bcpkix_version,../plugins/$bcpkix_file_name,4,true";

  if ! grep -q "$bcprov_text" "$api_publisher_bundles_info" ; then
    echo  $bcprov_text >> $api_publisher_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$api_publisher_bundles_info" ; then
    echo  $bcpkix_text >> $api_publisher_bundles_info;
    sever_restart_required=true
  fi

  if ! grep -q "$bcprov_text" "$api_devportal_bundles_info" ; then
    echo  $bcprov_text >> $api_devportal_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$api_devportal_bundles_info" ; then
    echo  $bcpkix_text >> $api_devportal_bundles_info;
    sever_restart_required=true
  fi

  if ! grep -q "$bcprov_text" "$api_key_manager_bundles_info" ; then
    echo  $bcprov_text >> $api_key_manager_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$api_key_manager_bundles_info" ; then
    echo  $bcpkix_text >> $api_key_manager_bundles_info;
    sever_restart_required=true
  fi

  if ! grep -q "$bcprov_text" "$default_bundles_info" ; then
    echo  $bcprov_text >> $default_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$default_bundles_info" ; then
    echo  $bcpkix_text >> $default_bundles_info;
    sever_restart_required=true
  fi

  if ! grep -q "$bcprov_text" "$control_plane_bundles_info" ; then
    echo  $bcprov_text >> $control_plane_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$control_plane_bundles_info" ; then
    echo  $bcpkix_text >> $control_plane_bundles_info;
    sever_restart_required=true
  fi

  if ! grep -q "$bcprov_text" "$traffic_manager_bundles_info" ; then
    echo  $bcprov_text >> $traffic_manager_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$traffic_manager_bundles_info" ; then
    echo  $bcpkix_text >> $traffic_manager_bundles_info;
    sever_restart_required=true
  fi

  if ! grep -q "$bcprov_text" "$gateway_worker_bundles_info" ; then
    echo  $bcprov_text >> $gateway_worker_bundles_info;
    sever_restart_required=true
  fi
  if ! grep -q "$bcpkix_text" "$gateway_worker_bundles_info" ; then
    echo  $bcpkix_text >> $gateway_worker_bundles_info;
    sever_restart_required=true
  fi

elif [ "$ARGUMENT" = "VERIFY" ] || [ "$ARGUMENT" = "verify" ]; then
	verify=true;
	if [ -f $CARBON_HOME/repository/components/plugins/bcprov-jdk*.jar ]; then
		location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcprov-jdk*.jar" | head -1)
		file_name=$(basename "$location")
		verify=false
		echo "Found $file_name in plugins folder. This jar should be removed."
	fi
	if [ -f $CARBON_HOME/repository/components/plugins/bcprov-jdk*.jar ]; then
		location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcpkix-jdk*.jar" | head -1)
		file_name=$(basename "$location")
		verify=false
		echo "Found $file_name in plugins folder. This jar should be removed."
	fi
	if [ -f $CARBON_HOME/repository/components/lib/bc-fips*.jar ]; then
    if [ ! -f $CARBON_HOME/repository/components/lib/bc-fips-$BC_FIPS_VERSION.jar ]; then
			verify=false
			echo "There is an update for bc-fips. Run the script again to get updates."
		fi
	else
		verify=false
		echo "bc-fips_$BC_FIPS_VERSION.jar can not be found  in components/lib folder. This jar should be added."
	fi
	if [ -f $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar ]; then
    if [ ! -f $CARBON_HOME/repository/components/lib/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar ]; then
      verify=false
      echo "There is an update for bcpkix-fips. Run the script again to get updates."

		fi
	else
		verify=false
		echo "bcpkix-fips_$BCPKIX_FIPS_VERSION.jar can not be found in components/lib folder. This jar should be added."
	fi

	if grep -q "bcprov-jdk" "$api_publisher_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in api-publisher bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$api_publisher_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in api-publisher bundles.info. This should be removed.";
  fi
	if grep -q "bcprov-jdk" "$api_devportal_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in api-devportal bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$api_devportal_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in api-devportal bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk" "$api_key_manager_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in api-key-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$api_key_manager_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in api-key-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk" "$default_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in default bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$default_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in default bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk" "$control_plane_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in control-plane bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$control_plane_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in control-plane bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk" "$traffic_manager_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in traffic-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$traffic_manager_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in traffic-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk" "$gateway_worker_bundles_info" ; then
    verify=false
    echo  "Found bcprov entry in gateway-worker bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk" "$gateway_worker_bundles_info" ; then
    verify=false
    echo  "Found bcpkix entry in gateway-worker bundles.info. This should be removed.";
  fi

	if [ $verify = true ]; then
		echo "Verified : Product is FIPS compliant."
	else 	echo "Verification failed : Product is not FIPS compliant."
	fi

else
  while getopts "f:m:" opt; do
    case $opt in
      f)
        arg1=$OPTARG
          ;;
      m)
          arg2=$OPTARG
          ;;
      \?)
        echo "Invalid option: -$OPTARG" >&2
        exit 1
        ;;
    esac
  done

	if [ ! -d "$homeDir/.wso2-bc" ]; then
    		mkdir "$homeDir/.wso2-bc"
	fi
	if [ ! -d "$homeDir/.wso2-bc/backup" ]; then
    		mkdir "$homeDir/.wso2-bc/backup"
	fi
	if [ -f $CARBON_HOME/repository/components/plugins/bcprov-jdk*.jar ]; then
	    sever_restart_required=true
	    location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcprov-jdk*.jar" | head -1)
	    echo "Removing existing bcprov jar from plugins folder."
      if [ -f $homeDir/.wso2-bc/backup/bcprov-jdk*.jar ]; then
        rm $homeDir/.wso2-bc/backup/bcprov-jdk*.jar
      fi
	    mv "$location" "$homeDir/.wso2-bc/backup"
	    bcprov_file_name=$(basename "$location")
      echo "Successfully removed $bcprov_file_name from component/plugins."
	fi
	if [ -f $CARBON_HOME/repository/components/plugins/bcpkix-jdk*.jar ]; then
	   	sever_restart_required=true
      echo "Removing existing bcpkix jar from plugins folder."
      location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcpkix-jdk*.jar" | head -1)
      if [ -f $homeDir/.wso2-bc/backup/bcpkix-jdk*.jar ]; then
        rm $homeDir/.wso2-bc/backup/bcpkix-jdk*.jar
      fi
      mv "$location" "$homeDir/.wso2-bc/backup"
      bcpkix_file_name=$(basename "$location")
      echo "Successfully removed $bcpkix_file_name from component/plugins."
	fi

  if grep -q "bcprov-jdk" "$api_publisher_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e '/bcprov-jdk/d' $api_publisher_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$api_publisher_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $api_publisher_bundles_info
  fi
  if grep -q "bcprov-jdk" "$api_devportal_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcprov-jdk/d' $api_devportal_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$api_devportal_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $api_devportal_bundles_info
  fi
  if grep -q "bcprov-jdk" "$api_key_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcprov-jdk/d' $api_key_manager_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$api_key_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $api_key_manager_bundles_info
  fi
  if grep -q "bcprov-jdk" "$default_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcprov-jdk/d' $default_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$default_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $default_bundles_info
  fi
  if grep -q "bcprov-jdk" "$control_plane_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcprov-jdk/d' $control_plane_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$control_plane_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $control_plane_bundles_info
  fi
  if grep -q "bcprov-jdk" "$traffic_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcprov-jdk/d' $traffic_manager_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$traffic_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $traffic_manager_bundles_info
  fi
  if grep -q "bcprov-jdk" "$gateway_worker_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcprov-jdk/d' $gateway_worker_bundles_info
  fi
  if grep -q "bcpkix-jdk" "$gateway_worker_bundles_info" ; then
    sever_restart_required=true
    sed -i'' -e  '/bcpkix-jdk/d' $gateway_worker_bundles_info
  fi

	if [ -e $CARBON_HOME/repository/components/lib/bc-fips*.jar ]; then
    location=$(find "$CARBON_HOME/repository/components/lib/" -type f -name "bc-fips*.jar" | head -1)
		if [ ! $location = "$CARBON_HOME/repository/components/lib/bc-fips-$BC_FIPS_VERSION.jar" ]; then
      sever_restart_required=true
        echo "There is an update for bc-fips. Therefore Remove existing bc-fips jar from lib folder."
        rm rm $CARBON_HOME/repository/components/lib/bc-fips*.jar 2> /dev/null
      echo "Successfully removed bc-fips_$BC_FIPS_VERSION.jar from component/lib."
      if [ -f $CARBON_HOME/repository/components/dropins/bc_fips*.jar ]; then
        sever_restart_required=true
        echo "Removing existing bc-fips jar from dropins folder."
        rm rm $CARBON_HOME/repository/components/dropins/bc_fips*.jar 2> /dev/null
        echo "Successfully removed bc-fips_$BC_FIPS_VERSION.jar from component/dropins."
      fi
		fi
	fi

	if [ ! -e $CARBON_HOME/repository/components/lib/bc-fips*.jar ]; then
		sever_restart_required=true
		if [ -z "$arg1" ] && [ -z "$arg2" ]; then
      echo "Downloading required bc-fips jar : bc-fips-$BC_FIPS_VERSION"
      curl https://repo1.maven.org/maven2/org/bouncycastle/bc-fips/$BC_FIPS_VERSION/bc-fips-$BC_FIPS_VERSION.jar -o $CARBON_HOME/repository/components/lib/bc-fips-$BC_FIPS_VERSION.jar
      ACTUAL_CHECKSUM=$(shasum $CARBON_HOME/repository/components/lib/bc-fips*.jar | cut -d' ' -f1)
      if [ "$EXPECTED_BC_FIPS_CHECKSUM" = "$ACTUAL_CHECKSUM" ]; then
        echo "Checksum verified: The downloaded bc-fips-$BC_FIPS_VERSION.jar is valid."
      else
        echo "Checksum verification failed: The downloaded bc-fips-$BC_FIPS_VERSION.jar may be corrupted."
      fi
    elif [ ! -z "$arg1" ] && [ -z "$arg2" ]; then
      if [ ! -e $arg1/bc-fips-$BC_FIPS_VERSION.jar ]; then
        echo "Can not be found required bc-fips-$BC_FIPS_VERSION.jar in given file path : $arg1."
      else
        cp "$arg1/bc-fips-$BC_FIPS_VERSION.jar" "$CARBON_HOME/repository/components/lib"
        if [ $? -eq 0 ]; then
            echo "bc-fips JAR files copied successfully."
        else
            echo "Error copying bc-fips JAR file."
        fi
      fi
		else
      echo "Downloading required bc-fips jar : bc-fips-$BC_FIPS_VERSION"
      curl $arg2/org/bouncycastle/bc-fips/$BC_FIPS_VERSION/bc-fips-$BC_FIPS_VERSION.jar -o $CARBON_HOME/repository/components/lib/bc-fips-$BC_FIPS_VERSION.jar
      ACTUAL_CHECKSUM=$(shasum $CARBON_HOME/repository/components/lib/bc-fips*.jar | cut -d' ' -f1)
      if [ "$EXPECTED_BC_FIPS_CHECKSUM" = "$ACTUAL_CHECKSUM" ]; then
        echo "Checksum verified: The downloaded bc-fips-$BC_FIPS_VERSION.jar is valid."
      else
        echo "Checksum verification failed: The downloaded bc-fips-$BC_FIPS_VERSION.jar may be corrupted."
      fi
    fi
	fi

	if [ -e $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar ]; then
	    location=$(find "$CARBON_HOME/repository/components/lib/" -type f -name "bcpkix-fips*.jar" | head -1)
		if [ ! $location = "$CARBON_HOME/repository/components/lib/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar" ]; then
      sever_restart_required=true
        echo "There is an update for bcpkix-fips. Therefore Remove existing bcpkix-fips jar from lib folder."
        rm rm $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar 2> /dev/null
        echo "Successfully removed bcpkix-fips_$BCPKIX_FIPS_VERSION.jar from component/lib."
      if [ -f $CARBON_HOME/repository/components/dropins/bcpkix-fips*.jar ]; then
        echo "Removing existing bcpkix-fips jar from dropins folder."
        rm rm $CARBON_HOME/repository/components/dropins/bcpkix_fips*.jar 2> /dev/null
        echo "Successfully removed bcpkix-fips_$BCPKIX_FIPS_VERSION.jar from component/dropins."
      fi
		fi
	fi

	if [ ! -e $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar ]; then
    sever_restart_required=true
    if [ -z "$arg1" ] && [ -z "$arg2" ]; then
      echo "Downloading required bcpkix-fips jar : bcpkix-fips-$BCPKIX_FIPS_VERSION"
      curl https://repo1.maven.org/maven2/org/bouncycastle/bcpkix-fips/$BCPKIX_FIPS_VERSION/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar -o $CARBON_HOME/repository/components/lib/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar
      ACTUAL_CHECKSUM=$(shasum $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar | cut -d' ' -f1)
        if [ "$EXPECTED_BCPKIX_FIPS_CHECKSUM" = "$ACTUAL_CHECKSUM" ]; then
          echo "Checksum verified: The downloaded bcpkix-fips-$BCPKIX_FIPS_VERSION.jar is valid."
      else
          echo "Checksum verification failed: The downloaded bcpkix-fips-$BCPKIX_FIPS_VERSION.jar may be corrupted."
      fi
    elif [ ! -z "$arg1" ] && [ -z "$arg2" ]; then
      if [ ! -e $arg1/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar ]; then
        echo "Can not be found required bcpkix-fips-$BCPKIX_FIPS_VERSION.jar in given file path : $arg1."
      else
      cp "$arg1/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar" "$CARBON_HOME/repository/components/lib"
      if [ $? -eq 0 ]; then
          echo "bcpkix-fips JAR files copied successfully."
      else
          echo "Error copying bcpkix-fips JAR file."
      fi
    fi
		else
			echo "Downloading required bcpkix-fips jar : bcpkix-fips-$BCPKIX_FIPS_VERSION"
      curl $arg2/org/bouncycastle/bcpkix-fips/$BCPKIX_FIPS_VERSION/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar -o $CARBON_HOME/repository/components/lib/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar
			ACTUAL_CHECKSUM=$(sha1sucam $CARBON_HOME/repository/components/lib/bc-fips*.jar | cut -d' ' -f1)
      if [ "$EXPECTED_BCPKIX_FIPS_CHECKSUM" = "$ACTUAL_CHECKSUM" ]; then
          echo "Checksum verified: The downloaded bcpkix-fips-$BCPKIX_FIPS_VERSION.jar is valid."
      else
          echo "Checksum verification failed: The downloaded bcpkix-fips-$BCPKIX_FIPS_VERSION.jar may be corrupted."
      fi
    fi
	fi
fi

if [ "$sever_restart_required" = true ] ; then
    echo "Please restart the server."
fi