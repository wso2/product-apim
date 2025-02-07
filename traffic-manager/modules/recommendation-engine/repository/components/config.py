import yaml
import os

dirname = os.path.dirname(__file__)

CONFIG_FILE = os.path.join(dirname,'../conf/config.yaml')
with open(CONFIG_FILE, 'r') as stream:
    config_properties = yaml.safe_load(stream)