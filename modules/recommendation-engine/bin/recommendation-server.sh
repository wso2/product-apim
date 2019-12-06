#!/bin/bash
# main script to start the recommendation server

cd ../
source venv/bin/activate
cd components
if command -v python3; then
  python3 server.py 
elif command -v python; then
  python server.py 
else
  echo "Python 3 is required for the command!"
  exit 1
fi




