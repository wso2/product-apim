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
import jwt
from jwt.contrib.algorithms.pycrypto import RSAAlgorithm
from apscheduler.scheduler import Scheduler
import logging
logging.basicConfig(level=logging.DEBUG)

API_CRT = "utils/Certificates/server.crt"
API_KEY = "utils/Certificates/server.key"

HTTPS_ENABLED = True
VERIFY_USER = True

API_HOST = config_properties['host']
API_PORT = config_properties['port']

RECOMMENDATION_COUNT = config_properties['recommendations_count']
MIN_API_COUNT = config_properties['minimum_APIs_to_start_recommendations']
SEARCH_DETAILS_VALID_TIME = config_properties['search_details_valid_months']
MINIMUM_SEARCH_QUERIES = config_properties['minimum_search_queries']
MONGODB_URL = config_properties['mongodb_url']

cron = Scheduler(daemon=True)
app = flask.Flask(__name__)

app.config['BASIC_AUTH_USERNAME'] = config_properties['username']
app.config['BASIC_AUTH_PASSWORD'] = config_properties['password']
basic_auth = BasicAuth(app)

jwt.unregister_algorithm('RS256')
jwt.register_algorithm('RS256', RSAAlgorithm(RSAAlgorithm.SHA256))

@cron.interval_schedule(minutes=10) 
def update_user_recommendations_db():
    """
    Processing recommendations periodically with a time interval of 24 hours and 
    storing them in a db so that these pre-processed recommendations can be retrieved whenever needed.
    """
    try:
        process_user_info(SEARCH_DETAILS_VALID_TIME, MINIMUM_SEARCH_QUERIES)
        logging.info("Processing periodic user recommendations")
    except Exception as e:
        logging.error(e)

@app.route('/getRecommendations', methods=['GET'])
@basic_auth.required
def recommend_apis():
    """
    Returns API recommendations for a given user in the requested tenant domain.
    """
    try:
        requested_tenant = request.headers['Account']
        jwt_token = request.headers['X-JWT-Assertion']
        organization = get_company_from_jwt(jwt_token)
        print (str(organization))
        user = organization + "_" + request.headers['User']
        recommendations = get_user_recommendations(user, requested_tenant, organization, RECOMMENDATION_COUNT, MIN_API_COUNT)
        if recommendations:
            response = jsonify({'user': user, 'requestedTenantDomain': requested_tenant, 'userRecommendations': recommendations})
        else:
            response = jsonify({'user': user, 'requestedTenantDomain': requested_tenant, 'userRecommendations':[]})
    except Exception as e:
        logging.error(e)
        response = jsonify({'user': user, 'requestedTenantDomain': requested_tenant, 'userRecommendations':[]})
    print(str(response.json))
    return response

@app.route('/publishEvents', methods=['POST']) 
@basic_auth.required
def receive_events():

    data = request.data
    jsonData = json.loads(data[36:-4].decode("utf-8"))
    jwt_token = request.headers['X-JWT-Assertion']
    organization = get_company_from_jwt(jwt_token)
    print (str(organization))

    try:
        action = jsonData["action"]
        payload = jsonData["payload"]
        print("Event action: " + str(action) + " and payload: " + str(payload))
        if (action == 'ADD_API'):
            response = add_API(payload, organization)
        elif(action == 'DELETE_API'):
            response = delete_API(payload, organization)
        elif(action == 'ADD_NEW_APPLICATION'):
            response = add_application(payload, organization)
        elif(action == 'UPDATED_APPLICATION'):
            response = update_application(payload, organization)
        elif(action == 'DELETE_APPLICATION'):
            response = delete_application(payload, organization)
        elif(action == 'ADD_USER_SEARCHED_QUERY'):
            response = add_search_query(payload, organization)
        elif(action == 'ADD_USER_CLICKED_API'):
            response = add_clicked_API(payload, organization)   
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response

def add_API(API_data, organization):
    try:
        add_api_to_db(API_data, organization)
        response = Response(status=200)    
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response
    
def delete_API(payload, organization):
    try:
        tenant = payload['tenant']
        api = payload['api_name']
        delete_API_from_db(tenant,api, organization)
        response = Response(status=200)    
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response

def add_application(application_data, organization):
    try:
        add_application_to_db(application_data, organization)
        response = Response(status=200)    
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response

def update_application(application_data, organization):
    try:
        update_application_in_db(application_data, organization)
        response = Response(status=200)    
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response

def delete_application(payload, organization):
    try:
        app_id = payload["appid"]
        delete_application_from_db(app_id, organization)
        response = Response(status=200)    
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response

def add_search_query(search_query, organization):
    try:
        add_search_query_to_db(search_query, organization)
        response = Response(status=200)    
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response

def add_clicked_API(clicked_API, organization):
    try:
        add_search_query_to_db(clicked_API, organization)
        response = Response(status=200)
    except Exception as e:
        logging.error(e)
        response = Response(status=500)
    return response      

def get_company_from_jwt(encoded_jwt):
    pemfile = open("public_key.pem", 'r')
    keystring = pemfile.read()
    pemfile.close()
    decoded_message = jwt.decode(encoded_jwt, keystring, algorithms=['RS256'])
    company = decoded_message["http://wso2.org/claims/applicationname"]
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

