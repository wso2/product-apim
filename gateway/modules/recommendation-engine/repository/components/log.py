import logging
import logging.handlers
import os
from config import config_properties

dirname = os.path.dirname(__file__)

LOG_FILENAME = filename = os.path.join(dirname, '../log/wso2carbon.log')
logger = logging.getLogger()
LOG_LEVEL = config_properties['log_level']

if LOG_LEVEL == "INFO":
    log_level = logging.INFO
elif LOG_LEVEL == "DEBUG":
    log_level = logging.DEBUG

def set_logger():
    logger.setLevel(log_level)
    formatter = logging.Formatter('%(asctime)s %(levelname)s: %(message)s')

    console_handler = logging.StreamHandler()
    console_handler.setFormatter(formatter)
    logger.addHandler(console_handler)

    file_handler = logging.handlers.TimedRotatingFileHandler(LOG_FILENAME, when='midnight', backupCount=30)
    #Uncomment the below file_handler for a size based Rotating File handler.
    #file_handler = logging.handlers.RotatingFileHandler(LOG_FILENAME,maxBytes=10485760,backupCount=10,encoding="utf-8")

    file_handler.setFormatter(formatter)
    logger.addHandler(file_handler)

set_logger()