import pandas as pd
from rake_nltk import Rake
import spacy
import gensim
import datetime
import pymongo
import json
import yaml
from log import logger

HIGH_WEIGHT = 4
NORMAL_WEIGHT = 3
LOW_WEIGHT = 1

WORD2VEC_MODEL = "../resources/word2vec_model.model"
STOP_WORDS = "../resources/stopList.txt"

TIME_STAMP = 'time_stamp'
USER = 'user'
TENANT = 'tenant'
ORG = 'org'

API_LIST = 'api_list'
USER_LIST = 'user_list'
API_DICTIONARIES = 'api_dictionaries'
USER_RECOMMENDATIONS = 'user_recommendations'
SEARCH_DETAILS = 'search_details'
RECOMMENDATIONS = 'recommendations'

API_IDS = 'api_ids'
API_NAME = 'api_name'
API_ID = 'api_id'

APPLICATION_DETAILS = 'application_details'
APPLICATION_ID = 'application_id'
APPLICATION_NAME = 'application_name'
APPLICATION_DESCRIPTION = 'application_description'

TAGS = 'tags'
CONTEXT = 'context'
RESOURCES = 'resources'
RESOURCE = 'resource'
SUMMARY = 'summary'
DESCRIPTION = 'description'
SEARCH_QUERY = 'search_query'

CONFIG_FILE = '../conf/config.yaml'
with open(CONFIG_FILE, 'r') as stream:
    config_properties = yaml.safe_load(stream)

MONGO_URL = "mongodb://" + config_properties['mongodb_url']
client = pymongo.MongoClient(MONGO_URL)

# Loading models
nlp = spacy.load('en')
word2vec_model = gensim.models.Word2Vec.load(WORD2VEC_MODEL)

def extract_keywords(text):
    stop_words_file = open(STOP_WORDS, 'r')
    contents =list(stop_words_file.read().split('\n'))
    rake_object = Rake(stopwords = contents)
    rake_object.extract_keywords_from_text(text)
    key_words_scores = rake_object.get_word_degrees()
    return key_words_scores

def get_lemma(words):
    """
    Get the lemmatized form of the given set of words
    """
    lemmas = {}
    try:
        for current_word in words:
            word_lemma = nlp(current_word)
            if word_lemma:
                word_lemma = word_lemma[0].lemma_

                if isinstance(words, dict):
                    if word_lemma in lemmas:
                        lemmas[word_lemma] += words[current_word]
                    else:
                        lemmas[word_lemma] = words[current_word]
                else:
                    if word_lemma not in lemmas:
                        lemmas[word_lemma]=1
    except Exception:
        logger.exception("Error when getting lemma for words")
        return words
    return lemmas

def get_synonyms(words,word2vec_model):
    """
    Find synonyms for a set of words using the gensim word2vec model and return the lemmatized synonyms
    """
    words_in_model=[]
    similar_lemmas = {}

    for word in words:
        if (word in word2vec_model):
            words_in_model.append(word)
    if words_in_model:
        similar_words=word2vec_model.wv.most_similar(positive=words_in_model,topn=10)

        for current_word in similar_words:
            nlp_word =nlp(current_word[0])
            if nlp_word:
                word_lemma = nlp_word[0].lemma_
            if ((word_lemma not in similar_lemmas) and (word_lemma not in words)):
                similar_lemmas[word_lemma] = current_word[1]
    return similar_lemmas

#-------------------------------
# Processing API dictionary
#-------------------------------

def create_API_dictionary(API_document):
    """
    Create a dictionary for the given API which contains the keywords relevant to the api and 
    the weight of each keyword.
    """
    # The dictionary that contains keywords of the API
    API_keyword_dictionary = {}

    # add the name of the API
    try:
        if API_NAME not in API_document.keys():
            return API_keyword_dictionary
        else:
            name_API = API_document[API_NAME]
            API_keyword_dictionary = add_API_name(name_API,API_keyword_dictionary)
            logger.debug("Name of" + name_API + " added to API keyword dictionary")
    except Exception:
        logger.exception("Error adding name of " + name_API + " to api dictionary")
        return API_keyword_dictionary

    # add the tags of the API
    try:
        if TAGS in API_document.keys():
            tags_API = API_document[TAGS]
            API_keyword_dictionary = add_tags(tags_API,API_keyword_dictionary)
            logger.debug("Tags added to API keyword dictionary")
    except Exception:
        logger.exception("Error adding tag to API keyword dictionary")

    # add the context of each API
    try:
        if CONTEXT in API_document.keys():
            context_API = API_document[CONTEXT]
            API_keyword_dictionary = add_context(context_API,API_keyword_dictionary)
            logger.debug("Context added to API keyword dictionary")
    except Exception:
        logger.exception("Error adding context to API keyword dictionary")

    # add the resource names of each API
    try:
        if RESOURCES in API_document.keys():
            resources = API_document[RESOURCES]
            API_keyword_dictionary = add_resources(resources,API_keyword_dictionary)
            logger.debug("Resource added to API keyword dictionary")
    except Exception:
        logger.exception("Error adding Resource to API keyword dictionary")

    # Add keywords from the API description
    try:
        if DESCRIPTION in API_document.keys():
            API_description = API_document[DESCRIPTION]
            API_keyword_dictionary = add_API_description(API_description,API_keyword_dictionary)
            logger.debug("Description added to API keyword dictionary")
    except Exception:
        logger.exception("Error adding Description to API keyword dictionary")

    # Get lemmas of the keywords
    API_keyword_dictionary = get_lemma(API_keyword_dictionary)
    logger.debug("API dictionary successfully created for API")
    return API_keyword_dictionary

def add_API_name(name,keyword_dictionary):
    keyword_dictionary[name.lower()] = HIGH_WEIGHT
    names = extract_keywords(name).keys()
    for sub_name in names:
        if sub_name.lower() not in keyword_dictionary:
            keyword_dictionary[sub_name.lower()] = NORMAL_WEIGHT
    return keyword_dictionary

def add_tags(tags, keyword_dictionary):
    if tags == "[]":
        return keyword_dictionary

    tags = list(tags.lstrip('[').rstrip(']').split(','))
    for sub_tag in tags:
        sub_tag = sub_tag.strip().lower()
        if sub_tag in keyword_dictionary:
            keyword_dictionary[sub_tag] += NORMAL_WEIGHT
        else:
            keyword_dictionary[sub_tag] = HIGH_WEIGHT
    return keyword_dictionary

def add_context(context,keyword_dictionary):
    context_words = context.lstrip("/").split('/')
    if context_words[0]=="t":
        context_words = context_words[2:-1]
    else:
        context_words = context_words[:-1]

    for word in context_words:
        word_keys=extract_keywords(word).keys()
        if word in keyword_dictionary:
            keyword_dictionary[word]+=LOW_WEIGHT
        else:
            keyword_dictionary[word]=LOW_WEIGHT
        for word_key in word_keys:
            if word_key not in context_words:
                if word_key in keyword_dictionary:
                    keyword_dictionary[word_key] += LOW_WEIGHT
                else:
                    keyword_dictionary[word_key] = LOW_WEIGHT
    return keyword_dictionary

def add_resources(resources,keyword_dictionary):
    for resourceObj in resources:
        resource = resourceObj[RESOURCE]
        if resource == "*":
            continue
        if resource in keyword_dictionary:
            keyword_dictionary[resource] += LOW_WEIGHT
        else:
            keyword_dictionary[resource]=LOW_WEIGHT
        add_resource_details(resourceObj,RESOURCE,keyword_dictionary)
        add_resource_details(resourceObj,SUMMARY,keyword_dictionary)
        add_resource_details(resourceObj,DESCRIPTION,keyword_dictionary)
    return keyword_dictionary

def add_resource_details(resource,key,keyword_dictionary):
    if key in resource.keys():
        description_keys = extract_keywords(resource[key]).keys()
        for description_key in description_keys:
            if description_key in keyword_dictionary:
                keyword_dictionary[description_key] += LOW_WEIGHT
            else:
                keyword_dictionary[description_key] = LOW_WEIGHT
    return keyword_dictionary

def add_API_description(description,keyword_dictionary):
    if description=='null':
        return keyword_dictionary
    if isinstance(description, str):
        description_keywords = extract_keywords(description)
        keyword_dictionary = {key: keyword_dictionary.get(key, 0) + description_keywords.get(key, 0)
                             for key in set(keyword_dictionary) | set(description_keywords)}
    return keyword_dictionary

#------------------------------
# Processing User Dictionary
#------------------------------

def create_user_dictionary(user_name, time_limit):
    """
    Create a dictionary for the given user with keywords relevant to the user and their weights.
    """
    # Dictionary that contains keywords related to the user
    user_keyword_dictionary = {}

    # Getting User Details from the 'User_details' collection
    try:
        search_result = get_user_db_entries(SEARCH_DETAILS, user_name, time_limit)
    except Exception:
        logger.exception("Error getting user search details for the user" + user_name + " from db")
        search_result = []

    # Add the names of the APIs clicked by the user to the dictionary
    for query in search_result:
        try:
            if API_NAME in query.keys():
                clicked_API = query[API_NAME]
                user_keyword_dictionary = add_clicked_APIs(clicked_API,user_keyword_dictionary)
        except Exception:
            logger.exception("Error adding clicked api to User Keyword dictionary")

    # Add the queries searched by the user
        try:
            if SEARCH_QUERY in query.keys():
                # search_query = query['Search_query'].lower()
                search_query = query[SEARCH_QUERY]
                if TAGS in search_query:
                    search_query = search_query.split("=")[1]
                    user_keyword_dictionary = add_clicked_tag(search_query, user_keyword_dictionary)
        except Exception:
            logger.exception("Error adding clicked tag to User Keyword dictionary")
            continue

    # add Application Details
    try:
        app_result = get_user_db_entries(APPLICATION_DETAILS, user_name, time_limit)
    except Exception:
        logger.exception("Error getting application details for the user " + user_name)
        app_result = []

    for app in app_result:
        application_keywords=[]
        try:
            application_name = app[APPLICATION_NAME]
            user_keyword_dictionary, application_keywords = add_app_name(application_name,
            application_keywords, user_keyword_dictionary)
            logger.debug("Application "+ application_name+ " was added to User Keyword dictionary")
        except Exception:
            logger.exception("Error adding application name to User Keyword dictionary")
        try:
            application_description = app[APPLICATION_DESCRIPTION]
            user_keyword_dictionary, application_keywords = add_app_description(application_description,
            application_keywords, user_keyword_dictionary)
        except Exception:
            logger.exception("Error adding description for application")
            continue

        try:
            synonyms = get_synonyms(application_keywords,word2vec_model)
            user_keyword_dictionary = {key: user_keyword_dictionary.get(key, 0) + synonyms.get(key, 0)
                                    for key in set(user_keyword_dictionary) | set(synonyms)}
        except Exception:
            logger.exception("Error creating synonyms for app")

    # Getting the lemmas of the keywords    
    user_keyword_dictionary = get_lemma(user_keyword_dictionary)
    logger.debug("User dictionary created for user "+ user_name)
    return user_keyword_dictionary

def get_user_db_entries(table_name, user, time_limit):
    """
    Retrieve the entries after the given time limit from the needed db collection.
    If time_limit is set to -1 all the entries are retrieved.
    """
    if time_limit == -1:
        db_table = connect_db(table_name)
        result = db_table.find({USER:user})
        return result
    else:
        db_table = connect_db(table_name)
        result_newer = db_table.find({TIME_STAMP:{"$gt": time_limit}, USER:user})
        return result_newer

def add_clicked_APIs(API,keyword_dictionary):
    API = API.lower()
    if API in keyword_dictionary:
        keyword_dictionary[API] += NORMAL_WEIGHT
    else:
        keyword_dictionary[API] = HIGH_WEIGHT
    names = extract_keywords(API).keys()
    for name in names:
        name = name.lower()
        if name not in keyword_dictionary:
            keyword_dictionary[name] = NORMAL_WEIGHT
    return keyword_dictionary

def add_clicked_tag(tag,keyword_dictionary):
    tag = tag.lower()
    if tag in keyword_dictionary:
        keyword_dictionary[tag]+= NORMAL_WEIGHT
    else:
        keyword_dictionary[tag]= HIGH_WEIGHT
    return keyword_dictionary

def add_app_name(app_name, app_words, keyword_dictionary):
    sub_names = app_name.split()
    for sub_name in sub_names:
        sub_name = sub_name.lower()
        if sub_name in keyword_dictionary:
            keyword_dictionary[sub_name]+=NORMAL_WEIGHT
        else:
            keyword_dictionary[sub_name]=HIGH_WEIGHT
        if sub_name not in app_words:
            app_words.append(sub_name)
    return keyword_dictionary, app_words

def add_app_description(app_description, app_words, keyword_dictionary):
    if isinstance(app_description, str):
        description_keywords = extract_keywords(app_description)
        keyword_dictionary = {key: keyword_dictionary.get(key, 0) + description_keywords.get(key, 0)
                                for key in set(keyword_dictionary) | set(description_keywords)}
    main_keywords = sorted(description_keywords.items() , reverse=True, key=lambda x: x[1])[:5]
    for element in main_keywords :
        app_words.append(element[0] )
    return keyword_dictionary,app_words


# Connection to the MongoDB
def connect_db(table_name):
    database = client["RecommendationSystemDB"]
    table = database[table_name]
    return table