from db_modifier import * 
import flask
from flask import request, current_app
from flask import jsonify, Response
from flask import Flask, render_template
from flask_basicauth import BasicAuth
from werkzeug import serving
import ssl
import sys
import atexit
from apscheduler.scheduler import Scheduler
from log import logger
import base64
import json

API_CRT = "../resources/Certificates/server.crt"
API_KEY = "../resources/Certificates/server.key"

HTTPS_ENABLED = True
VERIFY_USER = True

API_HOST = config_properties['host']
API_PORT = config_properties['port']

MIN_API_COUNT = config_properties['minimum_APIs_to_start_recommendations']
SEARCH_DETAILS_VALID_TIME = config_properties['search_details_valid_months']
MINIMUM_SEARCH_QUERIES = config_properties['minimum_search_queries']
CRON_INTERVAL = config_properties['cron_job_interval_in_hours']
MONGODB_URL = config_properties['mongodb_url']

cron = Scheduler(daemon=True)
app = flask.Flask(__name__)

app.config['BASIC_AUTH_USERNAME'] = config_properties['username']
app.config['BASIC_AUTH_PASSWORD'] = config_properties['password']
basic_auth = BasicAuth(app)

@cron.interval_schedule(hours=CRON_INTERVAL)
def update_user_recommendations_db():
    """
    Processing recommendations periodically with a time interval of 24 hours and 
    storing them in a db so that these pre-processed recommendations can be retrieved whenever needed.
    """
    try:
        logger.info("Processing periodic user recommendations")
        process_user_info(SEARCH_DETAILS_VALID_TIME, MINIMUM_SEARCH_QUERIES)
        logger.info("Periodic user recommendations processed successfully")
    except Exception:
        logger.exception("Error when processing periodic user recommendations")

@app.route('/getRecommendations', methods=['GET'])
@basic_auth.required
def recommend_apis():
    """
    Returns API recommendations for a given user in the requested tenant domain.
    """
    try:
        requested_tenant = request.headers['Account']
        try:
            jwt_token = request.headers['X-JWT-Assertion']
            organization = get_company_from_jwt(jwt_token)
        except Exception:
            logger.debug("Error when extracting company details from JWT, default Company selected")
            organization = "Company"
        user = organization + "_" + request.headers['User']
        recommendations = get_user_recommendations(user, requested_tenant, organization, MIN_API_COUNT)
        if recommendations:
            response = jsonify({'user': user, 'requestedTenantDomain': requested_tenant, 'userRecommendations': recommendations})
        else:
            response = jsonify({'user': user, 'requestedTenantDomain': requested_tenant, 'userRecommendations':[]})
    except Exception:
        logger.exception("Exception occurred when getting recommendations for user " + user)
        response = jsonify({'user': user, 'requestedTenantDomain': requested_tenant, 'userRecommendations':[]})
    logger.debug("Server responded to the user " + user + " with " + str(recommendations))
    return response

@app.route('/publishEvents', methods=['POST']) 
@basic_auth.required
def receive_events():
    """
    Recive events related to API recommendations feature.
    """
    data = request.data
    jsonData = json.loads(data[36:-4].decode("utf-8"))
    try:
        jwt_token = request.headers['X-JWT-Assertion']
        organization = get_company_from_jwt(jwt_token)
    except Exception:
        logger.debug("Error when extracting company details from JWT, default Company selected")
        organization = "Company"
    try:
        action = jsonData["action"]
        payload = jsonData["payload"]
        logger.debug("Recommendation event received with Action " + action)
        if (action == 'ADD_API'):
            response = add_API(payload, organization)
        elif(action == 'DELETE_API'):
            response = delete_API(payload, organization)
        elif(action == 'ADD_APPLICATION'):
            response = add_application(payload, organization)
        elif(action == 'DELETE_APPLICATION'):
            response = delete_application(payload, organization)
        elif(action == 'ADD_USER_SEARCHED_QUERY'):
            response = add_search_query(payload, organization)
        elif(action == 'ADD_USER_CLICKED_API'):
            response = add_clicked_API(payload, organization)
        else:
            logger.error("Incorrect action " + action + " used for recommendation event")
            response = Response(status=500)
    except Exception:
        logger.exception("Error occurred when adding the" + action + " event")
        response = Response(status=500)
    return response

@app.route('/health', methods=['GET'])
def health_check():
    """
    Health check resource.
    """
    response = jsonify({'status': 'healthy'})
    return response

def add_API(API_data, organization):
    try:
        add_api_to_db(API_data, organization)
        response = Response(status=200)    
    except Exception:
        logger.exception("Error occurred when adding an API")
        response = Response(status=500)
    return response
    
def delete_API(payload, organization):
    try:
        tenant = payload['tenant']
        api = payload['api_name']
        delete_API_from_db(tenant,api, organization)
        response = Response(status=200)    
    except Exception:
        logger.exception("Error occurred when deleting an API")
        response = Response(status=500)
    return response

def add_application(application_data, organization):
    try:
        update_application_in_db(application_data, organization)
        response = Response(status=200)    
    except Exception:
        logger.exception("Error occurred when adding an application")
        response = Response(status=500)
    return response

def delete_application(payload, organization):
    try:
        app_id = payload["appid"]
        delete_application_from_db(app_id, organization)
        response = Response(status=200)    
    except Exception:
        logger.exception("Error occurred when deleting an application")
        response = Response(status=500)
    return response

def add_search_query(search_query, organization):
    try:
        add_search_query_to_db(search_query, organization)
        response = Response(status=200)    
    except Exception:
        logger.exception("Error occurred when adding a search query")
        response = Response(status=500)
    return response

def add_clicked_API(clicked_API, organization):
    try:
        add_search_query_to_db(clicked_API, organization)
        response = Response(status=200)
    except Exception:
        logger.exception("Error occurred when adding a clicked API")
        response = Response(status=500)
    return response      

def get_company_from_jwt(encoded_jwt):
    content = encoded_jwt.split(".")
    base64_bytes = content[1].encode('ascii')
    un_encoded_bytes = base64.b64decode(base64_bytes)
    message = un_encoded_bytes.decode('ascii')
    messageJson = json.loads(message)
    company = messageJson["http://wso2.org/claims/applicationname"]
    return company 

def main():
    cron.start()
    context = None
    if HTTPS_ENABLED:
        try:
            context = ssl.SSLContext(ssl.PROTOCOL_TLSv1_2)
            context.load_cert_chain(API_CRT, API_KEY)
        except Exception as e:
            sys.exit("Error starting flask server. " +
                "Missing cert or key. Details: {}"
                .format(e))

    update_user_recommendations_db()
    serving.run_simple(API_HOST, API_PORT, app, ssl_context=context, use_debugger=True)

if __name__ == '__main__':
    main()

