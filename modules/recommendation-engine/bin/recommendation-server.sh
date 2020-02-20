#!/bin/bash
# main script to start the recommendation server

cd ../

cd repository/components
if [[ $1 == "stop" ]]; then
  CMD="stop"
elif command -v python3; then
  python3 recommendation-server.py 
elif command -v python; then
  python recommendation-server.py 
else
  echo "Python 3 is required for the command!"
  exit 1
fi

if [ "$CMD" = "stop" ]; then
  if ps | grep -v "grep" | grep "recommendation-server.py"
  then
      # Getting the PID of the process
      PID=`/bin/ps -fu $USER| grep "recommendation-server.py" | grep -v "grep" | awk '{print $2}'`

      # Number of seconds to wait before cusing "kill -9"
      WAIT_SECONDS=10

      # Counter to keep count of how many seconds have passed
      count=0

      while kill $PID > /dev/null
      do
          # Wait for one second
          sleep 1
          # Increment the second counter
          ((count++))

          # Has the process been killed? If so, exit the loop.
          if ! ps -p $PID > /dev/null ; then
              break
          fi

          # Have we exceeded $WAIT_SECONDS? If so, kill the process with "kill -9"
          # and exit the loop
          if [ $count -gt $WAIT_SECONDS ]; then
              kill -9 $PID
              break
          fi
      done   
  else
    echo "Server is stopped"
  fi
fi


