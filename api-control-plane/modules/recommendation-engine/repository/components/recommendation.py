import pandas as pd
from dictionary_processor import *
from log import logger

PROGRESSION_FACTOR = 1.2

def create_matrix(tenant_dictionary):
    """
    Creates the API-keyword matrix where the rows represent APIs and columns represent all the keywords.
    The matrix contain the weights of each keyword in the APIs.

    @type  tenant_dictionary: dictionary
    @rtype:   pandas dataframe
    """
    matrix = pd.DataFrame()
    for API_name in tenant_dictionary:
        API_dictionary = tenant_dictionary[API_name]
        for keyword in API_dictionary:
            if keyword not in matrix.columns:
                matrix[keyword]=0
            matrix.at[API_name, keyword] = API_dictionary[keyword]
    matrix = matrix.fillna(0)
    return matrix

def get_pearson_correlation(user,matrix):
    """
    Calculate the similarity between a given row with other rows using Pearson Correlation and sort them.

    @type  user:    String
    @type  matrix:  pandas dataframe
    @rtype:   list of tuples in the form of [(x,y), (a,b)]
    """
    transposed_matrix = matrix.transpose()
    user_data = transposed_matrix[user]
    correlation = transposed_matrix.corrwith(user_data)
    most_similar = correlation.sort_values(ascending=False)
    return most_similar

def generate_recommendations(user, tenant, organization, time_limit):
    """
    @type  user: String
    @type   tenant: String
    @type   time_limit: int
    @rtype:   dictionary
    """
    API_dictionary_collection = connect_db(API_DICTIONARIES)
    user_dictionary = {}
    tenant_entry = API_dictionary_collection.find({ ORG:organization, TENANT : tenant })
    if tenant_entry.count()>0:
        API_dictionaries = tenant_entry[0][API_DICTIONARIES]
        user_dictionary = create_user_dictionary(user,time_limit)
    else:
        API_dictionaries = {}
    if bool(user_dictionary) and bool(API_dictionaries):

        # Adding the keywords from the user dictionary to the API-keyword matrix as a new row
        API_dictionaries[USER] = user_dictionary
        overall_matrix = create_matrix(API_dictionaries)

        # Calculating the similarity between the row-User with other rows(APIs) sorting them 
        # to get the list of APIs in the descending order of similarity to the given user.

        recommendations = dict(get_pearson_correlation(USER,overall_matrix))
        recommendations.pop(USER, None)
        logger.debug('Initial recommendations generated for user ' + user + " are " +str(recommendations))
        return recommendations
    else:
        return {}

def get_recommendations_from_db(user, tenant, organization):
    """
    Get pre-processed recommendations stored in the db, for the given user for the requested tenant domain

    @type  user: String
    @type   tenant: String
    @rtype  older_recommendations:   dictionary
    @rtype  time_limit:   int
    """
    table_recommendations = connect_db(USER_RECOMMENDATIONS)
    user_entry = table_recommendations.find({USER:user, TENANT: tenant, ORG: organization})
    if user_entry.count()>0:
        try:
            time_limit = user_entry[0][TIME_STAMP]
            older_recommendations = user_entry[0][RECOMMENDATIONS]
        except Exception:
            logger.exception("Error getting preprocessed recomendations from db for user " + user + " for tenant " + tenant)
            time_limit = -1
            older_recommendations = {}
    else:
        older_recommendations = {}
        time_limit = -1
    logger.debug("Older recommendations got for user " + user + str(older_recommendations))
    return older_recommendations, time_limit

def combine_recommendations(old_recommendations, new_recommendations):
    """
    Combine pre-processed recommendations and real-time processed recommendations and
    return them as a list of API names

    @type  old_recommendations: dictionary
    @type   new_recommendations: dictionary
    @rtype  :   dictionary

    """
    try:
        combined_recommendations = {}
        for API in new_recommendations:
            weight = new_recommendations[API]
            weight = weight*PROGRESSION_FACTOR
            if API in old_recommendations.keys():
                final_weight = max(old_recommendations[API],weight)
            else:
                final_weight = weight
            combined_recommendations[API] = final_weight
        combined_recommendations = dict(sorted(combined_recommendations.items() , reverse=True, key=lambda x: x[1]))
        logger.debug("Recommendations combined")
        return combined_recommendations
    except Exception:
        logger.exception("Error combining recommendations")
        return {}

def generate_final_recommendation(user, tenant, organization):
    """
    @type   user: String
    @type   tenant: String
    @type   organization: String
    @rtype  list
    """
    older_recommendations, time_limit = get_recommendations_from_db(user, tenant, organization)
    new_recommendations = generate_recommendations(user, tenant, organization, time_limit)
    if not older_recommendations:
        final_recommendations = new_recommendations
    elif not new_recommendations:
        final_recommendations = older_recommendations
    else:
        final_recommendations = combine_recommendations(older_recommendations,new_recommendations)
    return [api for api in final_recommendations if final_recommendations[api]>0]

  