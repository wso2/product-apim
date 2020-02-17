# from dictionary_processor import *
from recommendation import *
from log import logger

def add_api_to_db(API, organization):
    update_API_dictionary(API,organization)
    update_API_id_db(API, organization)

def delete_API_from_db(tenant, API_name, organization):
    delete_API_dictionary(tenant, API_name, organization)
    delete_api_id_db(tenant, API_name, organization)
    logger.debug("api " + API_name + " belongs to "+ tenant + " deleted from db")

def update_application_in_db(application, organization):
    time_stamp = get_current_time()
    db_table = connect_db(APPLICATION_DETAILS)
    app_identifier = { ORG : organization, APPLICATION_ID : application[APPLICATION_ID] }
    entry = db_table.find(app_identifier)
    if entry.count()>0:
        updated_values = { "$set": { APPLICATION_NAME: application[APPLICATION_NAME],
                    APPLICATION_DESCRIPTION: application[APPLICATION_DESCRIPTION], TIME_STAMP: time_stamp } }
        db_table.update_one(app_identifier, updated_values)
        logger.debug("Application " + application["application_name"] + " was updated in the database")
    else:
        user = organization + "_" + application[USER]
        application[USER] = user
        application[TIME_STAMP] = time_stamp
        application[ORG] = organization
        result = db_table.insert(application,check_keys=False)
        logger.debug("Application " + application["application_name"] + " was added to database")
    
def delete_application_from_db(app_ID, organization):
    db_table = connect_db(APPLICATION_DETAILS)
    app_identifier = { ORG : organization, APPLICATION_ID : app_ID }
    entry = db_table.find(app_identifier)
    if entry.count()>0:
        result = db_table.delete_one({ ORG : organization, APPLICATION_ID:app_ID })
        logger.debug ("Application with ID " + str(app_ID) + " of " + organization + " was deleted from the db")

def add_search_query_to_db(search_query, organization):
    """
    Add Search queries, clicked apis and clicked tags at the dev portal to the 'Search_details' collection.
    """
    time_stamp = get_current_time()
    db_table = connect_db(SEARCH_DETAILS)
    search_query[TIME_STAMP] = time_stamp
    user = organization + "_" + search_query[USER]
    search_query[USER] = user
    update_user_list_db(user, organization)
    result = db_table.insert(search_query,check_keys=False)
    logger.debug ("Search query " + str(search_query) + " was added to db")
  
def get_user_recommendations(user, tenant, organization, min_api_count):
    """
    Get the recommendations as a list of api names. The no. of apis recommended is configurable.
    """
    update_user_list_db(user, organization)
    api_list_db = connect_db(API_LIST)
    api_list = api_list_db.find({ ORG:organization, TENANT : tenant })
    if api_list.count()>0:
        API_ids = api_list[0][API_IDS]
        apis_count = len(API_ids.keys())
        if apis_count>min_api_count:
            recommendations = generate_final_recommendation(user, tenant, organization)
            recommended_api_list = []
            for api in recommendations:
                if api in API_ids.keys():
                    recommended_api_list.append({'id': API_ids[api], 'name':api})
            return recommended_api_list
        else:
            return None
    else:
        return None
    
    

def get_current_time():
    time_stamp = str(datetime.datetime.today())
    return time_stamp
    
def update_user_list_db(user_name, organization):
    """
    'User_List' collection contains a list of all the users
    """
    user_db = connect_db(USER_LIST)
    entry = {USER : user_name, ORG: organization}
    if user_db.find(entry).count()==0:
        user_db.insert(entry,check_keys=False)

def update_API_id_db(API, organization):
    """
    'API_List' collection contains the name and id of each API.
    This method adds a new entry to that collection, with API name and id of APIs of each tenant.
    """

    API_id = API[API_ID]
    API_name = API[API_NAME]
    tenant = API[TENANT]
    api_id_db = connect_db(API_LIST)
    entry = api_id_db.find({ORG: organization, TENANT : tenant})
    if entry.count()>0:
        api_ids = entry[0][API_IDS]
        if API_id not in api_ids.keys():
            api_ids[API_name] = API_id
        entry_identifier = {ORG: organization,TENANT : tenant}
        updated_entry = {"$set": { API_IDS: api_ids}}
        api_id_db.update_one(entry_identifier, updated_entry)
    else:
        entry = {ORG: organization, TENANT : tenant, API_IDS: {API_name:API_id}}
        result = api_id_db.insert(entry,check_keys=False)

def delete_api_id_db(tenant, API_name, organization):
    """
    This method deletes the entry with the given API name and tenant, from the 'API_List' collection.
    """
    api_id_db = connect_db(API_LIST)
    entry = api_id_db.find({ORG: organization, TENANT : tenant})
    if entry.count()>0:
        api_ids = entry[0][API_IDS]
        result = api_ids.pop(API_name, None)
        entry_identifier = {ORG: organization, TENANT : tenant}
        updated_entry = {"$set": { API_IDS: api_ids}}
        api_id_db.update_one(entry_identifier, updated_entry)


def update_API_dictionary(API, organization):
    """
    Creates the keyword dictionary for the given api and
    store the api dictionary in the 'API_dictionaries' collection under the relevant tenant domain.
    """

    API_id = API[API_ID]
    tenant = API[TENANT]
    API_name = API[API_NAME]
    API_dictionary = create_API_dictionary(API)
    dictionary_table = connect_db(API_DICTIONARIES)
    tenant_entry = dictionary_table.find({ORG:organization, TENANT:tenant})
    if tenant_entry.count()>0:
        API_dictionaries = tenant_entry[0][API_DICTIONARIES]
        API_dictionaries[API_name] = API_dictionary
        entry_identifier = { ORG:organization, TENANT: tenant }
        updated_dictionary = {"$set": { API_DICTIONARIES: API_dictionaries}}
        dictionary_table.update_one(entry_identifier, updated_dictionary)
    else:
        tenant_entry = { ORG:organization, TENANT: tenant, API_DICTIONARIES: {API_name: API_dictionary}}
        result = dictionary_table.insert(tenant_entry,check_keys=False)
    logger.debug("API dictionary was successfull updated for API " + API_name)
    
def delete_API_dictionary(tenant, API_name, organization):
    """
    Deletes the API_dictionary from the collection when the api is deleted.
    """
    dictionary_table = connect_db(API_DICTIONARIES)
    tenant_entry = dictionary_table.find({ORG:organization, TENANT : tenant})
    if tenant_entry.count()>0:
        API_dictionaries = tenant_entry[0][API_DICTIONARIES]
        result = API_dictionaries.pop(API_name, None)
        entry_identifier = {ORG:organization, TENANT : tenant }
        updated_dictionary = {"$set": { API_DICTIONARIES: API_dictionaries}}
        dictionary_table.update_one(entry_identifier, updated_dictionary)
        logger.debug("API dictionary was deleted for API " + API_name)
    

def modify_search_db(user, months, min_entries):
    """
    Removes older search queries from the "Search_details" collection
    """
    time_limit = str(datetime.datetime.today()-datetime.timedelta(months*365/12))
    
    db_table = connect_db(SEARCH_DETAILS)
    result=db_table.find({USER : user})
    result_newer = db_table.find({TIME_STAMP:{"$gt": time_limit}, USER : user})
    new_entry_count = result_newer.count()          # No. of entries within 3 months
    extra_entry_count = result.count()-min_entries
    
    # Removing the rest if the no. of entries within 'months' is greater than 'min_entries'
    if new_entry_count>min_entries:             
        db_table.remove({TIME_STAMP: {"$lt": time_limit}, USER : user})
    elif extra_entry_count>0:
        extra_time_limit = db_table.find({ USER : user})[extra_entry_count]["_id"]
        db_table.remove({'_id':{"$lt":extra_time_limit}, USER : user})

def modify_user_recommendations_db(user, tenant, organization):
    """
    Generate api recommendations for the given user in the given tenant domain and
    store them in the 'User_recommendations' collection
    """
    time_limit = str(datetime.datetime.now())
    recommendation = generate_recommendations(user, tenant, organization, time_limit = -1)
    dictionary_table = connect_db(USER_RECOMMENDATIONS)
    db_entry = {TIME_STAMP:time_limit, USER:user, TENANT: tenant, ORG: organization, RECOMMENDATIONS:recommendation}
    dictionary_table.remove({ USER:user, TENANT: tenant, ORG: organization })
    result = dictionary_table.insert(db_entry,check_keys=False)

def process_user_info(months, min_entries):
    """
    Process details of each user available in the 'User_List' collection
    """
    api_dictionaries_table = connect_db(API_DICTIONARIES)
    users_table = connect_db(USER_LIST)
    users = users_table.find()
    if users.count()>0:
        for user in users:
            user_name = user[USER]
            user_organization = user[ORG]
            modify_search_db(user_name, months, min_entries)
            tenant_dictionaries = api_dictionaries_table.find({ORG:user_organization})
            if tenant_dictionaries.count()>0:
                for tenant in tenant_dictionaries:
                    tenant_name = tenant[TENANT]
                    organization_name = tenant[ORG]
                    modify_user_recommendations_db(user_name, tenant_name, organization_name)

