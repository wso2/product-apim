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

BC_FIPS_VERSION=1.0.2.3;
BCPKIX_FIPS_VERSION=1.0.7;

EXPECTED_BC_FIPS_CHECKSUM="da62b32cb72591f5b4d322e6ab0ce7de3247b534"
EXPECTED_BCPKIX_FIPS_CHECKSUM="fe07959721cfa2156be9722ba20fdfee2b5441b0"

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
    echo "Successfully removed bcpkix-fips_$BCPKIX_JDK15ON_VERSION.jar  from component/lib."
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
    echo "Successfully removed bcpkix_fips_$BCPKIX_JDK15ON_VERSION.jar from component/dropins."
  fi
  if [ ! -e $CARBON_HOME/repository/components/plugins/bcprov-jdk15on*.jar ]; then
      sever_restart_required=true
      if [ -e $homeDir/.wso2-bc/backup/bcprov-jdk15on*.jar ]; then
        location=$(find "$homeDir/.wso2-bc/backup/" -type f -name "bcprov-jdk15on*.jar" | head -1)
        bcprov_file_name=$(basename "$location")
        bcprov_version=${bcprov_file_name#*_}
        bcprov_version=${bcprov_version%.jar}
        mv "$location" "$CARBON_HOME/repository/components/plugins"
        echo "Moved $bcprov_file_name from $homeDir/.wso2-bc/backup to components/plugins."
      else
        echo "Required bcprov-jdk15on jar is not available in $homeDir/.wso2-bc/backup. Download the jar from maven central repository."
      fi
  fi
  if [ ! -e $CARBON_HOME/repository/components/plugins/bcpkix-jdk15on*.jar ]; then
      sever_restart_required=true
      if [ -e $homeDir/.wso2-bc/backup/bcpkix-jdk15on*.jar ]; then
        location=$(find "$homeDir/.wso2-bc/backup/" -type f -name "bcpkix-jdk15on*.jar" | head -1)
        bcpkix_file_name=$(basename "$location")
        bcpkix_version=${bcpkix_file_name#*_}
        bcpkix_version=${bcpkix_version%.jar}
        mv "$location" "$CARBON_HOME/repository/components/plugins"
        echo "Moved $bcpkix_file_name from $homeDir/.wso2-bc/backup to components/plugins."
      else
        echo "Required bcpkix-jdk15on jar is not available in $homeDir/.wso2-bc/backup. Download the jar from maven central repository."
      fi
  fi

  bcprov_text="bcprov-jdk15on,$bcprov_version,../plugins/$bcprov_file_name,4,true";
  bcpkix_text="bcpkix-jdk15on,$bcpkix_version,../plugins/$bcpkix_file_name,4,true";

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
	if [ -f $CARBON_HOME/repository/components/plugins/bcprov-jdk15on*.jar ]; then
		location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcprov-jdk15on*.jar" | head -1)
		file_name=$(basename "$location")
		verify=false
		echo "Found $file_name in plugins folder. This jar should be removed."
	fi
	if [ -f $CARBON_HOME/repository/components/plugins/bcprov-jdk15on*.jar ]; then
		location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcpkix-jdk15on*.jar" | head -1)
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

	if grep -q "bcprov-jdk15on" "$api_publisher_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in api-publisher bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$api_publisher_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in api-publisher bundles.info. This should be removed.";
  fi
	if grep -q "bcprov-jdk15on" "$api_devportal_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in api-devportal bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$api_devportal_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in api-devportal bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk15on" "$api_key_manager_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in api-key-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$api_key_manager_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in api-key-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk15on" "$default_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in default bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$default_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in default bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk15on" "$control_plane_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in control-plane bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$control_plane_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in control-plane bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk15on" "$traffic_manager_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in traffic-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$traffic_manager_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in traffic-manager bundles.info. This should be removed.";
  fi
  if grep -q "bcprov-jdk15on" "$gateway_worker_bundles_info" ; then
    verify=false
    echo  "Found bcprov-jdk15on entry in gateway-worker bundles.info. This should be removed.";
  fi
  if grep -q "bcpkix-jdk15on" "$gateway_worker_bundles_info" ; then
    verify=false
    echo  "Found bcpkix-jdk15on entry in gateway-worker bundles.info. This should be removed.";
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
	if [ -f $CARBON_HOME/repository/components/plugins/bcprov-jdk15on*.jar ]; then
	    sever_restart_required=true
	    location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcprov-jdk15on*.jar" | head -1)
	    echo "Removing existing bcpkix-jdk15on jar from plugins folder."
      if [ -f $homeDir/.wso2-bc/backup/bcprov-jdk15on*.jar ]; then
        rm $homeDir/.wso2-bc/backup/bcprov-jdk15on*.jar
      fi
	    mv "$location" "$homeDir/.wso2-bc/backup"
	    bcprov_file_name=$(basename "$location")
      echo "Successfully removed $bcprov_file_name from component/plugins."
	fi
	if [ -f $CARBON_HOME/repository/components/plugins/bcpkix-jdk15on*.jar ]; then
	   	sever_restart_required=true
      echo "Removing existing bcpkix-jdk15on jar from plugins folder."
      location=$(find "$CARBON_HOME/repository/components/plugins/" -type f -name "bcpkix-jdk15on*.jar" | head -1)
      if [ -f $homeDir/.wso2-bc/backup/bcpkix-jdk15on*.jar ]; then
        rm $homeDir/.wso2-bc/backup/bcpkix-jdk15on*.jar
      fi
      mv "$location" "$homeDir/.wso2-bc/backup"
      bcpkix_file_name=$(basename "$location")
      echo "Successfully removed $bcpkix_file_name from component/plugins."
	fi

  if grep -q "bcprov-jdk15on" "$api_publisher_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $api_publisher_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$api_publisher_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $api_publisher_bundles_info
  fi
  if grep -q "bcprov-jdk15on" "$api_devportal_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $api_devportal_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$api_devportal_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $api_devportal_bundles_info
  fi
  if grep -q "bcprov-jdk15on" "$api_key_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $api_key_manager_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$api_key_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $api_key_manager_bundles_info
  fi
  if grep -q "bcprov-jdk15on" "$default_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $default_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$default_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $default_bundles_info
  fi
  if grep -q "bcprov-jdk15on" "$control_plane_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $control_plane_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$control_plane_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $control_plane_bundles_info
  fi
  if grep -q "bcprov-jdk15on" "$traffic_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $traffic_manager_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$traffic_manager_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $traffic_manager_bundles_info
  fi
  if grep -q "bcprov-jdk15on" "$gateway_worker_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcprov-jdk15on/d' $gateway_worker_bundles_info
  fi
  if grep -q "bcpkix-jdk15on" "$gateway_worker_bundles_info" ; then
    sever_restart_required=true
    sed -i '/bcpkix-jdk15on/d' $gateway_worker_bundles_info
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
      ACTUAL_CHECKSUM=$(sha1sum $CARBON_HOME/repository/components/lib/bc-fips*.jar | cut -d' ' -f1)
      if [ "$EXPECTED_BC_FIPS_CHECKSUM" = "$ACTUAL_CHECKSUM" ]; then
        echo "Checksum verified: The downloaded bc-fips-$BC_FIPS_VERSION.jar is valid."
      else
        echo "Checksum verification failed: The downloaded bc-fips-$BC_FIPS_VERSION.jar may be corrupted."
      fi
    elif [ ! -z "$arg1" ] && [ -z "$arg2" ]; then
      if [ ! -e $arg1/bcpkix-fips-$BCPKIX_FIPS_VERSION.jar ]; then
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
      ACTUAL_CHECKSUM=$(sha1sum $CARBON_HOME/repository/components/lib/bc-fips*.jar | cut -d' ' -f1)
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
      ACTUAL_CHECKSUM=$(sha1sum $CARBON_HOME/repository/components/lib/bcpkix-fips*.jar | cut -d' ' -f1)
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
      if [ "$EXPECTED_BC_FIPS_CHECKSUM" = "$ACTUAL_CHECKSUM" ]; then
          echo "Checksum verified: The downloaded bc-fips-$BC_FIPS_VERSION.jar is valid."
      else
          echo "Checksum verification failed: The downloaded bc-fips-$BC_FIPS_VERSION.jar may be corrupted."
      fi
    fi
	fi
fi

if [ "$sever_restart_required" = true ] ; then
    echo "Please restart the server."
fi