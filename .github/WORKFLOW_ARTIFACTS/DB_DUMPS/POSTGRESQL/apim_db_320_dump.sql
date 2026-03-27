--
-- PostgreSQL database dump
--

\restrict 9ths1bMTVM7pZenZgUHp77YVdBMkzogKl6urOnQiAfPvB38cZ1lZGpjvGHA35cZ

-- Dumped from database version 16.10 (Debian 16.10-1.pgdg13+1)
-- Dumped by pg_dump version 16.10 (Debian 16.10-1.pgdg13+1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: update_modified_column(); Type: FUNCTION; Schema: public; Owner: apim_user
--

CREATE FUNCTION public.update_modified_column() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    NEW.TIME_STAMP= now();
    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_modified_column() OWNER TO apim_user;

--
-- Name: am_alert_emaillist_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_alert_emaillist_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_alert_emaillist_seq OWNER TO apim_user;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: am_alert_emaillist; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_alert_emaillist (
    email_list_id integer DEFAULT nextval('public.am_alert_emaillist_seq'::regclass) NOT NULL,
    user_name character varying(255) NOT NULL,
    stake_holder character varying(100) NOT NULL
);


ALTER TABLE public.am_alert_emaillist OWNER TO apim_user;

--
-- Name: am_alert_emaillist_details; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_alert_emaillist_details (
    email_list_id integer NOT NULL,
    email character varying(255) NOT NULL
);


ALTER TABLE public.am_alert_emaillist_details OWNER TO apim_user;

--
-- Name: am_alert_types_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_alert_types_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_alert_types_seq OWNER TO apim_user;

--
-- Name: am_alert_types; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_alert_types (
    alert_type_id integer DEFAULT nextval('public.am_alert_types_seq'::regclass) NOT NULL,
    alert_type_name character varying(255) NOT NULL,
    stake_holder character varying(100) NOT NULL
);


ALTER TABLE public.am_alert_types OWNER TO apim_user;

--
-- Name: am_alert_types_values; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_alert_types_values (
    alert_type_id integer NOT NULL,
    user_name character varying(255) NOT NULL,
    stake_holder character varying(100) NOT NULL
);


ALTER TABLE public.am_alert_types_values OWNER TO apim_user;

--
-- Name: am_api_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_sequence OWNER TO apim_user;

--
-- Name: am_api; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api (
    api_id integer DEFAULT nextval('public.am_api_sequence'::regclass) NOT NULL,
    api_provider character varying(256),
    api_name character varying(256),
    api_version character varying(30),
    context character varying(256),
    context_template character varying(256),
    api_tier character varying(256),
    api_type character varying(10),
    created_by character varying(100),
    created_time timestamp without time zone,
    updated_by character varying(100),
    updated_time timestamp without time zone
);


ALTER TABLE public.am_api OWNER TO apim_user;

--
-- Name: am_api_categories; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_categories (
    uuid character varying(50) NOT NULL,
    name character varying(255),
    description character varying(1024),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.am_api_categories OWNER TO apim_user;

--
-- Name: am_api_client_certificate; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_client_certificate (
    tenant_id integer NOT NULL,
    alias character varying(45) NOT NULL,
    api_id integer NOT NULL,
    certificate bytea NOT NULL,
    removed boolean DEFAULT false NOT NULL,
    tier_name character varying(512)
);


ALTER TABLE public.am_api_client_certificate OWNER TO apim_user;

--
-- Name: am_api_comments; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_comments (
    comment_id character varying(255) NOT NULL,
    comment_text character varying(512),
    commented_user character varying(255),
    date_commented timestamp without time zone NOT NULL,
    api_id integer
);


ALTER TABLE public.am_api_comments OWNER TO apim_user;

--
-- Name: am_api_default_version_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_default_version_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_default_version_pk_seq OWNER TO apim_user;

--
-- Name: am_api_default_version; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_default_version (
    default_version_id integer DEFAULT nextval('public.am_api_default_version_pk_seq'::regclass) NOT NULL,
    api_name character varying(256) NOT NULL,
    api_provider character varying(256) NOT NULL,
    default_api_version character varying(30),
    published_default_api_version character varying(30)
);


ALTER TABLE public.am_api_default_version OWNER TO apim_user;

--
-- Name: am_api_lc_event_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_lc_event_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_lc_event_sequence OWNER TO apim_user;

--
-- Name: am_api_lc_event; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_lc_event (
    event_id integer DEFAULT nextval('public.am_api_lc_event_sequence'::regclass) NOT NULL,
    api_id integer NOT NULL,
    previous_state character varying(50),
    new_state character varying(50) NOT NULL,
    user_id character varying(50) NOT NULL,
    tenant_id integer NOT NULL,
    event_date timestamp without time zone NOT NULL
);


ALTER TABLE public.am_api_lc_event OWNER TO apim_user;

--
-- Name: am_api_lc_publish_events_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_lc_publish_events_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_lc_publish_events_pk_seq OWNER TO apim_user;

--
-- Name: am_api_lc_publish_events; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_lc_publish_events (
    id integer DEFAULT nextval('public.am_api_lc_publish_events_pk_seq'::regclass) NOT NULL,
    tenant_domain character varying(500) NOT NULL,
    api_id character varying(500) NOT NULL,
    event_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.am_api_lc_publish_events OWNER TO apim_user;

--
-- Name: am_api_product_mapping_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_product_mapping_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_product_mapping_sequence OWNER TO apim_user;

--
-- Name: am_api_product_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_product_mapping (
    api_product_mapping_id integer DEFAULT nextval('public.am_api_product_mapping_sequence'::regclass) NOT NULL,
    api_id integer,
    url_mapping_id integer
);


ALTER TABLE public.am_api_product_mapping OWNER TO apim_user;

--
-- Name: am_api_ratings; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_ratings (
    rating_id character varying(255) NOT NULL,
    api_id integer,
    rating integer,
    subscriber_id integer
);


ALTER TABLE public.am_api_ratings OWNER TO apim_user;

--
-- Name: am_api_ratings_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_ratings_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_ratings_sequence OWNER TO apim_user;

--
-- Name: am_api_resource_scope_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_resource_scope_mapping (
    scope_name character varying(256) NOT NULL,
    url_mapping_id integer NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.am_api_resource_scope_mapping OWNER TO apim_user;

--
-- Name: am_api_system_apps_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_system_apps_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_system_apps_sequence OWNER TO apim_user;

--
-- Name: am_api_throttle_policy_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_throttle_policy_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_throttle_policy_seq OWNER TO apim_user;

--
-- Name: am_api_throttle_policy; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_throttle_policy (
    policy_id integer DEFAULT nextval('public.am_api_throttle_policy_seq'::regclass) NOT NULL,
    name character varying(512) NOT NULL,
    display_name character varying(512) DEFAULT NULL::character varying,
    tenant_id integer NOT NULL,
    description character varying(1024),
    default_quota_type character varying(25) NOT NULL,
    default_quota integer NOT NULL,
    default_quota_unit character varying(10),
    default_unit_time integer NOT NULL,
    default_time_unit character varying(25) NOT NULL,
    applicable_level character varying(25) NOT NULL,
    is_deployed boolean DEFAULT false NOT NULL,
    uuid character varying(256)
);


ALTER TABLE public.am_api_throttle_policy OWNER TO apim_user;

--
-- Name: am_api_url_mapping_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_api_url_mapping_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_api_url_mapping_sequence OWNER TO apim_user;

--
-- Name: am_api_url_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_api_url_mapping (
    url_mapping_id integer DEFAULT nextval('public.am_api_url_mapping_sequence'::regclass) NOT NULL,
    api_id integer NOT NULL,
    http_method character varying(20),
    auth_scheme character varying(50),
    url_pattern character varying(512),
    throttling_tier character varying(512) DEFAULT NULL::character varying,
    mediation_script bytea
);


ALTER TABLE public.am_api_url_mapping OWNER TO apim_user;

--
-- Name: am_app_key_domain_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_app_key_domain_mapping (
    consumer_key character varying(255) NOT NULL,
    authz_domain character varying(255) DEFAULT 'ALL'::character varying NOT NULL
);


ALTER TABLE public.am_app_key_domain_mapping OWNER TO apim_user;

--
-- Name: am_application_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_application_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_application_sequence OWNER TO apim_user;

--
-- Name: am_application; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_application (
    application_id integer DEFAULT nextval('public.am_application_sequence'::regclass) NOT NULL,
    name character varying(100),
    subscriber_id integer,
    application_tier character varying(50) DEFAULT 'Unlimited'::character varying,
    callback_url character varying(512),
    description character varying(512),
    application_status character varying(50) DEFAULT 'APPROVED'::character varying,
    group_id character varying(100),
    created_by character varying(100),
    created_time timestamp without time zone,
    updated_by character varying(100),
    updated_time timestamp without time zone,
    uuid character varying(256),
    token_type character varying(10)
);


ALTER TABLE public.am_application OWNER TO apim_user;

--
-- Name: am_application_attributes; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_application_attributes (
    application_id integer NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(1024) NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.am_application_attributes OWNER TO apim_user;

--
-- Name: am_application_group_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_application_group_mapping (
    application_id integer NOT NULL,
    group_id character varying(512) NOT NULL,
    tenant character varying(255) NOT NULL
);


ALTER TABLE public.am_application_group_mapping OWNER TO apim_user;

--
-- Name: am_application_key_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_application_key_mapping (
    uuid character varying(100),
    application_id integer NOT NULL,
    consumer_key character varying(512),
    key_type character varying(512) NOT NULL,
    state character varying(30),
    create_mode character varying(30) DEFAULT 'CREATED'::character varying,
    app_info bytea,
    key_manager character varying(100) NOT NULL
);


ALTER TABLE public.am_application_key_mapping OWNER TO apim_user;

--
-- Name: am_application_registration_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_application_registration_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_application_registration_sequence OWNER TO apim_user;

--
-- Name: am_application_registration; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_application_registration (
    reg_id integer DEFAULT nextval('public.am_application_registration_sequence'::regclass) NOT NULL,
    subscriber_id integer,
    wf_ref character varying(255) NOT NULL,
    app_id integer,
    token_type character varying(30),
    token_scope character varying(1500) DEFAULT 'default'::character varying,
    inputs character varying(1000),
    allowed_domains character varying(256),
    validity_period bigint,
    key_manager character varying(255) NOT NULL
);


ALTER TABLE public.am_application_registration OWNER TO apim_user;

--
-- Name: am_block_conditions_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_block_conditions_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_block_conditions_seq OWNER TO apim_user;

--
-- Name: am_block_conditions; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_block_conditions (
    condition_id integer DEFAULT nextval('public.am_block_conditions_seq'::regclass) NOT NULL,
    type character varying(45) DEFAULT NULL::character varying,
    value character varying(512) DEFAULT NULL::character varying,
    enabled character varying(45) DEFAULT NULL::character varying,
    domain character varying(45) DEFAULT NULL::character varying,
    uuid character varying(256)
);


ALTER TABLE public.am_block_conditions OWNER TO apim_user;

--
-- Name: am_certificate_metadata; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_certificate_metadata (
    tenant_id integer NOT NULL,
    alias character varying(255) NOT NULL,
    end_point character varying(255) NOT NULL
);


ALTER TABLE public.am_certificate_metadata OWNER TO apim_user;

--
-- Name: am_condition_group_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_condition_group_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_condition_group_seq OWNER TO apim_user;

--
-- Name: am_condition_group; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_condition_group (
    condition_group_id integer DEFAULT nextval('public.am_condition_group_seq'::regclass) NOT NULL,
    policy_id integer NOT NULL,
    quota_type character varying(25),
    quota integer NOT NULL,
    quota_unit character varying(10) DEFAULT NULL::character varying,
    unit_time integer NOT NULL,
    time_unit character varying(25) NOT NULL,
    description character varying(1024) DEFAULT NULL::character varying
);


ALTER TABLE public.am_condition_group OWNER TO apim_user;

--
-- Name: am_correlation_configs; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_correlation_configs (
    component_name character varying(45) NOT NULL,
    enabled character varying(45) NOT NULL
);


ALTER TABLE public.am_correlation_configs OWNER TO apim_user;

--
-- Name: am_correlation_properties; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_correlation_properties (
    property_name character varying(45) NOT NULL,
    component_name character varying(45) NOT NULL,
    property_value character varying(1023) NOT NULL
);


ALTER TABLE public.am_correlation_properties OWNER TO apim_user;

--
-- Name: am_external_stores_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_external_stores_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_external_stores_sequence OWNER TO apim_user;

--
-- Name: am_external_stores; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_external_stores (
    apistore_id integer DEFAULT nextval('public.am_external_stores_sequence'::regclass) NOT NULL,
    api_id integer,
    store_id character varying(255) NOT NULL,
    store_display_name character varying(255) NOT NULL,
    store_endpoint character varying(255) NOT NULL,
    store_type character varying(255) NOT NULL,
    last_updated_time timestamp without time zone
);


ALTER TABLE public.am_external_stores OWNER TO apim_user;

--
-- Name: am_graphql_complexity; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_graphql_complexity (
    uuid character varying(256) NOT NULL,
    api_id integer NOT NULL,
    type character varying(256),
    field character varying(256),
    complexity_value integer
);


ALTER TABLE public.am_graphql_complexity OWNER TO apim_user;

--
-- Name: am_gw_api_artifacts; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_gw_api_artifacts (
    api_id character varying(255) NOT NULL,
    artifact bytea,
    gateway_instruction character varying(20),
    gateway_label character varying(255) NOT NULL,
    time_stamp timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.am_gw_api_artifacts OWNER TO apim_user;

--
-- Name: am_gw_published_api_details; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_gw_published_api_details (
    api_id character varying(255) NOT NULL,
    tenant_domain character varying(255),
    api_provider character varying(255),
    api_name character varying(255),
    api_version character varying(255)
);


ALTER TABLE public.am_gw_published_api_details OWNER TO apim_user;

--
-- Name: am_header_field_condition_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_header_field_condition_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_header_field_condition_seq OWNER TO apim_user;

--
-- Name: am_header_field_condition; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_header_field_condition (
    header_field_id integer DEFAULT nextval('public.am_header_field_condition_seq'::regclass) NOT NULL,
    condition_group_id integer NOT NULL,
    header_field_name character varying(255) DEFAULT NULL::character varying,
    header_field_value character varying(255) DEFAULT NULL::character varying,
    is_header_field_mapping boolean DEFAULT true
);


ALTER TABLE public.am_header_field_condition OWNER TO apim_user;

--
-- Name: am_ip_condition_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_ip_condition_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_ip_condition_seq OWNER TO apim_user;

--
-- Name: am_ip_condition; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_ip_condition (
    am_ip_condition_id integer DEFAULT nextval('public.am_ip_condition_seq'::regclass) NOT NULL,
    starting_ip character varying(45),
    ending_ip character varying(45),
    specific_ip character varying(45),
    within_ip_range boolean DEFAULT true,
    condition_group_id integer
);


ALTER TABLE public.am_ip_condition OWNER TO apim_user;

--
-- Name: am_jwt_claim_condition_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_jwt_claim_condition_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_jwt_claim_condition_seq OWNER TO apim_user;

--
-- Name: am_jwt_claim_condition; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_jwt_claim_condition (
    jwt_claim_id integer DEFAULT nextval('public.am_jwt_claim_condition_seq'::regclass) NOT NULL,
    condition_group_id integer NOT NULL,
    claim_uri character varying(512) DEFAULT NULL::character varying,
    claim_attrib character varying(1024) DEFAULT NULL::character varying,
    is_claim_mapping boolean DEFAULT true
);


ALTER TABLE public.am_jwt_claim_condition OWNER TO apim_user;

--
-- Name: am_key_manager; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_key_manager (
    uuid character varying(50) NOT NULL,
    name character varying(100),
    display_name character varying(100),
    description character varying(256),
    type character varying(45),
    configuration bytea,
    enabled boolean DEFAULT true,
    tenant_domain character varying(100)
);


ALTER TABLE public.am_key_manager OWNER TO apim_user;

--
-- Name: am_label_urls; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_label_urls (
    label_id character varying(50) NOT NULL,
    access_url character varying(255) NOT NULL
);


ALTER TABLE public.am_label_urls OWNER TO apim_user;

--
-- Name: am_labels; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_labels (
    label_id character varying(50) NOT NULL,
    name character varying(255),
    description character varying(1024),
    tenant_domain character varying(255)
);


ALTER TABLE public.am_labels OWNER TO apim_user;

--
-- Name: am_monetization_usage; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_monetization_usage (
    id character varying(100) NOT NULL,
    state character varying(50) NOT NULL,
    status character varying(50) NOT NULL,
    started_time character varying(50) NOT NULL,
    published_time character varying(50) NOT NULL
);


ALTER TABLE public.am_monetization_usage OWNER TO apim_user;

--
-- Name: am_notification_subscriber; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_notification_subscriber (
    uuid character varying(255) NOT NULL,
    category character varying(255),
    notification_method character varying(255),
    subscriber_address character varying(255) NOT NULL
);


ALTER TABLE public.am_notification_subscriber OWNER TO apim_user;

--
-- Name: am_policy_application_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_policy_application_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_policy_application_seq OWNER TO apim_user;

--
-- Name: am_policy_application; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_policy_application (
    policy_id integer DEFAULT nextval('public.am_policy_application_seq'::regclass) NOT NULL,
    name character varying(512) NOT NULL,
    display_name character varying(512) DEFAULT NULL::character varying,
    tenant_id integer NOT NULL,
    description character varying(1024) DEFAULT NULL::character varying,
    quota_type character varying(25) NOT NULL,
    quota integer NOT NULL,
    quota_unit character varying(10) DEFAULT NULL::character varying,
    unit_time integer NOT NULL,
    time_unit character varying(25) NOT NULL,
    is_deployed boolean DEFAULT false NOT NULL,
    custom_attributes bytea,
    uuid character varying(256)
);


ALTER TABLE public.am_policy_application OWNER TO apim_user;

--
-- Name: am_policy_global_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_policy_global_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_policy_global_seq OWNER TO apim_user;

--
-- Name: am_policy_global; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_policy_global (
    policy_id integer DEFAULT nextval('public.am_policy_global_seq'::regclass) NOT NULL,
    name character varying(512) NOT NULL,
    key_template character varying(512) NOT NULL,
    tenant_id integer NOT NULL,
    description character varying(1024) DEFAULT NULL::character varying,
    siddhi_query bytea,
    is_deployed boolean DEFAULT false NOT NULL,
    uuid character varying(256)
);


ALTER TABLE public.am_policy_global OWNER TO apim_user;

--
-- Name: am_policy_hard_throttling_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_policy_hard_throttling_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_policy_hard_throttling_seq OWNER TO apim_user;

--
-- Name: am_policy_hard_throttling; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_policy_hard_throttling (
    policy_id integer DEFAULT nextval('public.am_policy_hard_throttling_seq'::regclass) NOT NULL,
    name character varying(512) NOT NULL,
    tenant_id integer NOT NULL,
    description character varying(1024) DEFAULT NULL::character varying,
    quota_type character varying(25) NOT NULL,
    quota integer NOT NULL,
    quota_unit character varying(10) DEFAULT NULL::character varying,
    unit_time integer NOT NULL,
    time_unit character varying(25) NOT NULL,
    is_deployed boolean DEFAULT false NOT NULL
);


ALTER TABLE public.am_policy_hard_throttling OWNER TO apim_user;

--
-- Name: am_policy_subscription_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_policy_subscription_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_policy_subscription_seq OWNER TO apim_user;

--
-- Name: am_policy_subscription; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_policy_subscription (
    policy_id integer DEFAULT nextval('public.am_policy_subscription_seq'::regclass) NOT NULL,
    name character varying(512) NOT NULL,
    display_name character varying(512) DEFAULT NULL::character varying,
    tenant_id integer NOT NULL,
    description character varying(1024) DEFAULT NULL::character varying,
    quota_type character varying(25) NOT NULL,
    quota integer NOT NULL,
    quota_unit character varying(10),
    unit_time integer NOT NULL,
    time_unit character varying(25) NOT NULL,
    rate_limit_count integer,
    rate_limit_time_unit character varying(25) DEFAULT NULL::character varying,
    is_deployed boolean DEFAULT false NOT NULL,
    custom_attributes bytea,
    stop_on_quota_reach boolean DEFAULT false NOT NULL,
    billing_plan character varying(20) NOT NULL,
    uuid character varying(256),
    monetization_plan character varying(25) DEFAULT NULL::character varying,
    fixed_rate character varying(15) DEFAULT NULL::character varying,
    billing_cycle character varying(15) DEFAULT NULL::character varying,
    price_per_request character varying(15) DEFAULT NULL::character varying,
    currency character varying(15) DEFAULT NULL::character varying,
    max_complexity integer DEFAULT 0 NOT NULL,
    max_depth integer DEFAULT 0 NOT NULL
);


ALTER TABLE public.am_policy_subscription OWNER TO apim_user;

--
-- Name: am_query_parameter_condition_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_query_parameter_condition_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_query_parameter_condition_seq OWNER TO apim_user;

--
-- Name: am_query_parameter_condition; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_query_parameter_condition (
    query_parameter_id integer DEFAULT nextval('public.am_query_parameter_condition_seq'::regclass) NOT NULL,
    condition_group_id integer NOT NULL,
    parameter_name character varying(255) DEFAULT NULL::character varying,
    parameter_value character varying(255) DEFAULT NULL::character varying,
    is_param_mapping boolean DEFAULT true
);


ALTER TABLE public.am_query_parameter_condition OWNER TO apim_user;

--
-- Name: am_revoked_jwt; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_revoked_jwt (
    uuid character varying(255) NOT NULL,
    signature character varying(2048) NOT NULL,
    expiry_timestamp bigint NOT NULL,
    tenant_id integer DEFAULT '-1'::integer,
    token_type character varying(15) DEFAULT 'DEFAULT'::character varying,
    time_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP
);


ALTER TABLE public.am_revoked_jwt OWNER TO apim_user;

--
-- Name: am_scope_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_scope_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_scope_pk_seq OWNER TO apim_user;

--
-- Name: am_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_scope (
    scope_id integer DEFAULT nextval('public.am_scope_pk_seq'::regclass) NOT NULL,
    name character varying(255) NOT NULL,
    display_name character varying(255) NOT NULL,
    description character varying(512),
    tenant_id integer DEFAULT '-1'::integer NOT NULL,
    scope_type character varying(255) NOT NULL
);


ALTER TABLE public.am_scope OWNER TO apim_user;

--
-- Name: am_scope_binding; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_scope_binding (
    scope_id integer NOT NULL,
    scope_binding character varying(255) NOT NULL,
    binding_type character varying(255) NOT NULL
);


ALTER TABLE public.am_scope_binding OWNER TO apim_user;

--
-- Name: am_security_audit_uuid_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_security_audit_uuid_mapping (
    api_id integer NOT NULL,
    audit_uuid character varying(255) NOT NULL
);


ALTER TABLE public.am_security_audit_uuid_mapping OWNER TO apim_user;

--
-- Name: am_shared_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_shared_scope (
    name character varying(255),
    uuid character varying(256) NOT NULL,
    tenant_id integer
);


ALTER TABLE public.am_shared_scope OWNER TO apim_user;

--
-- Name: am_subscriber_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_subscriber_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_subscriber_sequence OWNER TO apim_user;

--
-- Name: am_subscriber; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_subscriber (
    subscriber_id integer DEFAULT nextval('public.am_subscriber_sequence'::regclass) NOT NULL,
    user_id character varying(50) NOT NULL,
    tenant_id integer NOT NULL,
    email_address character varying(256),
    date_subscribed timestamp without time zone NOT NULL,
    created_by character varying(100),
    created_time timestamp without time zone,
    updated_by character varying(100),
    updated_time timestamp without time zone
);


ALTER TABLE public.am_subscriber OWNER TO apim_user;

--
-- Name: am_subscription_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_subscription_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_subscription_sequence OWNER TO apim_user;

--
-- Name: am_subscription; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_subscription (
    subscription_id integer DEFAULT nextval('public.am_subscription_sequence'::regclass) NOT NULL,
    tier_id character varying(50),
    tier_id_pending character varying(50),
    api_id integer,
    last_accessed timestamp without time zone,
    application_id integer,
    sub_status character varying(50),
    subs_create_state character varying(50) DEFAULT 'SUBSCRIBE'::character varying,
    created_by character varying(100),
    created_time timestamp without time zone,
    updated_by character varying(100),
    updated_time timestamp without time zone,
    uuid character varying(256)
);


ALTER TABLE public.am_subscription OWNER TO apim_user;

--
-- Name: am_subscription_key_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_subscription_key_mapping (
    subscription_id integer NOT NULL,
    access_token character varying(512) NOT NULL,
    key_type character varying(512) NOT NULL
);


ALTER TABLE public.am_subscription_key_mapping OWNER TO apim_user;

--
-- Name: am_system_apps; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_system_apps (
    id integer DEFAULT nextval('public.am_api_system_apps_sequence'::regclass) NOT NULL,
    name character varying(50) NOT NULL,
    consumer_key character varying(512) NOT NULL,
    consumer_secret character varying(512) NOT NULL,
    tenant_domain character varying(255) DEFAULT 'carbon.super'::character varying,
    created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.am_system_apps OWNER TO apim_user;

--
-- Name: am_tenant_themes; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_tenant_themes (
    tenant_id integer NOT NULL,
    theme bytea NOT NULL
);


ALTER TABLE public.am_tenant_themes OWNER TO apim_user;

--
-- Name: am_throttle_tier_permissions_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_throttle_tier_permissions_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_throttle_tier_permissions_seq OWNER TO apim_user;

--
-- Name: am_throttle_tier_permissions; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_throttle_tier_permissions (
    throttle_tier_permissions_id integer DEFAULT nextval('public.am_throttle_tier_permissions_seq'::regclass) NOT NULL,
    tier character varying(50),
    permissions_type character varying(50),
    roles character varying(512),
    tenant_id integer
);


ALTER TABLE public.am_throttle_tier_permissions OWNER TO apim_user;

--
-- Name: am_tier_permissions_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_tier_permissions_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_tier_permissions_sequence OWNER TO apim_user;

--
-- Name: am_tier_permissions; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_tier_permissions (
    tier_permissions_id integer DEFAULT nextval('public.am_tier_permissions_sequence'::regclass) NOT NULL,
    tier character varying(50) NOT NULL,
    permissions_type character varying(50) NOT NULL,
    roles character varying(512) NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.am_tier_permissions OWNER TO apim_user;

--
-- Name: am_usage_uploaded_files; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_usage_uploaded_files (
    tenant_domain character varying(255) NOT NULL,
    file_name character varying(255) NOT NULL,
    file_timestamp timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    file_processed integer DEFAULT 0,
    file_content bytea
);


ALTER TABLE public.am_usage_uploaded_files OWNER TO apim_user;

--
-- Name: am_user; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_user (
    user_id character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL
);


ALTER TABLE public.am_user OWNER TO apim_user;

--
-- Name: am_workflows_sequence; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.am_workflows_sequence
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.am_workflows_sequence OWNER TO apim_user;

--
-- Name: am_workflows; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.am_workflows (
    wf_id integer DEFAULT nextval('public.am_workflows_sequence'::regclass) NOT NULL,
    wf_reference character varying(255) NOT NULL,
    wf_type character varying(255) NOT NULL,
    wf_status character varying(255) NOT NULL,
    wf_created_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    wf_updated_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    wf_status_desc character varying(1000),
    tenant_id integer,
    tenant_domain character varying(255),
    wf_external_reference character varying(255) NOT NULL,
    wf_metadata bytea,
    wf_properties bytea
);


ALTER TABLE public.am_workflows OWNER TO apim_user;

--
-- Name: cm_consent_receipt_property; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_consent_receipt_property (
    consent_receipt_id character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    value character varying(1023) NOT NULL
);


ALTER TABLE public.cm_consent_receipt_property OWNER TO apim_user;

--
-- Name: cm_pii_category_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.cm_pii_category_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cm_pii_category_pk_seq OWNER TO apim_user;

--
-- Name: cm_pii_category; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_pii_category (
    id integer DEFAULT nextval('public.cm_pii_category_pk_seq'::regclass) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(1023),
    display_name character varying(255),
    is_sensitive integer NOT NULL,
    tenant_id integer DEFAULT '-1234'::integer
);


ALTER TABLE public.cm_pii_category OWNER TO apim_user;

--
-- Name: cm_purpose_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.cm_purpose_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cm_purpose_pk_seq OWNER TO apim_user;

--
-- Name: cm_purpose; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_purpose (
    id integer DEFAULT nextval('public.cm_purpose_pk_seq'::regclass) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(1023),
    purpose_group character varying(255) NOT NULL,
    group_type character varying(255) NOT NULL,
    tenant_id integer DEFAULT '-1234'::integer
);


ALTER TABLE public.cm_purpose OWNER TO apim_user;

--
-- Name: cm_purpose_category_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.cm_purpose_category_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cm_purpose_category_pk_seq OWNER TO apim_user;

--
-- Name: cm_purpose_category; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_purpose_category (
    id integer DEFAULT nextval('public.cm_purpose_category_pk_seq'::regclass) NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(1023),
    tenant_id integer DEFAULT '-1234'::integer
);


ALTER TABLE public.cm_purpose_category OWNER TO apim_user;

--
-- Name: cm_purpose_pii_cat_assoc; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_purpose_pii_cat_assoc (
    purpose_id integer NOT NULL,
    cm_pii_category_id integer NOT NULL,
    is_mandatory integer NOT NULL
);


ALTER TABLE public.cm_purpose_pii_cat_assoc OWNER TO apim_user;

--
-- Name: cm_receipt; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_receipt (
    consent_receipt_id character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    jurisdiction character varying(255) NOT NULL,
    consent_timestamp timestamp without time zone NOT NULL,
    collection_method character varying(255) NOT NULL,
    language character varying(255) NOT NULL,
    pii_principal_id character varying(255) NOT NULL,
    principal_tenant_id integer DEFAULT '-1234'::integer,
    policy_url character varying(255) NOT NULL,
    state character varying(255) NOT NULL,
    pii_controller character varying(2048) NOT NULL
);


ALTER TABLE public.cm_receipt OWNER TO apim_user;

--
-- Name: cm_receipt_sp_assoc_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.cm_receipt_sp_assoc_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cm_receipt_sp_assoc_pk_seq OWNER TO apim_user;

--
-- Name: cm_receipt_sp_assoc; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_receipt_sp_assoc (
    id integer DEFAULT nextval('public.cm_receipt_sp_assoc_pk_seq'::regclass) NOT NULL,
    consent_receipt_id character varying(255) NOT NULL,
    sp_name character varying(255) NOT NULL,
    sp_display_name character varying(255),
    sp_description character varying(255),
    sp_tenant_id integer DEFAULT '-1234'::integer
);


ALTER TABLE public.cm_receipt_sp_assoc OWNER TO apim_user;

--
-- Name: cm_sp_purpose_assoc_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.cm_sp_purpose_assoc_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.cm_sp_purpose_assoc_pk_seq OWNER TO apim_user;

--
-- Name: cm_sp_purpose_assoc; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_sp_purpose_assoc (
    id integer DEFAULT nextval('public.cm_sp_purpose_assoc_pk_seq'::regclass) NOT NULL,
    receipt_sp_assoc integer NOT NULL,
    purpose_id integer NOT NULL,
    consent_type character varying(255) NOT NULL,
    is_primary_purpose integer NOT NULL,
    termination character varying(255) NOT NULL,
    third_party_disclosure integer NOT NULL,
    third_party_name character varying(255)
);


ALTER TABLE public.cm_sp_purpose_assoc OWNER TO apim_user;

--
-- Name: cm_sp_purpose_pii_cat_assoc; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_sp_purpose_pii_cat_assoc (
    sp_purpose_assoc_id integer NOT NULL,
    pii_category_id integer NOT NULL,
    validity character varying(1023)
);


ALTER TABLE public.cm_sp_purpose_pii_cat_assoc OWNER TO apim_user;

--
-- Name: cm_sp_purpose_purpose_cat_assc; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.cm_sp_purpose_purpose_cat_assc (
    sp_purpose_assoc_id integer NOT NULL,
    purpose_category_id integer NOT NULL
);


ALTER TABLE public.cm_sp_purpose_purpose_cat_assc OWNER TO apim_user;

--
-- Name: fido2_device_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.fido2_device_store (
    tenant_id integer,
    domain_name character varying(255) NOT NULL,
    user_name character varying(45) NOT NULL,
    time_registered timestamp without time zone,
    user_handle character varying(64) NOT NULL,
    credential_id character varying(200) NOT NULL,
    public_key_cose character varying(1024) NOT NULL,
    signature_count bigint,
    user_identity character varying(512) NOT NULL,
    display_name character varying(255),
    is_usernameless_supported character(1) DEFAULT '0'::bpchar
);


ALTER TABLE public.fido2_device_store OWNER TO apim_user;

--
-- Name: fido_device_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.fido_device_store (
    tenant_id integer NOT NULL,
    domain_name character varying(255) NOT NULL,
    user_name character varying(45) NOT NULL,
    time_registered timestamp without time zone,
    key_handle character varying(200) NOT NULL,
    device_data character varying(2048) NOT NULL
);


ALTER TABLE public.fido_device_store OWNER TO apim_user;

--
-- Name: idn_associated_id_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_associated_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_associated_id_seq OWNER TO apim_user;

--
-- Name: idn_associated_id; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_associated_id (
    id integer DEFAULT nextval('public.idn_associated_id_seq'::regclass) NOT NULL,
    idp_user_id character varying(255) NOT NULL,
    tenant_id integer DEFAULT '-1234'::integer,
    idp_id integer NOT NULL,
    domain_name character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL,
    association_id character(36) NOT NULL
);


ALTER TABLE public.idn_associated_id OWNER TO apim_user;

--
-- Name: idn_auth_session_app_info; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_session_app_info (
    session_id character varying(100) NOT NULL,
    subject character varying(100) NOT NULL,
    app_id integer NOT NULL,
    inbound_auth_type character varying(255) NOT NULL
);


ALTER TABLE public.idn_auth_session_app_info OWNER TO apim_user;

--
-- Name: idn_auth_session_meta_data; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_session_meta_data (
    session_id character varying(100) NOT NULL,
    property_type character varying(100) NOT NULL,
    value character varying(255) NOT NULL
);


ALTER TABLE public.idn_auth_session_meta_data OWNER TO apim_user;

--
-- Name: idn_auth_session_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_session_store (
    session_id character varying(100) NOT NULL,
    session_type character varying(100) NOT NULL,
    operation character varying(10) NOT NULL,
    session_object bytea,
    time_created bigint NOT NULL,
    tenant_id integer DEFAULT '-1'::integer,
    expiry_time bigint
);


ALTER TABLE public.idn_auth_session_store OWNER TO apim_user;

--
-- Name: idn_auth_temp_session_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_temp_session_store (
    session_id character varying(100) NOT NULL,
    session_type character varying(100) NOT NULL,
    operation character varying(10) NOT NULL,
    session_object bytea,
    time_created bigint NOT NULL,
    tenant_id integer DEFAULT '-1'::integer,
    expiry_time bigint
);


ALTER TABLE public.idn_auth_temp_session_store OWNER TO apim_user;

--
-- Name: idn_auth_user; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_user (
    user_id character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL,
    tenant_id integer NOT NULL,
    domain_name character varying(255) NOT NULL,
    idp_id integer NOT NULL
);


ALTER TABLE public.idn_auth_user OWNER TO apim_user;

--
-- Name: idn_auth_user_session_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_user_session_mapping (
    user_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL
);


ALTER TABLE public.idn_auth_user_session_mapping OWNER TO apim_user;

--
-- Name: idn_auth_wait_status_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_auth_wait_status_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_auth_wait_status_seq OWNER TO apim_user;

--
-- Name: idn_auth_wait_status; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_auth_wait_status (
    id integer DEFAULT nextval('public.idn_auth_wait_status_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    long_wait_key character varying(255) NOT NULL,
    wait_status character(1) DEFAULT '1'::bpchar NOT NULL,
    time_created timestamp without time zone,
    expire_time timestamp without time zone
);


ALTER TABLE public.idn_auth_wait_status OWNER TO apim_user;

--
-- Name: idn_base_table; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_base_table (
    product_name character varying(20) NOT NULL
);


ALTER TABLE public.idn_base_table OWNER TO apim_user;

--
-- Name: idn_certificate_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_certificate_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_certificate_pk_seq OWNER TO apim_user;

--
-- Name: idn_certificate; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_certificate (
    id integer DEFAULT nextval('public.idn_certificate_pk_seq'::regclass) NOT NULL,
    name character varying(100),
    certificate_in_pem bytea,
    tenant_id integer DEFAULT 0
);


ALTER TABLE public.idn_certificate OWNER TO apim_user;

--
-- Name: idn_claim_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_claim_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_claim_seq OWNER TO apim_user;

--
-- Name: idn_claim; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_claim (
    id integer DEFAULT nextval('public.idn_claim_seq'::regclass) NOT NULL,
    dialect_id integer NOT NULL,
    claim_uri character varying(255) NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.idn_claim OWNER TO apim_user;

--
-- Name: idn_claim_dialect_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_claim_dialect_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_claim_dialect_seq OWNER TO apim_user;

--
-- Name: idn_claim_dialect; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_claim_dialect (
    id integer DEFAULT nextval('public.idn_claim_dialect_seq'::regclass) NOT NULL,
    dialect_uri character varying(255) NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.idn_claim_dialect OWNER TO apim_user;

--
-- Name: idn_claim_mapped_attribute_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_claim_mapped_attribute_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_claim_mapped_attribute_seq OWNER TO apim_user;

--
-- Name: idn_claim_mapped_attribute; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_claim_mapped_attribute (
    id integer DEFAULT nextval('public.idn_claim_mapped_attribute_seq'::regclass) NOT NULL,
    local_claim_id integer,
    user_store_domain_name character varying(255) NOT NULL,
    attribute_name character varying(255) NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.idn_claim_mapped_attribute OWNER TO apim_user;

--
-- Name: idn_claim_mapping_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_claim_mapping_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_claim_mapping_seq OWNER TO apim_user;

--
-- Name: idn_claim_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_claim_mapping (
    id integer DEFAULT nextval('public.idn_claim_mapping_seq'::regclass) NOT NULL,
    ext_claim_id integer NOT NULL,
    mapped_local_claim_id integer NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.idn_claim_mapping OWNER TO apim_user;

--
-- Name: idn_claim_property_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_claim_property_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_claim_property_seq OWNER TO apim_user;

--
-- Name: idn_claim_property; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_claim_property (
    id integer DEFAULT nextval('public.idn_claim_property_seq'::regclass) NOT NULL,
    local_claim_id integer,
    property_name character varying(255) NOT NULL,
    property_value character varying(255) NOT NULL,
    tenant_id integer NOT NULL
);


ALTER TABLE public.idn_claim_property OWNER TO apim_user;

--
-- Name: idn_fed_auth_session_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_fed_auth_session_mapping (
    idp_session_id character varying(255) NOT NULL,
    session_id character varying(255) NOT NULL,
    idp_name character varying(255) NOT NULL,
    authenticator_id character varying(255),
    protocol_type character varying(255),
    time_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.idn_fed_auth_session_mapping OWNER TO apim_user;

--
-- Name: idn_function_library; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_function_library (
    name character varying(255) NOT NULL,
    description character varying(1023),
    type character varying(255) NOT NULL,
    tenant_id integer NOT NULL,
    data bytea NOT NULL
);


ALTER TABLE public.idn_function_library OWNER TO apim_user;

--
-- Name: idn_identity_meta_data; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_identity_meta_data (
    user_name character varying(255) NOT NULL,
    tenant_id integer DEFAULT '-1234'::integer NOT NULL,
    metadata_type character varying(255) NOT NULL,
    metadata character varying(255) NOT NULL,
    valid character varying(255) NOT NULL
);


ALTER TABLE public.idn_identity_meta_data OWNER TO apim_user;

--
-- Name: idn_identity_user_data; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_identity_user_data (
    tenant_id integer DEFAULT '-1234'::integer NOT NULL,
    user_name character varying(255) NOT NULL,
    data_key character varying(255) NOT NULL,
    data_value character varying(2048)
);


ALTER TABLE public.idn_identity_user_data OWNER TO apim_user;

--
-- Name: idn_oauth1a_access_token; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth1a_access_token (
    access_token character varying(512) NOT NULL,
    access_token_secret character varying(512),
    consumer_key_id integer,
    scope character varying(2048),
    authz_user character varying(512),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_oauth1a_access_token OWNER TO apim_user;

--
-- Name: idn_oauth1a_request_token; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth1a_request_token (
    request_token character varying(512) NOT NULL,
    request_token_secret character varying(512),
    consumer_key_id integer,
    callback_url character varying(2048),
    scope character varying(2048),
    authorized character varying(128),
    oauth_verifier character varying(512),
    authz_user character varying(512),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_oauth1a_request_token OWNER TO apim_user;

--
-- Name: idn_oauth2_access_token; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_access_token (
    token_id character varying(255) NOT NULL,
    access_token character varying(2048),
    refresh_token character varying(2048),
    consumer_key_id integer,
    authz_user character varying(100),
    tenant_id integer,
    user_domain character varying(50),
    user_type character varying(25),
    grant_type character varying(50),
    time_created timestamp without time zone,
    refresh_token_time_created timestamp without time zone,
    validity_period bigint,
    refresh_token_validity_period bigint,
    token_scope_hash character varying(32),
    token_state character varying(25) DEFAULT 'ACTIVE'::character varying,
    token_state_id character varying(128) DEFAULT 'NONE'::character varying,
    subject_identifier character varying(255),
    access_token_hash character varying(512),
    refresh_token_hash character varying(512),
    idp_id integer DEFAULT '-1'::integer NOT NULL,
    token_binding_ref character varying(32) DEFAULT 'NONE'::character varying
);


ALTER TABLE public.idn_oauth2_access_token OWNER TO apim_user;

--
-- Name: idn_oauth2_access_token_audit; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_access_token_audit (
    token_id character varying(255),
    access_token character varying(2048),
    refresh_token character varying(2048),
    consumer_key_id integer,
    authz_user character varying(100),
    tenant_id integer,
    user_domain character varying(50),
    user_type character varying(25),
    grant_type character varying(50),
    time_created timestamp without time zone,
    refresh_token_time_created timestamp without time zone,
    validity_period bigint,
    refresh_token_validity_period bigint,
    token_scope_hash character varying(32),
    token_state character varying(25),
    token_state_id character varying(128),
    subject_identifier character varying(255),
    access_token_hash character varying(512),
    refresh_token_hash character varying(512),
    invalidated_time timestamp without time zone,
    idp_id integer DEFAULT '-1'::integer NOT NULL
);


ALTER TABLE public.idn_oauth2_access_token_audit OWNER TO apim_user;

--
-- Name: idn_oauth2_access_token_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_access_token_scope (
    token_id character varying(255) NOT NULL,
    token_scope character varying(60) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_oauth2_access_token_scope OWNER TO apim_user;

--
-- Name: idn_oauth2_authorization_code; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_authorization_code (
    code_id character varying(255) NOT NULL,
    authorization_code character varying(2048),
    consumer_key_id integer,
    callback_url character varying(2048),
    scope character varying(2048),
    authz_user character varying(100),
    tenant_id integer,
    user_domain character varying(50),
    time_created timestamp without time zone,
    validity_period bigint,
    state character varying(25) DEFAULT 'ACTIVE'::character varying,
    token_id character varying(255),
    subject_identifier character varying(255),
    pkce_code_challenge character varying(255),
    pkce_code_challenge_method character varying(128),
    authorization_code_hash character varying(512),
    idp_id integer DEFAULT '-1'::integer NOT NULL
);


ALTER TABLE public.idn_oauth2_authorization_code OWNER TO apim_user;

--
-- Name: idn_oauth2_authz_code_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_authz_code_scope (
    code_id character varying(255) NOT NULL,
    scope character varying(60) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_oauth2_authz_code_scope OWNER TO apim_user;

--
-- Name: idn_oauth2_ciba_auth_code; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_ciba_auth_code (
    auth_code_key character(36) NOT NULL,
    auth_req_id character(36),
    issued_time timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    consumer_key character varying(255),
    last_polled_time timestamp without time zone NOT NULL,
    polling_interval integer,
    expires_in integer,
    authenticated_user_name character varying(255),
    user_store_domain character varying(100),
    tenant_id integer,
    auth_req_status character varying(100) DEFAULT 'REQUESTED'::character varying,
    idp_id integer
);


ALTER TABLE public.idn_oauth2_ciba_auth_code OWNER TO apim_user;

--
-- Name: idn_oauth2_ciba_request_scopes; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_ciba_request_scopes (
    auth_code_key character(36),
    scope character varying(255)
);


ALTER TABLE public.idn_oauth2_ciba_request_scopes OWNER TO apim_user;

--
-- Name: idn_oauth2_device_flow; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_device_flow (
    code_id character varying(255),
    device_code character varying(255) NOT NULL,
    user_code character varying(25),
    consumer_key_id integer,
    last_poll_time timestamp without time zone NOT NULL,
    expiry_time timestamp without time zone NOT NULL,
    time_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    poll_time bigint,
    status character varying(25) DEFAULT 'PENDING'::character varying,
    authz_user character varying(100),
    tenant_id integer,
    user_domain character varying(50),
    idp_id integer
);


ALTER TABLE public.idn_oauth2_device_flow OWNER TO apim_user;

--
-- Name: idn_oauth2_device_flow_scopes; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_device_flow_scopes (
    id integer NOT NULL,
    scope_id character varying(255),
    scope character varying(255)
);


ALTER TABLE public.idn_oauth2_device_flow_scopes OWNER TO apim_user;

--
-- Name: idn_oauth2_device_flow_scopes_id_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oauth2_device_flow_scopes_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oauth2_device_flow_scopes_id_seq OWNER TO apim_user;

--
-- Name: idn_oauth2_device_flow_scopes_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: apim_user
--

ALTER SEQUENCE public.idn_oauth2_device_flow_scopes_id_seq OWNED BY public.idn_oauth2_device_flow_scopes.id;


--
-- Name: idn_oauth2_resource_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_resource_scope (
    resource_path character varying(255) NOT NULL,
    scope_id integer NOT NULL,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_oauth2_resource_scope OWNER TO apim_user;

--
-- Name: idn_oauth2_scope_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oauth2_scope_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oauth2_scope_pk_seq OWNER TO apim_user;

--
-- Name: idn_oauth2_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_scope (
    scope_id integer DEFAULT nextval('public.idn_oauth2_scope_pk_seq'::regclass) NOT NULL,
    name character varying(255) NOT NULL,
    display_name character varying(255) NOT NULL,
    description character varying(512),
    tenant_id integer DEFAULT '-1'::integer NOT NULL,
    scope_type character varying(255) NOT NULL
);


ALTER TABLE public.idn_oauth2_scope OWNER TO apim_user;

--
-- Name: idn_oauth2_scope_binding; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_scope_binding (
    scope_id integer NOT NULL,
    scope_binding character varying(255) NOT NULL,
    binding_type character varying(255) NOT NULL
);


ALTER TABLE public.idn_oauth2_scope_binding OWNER TO apim_user;

--
-- Name: idn_oauth2_scope_validators; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_scope_validators (
    app_id integer NOT NULL,
    scope_validator character varying(128) NOT NULL
);


ALTER TABLE public.idn_oauth2_scope_validators OWNER TO apim_user;

--
-- Name: idn_oauth2_token_binding; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth2_token_binding (
    token_id character varying(255) NOT NULL,
    token_binding_type character varying(32),
    token_binding_ref character varying(32),
    token_binding_value character varying(1024),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_oauth2_token_binding OWNER TO apim_user;

--
-- Name: idn_oauth_consumer_apps_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oauth_consumer_apps_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oauth_consumer_apps_pk_seq OWNER TO apim_user;

--
-- Name: idn_oauth_consumer_apps; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oauth_consumer_apps (
    id integer DEFAULT nextval('public.idn_oauth_consumer_apps_pk_seq'::regclass) NOT NULL,
    consumer_key character varying(255),
    consumer_secret character varying(2048),
    username character varying(255),
    tenant_id integer DEFAULT 0,
    user_domain character varying(50),
    app_name character varying(255),
    oauth_version character varying(128),
    callback_url character varying(2048),
    grant_types character varying(1024),
    pkce_mandatory character(1) DEFAULT '0'::bpchar,
    pkce_support_plain character(1) DEFAULT '0'::bpchar,
    app_state character varying(25) DEFAULT 'ACTIVE'::character varying,
    user_access_token_expire_time bigint DEFAULT 3600,
    app_access_token_expire_time bigint DEFAULT 3600,
    refresh_token_expire_time bigint DEFAULT 84600,
    id_token_expire_time bigint DEFAULT 3600
);


ALTER TABLE public.idn_oauth_consumer_apps OWNER TO apim_user;

--
-- Name: idn_oidc_jti; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oidc_jti (
    jwt_id character varying(255) NOT NULL,
    exp_time timestamp without time zone NOT NULL,
    time_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.idn_oidc_jti OWNER TO apim_user;

--
-- Name: idn_oidc_property_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oidc_property_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oidc_property_seq OWNER TO apim_user;

--
-- Name: idn_oidc_property; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oidc_property (
    id integer DEFAULT nextval('public.idn_oidc_property_seq'::regclass) NOT NULL,
    tenant_id integer,
    consumer_key character varying(255),
    property_key character varying(255) NOT NULL,
    property_value character varying(2047)
);


ALTER TABLE public.idn_oidc_property OWNER TO apim_user;

--
-- Name: idn_oidc_req_object_claim_values_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oidc_req_object_claim_values_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oidc_req_object_claim_values_seq OWNER TO apim_user;

--
-- Name: idn_oidc_req_obj_claim_values; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oidc_req_obj_claim_values (
    id integer DEFAULT nextval('public.idn_oidc_req_object_claim_values_seq'::regclass) NOT NULL,
    req_object_claims_id integer,
    claim_values character varying(255)
);


ALTER TABLE public.idn_oidc_req_obj_claim_values OWNER TO apim_user;

--
-- Name: idn_oidc_req_object_claims_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oidc_req_object_claims_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oidc_req_object_claims_seq OWNER TO apim_user;

--
-- Name: idn_oidc_req_object_claims; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oidc_req_object_claims (
    id integer DEFAULT nextval('public.idn_oidc_req_object_claims_seq'::regclass) NOT NULL,
    req_object_id integer,
    claim_attribute character varying(255),
    essential character(1) DEFAULT '0'::bpchar NOT NULL,
    value character varying(255),
    is_userinfo character(1) DEFAULT '0'::bpchar NOT NULL
);


ALTER TABLE public.idn_oidc_req_object_claims OWNER TO apim_user;

--
-- Name: idn_oidc_request_object_ref_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oidc_request_object_ref_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oidc_request_object_ref_seq OWNER TO apim_user;

--
-- Name: idn_oidc_req_object_reference; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oidc_req_object_reference (
    id integer DEFAULT nextval('public.idn_oidc_request_object_ref_seq'::regclass) NOT NULL,
    consumer_key_id integer,
    code_id character varying(255),
    token_id character varying(255),
    session_data_key character varying(255)
);


ALTER TABLE public.idn_oidc_req_object_reference OWNER TO apim_user;

--
-- Name: idn_oidc_scope_claim_mapping_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_oidc_scope_claim_mapping_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_oidc_scope_claim_mapping_pk_seq OWNER TO apim_user;

--
-- Name: idn_oidc_scope_claim_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_oidc_scope_claim_mapping (
    id integer DEFAULT nextval('public.idn_oidc_scope_claim_mapping_pk_seq'::regclass) NOT NULL,
    scope_id integer NOT NULL,
    external_claim_id integer NOT NULL
);


ALTER TABLE public.idn_oidc_scope_claim_mapping OWNER TO apim_user;

--
-- Name: idn_openid_associations; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_openid_associations (
    handle character varying(255) NOT NULL,
    assoc_type character varying(255) NOT NULL,
    expire_in timestamp without time zone NOT NULL,
    mac_key character varying(255) NOT NULL,
    assoc_store character varying(128) DEFAULT 'SHARED'::character varying,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_openid_associations OWNER TO apim_user;

--
-- Name: idn_openid_remember_me; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_openid_remember_me (
    user_name character varying(255) NOT NULL,
    tenant_id integer DEFAULT 0 NOT NULL,
    cookie_value character varying(1024),
    created_time timestamp without time zone
);


ALTER TABLE public.idn_openid_remember_me OWNER TO apim_user;

--
-- Name: idn_openid_user_rps; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_openid_user_rps (
    user_name character varying(255) NOT NULL,
    tenant_id integer DEFAULT 0 NOT NULL,
    rp_url character varying(255) NOT NULL,
    trusted_always character varying(128) DEFAULT 'FALSE'::character varying,
    last_visit date NOT NULL,
    visit_count integer DEFAULT 0,
    default_profile_name character varying(255) DEFAULT 'DEFAULT'::character varying
);


ALTER TABLE public.idn_openid_user_rps OWNER TO apim_user;

--
-- Name: idn_password_history_data_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_password_history_data_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_password_history_data_pk_seq OWNER TO apim_user;

--
-- Name: idn_password_history_data; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_password_history_data (
    id integer DEFAULT nextval('public.idn_password_history_data_pk_seq'::regclass) NOT NULL,
    user_name character varying(255) NOT NULL,
    user_domain character varying(127) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer,
    salt_value character varying(255),
    hash character varying(255) NOT NULL,
    time_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);


ALTER TABLE public.idn_password_history_data OWNER TO apim_user;

--
-- Name: idn_recovery_data; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_recovery_data (
    user_name character varying(255) NOT NULL,
    user_domain character varying(127) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer NOT NULL,
    code character varying(255) NOT NULL,
    scenario character varying(255) NOT NULL,
    step character varying(127) NOT NULL,
    time_created timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    remaining_sets character varying(2500) DEFAULT NULL::character varying
);


ALTER TABLE public.idn_recovery_data OWNER TO apim_user;

--
-- Name: idn_saml2_artifact_store_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_saml2_artifact_store_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_saml2_artifact_store_seq OWNER TO apim_user;

--
-- Name: idn_saml2_artifact_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_saml2_artifact_store (
    id integer DEFAULT nextval('public.idn_saml2_artifact_store_seq'::regclass) NOT NULL,
    source_id character varying(255) NOT NULL,
    message_handler character varying(255) NOT NULL,
    authn_req_dto bytea NOT NULL,
    session_id character varying(255) NOT NULL,
    init_timestamp timestamp without time zone NOT NULL,
    exp_timestamp timestamp without time zone NOT NULL,
    assertion_id character varying(255)
);


ALTER TABLE public.idn_saml2_artifact_store OWNER TO apim_user;

--
-- Name: idn_saml2_assertion_store_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_saml2_assertion_store_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_saml2_assertion_store_seq OWNER TO apim_user;

--
-- Name: idn_saml2_assertion_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_saml2_assertion_store (
    id integer DEFAULT nextval('public.idn_saml2_assertion_store_seq'::regclass) NOT NULL,
    saml2_id character varying(255),
    saml2_issuer character varying(255),
    saml2_subject character varying(255),
    saml2_session_index character varying(255),
    saml2_authn_context_class_ref character varying(255),
    saml2_assertion character varying(4096),
    assertion bytea
);


ALTER TABLE public.idn_saml2_assertion_store OWNER TO apim_user;

--
-- Name: idn_scim_group_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_scim_group_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_scim_group_pk_seq OWNER TO apim_user;

--
-- Name: idn_scim_group; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_scim_group (
    id integer DEFAULT nextval('public.idn_scim_group_pk_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    role_name character varying(255) NOT NULL,
    attr_name character varying(1024) NOT NULL,
    attr_value character varying(1024)
);


ALTER TABLE public.idn_scim_group OWNER TO apim_user;

--
-- Name: idn_sts_store_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_sts_store_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_sts_store_pk_seq OWNER TO apim_user;

--
-- Name: idn_sts_store; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_sts_store (
    id integer DEFAULT nextval('public.idn_sts_store_pk_seq'::regclass) NOT NULL,
    token_id character varying(255) NOT NULL,
    token_content bytea NOT NULL,
    create_date timestamp without time zone NOT NULL,
    expire_date timestamp without time zone NOT NULL,
    state integer DEFAULT 0
);


ALTER TABLE public.idn_sts_store OWNER TO apim_user;

--
-- Name: idn_thrift_session; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_thrift_session (
    session_id character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL,
    created_time character varying(255) NOT NULL,
    last_modified_time character varying(255) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idn_thrift_session OWNER TO apim_user;

--
-- Name: idn_uma_permission_ticket_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_uma_permission_ticket_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_uma_permission_ticket_seq OWNER TO apim_user;

--
-- Name: idn_uma_permission_ticket; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_uma_permission_ticket (
    id integer DEFAULT nextval('public.idn_uma_permission_ticket_seq'::regclass) NOT NULL,
    pt character varying(255) NOT NULL,
    time_created timestamp without time zone NOT NULL,
    expiry_time timestamp without time zone NOT NULL,
    ticket_state character varying(25) DEFAULT 'ACTIVE'::character varying,
    tenant_id integer DEFAULT '-1234'::integer
);


ALTER TABLE public.idn_uma_permission_ticket OWNER TO apim_user;

--
-- Name: idn_uma_pt_resource_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_uma_pt_resource_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_uma_pt_resource_seq OWNER TO apim_user;

--
-- Name: idn_uma_pt_resource; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_uma_pt_resource (
    id integer DEFAULT nextval('public.idn_uma_pt_resource_seq'::regclass) NOT NULL,
    pt_resource_id integer NOT NULL,
    pt_id integer NOT NULL
);


ALTER TABLE public.idn_uma_pt_resource OWNER TO apim_user;

--
-- Name: idn_uma_pt_resource_scope_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_uma_pt_resource_scope_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_uma_pt_resource_scope_seq OWNER TO apim_user;

--
-- Name: idn_uma_pt_resource_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_uma_pt_resource_scope (
    id integer DEFAULT nextval('public.idn_uma_pt_resource_scope_seq'::regclass) NOT NULL,
    pt_resource_id integer NOT NULL,
    pt_scope_id integer NOT NULL
);


ALTER TABLE public.idn_uma_pt_resource_scope OWNER TO apim_user;

--
-- Name: idn_uma_resource_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_uma_resource_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_uma_resource_seq OWNER TO apim_user;

--
-- Name: idn_uma_resource; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_uma_resource (
    id integer DEFAULT nextval('public.idn_uma_resource_seq'::regclass) NOT NULL,
    resource_id character varying(255),
    resource_name character varying(255),
    time_created timestamp without time zone NOT NULL,
    resource_owner_name character varying(255),
    client_id character varying(255),
    tenant_id integer DEFAULT '-1234'::integer,
    user_domain character varying(50)
);


ALTER TABLE public.idn_uma_resource OWNER TO apim_user;

--
-- Name: idn_uma_resource_meta_data_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_uma_resource_meta_data_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_uma_resource_meta_data_seq OWNER TO apim_user;

--
-- Name: idn_uma_resource_meta_data; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_uma_resource_meta_data (
    id integer DEFAULT nextval('public.idn_uma_resource_meta_data_seq'::regclass) NOT NULL,
    resource_identity integer NOT NULL,
    property_key character varying(40),
    property_value character varying(255)
);


ALTER TABLE public.idn_uma_resource_meta_data OWNER TO apim_user;

--
-- Name: idn_uma_resource_scope_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idn_uma_resource_scope_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idn_uma_resource_scope_seq OWNER TO apim_user;

--
-- Name: idn_uma_resource_scope; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_uma_resource_scope (
    id integer DEFAULT nextval('public.idn_uma_resource_scope_seq'::regclass) NOT NULL,
    resource_identity integer NOT NULL,
    scope_name character varying(255)
);


ALTER TABLE public.idn_uma_resource_scope OWNER TO apim_user;

--
-- Name: idn_user_account_association; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idn_user_account_association (
    association_key character varying(255) NOT NULL,
    tenant_id integer NOT NULL,
    domain_name character varying(255) NOT NULL,
    user_name character varying(255) NOT NULL
);


ALTER TABLE public.idn_user_account_association OWNER TO apim_user;

--
-- Name: idp_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_seq OWNER TO apim_user;

--
-- Name: idp; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp (
    id integer DEFAULT nextval('public.idp_seq'::regclass) NOT NULL,
    tenant_id integer,
    name character varying(254) NOT NULL,
    is_enabled character(1) DEFAULT '1'::bpchar NOT NULL,
    is_primary character(1) DEFAULT '0'::bpchar NOT NULL,
    home_realm_id character varying(254),
    image bytea,
    certificate bytea,
    alias character varying(254),
    inbound_prov_enabled character(1) DEFAULT '0'::bpchar NOT NULL,
    inbound_prov_user_store_id character varying(254),
    user_claim_uri character varying(254),
    role_claim_uri character varying(254),
    description character varying(1024),
    default_authenticator_name character varying(254),
    default_pro_connector_name character varying(254),
    provisioning_role character varying(128),
    is_federation_hub character(1) DEFAULT '0'::bpchar NOT NULL,
    is_local_claim_dialect character(1) DEFAULT '0'::bpchar NOT NULL,
    display_name character varying(255),
    image_url character varying(1024),
    uuid character(36) NOT NULL
);


ALTER TABLE public.idp OWNER TO apim_user;

--
-- Name: idp_authenticator_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_authenticator_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_authenticator_seq OWNER TO apim_user;

--
-- Name: idp_authenticator; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_authenticator (
    id integer DEFAULT nextval('public.idp_authenticator_seq'::regclass) NOT NULL,
    tenant_id integer,
    idp_id integer,
    name character varying(255) NOT NULL,
    is_enabled character(1) DEFAULT '1'::bpchar,
    display_name character varying(255)
);


ALTER TABLE public.idp_authenticator OWNER TO apim_user;

--
-- Name: idp_authenticator_prop_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_authenticator_prop_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_authenticator_prop_seq OWNER TO apim_user;

--
-- Name: idp_authenticator_property; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_authenticator_property (
    id integer DEFAULT nextval('public.idp_authenticator_prop_seq'::regclass) NOT NULL,
    tenant_id integer,
    authenticator_id integer,
    property_key character varying(255) NOT NULL,
    property_value character varying(2047),
    is_secret character(1) DEFAULT '0'::bpchar
);


ALTER TABLE public.idp_authenticator_property OWNER TO apim_user;

--
-- Name: idp_claim_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_claim_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_claim_seq OWNER TO apim_user;

--
-- Name: idp_claim; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_claim (
    id integer DEFAULT nextval('public.idp_claim_seq'::regclass) NOT NULL,
    idp_id integer,
    tenant_id integer,
    claim character varying(254)
);


ALTER TABLE public.idp_claim OWNER TO apim_user;

--
-- Name: idp_claim_mapping_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_claim_mapping_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_claim_mapping_seq OWNER TO apim_user;

--
-- Name: idp_claim_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_claim_mapping (
    id integer DEFAULT nextval('public.idp_claim_mapping_seq'::regclass) NOT NULL,
    idp_claim_id integer,
    tenant_id integer,
    local_claim character varying(253),
    default_value character varying(255),
    is_requested character varying(128) DEFAULT '0'::character varying
);


ALTER TABLE public.idp_claim_mapping OWNER TO apim_user;

--
-- Name: idp_local_claim_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_local_claim_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_local_claim_seq OWNER TO apim_user;

--
-- Name: idp_local_claim; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_local_claim (
    id integer DEFAULT nextval('public.idp_local_claim_seq'::regclass) NOT NULL,
    tenant_id integer,
    idp_id integer,
    claim_uri character varying(255) NOT NULL,
    default_value character varying(255),
    is_requested character varying(128) DEFAULT '0'::character varying
);


ALTER TABLE public.idp_local_claim OWNER TO apim_user;

--
-- Name: idp_metadata_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_metadata_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_metadata_seq OWNER TO apim_user;

--
-- Name: idp_metadata; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_metadata (
    id integer DEFAULT nextval('public.idp_metadata_seq'::regclass) NOT NULL,
    idp_id integer,
    name character varying(255) NOT NULL,
    value character varying(255) NOT NULL,
    display_name character varying(255),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.idp_metadata OWNER TO apim_user;

--
-- Name: idp_prov_config_prop_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_prov_config_prop_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_prov_config_prop_seq OWNER TO apim_user;

--
-- Name: idp_prov_config_property; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_prov_config_property (
    id integer DEFAULT nextval('public.idp_prov_config_prop_seq'::regclass) NOT NULL,
    tenant_id integer,
    provisioning_config_id integer,
    property_key character varying(255) NOT NULL,
    property_value character varying(2048),
    property_blob_value bytea,
    property_type character(32) NOT NULL,
    is_secret character(1) DEFAULT '0'::bpchar
);


ALTER TABLE public.idp_prov_config_property OWNER TO apim_user;

--
-- Name: idp_prov_config_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_prov_config_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_prov_config_seq OWNER TO apim_user;

--
-- Name: idp_prov_entity_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_prov_entity_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_prov_entity_seq OWNER TO apim_user;

--
-- Name: idp_provisioning_config; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_provisioning_config (
    id integer DEFAULT nextval('public.idp_prov_config_seq'::regclass) NOT NULL,
    tenant_id integer,
    idp_id integer,
    provisioning_connector_type character varying(255) NOT NULL,
    is_enabled character(1) DEFAULT '0'::bpchar,
    is_blocking character(1) DEFAULT '0'::bpchar,
    is_rules_enabled character(1) DEFAULT '0'::bpchar
);


ALTER TABLE public.idp_provisioning_config OWNER TO apim_user;

--
-- Name: idp_provisioning_entity; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_provisioning_entity (
    id integer DEFAULT nextval('public.idp_prov_entity_seq'::regclass) NOT NULL,
    provisioning_config_id integer,
    entity_type character varying(255) NOT NULL,
    entity_local_userstore character varying(255) NOT NULL,
    entity_name character varying(255) NOT NULL,
    entity_value character varying(255),
    tenant_id integer,
    entity_local_id character varying(255)
);


ALTER TABLE public.idp_provisioning_entity OWNER TO apim_user;

--
-- Name: idp_role_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_role_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_role_seq OWNER TO apim_user;

--
-- Name: idp_role; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_role (
    id integer DEFAULT nextval('public.idp_role_seq'::regclass) NOT NULL,
    idp_id integer,
    tenant_id integer,
    role character varying(254)
);


ALTER TABLE public.idp_role OWNER TO apim_user;

--
-- Name: idp_role_mapping_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.idp_role_mapping_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.idp_role_mapping_seq OWNER TO apim_user;

--
-- Name: idp_role_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.idp_role_mapping (
    id integer DEFAULT nextval('public.idp_role_mapping_seq'::regclass) NOT NULL,
    idp_role_id integer,
    tenant_id integer,
    user_store_id character varying(253),
    local_role character varying(253)
);


ALTER TABLE public.idp_role_mapping OWNER TO apim_user;

--
-- Name: sp_app_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_app_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_app_seq OWNER TO apim_user;

--
-- Name: sp_app; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_app (
    id integer DEFAULT nextval('public.sp_app_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    app_name character varying(255) NOT NULL,
    user_store character varying(255) NOT NULL,
    username character varying(255) NOT NULL,
    description character varying(1024),
    role_claim character varying(512),
    auth_type character varying(255) NOT NULL,
    provisioning_userstore_domain character varying(512),
    is_local_claim_dialect character(1) DEFAULT '1'::bpchar,
    is_send_local_subject_id character(1) DEFAULT '0'::bpchar,
    is_send_auth_list_of_idps character(1) DEFAULT '0'::bpchar,
    is_use_tenant_domain_subject character(1) DEFAULT '1'::bpchar,
    is_use_user_domain_subject character(1) DEFAULT '1'::bpchar,
    enable_authorization character(1) DEFAULT '0'::bpchar,
    subject_claim_uri character varying(512),
    is_saas_app character(1) DEFAULT '0'::bpchar,
    is_dumb_mode character(1) DEFAULT '0'::bpchar,
    uuid character(36),
    image_url character varying(1024),
    access_url character varying(1024),
    is_discoverable character(1) DEFAULT '0'::bpchar
);


ALTER TABLE public.sp_app OWNER TO apim_user;

--
-- Name: sp_auth_script_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_auth_script_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_auth_script_seq OWNER TO apim_user;

--
-- Name: sp_auth_script; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_auth_script (
    id integer DEFAULT nextval('public.sp_auth_script_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    app_id integer NOT NULL,
    type character varying(255) NOT NULL,
    content bytea,
    is_enabled character(1) DEFAULT '0'::bpchar NOT NULL
);


ALTER TABLE public.sp_auth_script OWNER TO apim_user;

--
-- Name: sp_auth_step_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_auth_step_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_auth_step_seq OWNER TO apim_user;

--
-- Name: sp_auth_step; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_auth_step (
    id integer DEFAULT nextval('public.sp_auth_step_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    step_order integer DEFAULT 1,
    app_id integer NOT NULL,
    is_subject_step character(1) DEFAULT '0'::bpchar,
    is_attribute_step character(1) DEFAULT '0'::bpchar
);


ALTER TABLE public.sp_auth_step OWNER TO apim_user;

--
-- Name: sp_claim_dialect_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_claim_dialect_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_claim_dialect_seq OWNER TO apim_user;

--
-- Name: sp_claim_dialect; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_claim_dialect (
    id integer DEFAULT nextval('public.sp_claim_dialect_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    sp_dialect character varying(512) NOT NULL,
    app_id integer NOT NULL
);


ALTER TABLE public.sp_claim_dialect OWNER TO apim_user;

--
-- Name: sp_claim_mapping_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_claim_mapping_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_claim_mapping_seq OWNER TO apim_user;

--
-- Name: sp_claim_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_claim_mapping (
    id integer DEFAULT nextval('public.sp_claim_mapping_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    idp_claim character varying(512) NOT NULL,
    sp_claim character varying(512) NOT NULL,
    app_id integer NOT NULL,
    is_requested character varying(128) DEFAULT '0'::character varying,
    is_mandatory character varying(128) DEFAULT '0'::character varying,
    default_value character varying(255)
);


ALTER TABLE public.sp_claim_mapping OWNER TO apim_user;

--
-- Name: sp_federated_idp; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_federated_idp (
    id integer NOT NULL,
    tenant_id integer NOT NULL,
    authenticator_id integer NOT NULL
);


ALTER TABLE public.sp_federated_idp OWNER TO apim_user;

--
-- Name: sp_inbound_auth_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_inbound_auth_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_inbound_auth_seq OWNER TO apim_user;

--
-- Name: sp_inbound_auth; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_inbound_auth (
    id integer DEFAULT nextval('public.sp_inbound_auth_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    inbound_auth_key character varying(255),
    inbound_auth_type character varying(255) NOT NULL,
    inbound_config_type character varying(255) NOT NULL,
    prop_name character varying(255),
    prop_value character varying(1024),
    app_id integer NOT NULL
);


ALTER TABLE public.sp_inbound_auth OWNER TO apim_user;

--
-- Name: sp_metadata_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_metadata_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_metadata_seq OWNER TO apim_user;

--
-- Name: sp_metadata; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_metadata (
    id integer DEFAULT nextval('public.sp_metadata_seq'::regclass) NOT NULL,
    sp_id integer,
    name character varying(255) NOT NULL,
    value character varying(255) NOT NULL,
    display_name character varying(255),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.sp_metadata OWNER TO apim_user;

--
-- Name: sp_prov_connector_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_prov_connector_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_prov_connector_seq OWNER TO apim_user;

--
-- Name: sp_provisioning_connector; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_provisioning_connector (
    id integer DEFAULT nextval('public.sp_prov_connector_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    idp_name character varying(255) NOT NULL,
    connector_name character varying(255) NOT NULL,
    app_id integer NOT NULL,
    is_jit_enabled character(1) DEFAULT '0'::bpchar NOT NULL,
    blocking character(1) DEFAULT '0'::bpchar NOT NULL,
    rule_enabled character(1) DEFAULT '0'::bpchar NOT NULL
);


ALTER TABLE public.sp_provisioning_connector OWNER TO apim_user;

--
-- Name: sp_req_path_auth_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_req_path_auth_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_req_path_auth_seq OWNER TO apim_user;

--
-- Name: sp_req_path_authenticator; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_req_path_authenticator (
    id integer DEFAULT nextval('public.sp_req_path_auth_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    authenticator_name character varying(255) NOT NULL,
    app_id integer NOT NULL
);


ALTER TABLE public.sp_req_path_authenticator OWNER TO apim_user;

--
-- Name: sp_role_mapping_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_role_mapping_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_role_mapping_seq OWNER TO apim_user;

--
-- Name: sp_role_mapping; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_role_mapping (
    id integer DEFAULT nextval('public.sp_role_mapping_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    idp_role character varying(255) NOT NULL,
    sp_role character varying(255) NOT NULL,
    app_id integer NOT NULL
);


ALTER TABLE public.sp_role_mapping OWNER TO apim_user;

--
-- Name: sp_template_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.sp_template_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.sp_template_seq OWNER TO apim_user;

--
-- Name: sp_template; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.sp_template (
    id integer DEFAULT nextval('public.sp_template_seq'::regclass) NOT NULL,
    tenant_id integer NOT NULL,
    name character varying(255) NOT NULL,
    description character varying(1023),
    content bytea
);


ALTER TABLE public.sp_template OWNER TO apim_user;

--
-- Name: wf_bps_profile; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_bps_profile (
    profile_name character varying(45) NOT NULL,
    host_url_manager character varying(255),
    host_url_worker character varying(255),
    username character varying(45),
    password character varying(1023),
    callback_host character varying(45),
    tenant_id integer DEFAULT '-1'::integer NOT NULL
);


ALTER TABLE public.wf_bps_profile OWNER TO apim_user;

--
-- Name: wf_request; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_request (
    uuid character varying(45) NOT NULL,
    created_by character varying(255),
    tenant_id integer DEFAULT '-1'::integer,
    operation_type character varying(50),
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    status character varying(30),
    request bytea
);


ALTER TABLE public.wf_request OWNER TO apim_user;

--
-- Name: wf_request_entity_relationship; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_request_entity_relationship (
    request_id character varying(45) NOT NULL,
    entity_name character varying(255) NOT NULL,
    entity_type character varying(50) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer NOT NULL
);


ALTER TABLE public.wf_request_entity_relationship OWNER TO apim_user;

--
-- Name: wf_workflow; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_workflow (
    id character varying(45) NOT NULL,
    wf_name character varying(45),
    description character varying(255),
    template_id character varying(45),
    impl_id character varying(45),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.wf_workflow OWNER TO apim_user;

--
-- Name: wf_workflow_association_pk_seq; Type: SEQUENCE; Schema: public; Owner: apim_user
--

CREATE SEQUENCE public.wf_workflow_association_pk_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.wf_workflow_association_pk_seq OWNER TO apim_user;

--
-- Name: wf_workflow_association; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_workflow_association (
    id integer DEFAULT nextval('public.wf_workflow_association_pk_seq'::regclass) NOT NULL,
    assoc_name character varying(45),
    event_id character varying(45),
    assoc_condition character varying(2000),
    workflow_id character varying(45),
    is_enabled character(1) DEFAULT '1'::bpchar,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.wf_workflow_association OWNER TO apim_user;

--
-- Name: wf_workflow_config_param; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_workflow_config_param (
    workflow_id character varying(45) NOT NULL,
    param_name character varying(45) NOT NULL,
    param_value character varying(1000),
    param_qname character varying(45) NOT NULL,
    param_holder character varying(45) NOT NULL,
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.wf_workflow_config_param OWNER TO apim_user;

--
-- Name: wf_workflow_request_relation; Type: TABLE; Schema: public; Owner: apim_user
--

CREATE TABLE public.wf_workflow_request_relation (
    relationship_id character varying(45) NOT NULL,
    workflow_id character varying(45),
    request_id character varying(45),
    updated_at timestamp without time zone,
    status character varying(30),
    tenant_id integer DEFAULT '-1'::integer
);


ALTER TABLE public.wf_workflow_request_relation OWNER TO apim_user;

--
-- Name: idn_oauth2_device_flow_scopes id; Type: DEFAULT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow_scopes ALTER COLUMN id SET DEFAULT nextval('public.idn_oauth2_device_flow_scopes_id_seq'::regclass);


--
-- Data for Name: am_alert_emaillist; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_alert_emaillist (email_list_id, user_name, stake_holder) FROM stdin;
\.


--
-- Data for Name: am_alert_emaillist_details; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_alert_emaillist_details (email_list_id, email) FROM stdin;
\.


--
-- Data for Name: am_alert_types; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_alert_types (alert_type_id, alert_type_name, stake_holder) FROM stdin;
1	AbnormalResponseTime	publisher
2	AbnormalBackendTime	publisher
3	AbnormalRequestsPerMin	subscriber
4	AbnormalRequestPattern	subscriber
5	UnusualIPAccess	subscriber
6	FrequentTierLimitHitting	subscriber
7	ApiHealthMonitor	publisher
\.


--
-- Data for Name: am_alert_types_values; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_alert_types_values (alert_type_id, user_name, stake_holder) FROM stdin;
\.


--
-- Data for Name: am_api; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api (api_id, api_provider, api_name, api_version, context, context_template, api_tier, api_type, created_by, created_time, updated_by, updated_time) FROM stdin;
1	adp_crt_user	ADPRestAPI	1.0.0	/adp-rest/1.0.0	/adp-rest	\N	HTTP	adp_crt_user	2026-03-27 15:47:09.919	adp_crt_user	2026-03-27 15:47:16.953
2	adp_crt_user	ADPRestAPI	2.0.0	/adp-rest/2.0.0	/adp-rest	\N	HTTP	adp_crt_user	2026-03-27 15:47:19.24	\N	\N
3	adp_crt_user	ADPOAS2RestAPI	1.0.0	/adp-oas2-rest/1.0.0	/adp-oas2-rest	\N	HTTP	adp_crt_user	2026-03-27 15:47:21.891	adp_crt_user	2026-03-27 15:47:22.717
4	adp_crt_user	ADPOAS2RestAPI	2.0.0	/adp-oas2-rest/2.0.0	/adp-oas2-rest	\N	HTTP	adp_crt_user	2026-03-27 15:47:23.83	\N	\N
5	adp_crt_user	ADPOAS3RestAPI	1.0.0	/adp-oas3-rest/1.0.0	/adp-oas3-rest	ADP10PerMin	HTTP	adp_crt_user	2026-03-27 15:47:27.618	adp_crt_user	2026-03-27 15:47:28.442
6	adp_crt_user	ADPOAS3RestAPI	2.0.0	/adp-oas3-rest/2.0.0	/adp-oas3-rest	ADP10PerMin	HTTP	adp_crt_user	2026-03-27 15:47:29.655	adp_crt_user	2026-03-27 15:47:31.579
7	adp_crt_user	ADPPhoneVerificationAPI	1.0.0	/adp-phoneverify/1.0.0	/adp-phoneverify	\N	SOAP	adp_crt_user	2026-03-27 15:47:36.808	adp_crt_user	2026-03-27 15:47:37.696
8	adp_crt_user	ADPPhoneVerificationAPI	2.0.0	/adp-phoneverify/2.0.0	/adp-phoneverify	\N	SOAP	adp_crt_user	2026-03-27 15:47:38.851	\N	\N
9	adp_crt_user	ADPStarWarsAPI	1.0.0	/adp-swapi/1.0.0	/adp-swapi	\N	GRAPHQL	adp_crt_user	2026-03-27 15:47:40.677	adp_crt_user	2026-03-27 15:47:41.567
10	adp_crt_user	ADPStarWarsAPI	2.0.0	/adp-swapi/2.0.0	/adp-swapi	\N	GRAPHQL	adp_crt_user	2026-03-27 15:47:43.024	\N	\N
11	adp_crt_user	ADPIfElseAPI	1.0.0	/adp-ifelse/1.0.0	/adp-ifelse	\N	WS	adp_crt_user	2026-03-27 15:47:44.811	\N	\N
12	adp_crt_user	ADPIfElseAPI	2.0.0	/adp-ifelse/2.0.0	/adp-ifelse	\N	WS	adp_crt_user	2026-03-27 15:47:45.662	\N	\N
13	adp_pub_user	ADPAPIProduct	1.0.0	/adp-api-product	\N	\N	APIProduct	adp_pub_user	2026-03-27 15:47:47.426	adp_pub_user	2026-03-27 15:47:48.565
14	adp_crt_user	APIM18PublisherTest	1.0.0	/apiContext/1.0.0	/apiContext	\N	HTTP	adp_crt_user	2026-03-27 15:48:14.502	adp_crt_user	2026-03-27 15:48:22.178
15	adp_crt_user@adpexample.com	ADPRestAPI	1.0.0	/t/adpexample.com/adp-rest/1.0.0	/t/adpexample.com/adp-rest	\N	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:48:50.702	adp_crt_user@adpexample.com	2026-03-27 15:48:57.786
16	adp_crt_user@adpexample.com	ADPRestAPI	2.0.0	/t/adpexample.com/adp-rest/2.0.0	/t/adpexample.com/adp-rest	\N	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:49:01.153	\N	\N
17	adp_crt_user@adpexample.com	ADPOAS2RestAPI	1.0.0	/t/adpexample.com/adp-oas2-rest/1.0.0	/t/adpexample.com/adp-oas2-rest	\N	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:49:03.437	adp_crt_user@adpexample.com	2026-03-27 15:49:04.305
18	adp_crt_user@adpexample.com	ADPOAS2RestAPI	2.0.0	/t/adpexample.com/adp-oas2-rest/2.0.0	/t/adpexample.com/adp-oas2-rest	\N	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:49:05.492	\N	\N
19	adp_crt_user@adpexample.com	ADPOAS3RestAPI	1.0.0	/t/adpexample.com/adp-oas3-rest/1.0.0	/t/adpexample.com/adp-oas3-rest	ADP10PerMin	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:49:09.211	adp_crt_user@adpexample.com	2026-03-27 15:49:10.061
20	adp_crt_user@adpexample.com	ADPOAS3RestAPI	2.0.0	/t/adpexample.com/adp-oas3-rest/2.0.0	/t/adpexample.com/adp-oas3-rest	ADP10PerMin	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:49:11.201	adp_crt_user@adpexample.com	2026-03-27 15:49:13.003
21	adp_crt_user@adpexample.com	ADPPhoneVerificationAPI	1.0.0	/t/adpexample.com/adp-phoneverify/1.0.0	/t/adpexample.com/adp-phoneverify	\N	SOAP	adp_crt_user@adpexample.com	2026-03-27 15:49:17.152	adp_crt_user@adpexample.com	2026-03-27 15:49:18.07
22	adp_crt_user@adpexample.com	ADPPhoneVerificationAPI	2.0.0	/t/adpexample.com/adp-phoneverify/2.0.0	/t/adpexample.com/adp-phoneverify	\N	SOAP	adp_crt_user@adpexample.com	2026-03-27 15:49:19.287	\N	\N
23	adp_crt_user@adpexample.com	ADPStarWarsAPI	1.0.0	/t/adpexample.com/adp-swapi/1.0.0	/t/adpexample.com/adp-swapi	\N	GRAPHQL	adp_crt_user@adpexample.com	2026-03-27 15:49:21.093	adp_crt_user@adpexample.com	2026-03-27 15:49:21.96
24	adp_crt_user@adpexample.com	ADPStarWarsAPI	2.0.0	/t/adpexample.com/adp-swapi/2.0.0	/t/adpexample.com/adp-swapi	\N	GRAPHQL	adp_crt_user@adpexample.com	2026-03-27 15:49:23.302	\N	\N
25	adp_crt_user@adpexample.com	ADPIfElseAPI	1.0.0	/t/adpexample.com/adp-ifelse/1.0.0	/t/adpexample.com/adp-ifelse	\N	WS	adp_crt_user@adpexample.com	2026-03-27 15:49:25.168	\N	\N
26	adp_crt_user@adpexample.com	ADPIfElseAPI	2.0.0	/t/adpexample.com/adp-ifelse/2.0.0	/t/adpexample.com/adp-ifelse	\N	WS	adp_crt_user@adpexample.com	2026-03-27 15:49:26.156	\N	\N
27	adp_pub_user@adpexample.com	ADPAPIProduct	1.0.0	/t/adpexample.com/adp-api-product	\N	\N	APIProduct	adp_pub_user@adpexample.com	2026-03-27 15:49:28.183	adp_pub_user@adpexample.com	2026-03-27 15:49:29.319
28	adp_crt_user@adpexample.com	APIM18PublisherTest	1.0.0	/t/adpexample.com/apiContext/1.0.0	/t/adpexample.com/apiContext	\N	HTTP	adp_crt_user@adpexample.com	2026-03-27 15:49:54.472	adp_crt_user@adpexample.com	2026-03-27 15:50:02.134
29	adp_crt_user@adpsample.com	ADPRestAPI	1.0.0	/t/adpsample.com/adp-rest/1.0.0	/t/adpsample.com/adp-rest	\N	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:50:33.326	adp_crt_user@adpsample.com	2026-03-27 15:50:40.308
30	adp_crt_user@adpsample.com	ADPRestAPI	2.0.0	/t/adpsample.com/adp-rest/2.0.0	/t/adpsample.com/adp-rest	\N	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:50:42.199	\N	\N
31	adp_crt_user@adpsample.com	ADPOAS2RestAPI	1.0.0	/t/adpsample.com/adp-oas2-rest/1.0.0	/t/adpsample.com/adp-oas2-rest	\N	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:50:44.31	adp_crt_user@adpsample.com	2026-03-27 15:50:45.205
32	adp_crt_user@adpsample.com	ADPOAS2RestAPI	2.0.0	/t/adpsample.com/adp-oas2-rest/2.0.0	/t/adpsample.com/adp-oas2-rest	\N	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:50:46.144	\N	\N
33	adp_crt_user@adpsample.com	ADPOAS3RestAPI	1.0.0	/t/adpsample.com/adp-oas3-rest/1.0.0	/t/adpsample.com/adp-oas3-rest	ADP10PerMin	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:50:49.821	adp_crt_user@adpsample.com	2026-03-27 15:50:50.665
34	adp_crt_user@adpsample.com	ADPOAS3RestAPI	2.0.0	/t/adpsample.com/adp-oas3-rest/2.0.0	/t/adpsample.com/adp-oas3-rest	ADP10PerMin	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:50:51.886	adp_crt_user@adpsample.com	2026-03-27 15:50:53.713
35	adp_crt_user@adpsample.com	ADPPhoneVerificationAPI	1.0.0	/t/adpsample.com/adp-phoneverify/1.0.0	/t/adpsample.com/adp-phoneverify	\N	SOAP	adp_crt_user@adpsample.com	2026-03-27 15:50:57.987	adp_crt_user@adpsample.com	2026-03-27 15:50:58.887
36	adp_crt_user@adpsample.com	ADPPhoneVerificationAPI	2.0.0	/t/adpsample.com/adp-phoneverify/2.0.0	/t/adpsample.com/adp-phoneverify	\N	SOAP	adp_crt_user@adpsample.com	2026-03-27 15:50:59.979	\N	\N
37	adp_crt_user@adpsample.com	ADPStarWarsAPI	1.0.0	/t/adpsample.com/adp-swapi/1.0.0	/t/adpsample.com/adp-swapi	\N	GRAPHQL	adp_crt_user@adpsample.com	2026-03-27 15:51:02.206	adp_crt_user@adpsample.com	2026-03-27 15:51:03.154
38	adp_crt_user@adpsample.com	ADPStarWarsAPI	2.0.0	/t/adpsample.com/adp-swapi/2.0.0	/t/adpsample.com/adp-swapi	\N	GRAPHQL	adp_crt_user@adpsample.com	2026-03-27 15:51:04.219	\N	\N
39	adp_crt_user@adpsample.com	ADPIfElseAPI	1.0.0	/t/adpsample.com/adp-ifelse/1.0.0	/t/adpsample.com/adp-ifelse	\N	WS	adp_crt_user@adpsample.com	2026-03-27 15:51:05.976	\N	\N
40	adp_crt_user@adpsample.com	ADPIfElseAPI	2.0.0	/t/adpsample.com/adp-ifelse/2.0.0	/t/adpsample.com/adp-ifelse	\N	WS	adp_crt_user@adpsample.com	2026-03-27 15:51:06.846	\N	\N
41	adp_pub_user@adpsample.com	ADPAPIProduct	1.0.0	/t/adpsample.com/adp-api-product	\N	\N	APIProduct	adp_pub_user@adpsample.com	2026-03-27 15:51:08.624	adp_pub_user@adpsample.com	2026-03-27 15:51:09.594
42	adp_crt_user@adpsample.com	APIM18PublisherTest	1.0.0	/t/adpsample.com/apiContext/1.0.0	/t/adpsample.com/apiContext	\N	HTTP	adp_crt_user@adpsample.com	2026-03-27 15:51:34.894	adp_crt_user@adpsample.com	2026-03-27 15:51:42.698
\.


--
-- Data for Name: am_api_categories; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_categories (uuid, name, description, tenant_id) FROM stdin;
8aa17459-ae28-4da3-96ee-511d7cb4fd73	adp-imported	\N	-1234
53fe71bb-8498-4f1d-8aad-c9ec159f4d9f	adp-rest	\N	-1234
f5593936-a49f-4e78-8e67-daf04465c295	adp-imported	\N	1
bb0790ee-c386-49f2-b7b2-42279cf07259	adp-rest	\N	1
83b20837-a050-4ffd-a869-5ce92c81dc67	adp-imported	\N	2
bfdc08a7-0a96-47a6-86a8-ff834e6c01ee	adp-rest	\N	2
\.


--
-- Data for Name: am_api_client_certificate; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_client_certificate (tenant_id, alias, api_id, certificate, removed, tier_name) FROM stdin;
\.


--
-- Data for Name: am_api_comments; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_comments (comment_id, comment_text, commented_user, date_commented, api_id) FROM stdin;
\.


--
-- Data for Name: am_api_default_version; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_default_version (default_version_id, api_name, api_provider, default_api_version, published_default_api_version) FROM stdin;
1	ADPPhoneVerificationAPI	adp_crt_user	1.0.0	1.0.0
2	ADPPhoneVerificationAPI	adp_crt_user@adpexample.com	1.0.0	1.0.0
3	ADPPhoneVerificationAPI	adp_crt_user@adpsample.com	1.0.0	1.0.0
\.


--
-- Data for Name: am_api_lc_event; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_lc_event (event_id, api_id, previous_state, new_state, user_id, tenant_id, event_date) FROM stdin;
1	1	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:09.922
2	2	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:19.241
3	1	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:21.054
4	3	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:21.893
5	4	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:23.831
6	3	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:24.672
7	4	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:25.903
8	4	PUBLISHED	BLOCKED	adp_pub_user	-1234	2026-03-27 15:47:26.764
9	5	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:27.62
10	6	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:29.657
11	5	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:30.629
12	6	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:32.66
13	7	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:36.809
14	8	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:38.853
15	7	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:39.844
16	9	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:40.679
17	10	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:43.026
18	9	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:43.965
19	11	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:44.813
20	12	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:47:45.664
21	11	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:47:46.53
22	14	\N	CREATED	adp_crt_user	-1234	2026-03-27 15:48:14.505
23	14	CREATED	PUBLISHED	adp_pub_user	-1234	2026-03-27 15:48:23.83
24	15	\N	CREATED	adp_crt_user	1	2026-03-27 15:48:50.704
25	16	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:01.155
26	15	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:02.505
27	17	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:03.439
28	18	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:05.494
29	17	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:06.478
30	18	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:07.406
31	18	PUBLISHED	BLOCKED	adp_pub_user	1	2026-03-27 15:49:08.377
32	19	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:09.213
33	20	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:11.203
34	19	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:12.134
35	20	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:14.083
36	21	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:17.154
37	22	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:19.289
38	21	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:20.239
39	23	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:21.095
40	24	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:23.304
41	23	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:24.292
42	25	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:25.169
43	26	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:26.158
44	25	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:49:27.156
45	28	\N	CREATED	adp_crt_user	1	2026-03-27 15:49:54.474
46	28	CREATED	PUBLISHED	adp_pub_user	1	2026-03-27 15:50:04.062
47	29	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:33.328
48	30	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:42.201
49	29	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:50:43.467
50	31	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:44.312
51	32	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:46.146
52	31	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:50:47.076
53	32	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:50:47.999
54	32	PUBLISHED	BLOCKED	adp_pub_user	2	2026-03-27 15:50:48.945
55	33	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:49.822
56	34	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:51.888
57	33	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:50:52.871
58	34	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:50:54.629
59	35	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:57.989
60	36	\N	CREATED	adp_crt_user	2	2026-03-27 15:50:59.981
61	35	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:51:01.18
62	37	\N	CREATED	adp_crt_user	2	2026-03-27 15:51:02.208
63	38	\N	CREATED	adp_crt_user	2	2026-03-27 15:51:04.221
64	37	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:51:05.15
65	39	\N	CREATED	adp_crt_user	2	2026-03-27 15:51:05.978
66	40	\N	CREATED	adp_crt_user	2	2026-03-27 15:51:06.848
67	39	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:51:07.738
68	42	\N	CREATED	adp_crt_user	2	2026-03-27 15:51:34.895
69	42	CREATED	PUBLISHED	adp_pub_user	2	2026-03-27 15:51:44.284
\.


--
-- Data for Name: am_api_lc_publish_events; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_lc_publish_events (id, tenant_domain, api_id, event_time) FROM stdin;
\.


--
-- Data for Name: am_api_product_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_product_mapping (api_product_mapping_id, api_id, url_mapping_id) FROM stdin;
3	13	6
4	13	8
7	27	91
8	27	93
11	41	176
12	41	178
\.


--
-- Data for Name: am_api_ratings; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_ratings (rating_id, api_id, rating, subscriber_id) FROM stdin;
\.


--
-- Data for Name: am_api_resource_scope_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_resource_scope_mapping (scope_name, url_mapping_id, tenant_id) FROM stdin;
adp-local-scope-without-roles	6	-1234
adp-shared-scope-without-roles	6	-1234
adp-local-scope-without-roles	8	-1234
adp-shared-scope-with-roles	9	-1234
adp-local-scope-without-roles	11	-1234
adp-shared-scope-with-roles	14	-1234
adp-local-scope-without-roles	15	-1234
adp-shared-scope-without-roles	15	-1234
adp-admin	38	-1234
adp-film-subscriber	39	-1234
adp-admin	51	-1234
adp-film-subscriber	59	-1234
adp-local-scope-without-roles	91	1
adp-shared-scope-without-roles	91	1
adp-local-scope-without-roles	93	1
adp-shared-scope-with-roles	94	1
adp-local-scope-without-roles	96	1
adp-shared-scope-with-roles	97	1
adp-local-scope-without-roles	98	1
adp-shared-scope-without-roles	98	1
adp-admin	123	1
adp-film-subscriber	124	1
adp-admin	135	1
adp-film-subscriber	140	1
adp-local-scope-without-roles	176	2
adp-shared-scope-without-roles	176	2
adp-local-scope-without-roles	178	2
adp-shared-scope-with-roles	179	2
adp-local-scope-without-roles	181	2
adp-local-scope-without-roles	183	2
adp-shared-scope-without-roles	183	2
adp-shared-scope-with-roles	184	2
adp-admin	208	2
adp-film-subscriber	209	2
adp-admin	224	2
adp-film-subscriber	229	2
\.


--
-- Data for Name: am_api_throttle_policy; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_throttle_policy (policy_id, name, display_name, tenant_id, description, default_quota_type, default_quota, default_quota_unit, default_unit_time, default_time_unit, applicable_level, is_deployed, uuid) FROM stdin;
1	50KPerMin	50KPerMin	-1234	Allows 50000 requests per minute	requestCount	50000	\N	1	min	apiLevel	t	7295cf56-dfd1-4041-b28b-ebd9b31f8d20
2	20KPerMin	20KPerMin	-1234	Allows 20000 requests per minute	requestCount	20000	\N	1	min	apiLevel	t	1161bb46-79cf-478f-b550-7ae05bc94556
3	10KPerMin	10KPerMin	-1234	Allows 10000 requests per minute	requestCount	10000	\N	1	min	apiLevel	t	58467c96-f177-4425-a081-81ec1e8a5d2d
4	Unlimited	Unlimited	-1234	Allows unlimited requests	requestCount	2147483647	\N	1	min	apiLevel	t	257ce1c3-e19d-4868-ae25-59851f9aeeaf
5	ADP10PerMin	ADP10PerMin	-1234	10PerMinute	requestCount	10	\N	1	min	apiLevel	t	62149360-638c-4785-8adc-48585e799cf9
6	50KPerMin	50KPerMin	1	Allows 50000 requests per minute	requestCount	50000	\N	1	min	apiLevel	t	c7e07305-b94d-4687-982e-69fa26576927
7	20KPerMin	20KPerMin	1	Allows 20000 requests per minute	requestCount	20000	\N	1	min	apiLevel	t	b4f2eefe-bcc8-4d00-b5d8-646ebb344e62
8	10KPerMin	10KPerMin	1	Allows 10000 requests per minute	requestCount	10000	\N	1	min	apiLevel	t	619530c6-a386-44b2-ade6-4385a3defdbc
9	Unlimited	Unlimited	1	Allows unlimited requests	requestCount	2147483647	\N	1	min	apiLevel	t	0bd3ad93-aeed-4a24-abad-2b840c1c3963
10	ADP10PerMin	ADP10PerMin	1	10PerMinute	requestCount	10	\N	1	min	apiLevel	t	bdae1222-fc1f-4bb5-9a56-d5a653d7795b
11	50KPerMin	50KPerMin	2	Allows 50000 requests per minute	requestCount	50000	\N	1	min	apiLevel	t	6774d7a7-4412-4a4e-9c11-e46f6543edb0
12	20KPerMin	20KPerMin	2	Allows 20000 requests per minute	requestCount	20000	\N	1	min	apiLevel	t	c2c84664-5569-4a81-a4c3-1ec15df2331c
13	10KPerMin	10KPerMin	2	Allows 10000 requests per minute	requestCount	10000	\N	1	min	apiLevel	t	12ee35d4-35be-4bde-8fc6-7cfba1c6ccaf
14	Unlimited	Unlimited	2	Allows unlimited requests	requestCount	2147483647	\N	1	min	apiLevel	t	7b15066c-e514-41c1-bdab-50f648c35392
15	ADP10PerMin	ADP10PerMin	2	10PerMinute	requestCount	10	\N	1	min	apiLevel	t	5af4eb25-25d8-4baa-8f34-b99b2b709c9b
\.


--
-- Data for Name: am_api_url_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_api_url_mapping (url_mapping_id, api_id, http_method, auth_scheme, url_pattern, throttling_tier, mediation_script) FROM stdin;
6	1	GET	Application & Application User	/users/{id}	Unlimited	\N
7	1	DELETE	Application & Application User	/users/{id}	ADP10PerMin	\N
8	1	GET	Application & Application User	/users	Unlimited	\N
9	1	POST	Application & Application User	/users	Unlimited	\N
10	1	GET	None	/hello	Unlimited	\N
11	2	GET	Application & Application User	/users	Unlimited	\N
12	2	GET	None	/hello	Unlimited	\N
13	2	DELETE	Application & Application User	/users/{id}	ADP10PerMin	\N
14	2	POST	Application & Application User	/users	Unlimited	\N
15	2	GET	Application & Application User	/users/{id}	Unlimited	\N
17	3	GET	Application & Application User	/hello	Unlimited	\N
18	4	GET	Application & Application User	/hello	Unlimited	\N
20	5	GET	Application & Application User	/hello	ADP10PerMin	\N
22	6	GET	Application & Application User	/hello	ADP10PerMin	\N
24	7	POST	Application & Application User	/*	Unlimited	\N
25	8	POST	Application & Application User	/*	Unlimited	\N
38	9	QUERY	Any	allCharacters	Unlimited	\N
39	9	QUERY	Any	allDroids	Unlimited	\N
40	9	QUERY	Any	allHumans	ADP10PerMin	\N
41	9	QUERY	Any	character	Unlimited	\N
42	9	MUTATION	Any	createReview	Unlimited	\N
43	9	QUERY	Any	droid	Unlimited	\N
44	9	QUERY	Any	hero	Unlimited	\N
45	9	QUERY	Any	human	Unlimited	\N
46	9	SUBSCRIPTION	Any	reviewAdded	Unlimited	\N
47	9	QUERY	Any	reviews	Unlimited	\N
48	9	QUERY	Any	search	Unlimited	\N
49	9	QUERY	Any	starship	Unlimited	\N
50	10	QUERY	Any	human	Unlimited	\N
51	10	QUERY	Any	allCharacters	Unlimited	\N
52	10	MUTATION	Any	createReview	Unlimited	\N
53	10	SUBSCRIPTION	Any	reviewAdded	Unlimited	\N
54	10	QUERY	Any	allHumans	ADP10PerMin	\N
55	10	QUERY	Any	starship	Unlimited	\N
56	10	QUERY	Any	hero	Unlimited	\N
57	10	QUERY	Any	reviews	Unlimited	\N
58	10	QUERY	Any	character	Unlimited	\N
59	10	QUERY	Any	allDroids	Unlimited	\N
60	10	QUERY	Any	droid	Unlimited	\N
61	10	QUERY	Any	search	Unlimited	\N
62	11	GET	Any	/*	Unlimited	\N
63	11	PUT	Any	/*	Unlimited	\N
64	11	POST	Any	/*	Unlimited	\N
65	11	DELETE	Any	/*	Unlimited	\N
66	11	PATCH	Any	/*	Unlimited	\N
67	12	DELETE	Any	/*	Unlimited	\N
68	12	PUT	Any	/*	Unlimited	\N
69	12	GET	Any	/*	Unlimited	\N
70	12	PATCH	Any	/*	Unlimited	\N
71	12	POST	Any	/*	Unlimited	\N
77	14	GET	Application & Application User	/customers/{id}	Unlimited	\N
78	14	DELETE	Application & Application User	/customers/{id}	Unlimited	\N
79	14	PUT	Application & Application User	/customers	Unlimited	\N
80	14	POST	Application & Application User	/customers	Unlimited	\N
81	14	POST	Application & Application User	/customers/name	Unlimited	\N
82	14	GET	Application & Application User	/sec	Unlimited	\N
83	14	GET	Application & Application User	/handler	Unlimited	\N
84	14	GET	Application & Application User	/orders/{orderId}	Unlimited	\N
85	14	GET	Application & Application User	/check-header	Unlimited	\N
91	15	GET	Application & Application User	/users/{id}	Unlimited	\N
92	15	DELETE	Application & Application User	/users/{id}	ADP10PerMin	\N
93	15	GET	Application & Application User	/users	Unlimited	\N
94	15	POST	Application & Application User	/users	Unlimited	\N
95	15	GET	None	/hello	Unlimited	\N
96	16	GET	Application & Application User	/users	Unlimited	\N
97	16	POST	Application & Application User	/users	Unlimited	\N
98	16	GET	Application & Application User	/users/{id}	Unlimited	\N
99	16	DELETE	Application & Application User	/users/{id}	ADP10PerMin	\N
100	16	GET	None	/hello	Unlimited	\N
102	17	GET	Application & Application User	/hello	Unlimited	\N
103	18	GET	Application & Application User	/hello	Unlimited	\N
105	19	GET	Application & Application User	/hello	ADP10PerMin	\N
107	20	GET	Application & Application User	/hello	ADP10PerMin	\N
109	21	POST	Application & Application User	/*	Unlimited	\N
110	22	POST	Application & Application User	/*	Unlimited	\N
123	23	QUERY	Any	allCharacters	Unlimited	\N
124	23	QUERY	Any	allDroids	Unlimited	\N
125	23	QUERY	Any	allHumans	ADP10PerMin	\N
126	23	QUERY	Any	character	Unlimited	\N
127	23	MUTATION	Any	createReview	Unlimited	\N
128	23	QUERY	Any	droid	Unlimited	\N
129	23	QUERY	Any	hero	Unlimited	\N
130	23	QUERY	Any	human	Unlimited	\N
131	23	SUBSCRIPTION	Any	reviewAdded	Unlimited	\N
132	23	QUERY	Any	reviews	Unlimited	\N
133	23	QUERY	Any	search	Unlimited	\N
134	23	QUERY	Any	starship	Unlimited	\N
135	24	QUERY	Any	allCharacters	Unlimited	\N
136	24	QUERY	Any	human	Unlimited	\N
137	24	QUERY	Any	starship	Unlimited	\N
138	24	QUERY	Any	droid	Unlimited	\N
139	24	QUERY	Any	reviews	Unlimited	\N
140	24	QUERY	Any	allDroids	Unlimited	\N
141	24	MUTATION	Any	createReview	Unlimited	\N
142	24	QUERY	Any	allHumans	ADP10PerMin	\N
143	24	QUERY	Any	character	Unlimited	\N
144	24	QUERY	Any	hero	Unlimited	\N
145	24	SUBSCRIPTION	Any	reviewAdded	Unlimited	\N
146	24	QUERY	Any	search	Unlimited	\N
147	25	GET	Any	/*	Unlimited	\N
148	25	PUT	Any	/*	Unlimited	\N
149	25	POST	Any	/*	Unlimited	\N
150	25	DELETE	Any	/*	Unlimited	\N
151	25	PATCH	Any	/*	Unlimited	\N
152	26	PUT	Any	/*	Unlimited	\N
153	26	PATCH	Any	/*	Unlimited	\N
154	26	GET	Any	/*	Unlimited	\N
155	26	POST	Any	/*	Unlimited	\N
156	26	DELETE	Any	/*	Unlimited	\N
162	28	GET	Application & Application User	/customers/{id}	Unlimited	\N
163	28	DELETE	Application & Application User	/customers/{id}	Unlimited	\N
164	28	PUT	Application & Application User	/customers	Unlimited	\N
165	28	POST	Application & Application User	/customers	Unlimited	\N
166	28	POST	Application & Application User	/customers/name	Unlimited	\N
167	28	GET	Application & Application User	/sec	Unlimited	\N
168	28	GET	Application & Application User	/handler	Unlimited	\N
169	28	GET	Application & Application User	/orders/{orderId}	Unlimited	\N
170	28	GET	Application & Application User	/check-header	Unlimited	\N
176	29	GET	Application & Application User	/users/{id}	Unlimited	\N
177	29	DELETE	Application & Application User	/users/{id}	ADP10PerMin	\N
178	29	GET	Application & Application User	/users	Unlimited	\N
179	29	POST	Application & Application User	/users	Unlimited	\N
180	29	GET	None	/hello	Unlimited	\N
181	30	GET	Application & Application User	/users	Unlimited	\N
182	30	DELETE	Application & Application User	/users/{id}	ADP10PerMin	\N
183	30	GET	Application & Application User	/users/{id}	Unlimited	\N
184	30	POST	Application & Application User	/users	Unlimited	\N
185	30	GET	None	/hello	Unlimited	\N
187	31	GET	Application & Application User	/hello	Unlimited	\N
188	32	GET	Application & Application User	/hello	Unlimited	\N
190	33	GET	Application & Application User	/hello	ADP10PerMin	\N
192	34	GET	Application & Application User	/hello	ADP10PerMin	\N
194	35	POST	Application & Application User	/*	Unlimited	\N
195	36	POST	Application & Application User	/*	Unlimited	\N
208	37	QUERY	Any	allCharacters	Unlimited	\N
209	37	QUERY	Any	allDroids	Unlimited	\N
210	37	QUERY	Any	allHumans	ADP10PerMin	\N
211	37	QUERY	Any	character	Unlimited	\N
212	37	MUTATION	Any	createReview	Unlimited	\N
213	37	QUERY	Any	droid	Unlimited	\N
214	37	QUERY	Any	hero	Unlimited	\N
215	37	QUERY	Any	human	Unlimited	\N
216	37	SUBSCRIPTION	Any	reviewAdded	Unlimited	\N
217	37	QUERY	Any	reviews	Unlimited	\N
218	37	QUERY	Any	search	Unlimited	\N
219	37	QUERY	Any	starship	Unlimited	\N
220	38	QUERY	Any	starship	Unlimited	\N
221	38	QUERY	Any	reviews	Unlimited	\N
222	38	QUERY	Any	character	Unlimited	\N
223	38	QUERY	Any	hero	Unlimited	\N
224	38	QUERY	Any	allCharacters	Unlimited	\N
225	38	QUERY	Any	human	Unlimited	\N
226	38	MUTATION	Any	createReview	Unlimited	\N
227	38	QUERY	Any	search	Unlimited	\N
228	38	QUERY	Any	allHumans	ADP10PerMin	\N
229	38	QUERY	Any	allDroids	Unlimited	\N
230	38	SUBSCRIPTION	Any	reviewAdded	Unlimited	\N
231	38	QUERY	Any	droid	Unlimited	\N
232	39	GET	Any	/*	Unlimited	\N
233	39	PUT	Any	/*	Unlimited	\N
234	39	POST	Any	/*	Unlimited	\N
235	39	DELETE	Any	/*	Unlimited	\N
236	39	PATCH	Any	/*	Unlimited	\N
237	40	PATCH	Any	/*	Unlimited	\N
238	40	GET	Any	/*	Unlimited	\N
239	40	PUT	Any	/*	Unlimited	\N
240	40	POST	Any	/*	Unlimited	\N
241	40	DELETE	Any	/*	Unlimited	\N
247	42	GET	Application & Application User	/customers/{id}	Unlimited	\N
248	42	DELETE	Application & Application User	/customers/{id}	Unlimited	\N
249	42	PUT	Application & Application User	/customers	Unlimited	\N
250	42	POST	Application & Application User	/customers	Unlimited	\N
251	42	POST	Application & Application User	/customers/name	Unlimited	\N
252	42	GET	Application & Application User	/sec	Unlimited	\N
253	42	GET	Application & Application User	/handler	Unlimited	\N
254	42	GET	Application & Application User	/orders/{orderId}	Unlimited	\N
255	42	GET	Application & Application User	/check-header	Unlimited	\N
\.


--
-- Data for Name: am_app_key_domain_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_app_key_domain_mapping (consumer_key, authz_domain) FROM stdin;
\.


--
-- Data for Name: am_application; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_application (application_id, name, subscriber_id, application_tier, callback_url, description, application_status, group_id, created_by, created_time, updated_by, updated_time, uuid, token_type) FROM stdin;
1	DefaultApplication	1	Unlimited	\N	This is the default application	APPROVED		adp_sub_user	2026-03-27 15:48:06.655	\N	2026-03-27 15:48:06.655	c0a6fce7-324d-43d0-a01c-0268c529e1d0	JWT
2	ADPApplicationCS	1	ADP5PerMin	\N	adp application description	APPROVED		adp_sub_user	2026-03-27 15:48:06.73	\N	2026-03-27 15:48:06.73	09e6a494-d951-4636-98ca-c2a2103ee448	JWT
3	CustomerApp	1	Unlimited	\N	Test app for scenarios	APPROVED		adp_sub_user	2026-03-27 15:48:24.385	\N	2026-03-27 15:48:24.385	ffb0b7b8-0232-4fd2-a889-6da1306022b7	JWT
14	ADPCTSApplicationSC	3	Unlimited	\N	adp cross tenant subscription application description	APPROVED		adp_sub_user@adpsample.com	2026-03-27 15:51:47.833	\N	2026-03-27 15:51:47.833	7ca4c194-1515-4b07-a953-105497562701	JWT
4	ADPASApplicationCS	1	Unlimited	\N	adp application sharing application description	APPROVED		adp_sub_user	2026-03-27 15:48:26.194	\N	2026-03-27 15:48:26.194	7ac34850-a10b-4162-9334-7ceb798572ce	JWT
5	DefaultApplication	2	Unlimited	\N	This is the default application	APPROVED		adp_sub_user@adpexample.com	2026-03-27 15:49:46.919	\N	2026-03-27 15:49:46.919	2da356ba-d597-4e01-8c19-8e9b866cf86f	JWT
6	ADPApplicationEC	2	ADP5PerMin	\N	adp application description	APPROVED		adp_sub_user@adpexample.com	2026-03-27 15:49:46.934	\N	2026-03-27 15:49:46.934	c2b30a44-cfeb-4ee6-95df-5e9cf2ea5573	JWT
7	CustomerApp	2	Unlimited	\N	Test app for scenarios	APPROVED		adp_sub_user@adpexample.com	2026-03-27 15:50:04.611	\N	2026-03-27 15:50:04.611	8c600c85-e1eb-413f-ac5c-755202c4cf78	JWT
8	ADPASApplicationEC	2	Unlimited	\N	adp application sharing application description	APPROVED		adp_sub_user@adpexample.com	2026-03-27 15:50:06.415	\N	2026-03-27 15:50:06.415	62b23b97-de53-46b7-8ba0-badeaa0b9a10	JWT
9	ADPCTSApplicationEC	2	Unlimited	\N	adp cross tenant subscription application description	APPROVED		adp_sub_user@adpexample.com	2026-03-27 15:50:07.572	\N	2026-03-27 15:50:07.572	7a097522-254e-4874-b2ac-51de6dcb3194	JWT
10	DefaultApplication	3	Unlimited	\N	This is the default application	APPROVED		adp_sub_user@adpsample.com	2026-03-27 15:51:27.22	\N	2026-03-27 15:51:27.22	66a66cf8-0168-4bbd-8366-02ff84281b86	JWT
11	ADPApplicationSC	3	ADP5PerMin	\N	adp application description	APPROVED		adp_sub_user@adpsample.com	2026-03-27 15:51:27.228	\N	2026-03-27 15:51:27.228	7baef6fd-a463-4f92-ab95-e85d90b5da22	JWT
12	CustomerApp	3	Unlimited	\N	Test app for scenarios	APPROVED		adp_sub_user@adpsample.com	2026-03-27 15:51:44.837	\N	2026-03-27 15:51:44.837	a0ddd4e4-f17f-41c2-8d0b-b8f62ebc55cc	JWT
13	ADPASApplicationSC	3	Unlimited	\N	adp application sharing application description	APPROVED		adp_sub_user@adpsample.com	2026-03-27 15:51:46.612	\N	2026-03-27 15:51:46.612	5a4a0c94-df99-408b-9063-7a7284f2ad03	JWT
\.


--
-- Data for Name: am_application_attributes; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_application_attributes (application_id, name, value, tenant_id) FROM stdin;
\.


--
-- Data for Name: am_application_group_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_application_group_mapping (application_id, group_id, tenant) FROM stdin;
4	adp-application-group	carbon.super
8	adp-application-group	adpexample.com
13	adp-application-group	adpsample.com
\.


--
-- Data for Name: am_application_key_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_application_key_mapping (uuid, application_id, consumer_key, key_type, state, create_mode, app_info, key_manager) FROM stdin;
39a71eb2-3495-48a2-9590-c99c0f221a1c	2	IjE3DFqMvXwVFKXf5XM5fMFwafMa	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a22496a45334446714d76587756464b586635584d35664d467761664d61222c22636c69656e744e616d65223a226164705f7375625f757365725f4144504170706c69636174696f6e43535f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a226e36546a525858327265746e33464c565f786273716c6d6c51413461222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a223836343030222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a333630302c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22726566726573685f746f6b656e5f6578706972795f74696d65223a38363430307d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f4144504170706c69636174696f6e43535f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f75736572227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	f0e01e74-975e-431f-8bd8-034fa578f509
753bf207-7c9b-473a-ab3a-a587db212ef3	3	w1ykQHnJIB8G5PpxI1TUiM7W05oa	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a227731796b51486e4a494238473550707849315455694d375730356f61222c22636c69656e744e616d65223a226164705f7375625f757365725f437573746f6d65724170705f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a22425936426a634953474b6671616b3833656f69713254687066446f61222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a2232313437343833363436222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a323134373438333634362c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a323134373438333634362c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a323134373438333634362c22726566726573685f746f6b656e5f6578706972795f74696d65223a323134373438333634367d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f437573746f6d65724170705f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f75736572227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	f0e01e74-975e-431f-8bd8-034fa578f509
c8e69165-190e-43d7-ba27-43a44f6dfdee	6	63GebUlqAMneJSCXJTFLcHrlZQka	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a223633476562556c71414d6e654a5343584a54464c6348726c5a516b61222c22636c69656e744e616d65223a226164705f7375625f757365725f4144504170706c69636174696f6e45435f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a227839504455434b7656326b4c34424f4146314741356f6d78304a6f61222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a223836343030222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a333630302c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22726566726573685f746f6b656e5f6578706972795f74696d65223a38363430307d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f4144504170706c69636174696f6e45435f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f75736572406164706578616d706c652e636f6d227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	e2427239-6748-49e7-98d0-e7f07d03a1ab
826f8105-53c1-412e-87f0-84d1233d7520	7	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a22544471615a575a3669325175334c453345476a6a5045335a30724161222c22636c69656e744e616d65223a226164705f7375625f757365725f437573746f6d65724170705f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a22765f4c54524f543536464e5f6572306f417a5031786348626c666761222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a2232313437343833363436222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a323134373438333634362c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a323134373438333634362c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a323134373438333634362c22726566726573685f746f6b656e5f6578706972795f74696d65223a323134373438333634367d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f437573746f6d65724170705f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f75736572406164706578616d706c652e636f6d227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	e2427239-6748-49e7-98d0-e7f07d03a1ab
726dbbe2-b3ae-4e88-ac1b-27d5a88bca09	9	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a22376f384a597135474c50635a6f66446d346d31786b376a414a334961222c22636c69656e744e616d65223a226164705f7375625f757365725f4144504354534170706c69636174696f6e45435f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a225f51664b5f624b36386b307152335735314945726a375f706d656f61222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a223836343030222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a333630302c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22726566726573685f746f6b656e5f6578706972795f74696d65223a38363430307d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f4144504354534170706c69636174696f6e45435f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f75736572406164706578616d706c652e636f6d227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	f0e01e74-975e-431f-8bd8-034fa578f509
f63ef2a4-e22b-4a7f-bef3-9774270f48c5	11	JmEzoL3bVKApBDegL1atV0cCfEoa	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a224a6d457a6f4c3362564b4170424465674c3161745630634366456f61222c22636c69656e744e616d65223a226164705f7375625f757365725f4144504170706c69636174696f6e53435f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a226843546d5f593338444f32436269725f6956767a7975494e6f517361222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a223836343030222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a333630302c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22726566726573685f746f6b656e5f6578706972795f74696d65223a38363430307d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f4144504170706c69636174696f6e53435f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f757365724061647073616d706c652e636f6d227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	8a451785-ddad-47e6-8d04-4b2e1d55ec78
8658b47f-9b6a-4bdb-9be5-f222da3d083f	12	LrCi_oe6yaP1SeRfzzvccZIDECoa	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a224c7243695f6f653679615031536552667a7a7663635a494445436f61222c22636c69656e744e616d65223a226164705f7375625f757365725f437573746f6d65724170705f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a2252434a365f674d6e6e5251376f336661544a4f30537056786d4e3061222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a2232313437343833363436222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a323134373438333634362c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a323134373438333634362c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a323134373438333634362c22726566726573685f746f6b656e5f6578706972795f74696d65223a323134373438333634367d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f437573746f6d65724170705f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f757365724061647073616d706c652e636f6d227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	8a451785-ddad-47e6-8d04-4b2e1d55ec78
0b429059-404a-4327-87d7-21de8f726554	14	a6M8FWBd8aBCnoaYttsfjRGhVu0a	PRODUCTION	COMPLETED	CREATED	\\x7b22636c69656e744964223a2261364d3846574264386142436e6f6159747473666a52476856753061222c22636c69656e744e616d65223a226164705f7375625f757365725f4144504354534170706c69636174696f6e53435f50524f44554354494f4e222c2263616c6c4261636b55524c223a22222c22636c69656e74536563726574223a2246474f4d44433745574155796e5735776432694e6171587369724561222c22706172616d6574657273223a7b22746f6b656e53636f7065223a5b2264656661756c74225d2c2276616c6964697479506572696f64223a223836343030222c226772616e745f7479706573223a22726566726573685f746f6b656e2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a73616d6c322d6265617265722070617373776f726420636c69656e745f63726564656e7469616c73206977613a6e746c6d2075726e3a696574663a706172616d733a6f617574683a6772616e742d747970653a6a77742d626561726572222c226b65795f74797065223a2250524f44554354494f4e222c226164646974696f6e616c50726f70657274696573223a7b2269645f746f6b656e5f6578706972795f74696d65223a333630302c226170706c69636174696f6e5f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22757365725f6163636573735f746f6b656e5f6578706972795f74696d65223a333630302c22726566726573685f746f6b656e5f6578706972795f74696d65223a38363430307d2c2272656469726563745f75726973223a22222c22636c69656e745f6e616d65223a226164705f7375625f757365725f4144504354534170706c69636174696f6e53435f50524f44554354494f4e222c22757365726e616d65223a226164705f7375625f757365724061647073616d706c652e636f6d227d2c226973536161734170706c69636174696f6e223a747275652c2261707041747472696275746573223a7b7d2c22746f6b656e54797065223a224a5754227d	f0e01e74-975e-431f-8bd8-034fa578f509
\.


--
-- Data for Name: am_application_registration; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_application_registration (reg_id, subscriber_id, wf_ref, app_id, token_type, token_scope, inputs, allowed_domains, validity_period, key_manager) FROM stdin;
1	1	446081a3-3ac0-494f-a2a6-078de5318c79	2	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"86400","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"N\\/A\\",\\"user_access_token_expiry_time\\":\\"N\\/A\\",\\"refresh_token_expiry_time\\":\\"N\\/A\\",\\"id_token_expiry_time\\":\\"N\\/A\\"}","username":"adp_sub_user"}	ALL	0	f0e01e74-975e-431f-8bd8-034fa578f509
2	1	6e915d67-f25a-4040-8bb7-d2c3cf1a2602	3	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"2147483646","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"2147483646\\",\\"user_access_token_expiry_time\\":\\"2147483646\\",\\"refresh_token_expiry_time\\":\\"2147483646\\",\\"id_token_expiry_time\\":\\"2147483646\\"}","username":"adp_sub_user"}	ALL	0	f0e01e74-975e-431f-8bd8-034fa578f509
3	2	b086b60c-c1e6-430c-af27-d2b08e8653b8	6	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"86400","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"N\\/A\\",\\"user_access_token_expiry_time\\":\\"N\\/A\\",\\"refresh_token_expiry_time\\":\\"N\\/A\\",\\"id_token_expiry_time\\":\\"N\\/A\\"}","username":"adp_sub_user@adpexample.com"}	ALL	0	e2427239-6748-49e7-98d0-e7f07d03a1ab
4	2	4455f23b-05ee-4760-9a83-1b7ad78eb818	7	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"2147483646","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"2147483646\\",\\"user_access_token_expiry_time\\":\\"2147483646\\",\\"refresh_token_expiry_time\\":\\"2147483646\\",\\"id_token_expiry_time\\":\\"2147483646\\"}","username":"adp_sub_user@adpexample.com"}	ALL	0	e2427239-6748-49e7-98d0-e7f07d03a1ab
5	2	9d06f900-75bc-45d1-9893-deca79cbb14b	9	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"86400","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"N\\/A\\",\\"user_access_token_expiry_time\\":\\"N\\/A\\",\\"refresh_token_expiry_time\\":\\"N\\/A\\",\\"id_token_expiry_time\\":\\"N\\/A\\"}","username":"adp_sub_user@adpexample.com"}	ALL	0	f0e01e74-975e-431f-8bd8-034fa578f509
6	3	84f25a02-1ba8-4604-a8a3-fcfa6f6c6b2c	11	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"86400","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"N\\/A\\",\\"user_access_token_expiry_time\\":\\"N\\/A\\",\\"refresh_token_expiry_time\\":\\"N\\/A\\",\\"id_token_expiry_time\\":\\"N\\/A\\"}","username":"adp_sub_user@adpsample.com"}	ALL	0	8a451785-ddad-47e6-8d04-4b2e1d55ec78
7	3	cea97ab3-458e-4b18-86c4-bc2cf535a8ee	12	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"2147483646","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"2147483646\\",\\"user_access_token_expiry_time\\":\\"2147483646\\",\\"refresh_token_expiry_time\\":\\"2147483646\\",\\"id_token_expiry_time\\":\\"2147483646\\"}","username":"adp_sub_user@adpsample.com"}	ALL	0	8a451785-ddad-47e6-8d04-4b2e1d55ec78
8	3	01e1e490-bc4a-4acc-bd32-f7a348757752	14	PRODUCTION	default	{"tokenScope":"default","validityPeriod":"86400","callback_url":null,"grant_types":"refresh_token,urn:ietf:params:oauth:grant-type:saml2-bearer,password,client_credentials,iwa:ntlm,urn:ietf:params:oauth:grant-type:jwt-bearer","key_type":"PRODUCTION","additionalProperties":"{\\"application_access_token_expiry_time\\":\\"N\\/A\\",\\"user_access_token_expiry_time\\":\\"N\\/A\\",\\"refresh_token_expiry_time\\":\\"N\\/A\\",\\"id_token_expiry_time\\":\\"N\\/A\\"}","username":"adp_sub_user@adpsample.com"}	ALL	0	f0e01e74-975e-431f-8bd8-034fa578f509
\.


--
-- Data for Name: am_block_conditions; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_block_conditions (condition_id, type, value, enabled, domain, uuid) FROM stdin;
1	SUBSCRIPTION	/adp-oas2-rest/1.0.0:1.0.0:adp_sub_user-ADPApplicationCS:PRODUCTION	true	carbon.super	05205782-564b-4577-8574-b531220a1945
2	SUBSCRIPTION	/adp-oas3-rest/1.0.0:1.0.0:adp_sub_user-ADPApplicationCS:PRODUCTION	true	carbon.super	e6da3669-7612-4a54-a229-bce4b3985857
3	SUBSCRIPTION	/t/adpexample.com/adp-oas2-rest/1.0.0:1.0.0:adp_sub_user@adpexample.com-ADPApplicationEC:PRODUCTION	true	adpexample.com	cfbedcdb-c627-4b29-995e-ea69880a6a73
4	SUBSCRIPTION	/t/adpexample.com/adp-oas3-rest/1.0.0:1.0.0:adp_sub_user@adpexample.com-ADPApplicationEC:PRODUCTION	true	adpexample.com	673119bb-34ab-429c-b7b9-04df138476bd
5	SUBSCRIPTION	/t/adpsample.com/adp-oas2-rest/1.0.0:1.0.0:adp_sub_user@adpsample.com-ADPApplicationSC:PRODUCTION	true	adpsample.com	f8009d0a-04c1-4677-b0a0-8f6a6f967e4e
6	SUBSCRIPTION	/t/adpsample.com/adp-oas3-rest/1.0.0:1.0.0:adp_sub_user@adpsample.com-ADPApplicationSC:PRODUCTION	true	adpsample.com	cf01688c-cdf9-4076-9ab1-5bcef1512619
\.


--
-- Data for Name: am_certificate_metadata; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_certificate_metadata (tenant_id, alias, end_point) FROM stdin;
\.


--
-- Data for Name: am_condition_group; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_condition_group (condition_group_id, policy_id, quota_type, quota, quota_unit, unit_time, time_unit, description) FROM stdin;
\.


--
-- Data for Name: am_correlation_configs; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_correlation_configs (component_name, enabled) FROM stdin;
\.


--
-- Data for Name: am_correlation_properties; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_correlation_properties (property_name, component_name, property_value) FROM stdin;
\.


--
-- Data for Name: am_external_stores; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_external_stores (apistore_id, api_id, store_id, store_display_name, store_endpoint, store_type, last_updated_time) FROM stdin;
\.


--
-- Data for Name: am_graphql_complexity; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_graphql_complexity (uuid, api_id, type, field, complexity_value) FROM stdin;
\.


--
-- Data for Name: am_gw_api_artifacts; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_gw_api_artifacts (api_id, artifact, gateway_instruction, gateway_label, time_stamp) FROM stdin;
\.


--
-- Data for Name: am_gw_published_api_details; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_gw_published_api_details (api_id, tenant_domain, api_provider, api_name, api_version) FROM stdin;
\.


--
-- Data for Name: am_header_field_condition; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_header_field_condition (header_field_id, condition_group_id, header_field_name, header_field_value, is_header_field_mapping) FROM stdin;
\.


--
-- Data for Name: am_ip_condition; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_ip_condition (am_ip_condition_id, starting_ip, ending_ip, specific_ip, within_ip_range, condition_group_id) FROM stdin;
\.


--
-- Data for Name: am_jwt_claim_condition; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_jwt_claim_condition (jwt_claim_id, condition_group_id, claim_uri, claim_attrib, is_claim_mapping) FROM stdin;
\.


--
-- Data for Name: am_key_manager; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_key_manager (uuid, name, display_name, description, type, configuration, enabled, tenant_domain) FROM stdin;
f0e01e74-975e-431f-8bd8-034fa578f509	Resident Key Manager	\N	This is Resident Key Manager	default	\\x7b22746f6b656e5f666f726d61745f737472696e67223a225b7b5c22656e61626c655c223a747275652c5c22747970655c223a5c225245464552454e43455c222c5c2276616c75655c223a5c225b302d39612d66412d465d7b387d2d5b302d39612d66412d465d7b347d2d5b312d355d5b302d39612d66412d465d7b337d2d5b3839616241425d5b302d39612d66412d465d7b337d2d5b302d39612d66412d465d7b31327d5c227d5d227d	t	carbon.super
e2427239-6748-49e7-98d0-e7f07d03a1ab	Resident Key Manager	\N	This is Resident Key Manager	default	\\x7b22746f6b656e5f666f726d61745f737472696e67223a225b7b5c22656e61626c655c223a747275652c5c22747970655c223a5c225245464552454e43455c222c5c2276616c75655c223a5c225b302d39612d66412d465d7b387d2d5b302d39612d66412d465d7b347d2d5b312d355d5b302d39612d66412d465d7b337d2d5b3839616241425d5b302d39612d66412d465d7b337d2d5b302d39612d66412d465d7b31327d5c227d5d227d	t	adpexample.com
8a451785-ddad-47e6-8d04-4b2e1d55ec78	Resident Key Manager	\N	This is Resident Key Manager	default	\\x7b22746f6b656e5f666f726d61745f737472696e67223a225b7b5c22656e61626c655c223a747275652c5c22747970655c223a5c225245464552454e43455c222c5c2276616c75655c223a5c225b302d39612d66412d465d7b387d2d5b302d39612d66412d465d7b347d2d5b312d355d5b302d39612d66412d465d7b337d2d5b3839616241425d5b302d39612d66412d465d7b337d2d5b302d39612d66412d465d7b31327d5c227d5d227d	t	adpsample.com
\.


--
-- Data for Name: am_label_urls; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_label_urls (label_id, access_url) FROM stdin;
cfe933ea-2635-427c-b83c-108a44446927	https://localhost:9095
d18b90b8-2e91-465c-a8b8-194d3f5e20f3	https://localhost:9095
4b311b15-4fff-4e49-8348-db791672247e	https://localhost:9095
\.


--
-- Data for Name: am_labels; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_labels (label_id, name, description, tenant_domain) FROM stdin;
cfe933ea-2635-427c-b83c-108a44446927	MARKETING_STORE	Public microgateway for marketing	carbon.super
d18b90b8-2e91-465c-a8b8-194d3f5e20f3	MARKETING_STORE	Public microgateway for marketing	adpexample.com
4b311b15-4fff-4e49-8348-db791672247e	MARKETING_STORE	Public microgateway for marketing	adpsample.com
\.


--
-- Data for Name: am_monetization_usage; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_monetization_usage (id, state, status, started_time, published_time) FROM stdin;
\.


--
-- Data for Name: am_notification_subscriber; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_notification_subscriber (uuid, category, notification_method, subscriber_address) FROM stdin;
\.


--
-- Data for Name: am_policy_application; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_policy_application (policy_id, name, display_name, tenant_id, description, quota_type, quota, quota_unit, unit_time, time_unit, is_deployed, custom_attributes, uuid) FROM stdin;
1	50PerMin	50PerMin	-1234	Allows 50 request per minute	requestCount	50	\N	1	min	t	\N	0fe6fbbe-b50d-4be3-873f-696b44570f92
2	20PerMin	20PerMin	-1234	Allows 20 request per minute	requestCount	20	\N	1	min	t	\N	6a47d73c-e25b-4574-8293-4f9686622d82
3	10PerMin	10PerMin	-1234	Allows 10 request per minute	requestCount	10	\N	1	min	t	\N	9c7ce5dd-76e8-4cd7-a6b9-2642ac2899d2
4	Unlimited	Unlimited	-1234	Allows unlimited requests	requestCount	2147483647	\N	1	min	t	\N	da714840-e0cd-4bd2-8c00-2c76165d2226
5	ADP5PerMin	ADP5PerMin	-1234	5PerMinute	requestCount	5	\N	1	min	t	\N	0129fd40-e8cf-452d-a869-3ca11908e730
6	50PerMin	50PerMin	1	Allows 50 request per minute	requestCount	50	\N	1	min	t	\N	14d75981-96d1-47a8-93bd-210f7d1b8915
7	20PerMin	20PerMin	1	Allows 20 request per minute	requestCount	20	\N	1	min	t	\N	abdce0e3-b446-413a-9ebb-a65990d0c49a
8	10PerMin	10PerMin	1	Allows 10 request per minute	requestCount	10	\N	1	min	t	\N	2d1a1cdf-8942-4b1f-b3e3-1c11edb672d3
9	Unlimited	Unlimited	1	Allows unlimited requests	requestCount	2147483647	\N	1	min	t	\N	7f9f5097-6d2a-4f20-b2c6-1cc05455a973
10	ADP5PerMin	ADP5PerMin	1	5PerMinute	requestCount	5	\N	1	min	t	\N	12401675-bed9-44d3-939e-e1c289b69d57
11	50PerMin	50PerMin	2	Allows 50 request per minute	requestCount	50	\N	1	min	t	\N	91518d8e-e951-46c1-87c0-7cf07711c0f9
12	20PerMin	20PerMin	2	Allows 20 request per minute	requestCount	20	\N	1	min	t	\N	a86fdb1c-4c90-4aa4-ac30-384e8c41f65d
13	10PerMin	10PerMin	2	Allows 10 request per minute	requestCount	10	\N	1	min	t	\N	ef6e009b-5b32-40d9-80f4-f26c82edc608
14	Unlimited	Unlimited	2	Allows unlimited requests	requestCount	2147483647	\N	1	min	t	\N	ec001a31-b3e4-466f-ae1c-d38eb02ece36
15	ADP5PerMin	ADP5PerMin	2	5PerMinute	requestCount	5	\N	1	min	t	\N	6728e5b1-9795-4938-9408-55443b54dd09
\.


--
-- Data for Name: am_policy_global; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_policy_global (policy_id, name, key_template, tenant_id, description, siddhi_query, is_deployed, uuid) FROM stdin;
\.


--
-- Data for Name: am_policy_hard_throttling; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_policy_hard_throttling (policy_id, name, tenant_id, description, quota_type, quota, quota_unit, unit_time, time_unit, is_deployed) FROM stdin;
\.


--
-- Data for Name: am_policy_subscription; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_policy_subscription (policy_id, name, display_name, tenant_id, description, quota_type, quota, quota_unit, unit_time, time_unit, rate_limit_count, rate_limit_time_unit, is_deployed, custom_attributes, stop_on_quota_reach, billing_plan, uuid, monetization_plan, fixed_rate, billing_cycle, price_per_request, currency, max_complexity, max_depth) FROM stdin;
1	Gold	Gold	-1234	Allows 5000 requests per minute	requestCount	5000	\N	1	min	0	\N	t	\N	t	FREE	7bf9add1-c81e-4287-9843-7e90056b294e	\N	\N	\N	\N	\N	0	0
2	Silver	Silver	-1234	Allows 2000 requests per minute	requestCount	2000	\N	1	min	0	\N	t	\N	t	FREE	0f0d89bf-dd3a-469e-bc7d-75999cbed14e	\N	\N	\N	\N	\N	0	0
3	Bronze	Bronze	-1234	Allows 1000 requests per minute	requestCount	1000	\N	1	min	0	\N	t	\N	t	FREE	e0ba4a46-4782-4ba2-9ddb-ddb4937d180b	\N	\N	\N	\N	\N	0	0
4	Unauthenticated	Unauthenticated	-1234	Allows 500 request(s) per minute	requestCount	500	\N	1	min	0	\N	t	\N	t	FREE	0601a582-e997-4e43-a1d2-5b2da55e1a36	\N	\N	\N	\N	\N	0	0
5	Unlimited	Unlimited	-1234	Allows unlimited requests	requestCount	2147483647	\N	1	min	0	\N	t	\N	t	FREE	00815d6e-eb44-4433-ab7f-126d940ac640	\N	\N	\N	\N	\N	0	0
6	ADPBrass	ADPBrass	-1234	Allows 20 requests per minute	requestCount	20	\N	1	min	5	sec	t	\\x5b5d	t	FREE	3cfa4154-3236-4ac9-b8b8-cac16bfb8719	FixedRate		week			10	5
7	Gold	Gold	1	Allows 5000 requests per minute	requestCount	5000	\N	1	min	0	\N	t	\N	t	FREE	e8a356d1-1ed0-4999-b48f-66929283660a	\N	\N	\N	\N	\N	0	0
8	Silver	Silver	1	Allows 2000 requests per minute	requestCount	2000	\N	1	min	0	\N	t	\N	t	FREE	03298a61-3d76-400c-b76e-e83173d2838e	\N	\N	\N	\N	\N	0	0
9	Bronze	Bronze	1	Allows 1000 requests per minute	requestCount	1000	\N	1	min	0	\N	t	\N	t	FREE	fa1418f8-e8f3-43f1-9abe-e59666615abb	\N	\N	\N	\N	\N	0	0
10	Unauthenticated	Unauthenticated	1	Allows 500 request(s) per minute	requestCount	500	\N	1	min	0	\N	t	\N	t	FREE	1ff02782-d077-4dad-8313-0ca6f8e4c515	\N	\N	\N	\N	\N	0	0
11	Unlimited	Unlimited	1	Allows unlimited requests	requestCount	2147483647	\N	1	min	0	\N	t	\N	t	FREE	099c80d7-b57e-43dc-b87e-0bd4912fccb4	\N	\N	\N	\N	\N	0	0
12	ADPBrass	ADPBrass	1	Allows 20 requests per minute	requestCount	20	\N	1	min	5	sec	t	\\x5b5d	t	FREE	2ba54426-5e9a-4ebc-9fe7-cddbc54222ef	FixedRate		week			10	5
13	Gold	Gold	2	Allows 5000 requests per minute	requestCount	5000	\N	1	min	0	\N	t	\N	t	FREE	506462e8-a23e-4ef2-a807-262e1c283dad	\N	\N	\N	\N	\N	0	0
14	Silver	Silver	2	Allows 2000 requests per minute	requestCount	2000	\N	1	min	0	\N	t	\N	t	FREE	789f1202-b44c-4872-ad3b-de3877eb2435	\N	\N	\N	\N	\N	0	0
15	Bronze	Bronze	2	Allows 1000 requests per minute	requestCount	1000	\N	1	min	0	\N	t	\N	t	FREE	8ac4bfb9-abd8-4bc6-9956-a71fdb12d26b	\N	\N	\N	\N	\N	0	0
16	Unauthenticated	Unauthenticated	2	Allows 500 request(s) per minute	requestCount	500	\N	1	min	0	\N	t	\N	t	FREE	39929dae-99d6-4f6e-bd9f-ce56211c641b	\N	\N	\N	\N	\N	0	0
17	Unlimited	Unlimited	2	Allows unlimited requests	requestCount	2147483647	\N	1	min	0	\N	t	\N	t	FREE	8ccbe49c-a469-4aa7-ab05-f18e348a323c	\N	\N	\N	\N	\N	0	0
18	ADPBrass	ADPBrass	2	Allows 20 requests per minute	requestCount	20	\N	1	min	5	sec	t	\\x5b5d	t	FREE	99efcd60-afee-4b14-86b8-286913f872a8	FixedRate		week			10	5
\.


--
-- Data for Name: am_query_parameter_condition; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_query_parameter_condition (query_parameter_id, condition_group_id, parameter_name, parameter_value, is_param_mapping) FROM stdin;
\.


--
-- Data for Name: am_revoked_jwt; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_revoked_jwt (uuid, signature, expiry_timestamp, tenant_id, token_type, time_created) FROM stdin;
054a44a4-28e8-4bbb-8ec7-f3537038e313	8795e1e1-3931-4251-bc19-da1b916d61a8	1774610410060	-1234	JWT	2026-03-27 15:50:10.133451
68c32289-775b-466d-b434-94d1324ad8b9	c1e729f8-09bd-465d-a153-fd941342e2b2	1774610510335	-1234	JWT	2026-03-27 15:51:50.354356
\.


--
-- Data for Name: am_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_scope (scope_id, name, display_name, description, tenant_id, scope_type) FROM stdin;
1	adp-shared-scope-with-roles	ADP Shared Scope with Roles	Shared scope with role mapping	-1234	OAUTH2
2	adp-shared-scope-without-roles	ADP Shared Scope without Roles	Shared scope without role mapping	-1234	OAUTH2
3	adp-local-scope-without-roles	adp-local-scope-without-roles	ADP local scope without roles	-1234	OAUTH2
4	adp-film-subscriber	adp-film-subscriber	ADP film subscriber	-1234	OAUTH2
5	adp-admin	adp-admin	ADP admin	-1234	OAUTH2
6	adp-shared-scope-with-roles	ADP Shared Scope with Roles	Shared scope with role mapping	1	OAUTH2
7	adp-shared-scope-without-roles	ADP Shared Scope without Roles	Shared scope without role mapping	1	OAUTH2
8	adp-local-scope-without-roles	adp-local-scope-without-roles	ADP local scope without roles	1	OAUTH2
9	adp-film-subscriber	adp-film-subscriber	ADP film subscriber	1	OAUTH2
10	adp-admin	adp-admin	ADP admin	1	OAUTH2
11	adp-shared-scope-with-roles	ADP Shared Scope with Roles	Shared scope with role mapping	2	OAUTH2
12	adp-shared-scope-without-roles	ADP Shared Scope without Roles	Shared scope without role mapping	2	OAUTH2
13	adp-local-scope-without-roles	adp-local-scope-without-roles	ADP local scope without roles	2	OAUTH2
14	adp-film-subscriber	adp-film-subscriber	ADP film subscriber	2	OAUTH2
15	adp-admin	adp-admin	ADP admin	2	OAUTH2
\.


--
-- Data for Name: am_scope_binding; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_scope_binding (scope_id, scope_binding, binding_type) FROM stdin;
1	ADP_CREATOR	DEFAULT
1	ADP_SUBSCRIBER	DEFAULT
5	admin	DEFAULT
6	ADP_CREATOR	DEFAULT
6	ADP_SUBSCRIBER	DEFAULT
10	admin	DEFAULT
11	ADP_CREATOR	DEFAULT
11	ADP_SUBSCRIBER	DEFAULT
15	admin	DEFAULT
\.


--
-- Data for Name: am_security_audit_uuid_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_security_audit_uuid_mapping (api_id, audit_uuid) FROM stdin;
\.


--
-- Data for Name: am_shared_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_shared_scope (name, uuid, tenant_id) FROM stdin;
adp-shared-scope-with-roles	4cb56e78-6af0-4bf7-92c6-ca18401f82fc	-1234
adp-shared-scope-without-roles	a74618c6-3b37-4716-9a06-46d1b0b0ef4f	-1234
adp-shared-scope-with-roles	06b78735-9108-4915-a32c-441f6ea07535	1
adp-shared-scope-without-roles	e6c6ed0f-2847-4f50-9dd5-9e54c0697fbc	1
adp-shared-scope-with-roles	e0b03772-c77f-4104-a8fe-00e4b5303c73	2
adp-shared-scope-without-roles	cf5a5cb3-879b-456d-b67c-255601fd8304	2
\.


--
-- Data for Name: am_subscriber; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_subscriber (subscriber_id, user_id, tenant_id, email_address, date_subscribed, created_by, created_time, updated_by, updated_time) FROM stdin;
1	adp_sub_user	-1234		2026-03-27 15:48:06.648	adp_sub_user	2026-03-27 15:48:06.648	\N	2026-03-27 15:48:06.648
2	adp_sub_user@adpexample.com	1		2026-03-27 15:49:46.91	adp_sub_user@adpexample.com	2026-03-27 15:49:46.91	\N	2026-03-27 15:49:46.91
3	adp_sub_user@adpsample.com	2		2026-03-27 15:51:27.214	adp_sub_user@adpsample.com	2026-03-27 15:51:27.214	\N	2026-03-27 15:51:27.214
\.


--
-- Data for Name: am_subscription; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_subscription (subscription_id, tier_id, tier_id_pending, api_id, last_accessed, application_id, sub_status, subs_create_state, created_by, created_time, updated_by, updated_time, uuid) FROM stdin;
1	ADPBrass	ADPBrass	1	\N	2	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:07.351	\N	2026-03-27 15:48:07.351	b2c79e10-3a10-427c-bcb3-ba345723196e
22	ADPBrass	ADPBrass	37	\N	11	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:30.154	\N	2026-03-27 15:51:30.154	e190ee96-95de-4350-a41e-16f3b3ece92f
4	Unlimited	Unlimited	7	\N	2	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:09.186	\N	2026-03-27 15:48:09.186	cd0f524e-a033-428e-a522-7ec922b23615
5	ADPBrass	ADPBrass	9	\N	2	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:09.771	\N	2026-03-27 15:48:09.771	a0422301-5054-41b2-9659-8b43538070e8
6	Gold	Gold	11	\N	2	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:10.36	\N	2026-03-27 15:48:10.36	55553c9a-3a68-4580-96e6-0e3ae8f558af
7	ADPBrass	ADPBrass	13	\N	2	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:10.928	\N	2026-03-27 15:48:10.928	7f369e07-7ed1-4c92-b89b-3d87a78b48e5
8	Unlimited	Unlimited	14	\N	3	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:24.995	\N	2026-03-27 15:48:24.995	e8d61b83-d04c-450b-85c4-5404ff09a3a2
2	Unlimited	Unlimited	3	\N	2	BLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:07.975	\N	2026-03-27 15:48:28.253	1ec73024-1d9e-4c1d-b83b-485402674544
3	Unlimited	Unlimited	5	\N	2	PROD_ONLY_BLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:48:08.581	\N	2026-03-27 15:48:28.808	fac0b09c-071d-4c04-b2f5-4652eb0ebc2d
9	ADPBrass	ADPBrass	15	\N	6	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:47.494	\N	2026-03-27 15:49:47.494	bf694df2-f7d9-4d92-8a09-5b5e19d01756
23	Gold	Gold	39	\N	11	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:30.781	\N	2026-03-27 15:51:30.781	e3b59c01-0d32-4f3c-93cb-be25d28c8304
12	Unlimited	Unlimited	21	\N	6	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:49.254	\N	2026-03-27 15:49:49.254	7d0a9eaa-35e9-4c7e-9457-5d39cc621d92
13	ADPBrass	ADPBrass	23	\N	6	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:49.857	\N	2026-03-27 15:49:49.857	92da9e18-a2fa-428c-94b4-c820198b17e3
14	Gold	Gold	25	\N	6	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:50.444	\N	2026-03-27 15:49:50.444	82a281ca-6e3b-4b16-bf1b-7cb3d860e38d
15	ADPBrass	ADPBrass	27	\N	6	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:51.081	\N	2026-03-27 15:49:51.081	3eb78a41-3ddb-492b-adc5-7aa4ee008911
16	Unlimited	Unlimited	28	\N	7	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:50:05.168	\N	2026-03-27 15:50:05.168	36032399-c0cc-4df7-9ad1-49ff9bc462e1
17	Unlimited	Unlimited	5	\N	9	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:50:08.844	\N	2026-03-27 15:50:08.844	6b4c4170-a2af-4f5d-8624-855ca134984b
10	Unlimited	Unlimited	17	\N	6	BLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:48.081	\N	2026-03-27 15:50:11.237	08edfd36-31ae-4b38-8252-b5e54a721b1d
11	Unlimited	Unlimited	19	\N	6	PROD_ONLY_BLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:49:48.659	\N	2026-03-27 15:50:11.786	bd439ce8-1fb1-4751-a5ba-a1aca889c809
18	ADPBrass	ADPBrass	29	\N	11	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:27.804	\N	2026-03-27 15:51:27.804	6564c5ad-650e-4c3f-97ab-4c939dd7739a
21	Unlimited	Unlimited	35	\N	11	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:29.538	\N	2026-03-27 15:51:29.538	ee1b6881-ee91-4992-8610-309822d2d7ae
24	ADPBrass	ADPBrass	41	\N	11	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:31.383	\N	2026-03-27 15:51:31.383	7a0b707e-4752-4c20-94c7-fe6478e3528d
25	Unlimited	Unlimited	42	\N	12	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:45.417	\N	2026-03-27 15:51:45.417	cdd570f1-9474-4324-ab68-86997e17a812
26	Unlimited	Unlimited	5	\N	14	UNBLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:49.098	\N	2026-03-27 15:51:49.098	3aa4ab31-a870-4d54-a9e2-8c598dfe7fd7
19	Unlimited	Unlimited	31	\N	11	BLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:28.391	\N	2026-03-27 15:51:51.48	9e369a9a-02ee-4b17-a3a3-3549b5203055
20	Unlimited	Unlimited	33	\N	11	PROD_ONLY_BLOCKED	SUBSCRIBE	adp_sub_user	2026-03-27 15:51:28.964	\N	2026-03-27 15:51:52.048	3451ed95-19f5-40ad-aaf2-8e1383185f85
\.


--
-- Data for Name: am_subscription_key_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_subscription_key_mapping (subscription_id, access_token, key_type) FROM stdin;
\.


--
-- Data for Name: am_system_apps; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_system_apps (id, name, consumer_key, consumer_secret, tenant_domain, created_time) FROM stdin;
\.


--
-- Data for Name: am_tenant_themes; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_tenant_themes (tenant_id, theme) FROM stdin;
\.


--
-- Data for Name: am_throttle_tier_permissions; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_throttle_tier_permissions (throttle_tier_permissions_id, tier, permissions_type, roles, tenant_id) FROM stdin;
1	ADPBrass	allow	Internal/everyone	-1234
2	ADPBrass	allow	Internal/everyone	1
3	ADPBrass	allow	Internal/everyone	2
\.


--
-- Data for Name: am_tier_permissions; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_tier_permissions (tier_permissions_id, tier, permissions_type, roles, tenant_id) FROM stdin;
\.


--
-- Data for Name: am_usage_uploaded_files; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_usage_uploaded_files (tenant_domain, file_name, file_timestamp, file_processed, file_content) FROM stdin;
\.


--
-- Data for Name: am_user; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_user (user_id, user_name) FROM stdin;
\.


--
-- Data for Name: am_workflows; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.am_workflows (wf_id, wf_reference, wf_type, wf_status, wf_created_time, wf_updated_time, wf_status_desc, tenant_id, tenant_domain, wf_external_reference, wf_metadata, wf_properties) FROM stdin;
\.


--
-- Data for Name: cm_consent_receipt_property; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_consent_receipt_property (consent_receipt_id, name, value) FROM stdin;
\.


--
-- Data for Name: cm_pii_category; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_pii_category (id, name, description, display_name, is_sensitive, tenant_id) FROM stdin;
\.


--
-- Data for Name: cm_purpose; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_purpose (id, name, description, purpose_group, group_type, tenant_id) FROM stdin;
1	DEFAULT	For core functionalities of the product	DEFAULT	SP	-1234
\.


--
-- Data for Name: cm_purpose_category; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_purpose_category (id, name, description, tenant_id) FROM stdin;
1	DEFAULT	For core functionalities of the product	-1234
\.


--
-- Data for Name: cm_purpose_pii_cat_assoc; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_purpose_pii_cat_assoc (purpose_id, cm_pii_category_id, is_mandatory) FROM stdin;
\.


--
-- Data for Name: cm_receipt; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_receipt (consent_receipt_id, version, jurisdiction, consent_timestamp, collection_method, language, pii_principal_id, principal_tenant_id, policy_url, state, pii_controller) FROM stdin;
\.


--
-- Data for Name: cm_receipt_sp_assoc; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_receipt_sp_assoc (id, consent_receipt_id, sp_name, sp_display_name, sp_description, sp_tenant_id) FROM stdin;
\.


--
-- Data for Name: cm_sp_purpose_assoc; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_sp_purpose_assoc (id, receipt_sp_assoc, purpose_id, consent_type, is_primary_purpose, termination, third_party_disclosure, third_party_name) FROM stdin;
\.


--
-- Data for Name: cm_sp_purpose_pii_cat_assoc; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_sp_purpose_pii_cat_assoc (sp_purpose_assoc_id, pii_category_id, validity) FROM stdin;
\.


--
-- Data for Name: cm_sp_purpose_purpose_cat_assc; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.cm_sp_purpose_purpose_cat_assc (sp_purpose_assoc_id, purpose_category_id) FROM stdin;
\.


--
-- Data for Name: fido2_device_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.fido2_device_store (tenant_id, domain_name, user_name, time_registered, user_handle, credential_id, public_key_cose, signature_count, user_identity, display_name, is_usernameless_supported) FROM stdin;
\.


--
-- Data for Name: fido_device_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.fido_device_store (tenant_id, domain_name, user_name, time_registered, key_handle, device_data) FROM stdin;
\.


--
-- Data for Name: idn_associated_id; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_associated_id (id, idp_user_id, tenant_id, idp_id, domain_name, user_name, association_id) FROM stdin;
\.


--
-- Data for Name: idn_auth_session_app_info; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_session_app_info (session_id, subject, app_id, inbound_auth_type) FROM stdin;
\.


--
-- Data for Name: idn_auth_session_meta_data; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_session_meta_data (session_id, property_type, value) FROM stdin;
\.


--
-- Data for Name: idn_auth_session_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_session_store (session_id, session_type, operation, session_object, time_created, tenant_id, expiry_time) FROM stdin;
\.


--
-- Data for Name: idn_auth_temp_session_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_temp_session_store (session_id, session_type, operation, session_object, time_created, tenant_id, expiry_time) FROM stdin;
\.


--
-- Data for Name: idn_auth_user; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_user (user_id, user_name, tenant_id, domain_name, idp_id) FROM stdin;
\.


--
-- Data for Name: idn_auth_user_session_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_user_session_mapping (user_id, session_id) FROM stdin;
\.


--
-- Data for Name: idn_auth_wait_status; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_auth_wait_status (id, tenant_id, long_wait_key, wait_status, time_created, expire_time) FROM stdin;
\.


--
-- Data for Name: idn_base_table; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_base_table (product_name) FROM stdin;
WSO2 Identity Server
\.


--
-- Data for Name: idn_certificate; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_certificate (id, name, certificate_in_pem, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_claim; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_claim (id, dialect_id, claim_uri, tenant_id) FROM stdin;
1	1	http://wso2.org/claims/local	-1234
2	1	http://wso2.org/claims/locality	-1234
3	1	http://wso2.org/claims/country	-1234
4	1	http://wso2.org/claims/displayName	-1234
5	1	http://wso2.org/claims/identity/failedLoginLockoutCount	-1234
6	1	http://wso2.org/claims/mobile	-1234
7	1	http://wso2.org/claims/nickname	-1234
8	1	http://wso2.org/claims/identity/lockedReason	-1234
9	1	http://wso2.org/claims/fullname	-1234
10	1	http://wso2.org/claims/honorificPrefix	-1234
11	1	http://wso2.org/claims/skype	-1234
12	1	http://wso2.org/claims/addresses	-1234
13	1	http://wso2.org/claims/region	-1234
14	1	http://wso2.org/claims/middleName	-1234
15	1	http://wso2.org/claims/identity/lastLogonTime	-1234
16	1	http://wso2.org/claims/username	-1234
17	1	http://wso2.org/claims/preferredLanguage	-1234
18	1	http://wso2.org/claims/extendedExternalId	-1234
19	1	http://wso2.org/claims/timeZone	-1234
20	1	http://wso2.org/claims/lastname	-1234
21	1	http://wso2.org/claims/identity/lastLoginTime	-1234
22	1	http://wso2.org/claims/identity/preferredChannel	-1234
23	1	http://wso2.org/claims/costCenter	-1234
24	1	http://wso2.org/claims/location	-1234
25	1	http://wso2.org/claims/challengeQuestionUris	-1234
26	1	http://wso2.org/claims/identity/emailotp_disabled	-1234
27	1	http://wso2.org/claims/identity/accountLocked	-1234
28	1	http://wso2.org/claims/emails.other	-1234
29	1	http://wso2.org/claims/department	-1234
30	1	http://wso2.org/claims/photourl	-1234
31	1	http://wso2.org/claims/im	-1234
32	1	http://wso2.org/claims/postalcode	-1234
33	1	http://wso2.org/claims/emailaddress	-1234
34	1	http://wso2.org/claims/organization	-1234
35	1	http://wso2.org/claims/emails.work	-1234
36	1	http://wso2.org/claims/url	-1234
37	1	http://wso2.org/claims/addresses.locality	-1234
38	1	http://wso2.org/claims/identity/unlockTime	-1234
39	1	http://wso2.org/claims/phoneNumbers.work	-1234
40	1	http://wso2.org/claims/honorificSuffix	-1234
41	1	http://wso2.org/claims/identity/phoneVerified	-1234
42	1	http://wso2.org/claims/primaryChallengeQuestion	-1234
43	1	http://wso2.org/claims/addresses.formatted	-1234
44	1	http://wso2.org/claims/identity/secretkey	-1234
45	1	http://wso2.org/claims/identity/verifyEmail	-1234
46	1	http://wso2.org/claims/externalid	-1234
47	1	http://wso2.org/claims/challengeQuestion1	-1234
48	1	http://wso2.org/claims/phoneNumbers.fax	-1234
49	1	http://wso2.org/claims/modified	-1234
50	1	http://wso2.org/claims/formattedName	-1234
51	1	http://wso2.org/claims/identity/smsotp_disabled	-1234
52	1	http://wso2.org/claims/identity/askPassword	-1234
53	1	http://wso2.org/claims/challengeQuestion2	-1234
54	1	http://wso2.org/claims/phoneNumbers.home	-1234
55	1	http://wso2.org/claims/identity/failedPasswordRecoveryAttempts	-1234
56	1	http://wso2.org/claims/userid	-1234
57	1	http://wso2.org/claims/streetaddress	-1234
58	1	http://wso2.org/claims/telephone	-1234
59	1	http://wso2.org/claims/identity/emailaddress.pendingValue	-1234
60	1	http://wso2.org/claims/identity/accountDisabled	-1234
61	1	http://wso2.org/claims/givenname	-1234
62	1	http://wso2.org/claims/extendedDisplayName	-1234
63	1	http://wso2.org/claims/identity/failedLoginAttempts	-1234
64	1	http://wso2.org/claims/gtalk	-1234
65	1	http://wso2.org/claims/groups	-1234
66	1	http://wso2.org/claims/x509Certificates	-1234
67	1	http://wso2.org/claims/photos	-1234
68	1	http://wso2.org/claims/entitlements	-1234
69	1	http://wso2.org/claims/identity/lastPasswordUpdateTime	-1234
70	1	http://wso2.org/claims/identity/failedLoginAttemptsBeforeSuccess	-1234
71	1	http://wso2.org/claims/phoneNumbers	-1234
72	1	http://wso2.org/claims/dob	-1234
73	1	http://wso2.org/claims/phoneNumbers.other	-1234
74	1	http://wso2.org/claims/userprincipal	-1234
75	1	http://wso2.org/claims/emails.home	-1234
76	1	http://wso2.org/claims/stateorprovince	-1234
77	1	http://wso2.org/claims/oneTimePassword	-1234
78	1	http://wso2.org/claims/active	-1234
79	1	http://wso2.org/claims/resourceType	-1234
80	1	http://wso2.org/claims/identity/emailVerified	-1234
81	1	http://wso2.org/claims/userType	-1234
82	1	http://wso2.org/claims/otherphone	-1234
83	1	http://wso2.org/claims/thumbnail	-1234
84	1	http://wso2.org/claims/identity/adminForcedPasswordReset	-1234
85	1	http://wso2.org/claims/title	-1234
86	1	http://wso2.org/claims/extendedRef	-1234
87	1	http://wso2.org/claims/created	-1234
88	1	http://wso2.org/claims/gender	-1234
89	1	http://wso2.org/claims/role	-1234
90	1	http://wso2.org/claims/identity/accountState	-1234
91	1	http://wso2.org/claims/phoneNumbers.pager	-1234
92	5	gender	-1234
93	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress	-1234
94	3	urn:scim:schemas:core:1.0:timeZone	-1234
95	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value	-1234
96	3	urn:scim:schemas:core:1.0:photos.photo	-1234
97	7	dob	-1234
98	3	urn:scim:schemas:core:1.0:locale	-1234
99	6	http://eidas.europa.eu/attributes/legalperson/LegalName	-1234
100	3	urn:scim:schemas:core:1.0:name.givenName	-1234
101	8	http://eidas.europa.eu/attributes/naturalperson/PlaceOfBirth	-1234
102	3	urn:scim:schemas:core:1.0:phoneNumbers	-1234
103	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:organization	-1234
104	7	postcode	-1234
105	6	http://eidas.europa.eu/attributes/legalperson/EORI	-1234
106	4	urn:ietf:params:scim:schemas:core:2.0:User:emails	-1234
107	4	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers	-1234
108	4	urn:ietf:params:scim:schemas:core:2.0:User:name.familyName	-1234
109	4	urn:ietf:params:scim:schemas:core:2.0:User:photos.photo	-1234
110	3	urn:scim:schemas:core:1.0:photos.thumbnail	-1234
111	7	country	-1234
112	5	email_verified	-1234
113	4	urn:ietf:params:scim:schemas:core:2.0:User:title	-1234
114	3	urn:scim:schemas:core:1.0:userType	-1234
115	4	urn:ietf:params:scim:schemas:core:2.0:User:emails.home	-1234
116	3	urn:scim:schemas:core:1.0:active	-1234
117	3	urn:scim:schemas:core:1.0:phoneNumbers.pager	-1234
118	5	zoneinfo	-1234
119	3	urn:scim:schemas:core:1.0:name.honorificSuffix	-1234
120	3	urn:scim:schemas:core:1.0:photos	-1234
121	4	urn:ietf:params:scim:schemas:core:2.0:User:displayName	-1234
122	5	picture	-1234
123	3	urn:scim:schemas:core:1.0:meta.lastModified	-1234
124	4	urn:ietf:params:scim:schemas:core:2.0:User:x509Certificates.default	-1234
125	4	urn:ietf:params:scim:schemas:core:2.0:User:name.formatted	-1234
126	3	urn:scim:schemas:core:1.0:groups	-1234
127	3	urn:scim:schemas:core:1.0:phoneNumbers.home	-1234
128	3	urn:scim:schemas:core:1.0:externalId	-1234
129	3	urn:scim:schemas:core:1.0:roles	-1234
130	4	urn:ietf:params:scim:schemas:core:2.0:User:entitlements.default	-1234
131	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality	-1234
132	7	timezone	-1234
133	4	urn:ietf:params:scim:schemas:core:2.0:User:addresses	-1234
134	8	http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName	-1234
135	5	preferred_username	-1234
136	5	middle_name	-1234
137	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber	-1234
138	9	urn:ietf:params:scim:schemas:core:2.0:externalId	-1234
139	10	http://axschema.org/pref/language	-1234
140	3	urn:scim:schemas:core:1.0:addresses.formatted	-1234
141	4	urn:ietf:params:scim:schemas:core:2.0:User:userName	-1234
142	4	urn:ietf:params:scim:schemas:core:2.0:User:timezone	-1234
143	4	urn:ietf:params:scim:schemas:core:2.0:User:name.honorificSuffix	-1234
144	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/stateorprovince	-1234
145	6	http://eidas.europa.eu/attributes/legalperson/LegalPersonIdentifier	-1234
146	7	gender	-1234
147	7	language	-1234
148	5	sub	-1234
149	3	urn:scim:schemas:core:1.0:userName	-1234
150	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nickname	-1234
151	5	nickname	-1234
152	6	http://eidas.europa.eu/attributes/legalperson/TaxReference	-1234
153	10	http://axschema.org/contact/postalCode/home	-1234
154	9	urn:ietf:params:scim:schemas:core:2.0:meta.resourceType	-1234
155	3	urn:scim:schemas:core:1.0:ims.skype	-1234
156	3	urn:scim:schemas:core:1.0:preferredLanguage	-1234
157	3	urn:scim:schemas:core:1.0:phoneNumbers.other	-1234
158	8	http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName	-1234
159	4	urn:ietf:params:scim:schemas:core:2.0:User:name.middleName	-1234
160	4	urn:ietf:params:scim:schemas:core:2.0:User:addresses.work	-1234
161	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobile	-1234
162	7	fullname	-1234
163	10	http://axschema.org/namePerson/last	-1234
164	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname	-1234
165	7	email	-1234
166	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/gender	-1234
167	3	urn:scim:schemas:core:1.0:addresses.region	-1234
168	9	urn:ietf:params:scim:schemas:core:2.0:meta.location	-1234
169	3	urn:scim:schemas:core:1.0:emails.home	-1234
170	9	urn:ietf:params:scim:schemas:core:2.0:meta.lastModified	-1234
171	3	urn:scim:schemas:core:1.0:profileUrl	-1234
172	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth	-1234
173	3	urn:scim:schemas:core:1.0:name.familyName	-1234
174	9	urn:ietf:params:scim:schemas:core:2.0:meta.version	-1234
175	3	urn:scim:schemas:core:1.0:x509Certificates	-1234
176	3	urn:scim:schemas:core:1.0:ims	-1234
177	5	profile	-1234
178	3	urn:scim:schemas:core:1.0:meta.created	-1234
179	5	phone_number_verified	-1234
180	6	http://eidas.europa.eu/attributes/legalperson/LegalPersonAddress	-1234
181	4	urn:ietf:params:scim:schemas:core:2.0:User:ims.skype	-1234
182	8	http://eidas.europa.eu/attributes/naturalperson/BirthName	-1234
183	10	http://axschema.org/contact/country/home	-1234
184	6	http://eidas.europa.eu/attributes/legalperson/LEI	-1234
185	4	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.work	-1234
186	5	postal_code	-1234
187	4	urn:ietf:params:scim:schemas:core:2.0:User:emails.work	-1234
188	3	urn:scim:schemas:core:1.0:addresses.country	-1234
189	5	groups	-1234
190	3	urn:scim:schemas:core:1.0:emails.other	-1234
191	5	address	-1234
192	3	urn:scim:schemas:core:1.0:phoneNumbers.work	-1234
193	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname	-1234
288	12	http://wso2.org/claims/middleName	1
194	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier	-1234
195	3	urn:scim:schemas:core:1.0:phoneNumbers.mobile	-1234
196	6	http://eidas.europa.eu/attributes/legalperson/D-2012-17-EUIdentifier	-1234
197	4	urn:ietf:params:scim:schemas:core:2.0:User:active	-1234
198	5	country	-1234
199	3	urn:scim:schemas:core:1.0:meta.location	-1234
200	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.$ref	-1234
201	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:verifyEmail	-1234
202	3	urn:scim:schemas:core:1.0:addresses	-1234
203	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:askPassword	-1234
204	5	formatted	-1234
205	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:division	-1234
206	6	http://eidas.europa.eu/attributes/legalperson/SEED	-1234
207	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode	-1234
208	5	email	-1234
209	4	urn:ietf:params:scim:schemas:core:2.0:User:photos.thumbnail	-1234
210	7	nickname	-1234
211	5	upn	-1234
212	4	urn:ietf:params:scim:schemas:core:2.0:User:name.honorificPrefix	-1234
213	4	urn:ietf:params:scim:schemas:core:2.0:User:locale	-1234
214	3	urn:scim:schemas:core:1.0:ims.gtalk	-1234
215	3	urn:scim:schemas:core:1.0:addresses.postalCode	-1234
216	8	http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier	-1234
217	5	given_name	-1234
218	10	http://axschema.org/namePerson/first	-1234
219	5	locality	-1234
220	4	urn:ietf:params:scim:schemas:core:2.0:User:preferredLanguage	-1234
221	10	http://axschema.org/person/gender	-1234
222	10	http://axschema.org/pref/timezone	-1234
223	3	urn:scim:schemas:core:1.0:title	-1234
224	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/homephone	-1234
225	5	region	-1234
226	5	family_name	-1234
227	4	urn:ietf:params:scim:schemas:core:2.0:User:emails.other	-1234
228	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter	-1234
229	4	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.other	-1234
230	3	urn:scim:schemas:core:1.0:addresses.streetAddress	-1234
231	3	urn:scim:schemas:core:1.0:id	-1234
232	4	urn:ietf:params:scim:schemas:core:2.0:User:photos	-1234
233	4	urn:ietf:params:scim:schemas:core:2.0:User:groups	-1234
234	4	urn:ietf:params:scim:schemas:core:2.0:User:addresses.home	-1234
235	3	urn:scim:schemas:core:1.0:name.honorificPrefix	-1234
236	3	urn:scim:schemas:core:1.0:addresses.locality	-1234
237	3	urn:scim:schemas:core:1.0:name.middleName	-1234
238	3	urn:scim:schemas:core:1.0:entitlements	-1234
239	3	urn:scim:schemas:core:1.0:displayName	-1234
240	9	urn:ietf:params:scim:schemas:core:2.0:meta.created	-1234
241	5	name	-1234
242	5	locale	-1234
243	6	http://eidas.europa.eu/attributes/legalperson/VATRegistrationNumber	-1234
244	4	urn:ietf:params:scim:schemas:core:2.0:User:ims.gtalk	-1234
245	10	http://axschema.org/contact/email	-1234
246	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/otherphone	-1234
247	8	http://eidas.europa.eu/attributes/naturalperson/DateOfBirth	-1234
248	4	urn:ietf:params:scim:schemas:core:2.0:User:userType	-1234
249	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.displayName	-1234
250	5	street_address	-1234
251	4	urn:ietf:params:scim:schemas:core:2.0:User:nickName	-1234
252	5	website	-1234
253	4	urn:ietf:params:scim:schemas:core:2.0:User:roles.default	-1234
254	3	urn:scim:schemas:core:1.0:emails	-1234
255	3	urn:scim:schemas:core:1.0:emails.work	-1234
256	5	phone_number	-1234
257	8	http://eidas.europa.eu/attributes/naturalperson/Gender	-1234
258	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:pendingEmails.value	-1234
259	9	urn:ietf:params:scim:schemas:core:2.0:id	-1234
260	3	urn:scim:schemas:core:1.0:nickName	-1234
261	11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department	-1234
262	4	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.home	-1234
263	5	birthdate	-1234
264	8	http://eidas.europa.eu/attributes/naturalperson/CurrentAddress	-1234
265	6	http://eidas.europa.eu/attributes/legalperson/SIC	-1234
266	4	urn:ietf:params:scim:schemas:core:2.0:User:name.givenName	-1234
267	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress	-1234
268	4	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.mobile	-1234
269	2	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country	-1234
270	10	http://axschema.org/birthDate	-1234
271	5	updated_at	-1234
272	4	urn:ietf:params:scim:schemas:core:2.0:User:profileUrl	-1234
273	3	urn:scim:schemas:core:1.0:name.formatted	-1234
274	3	urn:scim:schemas:core:1.0:phoneNumbers.fax	-1234
275	12	http://wso2.org/claims/local	1
276	12	http://wso2.org/claims/locality	1
277	12	http://wso2.org/claims/country	1
278	12	http://wso2.org/claims/displayName	1
279	12	http://wso2.org/claims/identity/failedLoginLockoutCount	1
280	12	http://wso2.org/claims/mobile	1
281	12	http://wso2.org/claims/nickname	1
282	12	http://wso2.org/claims/identity/lockedReason	1
283	12	http://wso2.org/claims/fullname	1
284	12	http://wso2.org/claims/honorificPrefix	1
285	12	http://wso2.org/claims/skype	1
286	12	http://wso2.org/claims/addresses	1
287	12	http://wso2.org/claims/region	1
289	12	http://wso2.org/claims/identity/lastLogonTime	1
290	12	http://wso2.org/claims/username	1
291	12	http://wso2.org/claims/preferredLanguage	1
292	12	http://wso2.org/claims/extendedExternalId	1
293	12	http://wso2.org/claims/timeZone	1
294	12	http://wso2.org/claims/lastname	1
295	12	http://wso2.org/claims/identity/lastLoginTime	1
296	12	http://wso2.org/claims/identity/preferredChannel	1
297	12	http://wso2.org/claims/costCenter	1
298	12	http://wso2.org/claims/location	1
299	12	http://wso2.org/claims/challengeQuestionUris	1
300	12	http://wso2.org/claims/identity/emailotp_disabled	1
301	12	http://wso2.org/claims/identity/accountLocked	1
302	12	http://wso2.org/claims/emails.other	1
303	12	http://wso2.org/claims/department	1
304	12	http://wso2.org/claims/photourl	1
305	12	http://wso2.org/claims/im	1
306	12	http://wso2.org/claims/postalcode	1
307	12	http://wso2.org/claims/emailaddress	1
308	12	http://wso2.org/claims/organization	1
309	12	http://wso2.org/claims/emails.work	1
310	12	http://wso2.org/claims/url	1
311	12	http://wso2.org/claims/addresses.locality	1
312	12	http://wso2.org/claims/identity/unlockTime	1
313	12	http://wso2.org/claims/phoneNumbers.work	1
314	12	http://wso2.org/claims/honorificSuffix	1
315	12	http://wso2.org/claims/identity/phoneVerified	1
316	12	http://wso2.org/claims/primaryChallengeQuestion	1
317	12	http://wso2.org/claims/addresses.formatted	1
318	12	http://wso2.org/claims/identity/secretkey	1
319	12	http://wso2.org/claims/identity/verifyEmail	1
320	12	http://wso2.org/claims/externalid	1
321	12	http://wso2.org/claims/challengeQuestion1	1
322	12	http://wso2.org/claims/phoneNumbers.fax	1
323	12	http://wso2.org/claims/modified	1
324	12	http://wso2.org/claims/formattedName	1
325	12	http://wso2.org/claims/identity/smsotp_disabled	1
326	12	http://wso2.org/claims/identity/askPassword	1
327	12	http://wso2.org/claims/challengeQuestion2	1
328	12	http://wso2.org/claims/phoneNumbers.home	1
329	12	http://wso2.org/claims/identity/failedPasswordRecoveryAttempts	1
330	12	http://wso2.org/claims/userid	1
331	12	http://wso2.org/claims/streetaddress	1
332	12	http://wso2.org/claims/telephone	1
333	12	http://wso2.org/claims/identity/emailaddress.pendingValue	1
334	12	http://wso2.org/claims/identity/accountDisabled	1
335	12	http://wso2.org/claims/givenname	1
336	12	http://wso2.org/claims/extendedDisplayName	1
337	12	http://wso2.org/claims/identity/failedLoginAttempts	1
338	12	http://wso2.org/claims/gtalk	1
339	12	http://wso2.org/claims/groups	1
340	12	http://wso2.org/claims/x509Certificates	1
341	12	http://wso2.org/claims/photos	1
342	12	http://wso2.org/claims/entitlements	1
343	12	http://wso2.org/claims/identity/lastPasswordUpdateTime	1
344	12	http://wso2.org/claims/identity/failedLoginAttemptsBeforeSuccess	1
345	12	http://wso2.org/claims/phoneNumbers	1
346	12	http://wso2.org/claims/dob	1
347	12	http://wso2.org/claims/phoneNumbers.other	1
348	12	http://wso2.org/claims/userprincipal	1
349	12	http://wso2.org/claims/emails.home	1
350	12	http://wso2.org/claims/stateorprovince	1
351	12	http://wso2.org/claims/oneTimePassword	1
352	12	http://wso2.org/claims/active	1
353	12	http://wso2.org/claims/resourceType	1
354	12	http://wso2.org/claims/identity/emailVerified	1
355	12	http://wso2.org/claims/userType	1
356	12	http://wso2.org/claims/otherphone	1
357	12	http://wso2.org/claims/thumbnail	1
358	12	http://wso2.org/claims/identity/adminForcedPasswordReset	1
359	12	http://wso2.org/claims/title	1
360	12	http://wso2.org/claims/extendedRef	1
361	12	http://wso2.org/claims/created	1
362	12	http://wso2.org/claims/gender	1
363	12	http://wso2.org/claims/role	1
364	12	http://wso2.org/claims/identity/accountState	1
365	12	http://wso2.org/claims/phoneNumbers.pager	1
366	16	gender	1
367	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress	1
368	14	urn:scim:schemas:core:1.0:timeZone	1
369	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value	1
370	14	urn:scim:schemas:core:1.0:photos.photo	1
371	18	dob	1
372	14	urn:scim:schemas:core:1.0:locale	1
373	17	http://eidas.europa.eu/attributes/legalperson/LegalName	1
374	14	urn:scim:schemas:core:1.0:name.givenName	1
375	19	http://eidas.europa.eu/attributes/naturalperson/PlaceOfBirth	1
376	14	urn:scim:schemas:core:1.0:phoneNumbers	1
377	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:organization	1
378	18	postcode	1
379	17	http://eidas.europa.eu/attributes/legalperson/EORI	1
380	15	urn:ietf:params:scim:schemas:core:2.0:User:emails	1
381	15	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers	1
382	15	urn:ietf:params:scim:schemas:core:2.0:User:name.familyName	1
383	15	urn:ietf:params:scim:schemas:core:2.0:User:photos.photo	1
384	14	urn:scim:schemas:core:1.0:photos.thumbnail	1
385	18	country	1
386	16	email_verified	1
387	15	urn:ietf:params:scim:schemas:core:2.0:User:title	1
388	14	urn:scim:schemas:core:1.0:userType	1
389	15	urn:ietf:params:scim:schemas:core:2.0:User:emails.home	1
390	14	urn:scim:schemas:core:1.0:active	1
391	14	urn:scim:schemas:core:1.0:phoneNumbers.pager	1
392	16	zoneinfo	1
393	14	urn:scim:schemas:core:1.0:name.honorificSuffix	1
394	14	urn:scim:schemas:core:1.0:photos	1
395	15	urn:ietf:params:scim:schemas:core:2.0:User:displayName	1
396	16	picture	1
397	14	urn:scim:schemas:core:1.0:meta.lastModified	1
398	15	urn:ietf:params:scim:schemas:core:2.0:User:x509Certificates.default	1
399	15	urn:ietf:params:scim:schemas:core:2.0:User:name.formatted	1
400	14	urn:scim:schemas:core:1.0:groups	1
401	14	urn:scim:schemas:core:1.0:phoneNumbers.home	1
402	14	urn:scim:schemas:core:1.0:externalId	1
403	14	urn:scim:schemas:core:1.0:roles	1
404	15	urn:ietf:params:scim:schemas:core:2.0:User:entitlements.default	1
405	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality	1
406	18	timezone	1
407	15	urn:ietf:params:scim:schemas:core:2.0:User:addresses	1
408	19	http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName	1
409	16	preferred_username	1
410	16	middle_name	1
411	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber	1
412	20	urn:ietf:params:scim:schemas:core:2.0:externalId	1
413	21	http://axschema.org/pref/language	1
414	14	urn:scim:schemas:core:1.0:addresses.formatted	1
415	15	urn:ietf:params:scim:schemas:core:2.0:User:userName	1
416	15	urn:ietf:params:scim:schemas:core:2.0:User:timezone	1
417	15	urn:ietf:params:scim:schemas:core:2.0:User:name.honorificSuffix	1
418	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/stateorprovince	1
419	17	http://eidas.europa.eu/attributes/legalperson/LegalPersonIdentifier	1
420	18	gender	1
421	18	language	1
422	16	sub	1
423	14	urn:scim:schemas:core:1.0:userName	1
424	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nickname	1
425	16	nickname	1
426	17	http://eidas.europa.eu/attributes/legalperson/TaxReference	1
427	21	http://axschema.org/contact/postalCode/home	1
428	20	urn:ietf:params:scim:schemas:core:2.0:meta.resourceType	1
429	14	urn:scim:schemas:core:1.0:ims.skype	1
430	14	urn:scim:schemas:core:1.0:preferredLanguage	1
431	14	urn:scim:schemas:core:1.0:phoneNumbers.other	1
432	19	http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName	1
433	15	urn:ietf:params:scim:schemas:core:2.0:User:name.middleName	1
434	15	urn:ietf:params:scim:schemas:core:2.0:User:addresses.work	1
435	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobile	1
436	18	fullname	1
437	21	http://axschema.org/namePerson/last	1
438	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname	1
439	18	email	1
440	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/gender	1
441	14	urn:scim:schemas:core:1.0:addresses.region	1
442	20	urn:ietf:params:scim:schemas:core:2.0:meta.location	1
443	14	urn:scim:schemas:core:1.0:emails.home	1
444	20	urn:ietf:params:scim:schemas:core:2.0:meta.lastModified	1
445	14	urn:scim:schemas:core:1.0:profileUrl	1
446	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth	1
447	14	urn:scim:schemas:core:1.0:name.familyName	1
448	20	urn:ietf:params:scim:schemas:core:2.0:meta.version	1
449	14	urn:scim:schemas:core:1.0:x509Certificates	1
450	14	urn:scim:schemas:core:1.0:ims	1
451	16	profile	1
452	14	urn:scim:schemas:core:1.0:meta.created	1
453	16	phone_number_verified	1
454	17	http://eidas.europa.eu/attributes/legalperson/LegalPersonAddress	1
455	15	urn:ietf:params:scim:schemas:core:2.0:User:ims.skype	1
456	19	http://eidas.europa.eu/attributes/naturalperson/BirthName	1
457	21	http://axschema.org/contact/country/home	1
458	17	http://eidas.europa.eu/attributes/legalperson/LEI	1
459	15	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.work	1
460	16	postal_code	1
461	15	urn:ietf:params:scim:schemas:core:2.0:User:emails.work	1
462	14	urn:scim:schemas:core:1.0:addresses.country	1
463	16	groups	1
464	14	urn:scim:schemas:core:1.0:emails.other	1
465	16	address	1
466	14	urn:scim:schemas:core:1.0:phoneNumbers.work	1
467	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname	1
468	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier	1
469	14	urn:scim:schemas:core:1.0:phoneNumbers.mobile	1
470	17	http://eidas.europa.eu/attributes/legalperson/D-2012-17-EUIdentifier	1
471	15	urn:ietf:params:scim:schemas:core:2.0:User:active	1
472	16	country	1
473	14	urn:scim:schemas:core:1.0:meta.location	1
474	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.$ref	1
475	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:verifyEmail	1
476	14	urn:scim:schemas:core:1.0:addresses	1
477	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:askPassword	1
478	16	formatted	1
479	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:division	1
480	17	http://eidas.europa.eu/attributes/legalperson/SEED	1
481	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode	1
482	16	email	1
483	15	urn:ietf:params:scim:schemas:core:2.0:User:photos.thumbnail	1
484	18	nickname	1
485	16	upn	1
486	15	urn:ietf:params:scim:schemas:core:2.0:User:name.honorificPrefix	1
487	15	urn:ietf:params:scim:schemas:core:2.0:User:locale	1
488	14	urn:scim:schemas:core:1.0:ims.gtalk	1
489	14	urn:scim:schemas:core:1.0:addresses.postalCode	1
490	19	http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier	1
491	16	given_name	1
492	21	http://axschema.org/namePerson/first	1
493	16	locality	1
494	15	urn:ietf:params:scim:schemas:core:2.0:User:preferredLanguage	1
495	21	http://axschema.org/person/gender	1
496	21	http://axschema.org/pref/timezone	1
497	14	urn:scim:schemas:core:1.0:title	1
498	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/homephone	1
499	16	region	1
500	16	family_name	1
501	15	urn:ietf:params:scim:schemas:core:2.0:User:emails.other	1
502	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter	1
503	15	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.other	1
504	14	urn:scim:schemas:core:1.0:addresses.streetAddress	1
505	14	urn:scim:schemas:core:1.0:id	1
506	15	urn:ietf:params:scim:schemas:core:2.0:User:photos	1
507	15	urn:ietf:params:scim:schemas:core:2.0:User:groups	1
508	15	urn:ietf:params:scim:schemas:core:2.0:User:addresses.home	1
509	14	urn:scim:schemas:core:1.0:name.honorificPrefix	1
510	14	urn:scim:schemas:core:1.0:addresses.locality	1
511	14	urn:scim:schemas:core:1.0:name.middleName	1
512	14	urn:scim:schemas:core:1.0:entitlements	1
513	14	urn:scim:schemas:core:1.0:displayName	1
514	20	urn:ietf:params:scim:schemas:core:2.0:meta.created	1
515	16	name	1
516	16	locale	1
517	17	http://eidas.europa.eu/attributes/legalperson/VATRegistrationNumber	1
518	15	urn:ietf:params:scim:schemas:core:2.0:User:ims.gtalk	1
519	21	http://axschema.org/contact/email	1
520	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/otherphone	1
521	19	http://eidas.europa.eu/attributes/naturalperson/DateOfBirth	1
522	15	urn:ietf:params:scim:schemas:core:2.0:User:userType	1
523	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.displayName	1
524	16	street_address	1
525	15	urn:ietf:params:scim:schemas:core:2.0:User:nickName	1
526	16	website	1
527	15	urn:ietf:params:scim:schemas:core:2.0:User:roles.default	1
528	14	urn:scim:schemas:core:1.0:emails	1
529	14	urn:scim:schemas:core:1.0:emails.work	1
530	16	phone_number	1
531	19	http://eidas.europa.eu/attributes/naturalperson/Gender	1
532	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:pendingEmails.value	1
533	20	urn:ietf:params:scim:schemas:core:2.0:id	1
534	14	urn:scim:schemas:core:1.0:nickName	1
535	22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department	1
536	15	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.home	1
537	16	birthdate	1
538	19	http://eidas.europa.eu/attributes/naturalperson/CurrentAddress	1
539	17	http://eidas.europa.eu/attributes/legalperson/SIC	1
540	15	urn:ietf:params:scim:schemas:core:2.0:User:name.givenName	1
541	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress	1
542	15	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.mobile	1
543	13	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country	1
544	21	http://axschema.org/birthDate	1
545	16	updated_at	1
546	15	urn:ietf:params:scim:schemas:core:2.0:User:profileUrl	1
547	14	urn:scim:schemas:core:1.0:name.formatted	1
548	14	urn:scim:schemas:core:1.0:phoneNumbers.fax	1
549	23	http://wso2.org/claims/local	2
550	23	http://wso2.org/claims/locality	2
551	23	http://wso2.org/claims/country	2
552	23	http://wso2.org/claims/displayName	2
553	23	http://wso2.org/claims/identity/failedLoginLockoutCount	2
554	23	http://wso2.org/claims/mobile	2
555	23	http://wso2.org/claims/nickname	2
556	23	http://wso2.org/claims/identity/lockedReason	2
557	23	http://wso2.org/claims/fullname	2
558	23	http://wso2.org/claims/honorificPrefix	2
559	23	http://wso2.org/claims/skype	2
560	23	http://wso2.org/claims/addresses	2
561	23	http://wso2.org/claims/region	2
562	23	http://wso2.org/claims/middleName	2
563	23	http://wso2.org/claims/identity/lastLogonTime	2
564	23	http://wso2.org/claims/username	2
565	23	http://wso2.org/claims/preferredLanguage	2
566	23	http://wso2.org/claims/extendedExternalId	2
567	23	http://wso2.org/claims/timeZone	2
568	23	http://wso2.org/claims/lastname	2
569	23	http://wso2.org/claims/identity/lastLoginTime	2
570	23	http://wso2.org/claims/identity/preferredChannel	2
571	23	http://wso2.org/claims/costCenter	2
572	23	http://wso2.org/claims/location	2
573	23	http://wso2.org/claims/challengeQuestionUris	2
574	23	http://wso2.org/claims/identity/emailotp_disabled	2
575	23	http://wso2.org/claims/identity/accountLocked	2
576	23	http://wso2.org/claims/emails.other	2
577	23	http://wso2.org/claims/department	2
578	23	http://wso2.org/claims/photourl	2
579	23	http://wso2.org/claims/im	2
580	23	http://wso2.org/claims/postalcode	2
581	23	http://wso2.org/claims/emailaddress	2
582	23	http://wso2.org/claims/organization	2
583	23	http://wso2.org/claims/emails.work	2
584	23	http://wso2.org/claims/url	2
585	23	http://wso2.org/claims/addresses.locality	2
586	23	http://wso2.org/claims/identity/unlockTime	2
587	23	http://wso2.org/claims/phoneNumbers.work	2
588	23	http://wso2.org/claims/honorificSuffix	2
589	23	http://wso2.org/claims/identity/phoneVerified	2
590	23	http://wso2.org/claims/primaryChallengeQuestion	2
591	23	http://wso2.org/claims/addresses.formatted	2
592	23	http://wso2.org/claims/identity/secretkey	2
593	23	http://wso2.org/claims/identity/verifyEmail	2
594	23	http://wso2.org/claims/externalid	2
595	23	http://wso2.org/claims/challengeQuestion1	2
596	23	http://wso2.org/claims/phoneNumbers.fax	2
597	23	http://wso2.org/claims/modified	2
598	23	http://wso2.org/claims/formattedName	2
599	23	http://wso2.org/claims/identity/smsotp_disabled	2
600	23	http://wso2.org/claims/identity/askPassword	2
601	23	http://wso2.org/claims/challengeQuestion2	2
602	23	http://wso2.org/claims/phoneNumbers.home	2
603	23	http://wso2.org/claims/identity/failedPasswordRecoveryAttempts	2
604	23	http://wso2.org/claims/userid	2
605	23	http://wso2.org/claims/streetaddress	2
606	23	http://wso2.org/claims/telephone	2
607	23	http://wso2.org/claims/identity/emailaddress.pendingValue	2
608	23	http://wso2.org/claims/identity/accountDisabled	2
609	23	http://wso2.org/claims/givenname	2
610	23	http://wso2.org/claims/extendedDisplayName	2
611	23	http://wso2.org/claims/identity/failedLoginAttempts	2
612	23	http://wso2.org/claims/gtalk	2
613	23	http://wso2.org/claims/groups	2
614	23	http://wso2.org/claims/x509Certificates	2
615	23	http://wso2.org/claims/photos	2
616	23	http://wso2.org/claims/entitlements	2
617	23	http://wso2.org/claims/identity/lastPasswordUpdateTime	2
618	23	http://wso2.org/claims/identity/failedLoginAttemptsBeforeSuccess	2
619	23	http://wso2.org/claims/phoneNumbers	2
620	23	http://wso2.org/claims/dob	2
621	23	http://wso2.org/claims/phoneNumbers.other	2
622	23	http://wso2.org/claims/userprincipal	2
623	23	http://wso2.org/claims/emails.home	2
624	23	http://wso2.org/claims/stateorprovince	2
625	23	http://wso2.org/claims/oneTimePassword	2
626	23	http://wso2.org/claims/active	2
627	23	http://wso2.org/claims/resourceType	2
628	23	http://wso2.org/claims/identity/emailVerified	2
629	23	http://wso2.org/claims/userType	2
630	23	http://wso2.org/claims/otherphone	2
631	23	http://wso2.org/claims/thumbnail	2
632	23	http://wso2.org/claims/identity/adminForcedPasswordReset	2
633	23	http://wso2.org/claims/title	2
634	23	http://wso2.org/claims/extendedRef	2
635	23	http://wso2.org/claims/created	2
636	23	http://wso2.org/claims/gender	2
637	23	http://wso2.org/claims/role	2
638	23	http://wso2.org/claims/identity/accountState	2
639	23	http://wso2.org/claims/phoneNumbers.pager	2
640	27	gender	2
641	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/streetaddress	2
642	25	urn:scim:schemas:core:1.0:timeZone	2
643	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.value	2
644	25	urn:scim:schemas:core:1.0:photos.photo	2
645	29	dob	2
646	25	urn:scim:schemas:core:1.0:locale	2
647	28	http://eidas.europa.eu/attributes/legalperson/LegalName	2
648	25	urn:scim:schemas:core:1.0:name.givenName	2
649	30	http://eidas.europa.eu/attributes/naturalperson/PlaceOfBirth	2
650	25	urn:scim:schemas:core:1.0:phoneNumbers	2
651	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:organization	2
652	29	postcode	2
653	28	http://eidas.europa.eu/attributes/legalperson/EORI	2
654	26	urn:ietf:params:scim:schemas:core:2.0:User:emails	2
655	26	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers	2
656	26	urn:ietf:params:scim:schemas:core:2.0:User:name.familyName	2
657	26	urn:ietf:params:scim:schemas:core:2.0:User:photos.photo	2
658	25	urn:scim:schemas:core:1.0:photos.thumbnail	2
659	29	country	2
660	27	email_verified	2
661	26	urn:ietf:params:scim:schemas:core:2.0:User:title	2
662	25	urn:scim:schemas:core:1.0:userType	2
663	26	urn:ietf:params:scim:schemas:core:2.0:User:emails.home	2
664	25	urn:scim:schemas:core:1.0:active	2
665	25	urn:scim:schemas:core:1.0:phoneNumbers.pager	2
666	27	zoneinfo	2
667	25	urn:scim:schemas:core:1.0:name.honorificSuffix	2
668	25	urn:scim:schemas:core:1.0:photos	2
669	26	urn:ietf:params:scim:schemas:core:2.0:User:displayName	2
670	27	picture	2
671	25	urn:scim:schemas:core:1.0:meta.lastModified	2
672	26	urn:ietf:params:scim:schemas:core:2.0:User:x509Certificates.default	2
673	26	urn:ietf:params:scim:schemas:core:2.0:User:name.formatted	2
674	25	urn:scim:schemas:core:1.0:groups	2
675	25	urn:scim:schemas:core:1.0:phoneNumbers.home	2
676	25	urn:scim:schemas:core:1.0:externalId	2
677	25	urn:scim:schemas:core:1.0:roles	2
678	26	urn:ietf:params:scim:schemas:core:2.0:User:entitlements.default	2
679	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/locality	2
680	29	timezone	2
681	26	urn:ietf:params:scim:schemas:core:2.0:User:addresses	2
682	30	http://eidas.europa.eu/attributes/naturalperson/CurrentFamilyName	2
683	27	preferred_username	2
684	27	middle_name	2
685	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:employeeNumber	2
686	31	urn:ietf:params:scim:schemas:core:2.0:externalId	2
687	32	http://axschema.org/pref/language	2
688	25	urn:scim:schemas:core:1.0:addresses.formatted	2
689	26	urn:ietf:params:scim:schemas:core:2.0:User:userName	2
690	26	urn:ietf:params:scim:schemas:core:2.0:User:timezone	2
691	26	urn:ietf:params:scim:schemas:core:2.0:User:name.honorificSuffix	2
692	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/stateorprovince	2
693	28	http://eidas.europa.eu/attributes/legalperson/LegalPersonIdentifier	2
694	29	gender	2
695	29	language	2
696	27	sub	2
697	25	urn:scim:schemas:core:1.0:userName	2
698	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nickname	2
699	27	nickname	2
700	28	http://eidas.europa.eu/attributes/legalperson/TaxReference	2
701	32	http://axschema.org/contact/postalCode/home	2
702	31	urn:ietf:params:scim:schemas:core:2.0:meta.resourceType	2
703	25	urn:scim:schemas:core:1.0:ims.skype	2
704	25	urn:scim:schemas:core:1.0:preferredLanguage	2
705	25	urn:scim:schemas:core:1.0:phoneNumbers.other	2
706	30	http://eidas.europa.eu/attributes/naturalperson/CurrentGivenName	2
707	26	urn:ietf:params:scim:schemas:core:2.0:User:name.middleName	2
708	26	urn:ietf:params:scim:schemas:core:2.0:User:addresses.work	2
709	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/mobile	2
710	29	fullname	2
711	32	http://axschema.org/namePerson/last	2
712	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname	2
713	29	email	2
714	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/gender	2
715	25	urn:scim:schemas:core:1.0:addresses.region	2
716	31	urn:ietf:params:scim:schemas:core:2.0:meta.location	2
717	25	urn:scim:schemas:core:1.0:emails.home	2
718	31	urn:ietf:params:scim:schemas:core:2.0:meta.lastModified	2
719	25	urn:scim:schemas:core:1.0:profileUrl	2
720	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/dateofbirth	2
721	25	urn:scim:schemas:core:1.0:name.familyName	2
722	31	urn:ietf:params:scim:schemas:core:2.0:meta.version	2
723	25	urn:scim:schemas:core:1.0:x509Certificates	2
724	25	urn:scim:schemas:core:1.0:ims	2
725	27	profile	2
726	25	urn:scim:schemas:core:1.0:meta.created	2
727	27	phone_number_verified	2
728	28	http://eidas.europa.eu/attributes/legalperson/LegalPersonAddress	2
729	26	urn:ietf:params:scim:schemas:core:2.0:User:ims.skype	2
730	30	http://eidas.europa.eu/attributes/naturalperson/BirthName	2
731	32	http://axschema.org/contact/country/home	2
732	28	http://eidas.europa.eu/attributes/legalperson/LEI	2
733	26	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.work	2
734	27	postal_code	2
735	26	urn:ietf:params:scim:schemas:core:2.0:User:emails.work	2
736	25	urn:scim:schemas:core:1.0:addresses.country	2
737	27	groups	2
738	25	urn:scim:schemas:core:1.0:emails.other	2
739	27	address	2
740	25	urn:scim:schemas:core:1.0:phoneNumbers.work	2
741	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname	2
742	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/privatepersonalidentifier	2
743	25	urn:scim:schemas:core:1.0:phoneNumbers.mobile	2
744	28	http://eidas.europa.eu/attributes/legalperson/D-2012-17-EUIdentifier	2
745	26	urn:ietf:params:scim:schemas:core:2.0:User:active	2
746	27	country	2
747	25	urn:scim:schemas:core:1.0:meta.location	2
748	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.$ref	2
749	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:verifyEmail	2
750	25	urn:scim:schemas:core:1.0:addresses	2
751	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:askPassword	2
752	27	formatted	2
753	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:division	2
754	28	http://eidas.europa.eu/attributes/legalperson/SEED	2
755	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/postalcode	2
756	27	email	2
757	26	urn:ietf:params:scim:schemas:core:2.0:User:photos.thumbnail	2
758	29	nickname	2
759	27	upn	2
760	26	urn:ietf:params:scim:schemas:core:2.0:User:name.honorificPrefix	2
761	26	urn:ietf:params:scim:schemas:core:2.0:User:locale	2
762	25	urn:scim:schemas:core:1.0:ims.gtalk	2
763	25	urn:scim:schemas:core:1.0:addresses.postalCode	2
764	30	http://eidas.europa.eu/attributes/naturalperson/PersonIdentifier	2
765	27	given_name	2
766	32	http://axschema.org/namePerson/first	2
767	27	locality	2
768	26	urn:ietf:params:scim:schemas:core:2.0:User:preferredLanguage	2
769	32	http://axschema.org/person/gender	2
770	32	http://axschema.org/pref/timezone	2
771	25	urn:scim:schemas:core:1.0:title	2
772	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/homephone	2
773	27	region	2
774	27	family_name	2
775	26	urn:ietf:params:scim:schemas:core:2.0:User:emails.other	2
776	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:costCenter	2
777	26	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.other	2
778	25	urn:scim:schemas:core:1.0:addresses.streetAddress	2
779	25	urn:scim:schemas:core:1.0:id	2
780	26	urn:ietf:params:scim:schemas:core:2.0:User:photos	2
781	26	urn:ietf:params:scim:schemas:core:2.0:User:groups	2
782	26	urn:ietf:params:scim:schemas:core:2.0:User:addresses.home	2
783	25	urn:scim:schemas:core:1.0:name.honorificPrefix	2
784	25	urn:scim:schemas:core:1.0:addresses.locality	2
785	25	urn:scim:schemas:core:1.0:name.middleName	2
786	25	urn:scim:schemas:core:1.0:entitlements	2
787	25	urn:scim:schemas:core:1.0:displayName	2
788	31	urn:ietf:params:scim:schemas:core:2.0:meta.created	2
789	27	name	2
790	27	locale	2
791	28	http://eidas.europa.eu/attributes/legalperson/VATRegistrationNumber	2
792	26	urn:ietf:params:scim:schemas:core:2.0:User:ims.gtalk	2
793	32	http://axschema.org/contact/email	2
794	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/otherphone	2
795	30	http://eidas.europa.eu/attributes/naturalperson/DateOfBirth	2
796	26	urn:ietf:params:scim:schemas:core:2.0:User:userType	2
797	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:manager.displayName	2
798	27	street_address	2
799	26	urn:ietf:params:scim:schemas:core:2.0:User:nickName	2
800	27	website	2
801	26	urn:ietf:params:scim:schemas:core:2.0:User:roles.default	2
802	25	urn:scim:schemas:core:1.0:emails	2
803	25	urn:scim:schemas:core:1.0:emails.work	2
804	27	phone_number	2
805	30	http://eidas.europa.eu/attributes/naturalperson/Gender	2
806	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:pendingEmails.value	2
807	31	urn:ietf:params:scim:schemas:core:2.0:id	2
808	25	urn:scim:schemas:core:1.0:nickName	2
809	33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User:department	2
810	26	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.home	2
811	27	birthdate	2
812	30	http://eidas.europa.eu/attributes/naturalperson/CurrentAddress	2
813	28	http://eidas.europa.eu/attributes/legalperson/SIC	2
814	26	urn:ietf:params:scim:schemas:core:2.0:User:name.givenName	2
815	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress	2
816	26	urn:ietf:params:scim:schemas:core:2.0:User:phoneNumbers.mobile	2
817	24	http://schemas.xmlsoap.org/ws/2005/05/identity/claims/country	2
818	32	http://axschema.org/birthDate	2
819	27	updated_at	2
820	26	urn:ietf:params:scim:schemas:core:2.0:User:profileUrl	2
821	25	urn:scim:schemas:core:1.0:name.formatted	2
822	25	urn:scim:schemas:core:1.0:phoneNumbers.fax	2
\.


--
-- Data for Name: idn_claim_dialect; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_claim_dialect (id, dialect_uri, tenant_id) FROM stdin;
1	http://wso2.org/claims	-1234
2	http://schemas.xmlsoap.org/ws/2005/05/identity	-1234
3	urn:scim:schemas:core:1.0	-1234
4	urn:ietf:params:scim:schemas:core:2.0:User	-1234
5	http://wso2.org/oidc/claim	-1234
6	http://eidas.europa.eu/attributes/legalperson	-1234
7	http://schema.openid.net/2007/05/claims	-1234
8	http://eidas.europa.eu/attributes/naturalperson	-1234
9	urn:ietf:params:scim:schemas:core:2.0	-1234
10	http://axschema.org	-1234
11	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User	-1234
12	http://wso2.org/claims	1
13	http://schemas.xmlsoap.org/ws/2005/05/identity	1
14	urn:scim:schemas:core:1.0	1
15	urn:ietf:params:scim:schemas:core:2.0:User	1
16	http://wso2.org/oidc/claim	1
17	http://eidas.europa.eu/attributes/legalperson	1
18	http://schema.openid.net/2007/05/claims	1
19	http://eidas.europa.eu/attributes/naturalperson	1
20	urn:ietf:params:scim:schemas:core:2.0	1
21	http://axschema.org	1
22	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User	1
23	http://wso2.org/claims	2
24	http://schemas.xmlsoap.org/ws/2005/05/identity	2
25	urn:scim:schemas:core:1.0	2
26	urn:ietf:params:scim:schemas:core:2.0:User	2
27	http://wso2.org/oidc/claim	2
28	http://eidas.europa.eu/attributes/legalperson	2
29	http://schema.openid.net/2007/05/claims	2
30	http://eidas.europa.eu/attributes/naturalperson	2
31	urn:ietf:params:scim:schemas:core:2.0	2
32	http://axschema.org	2
33	urn:ietf:params:scim:schemas:extension:enterprise:2.0:User	2
\.


--
-- Data for Name: idn_claim_mapped_attribute; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_claim_mapped_attribute (id, local_claim_id, user_store_domain_name, attribute_name, tenant_id) FROM stdin;
1	1	PRIMARY	local	-1234
2	2	PRIMARY	local	-1234
3	3	PRIMARY	country	-1234
4	4	PRIMARY	displayName	-1234
5	5	PRIMARY	failedLockoutCount	-1234
6	6	PRIMARY	mobile	-1234
7	7	PRIMARY	nickName	-1234
8	8	PRIMARY	lockedReason	-1234
9	9	PRIMARY	cn	-1234
10	10	PRIMARY	honoricPrefix	-1234
11	11	PRIMARY	imSkype	-1234
12	12	PRIMARY	addresses	-1234
13	13	PRIMARY	region	-1234
14	14	PRIMARY	middleName	-1234
15	15	PRIMARY	lastLogonTime	-1234
16	16	PRIMARY	uid	-1234
17	17	PRIMARY	preferredLanguage	-1234
18	18	PRIMARY	extendedExternalId	-1234
19	19	PRIMARY	timeZone	-1234
20	20	PRIMARY	sn	-1234
21	21	PRIMARY	lastLoginTime	-1234
22	22	PRIMARY	preferredChannel	-1234
23	23	PRIMARY	costCenter	-1234
24	24	PRIMARY	location	-1234
25	25	PRIMARY	challengeQuestionUris	-1234
26	26	PRIMARY	emailOTPDisabled	-1234
27	27	PRIMARY	accountLock	-1234
28	28	PRIMARY	otherEmail	-1234
29	29	PRIMARY	departmentNumber	-1234
30	30	PRIMARY	photoUrl	-1234
31	31	PRIMARY	im	-1234
32	32	PRIMARY	postalcode	-1234
33	33	PRIMARY	mail	-1234
34	34	PRIMARY	organizationName	-1234
35	35	PRIMARY	workEmail	-1234
36	36	PRIMARY	url	-1234
37	37	PRIMARY	localityAddress	-1234
38	38	PRIMARY	unlockTime	-1234
39	39	PRIMARY	workPhone	-1234
40	40	PRIMARY	honoricSuffix	-1234
41	41	PRIMARY	phoneVerified	-1234
42	42	PRIMARY	primaryChallengeQuestion	-1234
43	43	PRIMARY	formattedAddress	-1234
44	44	PRIMARY	totpSecretkey	-1234
45	45	PRIMARY	verifyEmail	-1234
46	46	PRIMARY	externalId	-1234
47	47	PRIMARY	firstChallenge	-1234
48	48	PRIMARY	fax	-1234
49	49	PRIMARY	lastModifiedDate	-1234
50	50	PRIMARY	formattedName	-1234
51	51	PRIMARY	smsOTPDisabled	-1234
52	52	PRIMARY	askPassword	-1234
53	53	PRIMARY	secondChallenge	-1234
54	54	PRIMARY	homePhone	-1234
55	55	PRIMARY	failedRecoveryAttempts	-1234
56	56	PRIMARY	scimId	-1234
57	57	PRIMARY	streetAddress	-1234
58	58	PRIMARY	telephoneNumber	-1234
59	59	PRIMARY	pendingEmailAddress	-1234
60	60	PRIMARY	accountDisabled	-1234
61	61	PRIMARY	givenName	-1234
62	62	PRIMARY	extendedDisplayName	-1234
63	63	PRIMARY	failedLoginAttempts	-1234
64	64	PRIMARY	imGtalk	-1234
65	65	PRIMARY	groups	-1234
66	66	PRIMARY	x509Certificates	-1234
67	67	PRIMARY	photos	-1234
68	68	PRIMARY	entitlements	-1234
69	69	PRIMARY	lastPasswordUpdate	-1234
70	70	PRIMARY	failedLoginAttemptsBeforeSuccess	-1234
71	71	PRIMARY	phoneNumbers	-1234
72	72	PRIMARY	dateOfBirth	-1234
73	73	PRIMARY	otherPhoneNumber	-1234
74	74	PRIMARY	uid	-1234
75	75	PRIMARY	homeEmail	-1234
76	76	PRIMARY	stateOrProvinceName	-1234
77	77	PRIMARY	oneTimePassword	-1234
78	78	PRIMARY	active	-1234
79	79	PRIMARY	resourceType	-1234
80	80	PRIMARY	emailVerified	-1234
81	81	PRIMARY	userType	-1234
82	82	PRIMARY	otherPhone	-1234
83	83	PRIMARY	thumbnail	-1234
84	84	PRIMARY	forcePasswordReset	-1234
85	85	PRIMARY	title	-1234
86	86	PRIMARY	extendedRef	-1234
87	87	PRIMARY	createdDate	-1234
88	88	PRIMARY	gender	-1234
89	89	PRIMARY	role	-1234
90	90	PRIMARY	accountState	-1234
91	91	PRIMARY	pager	-1234
92	275	PRIMARY	local	1
93	276	PRIMARY	local	1
94	277	PRIMARY	country	1
95	278	PRIMARY	displayName	1
96	279	PRIMARY	failedLockoutCount	1
97	280	PRIMARY	mobile	1
98	281	PRIMARY	nickName	1
99	282	PRIMARY	lockedReason	1
100	283	PRIMARY	cn	1
101	284	PRIMARY	honoricPrefix	1
102	285	PRIMARY	imSkype	1
103	286	PRIMARY	addresses	1
104	287	PRIMARY	region	1
105	288	PRIMARY	middleName	1
106	289	PRIMARY	lastLogonTime	1
107	290	PRIMARY	uid	1
108	291	PRIMARY	preferredLanguage	1
109	292	PRIMARY	extendedExternalId	1
110	293	PRIMARY	timeZone	1
111	294	PRIMARY	sn	1
112	295	PRIMARY	lastLoginTime	1
113	296	PRIMARY	preferredChannel	1
114	297	PRIMARY	costCenter	1
115	298	PRIMARY	location	1
116	299	PRIMARY	challengeQuestionUris	1
117	300	PRIMARY	emailOTPDisabled	1
118	301	PRIMARY	accountLock	1
119	302	PRIMARY	otherEmail	1
120	303	PRIMARY	departmentNumber	1
121	304	PRIMARY	photoUrl	1
122	305	PRIMARY	im	1
123	306	PRIMARY	postalcode	1
124	307	PRIMARY	mail	1
125	308	PRIMARY	organizationName	1
126	309	PRIMARY	workEmail	1
127	310	PRIMARY	url	1
128	311	PRIMARY	localityAddress	1
129	312	PRIMARY	unlockTime	1
130	313	PRIMARY	workPhone	1
131	314	PRIMARY	honoricSuffix	1
132	315	PRIMARY	phoneVerified	1
133	316	PRIMARY	primaryChallengeQuestion	1
134	317	PRIMARY	formattedAddress	1
135	318	PRIMARY	totpSecretkey	1
136	319	PRIMARY	verifyEmail	1
137	320	PRIMARY	externalId	1
138	321	PRIMARY	firstChallenge	1
139	322	PRIMARY	fax	1
140	323	PRIMARY	lastModifiedDate	1
141	324	PRIMARY	formattedName	1
142	325	PRIMARY	smsOTPDisabled	1
143	326	PRIMARY	askPassword	1
144	327	PRIMARY	secondChallenge	1
145	328	PRIMARY	homePhone	1
146	329	PRIMARY	failedRecoveryAttempts	1
147	330	PRIMARY	scimId	1
148	331	PRIMARY	streetAddress	1
149	332	PRIMARY	telephoneNumber	1
150	333	PRIMARY	pendingEmailAddress	1
151	334	PRIMARY	accountDisabled	1
152	335	PRIMARY	givenName	1
153	336	PRIMARY	extendedDisplayName	1
154	337	PRIMARY	failedLoginAttempts	1
155	338	PRIMARY	imGtalk	1
156	339	PRIMARY	groups	1
157	340	PRIMARY	x509Certificates	1
158	341	PRIMARY	photos	1
159	342	PRIMARY	entitlements	1
160	343	PRIMARY	lastPasswordUpdate	1
161	344	PRIMARY	failedLoginAttemptsBeforeSuccess	1
162	345	PRIMARY	phoneNumbers	1
163	346	PRIMARY	dateOfBirth	1
164	347	PRIMARY	otherPhoneNumber	1
165	348	PRIMARY	uid	1
166	349	PRIMARY	homeEmail	1
167	350	PRIMARY	stateOrProvinceName	1
168	351	PRIMARY	oneTimePassword	1
169	352	PRIMARY	active	1
170	353	PRIMARY	resourceType	1
171	354	PRIMARY	emailVerified	1
172	355	PRIMARY	userType	1
173	356	PRIMARY	otherPhone	1
174	357	PRIMARY	thumbnail	1
175	358	PRIMARY	forcePasswordReset	1
176	359	PRIMARY	title	1
177	360	PRIMARY	extendedRef	1
178	361	PRIMARY	createdDate	1
179	362	PRIMARY	gender	1
180	363	PRIMARY	role	1
181	364	PRIMARY	accountState	1
182	365	PRIMARY	pager	1
183	549	PRIMARY	local	2
184	550	PRIMARY	local	2
185	551	PRIMARY	country	2
186	552	PRIMARY	displayName	2
187	553	PRIMARY	failedLockoutCount	2
188	554	PRIMARY	mobile	2
189	555	PRIMARY	nickName	2
190	556	PRIMARY	lockedReason	2
191	557	PRIMARY	cn	2
192	558	PRIMARY	honoricPrefix	2
193	559	PRIMARY	imSkype	2
194	560	PRIMARY	addresses	2
195	561	PRIMARY	region	2
196	562	PRIMARY	middleName	2
197	563	PRIMARY	lastLogonTime	2
198	564	PRIMARY	uid	2
199	565	PRIMARY	preferredLanguage	2
200	566	PRIMARY	extendedExternalId	2
201	567	PRIMARY	timeZone	2
202	568	PRIMARY	sn	2
203	569	PRIMARY	lastLoginTime	2
204	570	PRIMARY	preferredChannel	2
205	571	PRIMARY	costCenter	2
206	572	PRIMARY	location	2
207	573	PRIMARY	challengeQuestionUris	2
208	574	PRIMARY	emailOTPDisabled	2
209	575	PRIMARY	accountLock	2
210	576	PRIMARY	otherEmail	2
211	577	PRIMARY	departmentNumber	2
212	578	PRIMARY	photoUrl	2
213	579	PRIMARY	im	2
214	580	PRIMARY	postalcode	2
215	581	PRIMARY	mail	2
216	582	PRIMARY	organizationName	2
217	583	PRIMARY	workEmail	2
218	584	PRIMARY	url	2
219	585	PRIMARY	localityAddress	2
220	586	PRIMARY	unlockTime	2
221	587	PRIMARY	workPhone	2
222	588	PRIMARY	honoricSuffix	2
223	589	PRIMARY	phoneVerified	2
224	590	PRIMARY	primaryChallengeQuestion	2
225	591	PRIMARY	formattedAddress	2
226	592	PRIMARY	totpSecretkey	2
227	593	PRIMARY	verifyEmail	2
228	594	PRIMARY	externalId	2
229	595	PRIMARY	firstChallenge	2
230	596	PRIMARY	fax	2
231	597	PRIMARY	lastModifiedDate	2
232	598	PRIMARY	formattedName	2
233	599	PRIMARY	smsOTPDisabled	2
234	600	PRIMARY	askPassword	2
235	601	PRIMARY	secondChallenge	2
236	602	PRIMARY	homePhone	2
237	603	PRIMARY	failedRecoveryAttempts	2
238	604	PRIMARY	scimId	2
239	605	PRIMARY	streetAddress	2
240	606	PRIMARY	telephoneNumber	2
241	607	PRIMARY	pendingEmailAddress	2
242	608	PRIMARY	accountDisabled	2
243	609	PRIMARY	givenName	2
244	610	PRIMARY	extendedDisplayName	2
245	611	PRIMARY	failedLoginAttempts	2
246	612	PRIMARY	imGtalk	2
247	613	PRIMARY	groups	2
248	614	PRIMARY	x509Certificates	2
249	615	PRIMARY	photos	2
250	616	PRIMARY	entitlements	2
251	617	PRIMARY	lastPasswordUpdate	2
252	618	PRIMARY	failedLoginAttemptsBeforeSuccess	2
253	619	PRIMARY	phoneNumbers	2
254	620	PRIMARY	dateOfBirth	2
255	621	PRIMARY	otherPhoneNumber	2
256	622	PRIMARY	uid	2
257	623	PRIMARY	homeEmail	2
258	624	PRIMARY	stateOrProvinceName	2
259	625	PRIMARY	oneTimePassword	2
260	626	PRIMARY	active	2
261	627	PRIMARY	resourceType	2
262	628	PRIMARY	emailVerified	2
263	629	PRIMARY	userType	2
264	630	PRIMARY	otherPhone	2
265	631	PRIMARY	thumbnail	2
266	632	PRIMARY	forcePasswordReset	2
267	633	PRIMARY	title	2
268	634	PRIMARY	extendedRef	2
269	635	PRIMARY	createdDate	2
270	636	PRIMARY	gender	2
271	637	PRIMARY	role	2
272	638	PRIMARY	accountState	2
273	639	PRIMARY	pager	2
\.


--
-- Data for Name: idn_claim_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_claim_mapping (id, ext_claim_id, mapped_local_claim_id, tenant_id) FROM stdin;
1	92	88	-1234
2	93	57	-1234
3	94	19	-1234
4	95	88	-1234
5	96	30	-1234
6	97	72	-1234
7	98	1	-1234
8	99	62	-1234
9	100	61	-1234
10	101	3	-1234
11	102	71	-1234
12	103	34	-1234
13	104	32	-1234
14	105	29	-1234
15	106	33	-1234
16	107	71	-1234
17	108	20	-1234
18	109	30	-1234
19	110	83	-1234
20	111	3	-1234
21	112	80	-1234
22	113	85	-1234
23	114	81	-1234
24	115	75	-1234
25	116	78	-1234
26	117	91	-1234
27	118	19	-1234
28	119	40	-1234
29	120	67	-1234
30	121	4	-1234
31	122	30	-1234
32	123	49	-1234
33	124	66	-1234
34	125	50	-1234
35	126	65	-1234
36	127	54	-1234
37	128	46	-1234
38	129	89	-1234
39	130	68	-1234
40	131	2	-1234
41	132	19	-1234
42	133	12	-1234
43	134	20	-1234
44	135	4	-1234
45	136	14	-1234
46	137	18	-1234
47	138	46	-1234
48	139	17	-1234
49	140	43	-1234
50	141	16	-1234
51	142	19	-1234
52	143	40	-1234
53	144	76	-1234
54	145	18	-1234
55	146	88	-1234
56	147	17	-1234
57	148	16	-1234
58	149	16	-1234
59	150	7	-1234
60	151	7	-1234
61	152	32	-1234
62	153	32	-1234
63	154	79	-1234
64	155	11	-1234
65	156	17	-1234
66	157	73	-1234
67	158	61	-1234
68	159	14	-1234
69	160	13	-1234
70	161	6	-1234
71	162	9	-1234
72	163	20	-1234
73	164	61	-1234
74	165	33	-1234
75	166	88	-1234
76	167	13	-1234
77	168	24	-1234
78	169	75	-1234
79	170	49	-1234
80	171	36	-1234
81	172	72	-1234
82	173	20	-1234
83	174	31	-1234
84	175	66	-1234
85	176	31	-1234
86	177	36	-1234
87	178	87	-1234
88	179	41	-1234
89	180	12	-1234
90	181	11	-1234
91	182	16	-1234
92	183	3	-1234
93	184	86	-1234
94	185	39	-1234
95	186	32	-1234
96	187	35	-1234
97	188	3	-1234
98	189	89	-1234
99	190	28	-1234
100	191	12	-1234
101	192	39	-1234
102	193	20	-1234
103	194	31	-1234
104	195	6	-1234
105	196	46	-1234
106	197	78	-1234
107	198	3	-1234
108	199	24	-1234
109	200	86	-1234
110	201	45	-1234
111	202	12	-1234
112	203	52	-1234
113	204	43	-1234
114	205	76	-1234
115	206	7	-1234
116	207	32	-1234
117	208	33	-1234
118	209	83	-1234
119	210	7	-1234
120	211	74	-1234
121	212	10	-1234
122	213	1	-1234
123	214	64	-1234
124	215	32	-1234
125	216	56	-1234
126	217	61	-1234
127	218	61	-1234
128	219	2	-1234
129	220	17	-1234
130	221	88	-1234
131	222	19	-1234
132	223	85	-1234
133	224	54	-1234
134	225	13	-1234
135	226	20	-1234
136	227	28	-1234
137	228	23	-1234
138	229	73	-1234
139	230	57	-1234
140	231	56	-1234
141	232	67	-1234
142	233	65	-1234
143	234	37	-1234
144	235	10	-1234
145	236	37	-1234
146	237	14	-1234
147	238	68	-1234
148	239	4	-1234
149	240	87	-1234
150	241	9	-1234
151	242	1	-1234
152	243	31	-1234
153	244	64	-1234
154	245	33	-1234
155	246	82	-1234
156	247	72	-1234
157	248	81	-1234
158	249	62	-1234
159	250	57	-1234
160	251	7	-1234
161	252	36	-1234
162	253	89	-1234
163	254	33	-1234
164	255	35	-1234
165	256	58	-1234
166	257	88	-1234
167	258	59	-1234
168	259	56	-1234
169	260	7	-1234
170	261	29	-1234
171	262	54	-1234
172	263	72	-1234
173	264	12	-1234
174	265	7	-1234
175	266	61	-1234
176	267	33	-1234
177	268	6	-1234
178	269	3	-1234
179	270	72	-1234
180	271	49	-1234
181	272	36	-1234
182	273	50	-1234
183	274	48	-1234
184	366	362	1
185	367	331	1
186	368	293	1
187	369	362	1
188	370	304	1
189	371	346	1
190	372	275	1
191	373	336	1
192	374	335	1
193	375	277	1
194	376	345	1
195	377	308	1
196	378	306	1
197	379	303	1
198	380	307	1
199	381	345	1
200	382	294	1
201	383	304	1
202	384	357	1
203	385	277	1
204	386	354	1
205	387	359	1
206	388	355	1
207	389	349	1
208	390	352	1
209	391	365	1
210	392	293	1
211	393	314	1
212	394	341	1
213	395	278	1
214	396	304	1
215	397	323	1
216	398	340	1
217	399	324	1
218	400	339	1
219	401	328	1
220	402	320	1
221	403	363	1
222	404	342	1
223	405	276	1
224	406	293	1
225	407	286	1
226	408	294	1
227	409	278	1
228	410	288	1
229	411	292	1
230	412	320	1
231	413	291	1
232	414	317	1
233	415	290	1
234	416	293	1
235	417	314	1
236	418	350	1
237	419	292	1
238	420	362	1
239	421	291	1
240	422	290	1
241	423	290	1
242	424	281	1
243	425	281	1
244	426	306	1
245	427	306	1
246	428	353	1
247	429	285	1
248	430	291	1
249	431	347	1
250	432	335	1
251	433	288	1
252	434	287	1
253	435	280	1
254	436	283	1
255	437	294	1
256	438	335	1
257	439	307	1
258	440	362	1
259	441	287	1
260	442	298	1
261	443	349	1
262	444	323	1
263	445	310	1
264	446	346	1
265	447	294	1
266	448	305	1
267	449	340	1
268	450	305	1
269	451	310	1
270	452	361	1
271	453	315	1
272	454	286	1
273	455	285	1
274	456	290	1
275	457	277	1
276	458	360	1
277	459	313	1
278	460	306	1
279	461	309	1
280	462	277	1
281	463	363	1
282	464	302	1
283	465	286	1
284	466	313	1
285	467	294	1
286	468	305	1
287	469	280	1
288	470	320	1
289	471	352	1
290	472	277	1
291	473	298	1
292	474	360	1
293	475	319	1
294	476	286	1
295	477	326	1
296	478	317	1
297	479	350	1
298	480	281	1
299	481	306	1
300	482	307	1
301	483	357	1
302	484	281	1
303	485	348	1
304	486	284	1
305	487	275	1
306	488	338	1
307	489	306	1
308	490	330	1
309	491	335	1
310	492	335	1
311	493	276	1
312	494	291	1
313	495	362	1
314	496	293	1
315	497	359	1
316	498	328	1
317	499	287	1
318	500	294	1
319	501	302	1
320	502	297	1
321	503	347	1
322	504	331	1
323	505	330	1
324	506	341	1
325	507	339	1
326	508	311	1
327	509	284	1
328	510	311	1
329	511	288	1
330	512	342	1
331	513	278	1
332	514	361	1
333	515	283	1
334	516	275	1
335	517	305	1
336	518	338	1
337	519	307	1
338	520	356	1
339	521	346	1
340	522	355	1
341	523	336	1
342	524	331	1
343	525	281	1
344	526	310	1
345	527	363	1
346	528	307	1
347	529	309	1
348	530	332	1
349	531	362	1
350	532	333	1
351	533	330	1
352	534	281	1
353	535	303	1
354	536	328	1
355	537	346	1
356	538	286	1
357	539	281	1
358	540	335	1
359	541	307	1
360	542	280	1
361	543	277	1
362	544	346	1
363	545	323	1
364	546	310	1
365	547	324	1
366	548	322	1
367	640	636	2
368	641	605	2
369	642	567	2
370	643	636	2
371	644	578	2
372	645	620	2
373	646	549	2
374	647	610	2
375	648	609	2
376	649	551	2
377	650	619	2
378	651	582	2
379	652	580	2
380	653	577	2
381	654	581	2
382	655	619	2
383	656	568	2
384	657	578	2
385	658	631	2
386	659	551	2
387	660	628	2
388	661	633	2
389	662	629	2
390	663	623	2
391	664	626	2
392	665	639	2
393	666	567	2
394	667	588	2
395	668	615	2
396	669	552	2
397	670	578	2
398	671	597	2
399	672	614	2
400	673	598	2
401	674	613	2
402	675	602	2
403	676	594	2
404	677	637	2
405	678	616	2
406	679	550	2
407	680	567	2
408	681	560	2
409	682	568	2
410	683	552	2
411	684	562	2
412	685	566	2
413	686	594	2
414	687	565	2
415	688	591	2
416	689	564	2
417	690	567	2
418	691	588	2
419	692	624	2
420	693	566	2
421	694	636	2
422	695	565	2
423	696	564	2
424	697	564	2
425	698	555	2
426	699	555	2
427	700	580	2
428	701	580	2
429	702	627	2
430	703	559	2
431	704	565	2
432	705	621	2
433	706	609	2
434	707	562	2
435	708	561	2
436	709	554	2
437	710	557	2
438	711	568	2
439	712	609	2
440	713	581	2
441	714	636	2
442	715	561	2
443	716	572	2
444	717	623	2
445	718	597	2
446	719	584	2
447	720	620	2
448	721	568	2
449	722	579	2
450	723	614	2
451	724	579	2
452	725	584	2
453	726	635	2
454	727	589	2
455	728	560	2
456	729	559	2
457	730	564	2
458	731	551	2
459	732	634	2
460	733	587	2
461	734	580	2
462	735	583	2
463	736	551	2
464	737	637	2
465	738	576	2
466	739	560	2
467	740	587	2
468	741	568	2
469	742	579	2
470	743	554	2
471	744	594	2
472	745	626	2
473	746	551	2
474	747	572	2
475	748	634	2
476	749	593	2
477	750	560	2
478	751	600	2
479	752	591	2
480	753	624	2
481	754	555	2
482	755	580	2
483	756	581	2
484	757	631	2
485	758	555	2
486	759	622	2
487	760	558	2
488	761	549	2
489	762	612	2
490	763	580	2
491	764	604	2
492	765	609	2
493	766	609	2
494	767	550	2
495	768	565	2
496	769	636	2
497	770	567	2
498	771	633	2
499	772	602	2
500	773	561	2
501	774	568	2
502	775	576	2
503	776	571	2
504	777	621	2
505	778	605	2
506	779	604	2
507	780	615	2
508	781	613	2
509	782	585	2
510	783	558	2
511	784	585	2
512	785	562	2
513	786	616	2
514	787	552	2
515	788	635	2
516	789	557	2
517	790	549	2
518	791	579	2
519	792	612	2
520	793	581	2
521	794	630	2
522	795	620	2
523	796	629	2
524	797	610	2
525	798	605	2
526	799	555	2
527	800	584	2
528	801	637	2
529	802	581	2
530	803	583	2
531	804	606	2
532	805	636	2
533	806	607	2
534	807	604	2
535	808	555	2
536	809	577	2
537	810	602	2
538	811	620	2
539	812	560	2
540	813	555	2
541	814	609	2
542	815	581	2
543	816	554	2
544	817	551	2
545	818	620	2
546	819	597	2
547	820	584	2
548	821	598	2
549	822	596	2
\.


--
-- Data for Name: idn_claim_property; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_claim_property (id, local_claim_id, property_name, property_value, tenant_id) FROM stdin;
1	1	Description	Local	-1234
2	1	DisplayName	Local	-1234
3	2	Description	Locality	-1234
4	2	DisplayName	Locality	-1234
5	3	Description	Country	-1234
6	3	DisplayOrder	5	-1234
7	3	SupportedByDefault	true	-1234
8	3	DisplayName	Country	-1234
9	4	Description	Display Name	-1234
10	4	DisplayName	Display Name	-1234
11	5	Description	Failed Lockout Count	-1234
12	5	DisplayName	Failed Lockout Count	-1234
13	6	Description	Mobile	-1234
14	6	DisplayOrder	8	-1234
15	6	SupportedByDefault	true	-1234
16	6	DisplayName	Mobile	-1234
17	7	Description	Nick Name	-1234
18	7	DisplayName	Nick Name	-1234
19	8	Description	Locked Reason	-1234
20	8	DisplayName	Locked Reason	-1234
21	9	Description	Full Name	-1234
22	9	DisplayName	Full Name	-1234
23	10	Description	Honoric Prefix	-1234
24	10	DisplayName	Name - Honoric Prefix	-1234
25	11	Description	IM - Skype	-1234
26	11	DisplayName	IM - Skype	-1234
27	12	Description	Address	-1234
28	12	DisplayName	Address	-1234
29	13	Description	Region	-1234
30	13	DisplayName	Region	-1234
31	14	Description	Middle Name	-1234
32	14	DisplayName	Middle Name	-1234
33	15	Description	Last Logon Time	-1234
34	15	DisplayName	Last Logon	-1234
35	16	Description	Username	-1234
36	16	DisplayName	Username	-1234
37	17	Description	Preferred Language	-1234
38	17	DisplayName	Preferred Language	-1234
39	18	Description	Extended External ID	-1234
40	18	DisplayName	Extended External ID	-1234
41	19	Description	Time Zone	-1234
42	19	DisplayName	Time Zone	-1234
43	20	Description	Last Name	-1234
44	20	DisplayOrder	2	-1234
45	20	Required	true	-1234
46	20	SupportedByDefault	true	-1234
47	20	DisplayName	Last Name	-1234
48	21	Description	Last Login Time	-1234
49	21	DisplayName	Last Login	-1234
50	22	Description	Preferred Notification Channel	-1234
51	22	DisplayName	Preferred Channel	-1234
52	23	Description	Cost Center	-1234
53	23	DisplayName	Cost Center	-1234
54	24	Description	Location	-1234
55	24	DisplayName	Location	-1234
56	25	Description	Challenge Question	-1234
57	25	DisplayName	Challenge Question	-1234
58	26	Description	Claim to disable EmailOTP	-1234
59	26	DisplayName	Disable EmailOTP	-1234
60	27	Description	Account Locked	-1234
61	27	DisplayName	Account Locked	-1234
62	28	Description	Other Email	-1234
63	28	DisplayName	Emails - Other Email	-1234
64	29	ReadOnly	true	-1234
65	29	Description	Department	-1234
66	29	SupportedByDefault	true	-1234
67	29	DisplayName	Department	-1234
68	30	Description	Photo URL	-1234
69	30	DisplayName	Photo URIL	-1234
70	31	Description	IM	-1234
71	31	DisplayOrder	9	-1234
72	31	SupportedByDefault	true	-1234
73	31	DisplayName	IM	-1234
74	32	Description	Postal Code	-1234
75	32	DisplayName	Postal Code	-1234
76	33	Description	Email Address	-1234
77	33	DisplayOrder	6	-1234
78	33	Required	true	-1234
79	33	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
80	33	SupportedByDefault	true	-1234
81	33	DisplayName	Email	-1234
82	34	Description	Organization	-1234
83	34	DisplayOrder	3	-1234
84	34	SupportedByDefault	true	-1234
85	34	DisplayName	Organization	-1234
86	35	Description	Work Email	-1234
87	35	DisplayName	Emails - Work Email	-1234
88	36	Description	URL	-1234
89	36	DisplayOrder	10	-1234
90	36	SupportedByDefault	true	-1234
91	36	DisplayName	URL	-1234
92	37	Description	Address - Locality	-1234
93	37	DisplayName	Address - Locality	-1234
94	38	Description	Unlock Time	-1234
95	38	DisplayName	Unlock Time	-1234
96	39	Description	Work Phone	-1234
97	39	DisplayName	Phone Numbers - Work Phone Number	-1234
98	40	Description	Honoric Suffix	-1234
99	40	DisplayName	Name - Honoric Suffix	-1234
100	41	Description	Phone Verified	-1234
101	41	DisplayName	Phone Verified	-1234
102	42	Description	Primary Challenge Question	-1234
103	42	DisplayName	Primary Challenge Question	-1234
104	43	Description	Address - Formatted	-1234
105	43	DisplayName	Address - Formatted	-1234
106	44	Description	Claim to store the secret key	-1234
107	44	DisplayName	Secret Key	-1234
108	45	Description	Temporary claim to invoke email verified feature	-1234
109	45	DisplayName	Verify Email	-1234
110	46	ReadOnly	true	-1234
111	46	Description	Unique ID of the user used in external systems	-1234
112	46	DisplayName	External User ID	-1234
113	47	Description	Challenge Question1	-1234
114	47	DisplayName	Challenge Question1	-1234
115	48	Description	Fax Number	-1234
116	48	DisplayName	Phone Numbers - Fax Number	-1234
117	49	ReadOnly	true	-1234
118	49	Description	Last Modified timestamp of the user	-1234
119	49	DisplayName	Last Modified Time	-1234
120	50	Description	Formatted Name	-1234
121	50	DisplayName	Name - Formatted Name	-1234
122	51	Description	Claim to disable SMSOTP	-1234
123	51	DisplayName	Disable SMSOTP	-1234
124	52	Description	Temporary claim to invoke email ask Password feature	-1234
125	52	DisplayName	Ask Password	-1234
126	53	Description	Challenge Question2	-1234
127	53	DisplayName	Challenge Question2	-1234
128	54	Description	Home Phone	-1234
129	54	DisplayName	Phone Numbers - Home Phone Number	-1234
130	55	Description	Number of consecutive failed attempts done for password recovery	-1234
131	55	DisplayName	Failed Password Recovery Attempts	-1234
132	56	ReadOnly	true	-1234
133	56	Description	Unique ID of the user	-1234
134	56	DisplayName	User ID	-1234
135	57	Description	Address - Street	-1234
136	57	DisplayOrder	5	-1234
137	57	DisplayName	Address - Street	-1234
138	58	Description	Telephone	-1234
139	58	DisplayOrder	7	-1234
140	58	SupportedByDefault	true	-1234
141	58	DisplayName	Telephone	-1234
142	59	ReadOnly	true	-1234
143	59	Description	Claim to store newly updated email address until the new email address is verified.	-1234
144	59	DisplayName	Verification Pending Email	-1234
145	60	Description	Account Disabled	-1234
146	60	DisplayName	Account Disabled	-1234
147	61	Description	First Name	-1234
148	61	DisplayOrder	1	-1234
149	61	Required	true	-1234
150	61	SupportedByDefault	true	-1234
151	61	DisplayName	First Name	-1234
152	62	Description	Extended Display Name	-1234
153	62	DisplayName	Extended Display Name	-1234
154	63	Description	Failed Login Attempts	-1234
155	63	DisplayName	Failed Login Attempts	-1234
156	64	Description	IM - Gtalk	-1234
157	64	DisplayName	IM - Gtalk	-1234
158	65	Description	Groups	-1234
159	65	DisplayName	Groups	-1234
160	66	Description	X509Certificates	-1234
161	66	DisplayName	X509Certificates	-1234
162	67	Description	Photo	-1234
163	67	DisplayName	Photo	-1234
164	68	Description	Entitlements	-1234
165	68	DisplayName	Entitlements	-1234
166	69	Description	Last Password Update Time	-1234
167	69	DisplayName	Last Password Update	-1234
168	70	Description	Failed Attempts Before Success	-1234
169	70	DisplayName	Failed Attempts Before Success	-1234
170	71	Description	Phone Numbers	-1234
171	71	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
172	71	DisplayName	Phone Numbers	-1234
173	72	Description	Birth Date	-1234
174	72	DisplayName	Birth Date	-1234
175	73	Description	Other Phone Number	-1234
176	73	DisplayName	Phone Numbers - Other	-1234
177	74	Description	User Principal	-1234
178	74	DisplayName	User Principal	-1234
179	75	Description	Home Email	-1234
180	75	DisplayName	Emails - Home Email	-1234
181	76	Description	State	-1234
182	76	DisplayName	State	-1234
183	77	Description	One Time Password	-1234
184	77	DisplayName	One Time Password	-1234
185	78	Description	Status of the account	-1234
186	78	DisplayName	Active	-1234
187	79	Description	Resource Type	-1234
188	79	DisplayName	Resource Type	-1234
189	80	Description	Email Verified	-1234
190	80	DisplayName	Email Verified	-1234
191	81	Description	User Type	-1234
192	81	DisplayName	User Type	-1234
193	82	Description	Other Phone	-1234
194	82	DisplayName	Other Phone	-1234
195	83	Description	Photo - Thumbnail	-1234
196	83	DisplayName	Photo - Thumbnail	-1234
197	84	Description	Temporary claim to invoke email force password feature	-1234
198	84	DisplayName	Force Password Reset	-1234
199	85	Description	Title	-1234
200	85	DisplayName	Title	-1234
201	86	Description	Extended Ref	-1234
202	86	DisplayName	Extended Ref	-1234
203	87	ReadOnly	true	-1234
204	87	Description	Created timestamp of the user	-1234
205	87	DisplayName	Created Time	-1234
206	88	Description	Gender	-1234
207	88	DisplayName	Gender	-1234
208	89	ReadOnly	true	-1234
209	89	Description	Role	-1234
210	89	SupportedByDefault	true	-1234
211	89	DisplayName	Role	-1234
212	90	ReadOnly	true	-1234
213	90	Description	Account State	-1234
214	90	DisplayName	Account State	-1234
215	91	Description	Pager Number	-1234
216	91	DisplayName	Phone Numbers - Pager Number	-1234
217	92	ReadOnly	true	-1234
218	92	Description	End-User's gender. Values defined by this specification are female and male. Other values MAY be used when neither of the defined values are applicable.	-1234
219	92	SupportedByDefault	true	-1234
220	92	MappedLocalClaim	http://wso2.org/claims/gender	-1234
221	92	DisplayName	Gender	-1234
222	93	Description	Street Address	-1234
223	93	MappedLocalClaim	http://wso2.org/claims/streetaddress	-1234
224	93	DisplayName	Street Address	-1234
225	94	Description	Time Zone	-1234
226	94	DisplayOrder	2	-1234
227	94	Required	true	-1234
228	94	SupportedByDefault	true	-1234
229	94	MappedLocalClaim	http://wso2.org/claims/timeZone	-1234
230	94	DisplayName	Time Zone	-1234
231	95	Description	Manager - home	-1234
232	95	DisplayOrder	1	-1234
233	95	Required	true	-1234
234	95	SupportedByDefault	true	-1234
235	95	MappedLocalClaim	http://wso2.org/claims/gender	-1234
236	95	DisplayName	Manager - home	-1234
237	96	Description	Photo	-1234
238	96	DisplayOrder	5	-1234
239	96	SupportedByDefault	true	-1234
240	96	MappedLocalClaim	http://wso2.org/claims/photourl	-1234
241	96	DisplayName	Photo	-1234
242	97	Description	Date of Birth	-1234
243	97	DisplayOrder	6	-1234
244	97	SupportedByDefault	true	-1234
245	97	MappedLocalClaim	http://wso2.org/claims/dob	-1234
246	97	DisplayName	DOB	-1234
247	98	Description	Locality	-1234
248	98	DisplayOrder	2	-1234
249	98	Required	true	-1234
250	98	SupportedByDefault	true	-1234
251	98	MappedLocalClaim	http://wso2.org/claims/local	-1234
252	98	DisplayName	Locality	-1234
253	99	Description	Legal Person Name	-1234
254	99	DisplayOrder	1	-1234
255	99	Required	true	-1234
256	99	SupportedByDefault	true	-1234
257	99	MappedLocalClaim	http://wso2.org/claims/extendedDisplayName	-1234
258	99	DisplayName	Legal Person Name	-1234
259	100	Description	Given Name	-1234
260	100	DisplayOrder	1	-1234
261	100	Required	true	-1234
262	100	SupportedByDefault	true	-1234
263	100	MappedLocalClaim	http://wso2.org/claims/givenname	-1234
264	100	DisplayName	Name - Given Name	-1234
265	101	Description	Place of Birth	-1234
266	101	DisplayOrder	1	-1234
267	101	Required	true	-1234
268	101	SupportedByDefault	true	-1234
269	101	MappedLocalClaim	http://wso2.org/claims/country	-1234
270	101	DisplayName	Place of Birth	-1234
271	102	Description	Phone Numbers	-1234
272	102	DisplayOrder	3	-1234
273	102	Required	true	-1234
274	102	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
275	102	SupportedByDefault	true	-1234
276	102	MappedLocalClaim	http://wso2.org/claims/phoneNumbers	-1234
277	102	DisplayName	Phone Numbers	-1234
278	103	Description	Organization -division	-1234
279	103	DisplayOrder	1	-1234
280	103	Required	true	-1234
281	103	SupportedByDefault	true	-1234
282	103	MappedLocalClaim	http://wso2.org/claims/organization	-1234
283	103	DisplayName	Organization -division	-1234
284	104	Description	Postalcode	-1234
285	104	DisplayOrder	4	-1234
286	104	SupportedByDefault	true	-1234
287	104	MappedLocalClaim	http://wso2.org/claims/postalcode	-1234
288	104	DisplayName	Postalcode	-1234
289	105	Description	Economic Operator Registration and Identification	-1234
290	105	DisplayOrder	1	-1234
291	105	Required	true	-1234
292	105	SupportedByDefault	true	-1234
293	105	MappedLocalClaim	http://wso2.org/claims/department	-1234
294	105	DisplayName	Economic Operator Registration and Identification	-1234
295	106	Description	Email Addresses	-1234
296	106	DisplayOrder	5	-1234
297	106	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
298	106	SupportedByDefault	true	-1234
299	106	MappedLocalClaim	http://wso2.org/claims/emailaddress	-1234
300	106	DisplayName	Emails	-1234
301	107	Description	Phone Numbers	-1234
302	107	DisplayOrder	5	-1234
303	107	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
304	107	SupportedByDefault	true	-1234
305	107	MappedLocalClaim	http://wso2.org/claims/phoneNumbers	-1234
306	107	DisplayName	Phone Numbers	-1234
307	108	Description	Family Name	-1234
308	108	DisplayOrder	2	-1234
309	108	Required	true	-1234
310	108	SupportedByDefault	true	-1234
311	108	MappedLocalClaim	http://wso2.org/claims/lastname	-1234
312	108	DisplayName	Name - Family Name	-1234
313	109	Description	Photo	-1234
314	109	DisplayOrder	5	-1234
315	109	SupportedByDefault	true	-1234
316	109	MappedLocalClaim	http://wso2.org/claims/photourl	-1234
317	109	DisplayName	Photo	-1234
318	110	Description	Photo - Thumbnail	-1234
319	110	DisplayOrder	5	-1234
320	110	SupportedByDefault	true	-1234
321	110	MappedLocalClaim	http://wso2.org/claims/thumbnail	-1234
322	110	DisplayName	Photo - Thumbnail	-1234
323	111	Description	Country	-1234
324	111	DisplayOrder	5	-1234
325	111	SupportedByDefault	true	-1234
326	111	MappedLocalClaim	http://wso2.org/claims/country	-1234
327	111	DisplayName	Country	-1234
328	112	Description	True if the End-User's e-mail address has been verified; otherwise false. 	-1234
329	112	MappedLocalClaim	http://wso2.org/claims/identity/emailVerified	-1234
330	112	DisplayName	Email Verified	-1234
331	113	Description	Title	-1234
332	113	DisplayOrder	2	-1234
333	113	Required	true	-1234
334	113	SupportedByDefault	true	-1234
335	113	MappedLocalClaim	http://wso2.org/claims/title	-1234
336	113	DisplayName	Title	-1234
337	114	Description	User Type	-1234
338	114	DisplayOrder	2	-1234
339	114	Required	true	-1234
340	114	SupportedByDefault	true	-1234
341	114	MappedLocalClaim	http://wso2.org/claims/userType	-1234
342	114	DisplayName	User Type	-1234
343	115	Description	Home Email	-1234
344	115	DisplayOrder	5	-1234
345	115	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
346	115	SupportedByDefault	true	-1234
347	115	MappedLocalClaim	http://wso2.org/claims/emails.home	-1234
348	115	DisplayName	Emails - Home Email	-1234
349	116	Description	Active	-1234
350	116	DisplayOrder	2	-1234
351	116	Required	true	-1234
352	116	SupportedByDefault	true	-1234
353	116	MappedLocalClaim	http://wso2.org/claims/active	-1234
354	116	DisplayName	Active	-1234
355	117	Description	Pager Number	-1234
356	117	DisplayOrder	5	-1234
357	117	SupportedByDefault	true	-1234
358	117	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.pager	-1234
359	117	DisplayName	Phone Numbers - Pager Number	-1234
360	118	Description	String from zoneinfo time zone database representing the End-User's time zone. For example, Europe/Paris or America/Los_Angeles.	-1234
361	118	MappedLocalClaim	http://wso2.org/claims/timeZone	-1234
362	118	DisplayName	Zone Info	-1234
363	119	Description	Honoric Suffix	-1234
364	119	DisplayOrder	2	-1234
365	119	Required	true	-1234
366	119	SupportedByDefault	true	-1234
367	119	MappedLocalClaim	http://wso2.org/claims/honorificSuffix	-1234
368	119	DisplayName	Name - Honoric Suffix	-1234
369	120	Description	Photo	-1234
370	120	DisplayOrder	5	-1234
371	120	SupportedByDefault	true	-1234
372	120	MappedLocalClaim	http://wso2.org/claims/photos	-1234
373	120	DisplayName	Photo	-1234
374	121	Description	Display Name	-1234
375	121	DisplayOrder	2	-1234
376	121	Required	true	-1234
377	121	SupportedByDefault	true	-1234
378	121	MappedLocalClaim	http://wso2.org/claims/displayName	-1234
379	121	DisplayName	Display Name	-1234
380	122	Description	URL of the End-User's profile picture. This URL MUST refer to an image file (for example, a PNG, JPEG, or GIF image file)	-1234
381	122	DisplayOrder	9	-1234
382	122	SupportedByDefault	true	-1234
383	122	MappedLocalClaim	http://wso2.org/claims/photourl	-1234
384	122	DisplayName	Picture	-1234
385	123	Description	Meta - Last Modified	-1234
386	123	DisplayOrder	1	-1234
387	123	Required	true	-1234
388	123	SupportedByDefault	true	-1234
389	123	MappedLocalClaim	http://wso2.org/claims/modified	-1234
390	123	DisplayName	Meta - Last Modified	-1234
391	124	Description	X509Certificates	-1234
392	124	DisplayOrder	5	-1234
393	124	SupportedByDefault	true	-1234
394	124	MappedLocalClaim	http://wso2.org/claims/x509Certificates	-1234
395	124	DisplayName	X509Certificates	-1234
396	125	Description	Formatted Name	-1234
397	125	DisplayOrder	2	-1234
398	125	Required	true	-1234
399	125	SupportedByDefault	true	-1234
400	125	MappedLocalClaim	http://wso2.org/claims/formattedName	-1234
401	125	DisplayName	Name - Formatted Name	-1234
402	126	Description	Groups	-1234
403	126	DisplayOrder	5	-1234
404	126	SupportedByDefault	true	-1234
405	126	MappedLocalClaim	http://wso2.org/claims/groups	-1234
406	126	DisplayName	Groups	-1234
407	127	Description	Home Phone	-1234
408	127	DisplayOrder	5	-1234
409	127	SupportedByDefault	true	-1234
410	127	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	-1234
411	127	DisplayName	Phone Numbers - Home Phone Number	-1234
412	128	Description	External Id	-1234
413	128	DisplayOrder	1	-1234
414	128	Required	true	-1234
415	128	SupportedByDefault	true	-1234
416	128	MappedLocalClaim	http://wso2.org/claims/externalid	-1234
417	128	DisplayName	External Id	-1234
418	129	Description	Roles	-1234
419	129	DisplayOrder	5	-1234
420	129	SupportedByDefault	true	-1234
421	129	MappedLocalClaim	http://wso2.org/claims/role	-1234
422	129	DisplayName	Roles	-1234
423	130	Description	Entitlements	-1234
424	130	DisplayOrder	5	-1234
425	130	SupportedByDefault	true	-1234
426	130	MappedLocalClaim	http://wso2.org/claims/entitlements	-1234
427	130	DisplayName	Entitlements	-1234
428	131	Description	Locality	-1234
429	131	MappedLocalClaim	http://wso2.org/claims/locality	-1234
430	131	DisplayName	Locality	-1234
431	132	Description	Time Zone	-1234
432	132	DisplayOrder	9	-1234
433	132	SupportedByDefault	true	-1234
434	132	MappedLocalClaim	http://wso2.org/claims/timeZone	-1234
435	132	DisplayName	Time Zone	-1234
436	133	Description	Address	-1234
437	133	DisplayOrder	5	-1234
438	133	SupportedByDefault	true	-1234
439	133	MappedLocalClaim	http://wso2.org/claims/addresses	-1234
440	133	DisplayName	Address	-1234
441	134	Description	Current Family Name	-1234
442	134	DisplayOrder	1	-1234
443	134	Required	true	-1234
444	134	SupportedByDefault	true	-1234
445	134	MappedLocalClaim	http://wso2.org/claims/lastname	-1234
446	134	DisplayName	Current Family Name	-1234
447	135	Description	Shorthand name by which the End-User wishes to be referred to at the RP, such as janedoe or j.doe.	-1234
448	135	DisplayOrder	7	-1234
449	135	SupportedByDefault	true	-1234
450	135	MappedLocalClaim	http://wso2.org/claims/displayName	-1234
451	135	DisplayName	Preferred Username	-1234
452	136	Description	Middle name(s) of the End-User. Note that in some cultures, people can have multiple middle names; all can be present, with the names being separated by space characters. Also note that in some cultures, middle names are not used.	-1234
453	136	DisplayOrder	5	-1234
454	136	SupportedByDefault	true	-1234
455	136	MappedLocalClaim	http://wso2.org/claims/middleName	-1234
456	136	DisplayName	Middle Name	-1234
457	137	Description	Employee Number	-1234
458	137	DisplayOrder	1	-1234
459	137	Required	true	-1234
460	137	SupportedByDefault	true	-1234
461	137	MappedLocalClaim	http://wso2.org/claims/extendedExternalId	-1234
462	137	DisplayName	Employee Number	-1234
463	138	Description	External Id	-1234
464	138	DisplayOrder	1	-1234
465	138	Required	true	-1234
466	138	SupportedByDefault	true	-1234
467	138	MappedLocalClaim	http://wso2.org/claims/externalid	-1234
468	138	DisplayName	External Id	-1234
469	139	Description	Language	-1234
470	139	SupportedByDefault	true	-1234
471	139	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	-1234
472	139	DisplayName	Language	-1234
473	140	Description	Address - Formatted	-1234
474	140	DisplayOrder	5	-1234
475	140	SupportedByDefault	true	-1234
476	140	MappedLocalClaim	http://wso2.org/claims/addresses.formatted	-1234
477	140	DisplayName	Address - Formatted	-1234
478	141	Description	User Name	-1234
479	141	DisplayOrder	2	-1234
480	141	Required	true	-1234
481	141	SupportedByDefault	true	-1234
482	141	MappedLocalClaim	http://wso2.org/claims/username	-1234
483	141	DisplayName	User Name	-1234
484	142	Description	Time Zone	-1234
485	142	DisplayOrder	2	-1234
486	142	Required	true	-1234
487	142	SupportedByDefault	true	-1234
488	142	MappedLocalClaim	http://wso2.org/claims/timeZone	-1234
489	142	DisplayName	Time Zone	-1234
490	143	Description	Honoric Suffix	-1234
491	143	DisplayOrder	2	-1234
492	143	Required	true	-1234
493	143	SupportedByDefault	true	-1234
494	143	MappedLocalClaim	http://wso2.org/claims/honorificSuffix	-1234
495	143	DisplayName	Name - Honoric Suffix	-1234
496	144	Description	State	-1234
497	144	MappedLocalClaim	http://wso2.org/claims/stateorprovince	-1234
498	144	DisplayName	State	-1234
499	145	Description	Legal Person Identifier	-1234
500	145	DisplayOrder	1	-1234
501	145	Required	true	-1234
502	145	SupportedByDefault	true	-1234
503	145	MappedLocalClaim	http://wso2.org/claims/extendedExternalId	-1234
504	145	DisplayName	Legal Person Identifier	-1234
505	146	Description	Gender	-1234
506	146	DisplayOrder	8	-1234
507	146	SupportedByDefault	true	-1234
508	146	MappedLocalClaim	http://wso2.org/claims/gender	-1234
509	146	DisplayName	Gender	-1234
510	147	Description	Language	-1234
511	147	DisplayOrder	7	-1234
512	147	SupportedByDefault	true	-1234
513	147	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	-1234
514	147	DisplayName	Language	-1234
515	148	Description	Identifier for the End-User at the Issuer	-1234
516	148	DisplayOrder	1	-1234
517	148	Required	true	-1234
518	148	SupportedByDefault	true	-1234
519	148	MappedLocalClaim	http://wso2.org/claims/username	-1234
520	148	DisplayName	Subject	-1234
521	149	Description	User Name	-1234
522	149	DisplayOrder	2	-1234
523	149	Required	true	-1234
524	149	SupportedByDefault	true	-1234
525	149	MappedLocalClaim	http://wso2.org/claims/username	-1234
526	149	DisplayName	User Name	-1234
527	150	Description	Nick Name	-1234
528	150	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
529	150	DisplayName	Nick Name	-1234
530	151	Description	Casual name of the End-User that may or may not be the same as the given_name. For instance, a nickname value of Mike might be returned alongside a given_name value of Michael.	-1234
531	151	DisplayOrder	6	-1234
532	151	Required	true	-1234
533	151	SupportedByDefault	true	-1234
534	151	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
535	151	DisplayName	Nickname	-1234
536	152	Description	Tax Reference	-1234
537	152	DisplayOrder	1	-1234
538	152	Required	true	-1234
539	152	SupportedByDefault	true	-1234
540	152	MappedLocalClaim	http://wso2.org/claims/postalcode	-1234
541	152	DisplayName	Tax Reference	-1234
542	153	Description	Postalcode	-1234
543	153	SupportedByDefault	true	-1234
544	153	MappedLocalClaim	http://wso2.org/claims/postalcode	-1234
545	153	DisplayName	Postalcode	-1234
546	154	Description	Meta - Location	-1234
547	154	DisplayOrder	1	-1234
548	154	Required	true	-1234
549	154	SupportedByDefault	true	-1234
550	154	MappedLocalClaim	http://wso2.org/claims/resourceType	-1234
551	154	DisplayName	Meta - Location	-1234
552	155	Description	IM - Skype	-1234
553	155	DisplayOrder	5	-1234
554	155	SupportedByDefault	true	-1234
555	155	MappedLocalClaim	http://wso2.org/claims/skype	-1234
556	155	DisplayName	IM - Skype	-1234
557	156	Description	Preferred Language	-1234
558	156	DisplayOrder	2	-1234
559	156	Required	true	-1234
560	156	SupportedByDefault	true	-1234
561	156	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	-1234
562	156	DisplayName	Preferred Language	-1234
563	157	Description	Other Phone Number	-1234
564	157	DisplayOrder	5	-1234
565	157	SupportedByDefault	true	-1234
566	157	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.other	-1234
567	157	DisplayName	Phone Numbers - Other	-1234
568	158	Description	Current Given Name	-1234
569	158	DisplayOrder	1	-1234
570	158	Required	true	-1234
571	158	SupportedByDefault	true	-1234
572	158	MappedLocalClaim	http://wso2.org/claims/givenname	-1234
573	158	DisplayName	Current Given Name	-1234
574	159	Description	Middle Name	-1234
575	159	DisplayOrder	2	-1234
576	159	Required	true	-1234
577	159	SupportedByDefault	true	-1234
578	159	MappedLocalClaim	http://wso2.org/claims/middleName	-1234
579	159	DisplayName	Name - Middle Name	-1234
580	160	Description	Address - Work	-1234
581	160	DisplayOrder	5	-1234
582	160	SupportedByDefault	true	-1234
583	160	MappedLocalClaim	http://wso2.org/claims/region	-1234
584	160	DisplayName	Address - Work	-1234
585	161	Description	Mobile	-1234
586	161	MappedLocalClaim	http://wso2.org/claims/mobile	-1234
587	161	DisplayName	Mobile	-1234
588	162	Description	Full Name	-1234
589	162	DisplayOrder	2	-1234
590	162	Required	true	-1234
591	162	SupportedByDefault	true	-1234
592	162	MappedLocalClaim	http://wso2.org/claims/fullname	-1234
593	162	DisplayName	Full Name	-1234
594	163	Description	Last Name	-1234
595	163	Required	true	-1234
596	163	SupportedByDefault	true	-1234
597	163	MappedLocalClaim	http://wso2.org/claims/lastname	-1234
598	163	DisplayName	Last Name	-1234
599	164	Description	First Name	-1234
600	164	Required	true	-1234
601	164	SupportedByDefault	true	-1234
602	164	MappedLocalClaim	http://wso2.org/claims/givenname	-1234
603	164	DisplayName	First Name	-1234
604	165	Description	Email Address	-1234
605	165	DisplayOrder	3	-1234
606	165	Required	true	-1234
607	165	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
608	165	SupportedByDefault	true	-1234
609	165	MappedLocalClaim	http://wso2.org/claims/emailaddress	-1234
610	165	DisplayName	Email	-1234
611	166	Description	Gender	-1234
612	166	MappedLocalClaim	http://wso2.org/claims/gender	-1234
613	166	DisplayName	Gender	-1234
614	167	Description	Address - Region	-1234
615	167	DisplayOrder	5	-1234
616	167	SupportedByDefault	true	-1234
617	167	MappedLocalClaim	http://wso2.org/claims/region	-1234
618	167	DisplayName	Address - Region	-1234
619	168	Description	Meta - Location	-1234
620	168	DisplayOrder	1	-1234
621	168	Required	true	-1234
622	168	SupportedByDefault	true	-1234
623	168	MappedLocalClaim	http://wso2.org/claims/location	-1234
624	168	DisplayName	Meta - Location	-1234
625	169	Description	Home Email	-1234
626	169	DisplayOrder	5	-1234
627	169	SupportedByDefault	true	-1234
628	169	MappedLocalClaim	http://wso2.org/claims/emails.home	-1234
629	169	DisplayName	Emails - Home Email	-1234
630	170	Description	Meta - Last Modified	-1234
631	170	DisplayOrder	1	-1234
632	170	Required	true	-1234
633	170	SupportedByDefault	true	-1234
634	170	MappedLocalClaim	http://wso2.org/claims/modified	-1234
635	170	DisplayName	Meta - Last Modified	-1234
636	171	Description	Profile URL	-1234
637	171	DisplayOrder	2	-1234
638	171	Required	true	-1234
639	171	SupportedByDefault	true	-1234
640	171	MappedLocalClaim	http://wso2.org/claims/url	-1234
641	171	DisplayName	Profile URL	-1234
642	172	Description	Date of Birth	-1234
643	172	MappedLocalClaim	http://wso2.org/claims/dob	-1234
644	172	DisplayName	DOB	-1234
645	173	Description	Family Name	-1234
646	173	DisplayOrder	2	-1234
647	173	Required	true	-1234
648	173	SupportedByDefault	true	-1234
649	173	MappedLocalClaim	http://wso2.org/claims/lastname	-1234
650	173	DisplayName	Name - Family Name	-1234
651	174	Description	Meta - Version	-1234
652	174	DisplayOrder	1	-1234
653	174	Required	true	-1234
654	174	SupportedByDefault	true	-1234
655	174	MappedLocalClaim	http://wso2.org/claims/im	-1234
656	174	DisplayName	Meta - Version	-1234
657	175	Description	X509Certificates	-1234
658	175	DisplayOrder	5	-1234
659	175	SupportedByDefault	true	-1234
660	175	MappedLocalClaim	http://wso2.org/claims/x509Certificates	-1234
661	175	DisplayName	X509Certificates	-1234
662	176	Description	IM	-1234
663	176	DisplayOrder	5	-1234
664	176	SupportedByDefault	true	-1234
665	176	MappedLocalClaim	http://wso2.org/claims/im	-1234
666	176	DisplayName	IMS	-1234
667	177	Description	URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.	-1234
668	177	DisplayOrder	8	-1234
669	177	SupportedByDefault	true	-1234
670	177	MappedLocalClaim	http://wso2.org/claims/url	-1234
671	177	DisplayName	Profile	-1234
672	178	Description	Meta - Created	-1234
673	178	DisplayOrder	1	-1234
674	178	Required	true	-1234
675	178	SupportedByDefault	true	-1234
676	178	MappedLocalClaim	http://wso2.org/claims/created	-1234
677	178	DisplayName	Meta - Created	-1234
678	179	Description	True if the End-User's phone number has been verified; otherwise false.	-1234
679	179	MappedLocalClaim	http://wso2.org/claims/identity/phoneVerified	-1234
680	179	DisplayName	Phone Number Verified	-1234
681	180	Description	Legal Person Address	-1234
682	180	DisplayOrder	1	-1234
683	180	Required	true	-1234
684	180	SupportedByDefault	true	-1234
685	180	MappedLocalClaim	http://wso2.org/claims/addresses	-1234
686	180	DisplayName	Legal Person Address	-1234
687	181	Description	IM - Skype	-1234
688	181	DisplayOrder	5	-1234
689	181	SupportedByDefault	true	-1234
690	181	MappedLocalClaim	http://wso2.org/claims/skype	-1234
691	181	DisplayName	IM - Skype	-1234
692	182	Description	Birth Name	-1234
693	182	DisplayOrder	1	-1234
694	182	Required	true	-1234
695	182	SupportedByDefault	true	-1234
696	182	MappedLocalClaim	http://wso2.org/claims/username	-1234
697	182	DisplayName	Birth Name	-1234
698	183	Description	Country	-1234
699	183	SupportedByDefault	true	-1234
700	183	MappedLocalClaim	http://wso2.org/claims/country	-1234
701	183	DisplayName	Country	-1234
702	184	Description	LEI	-1234
703	184	DisplayOrder	1	-1234
704	184	Required	true	-1234
705	184	SupportedByDefault	true	-1234
706	184	MappedLocalClaim	http://wso2.org/claims/extendedRef	-1234
707	184	DisplayName	LEI	-1234
708	185	Description	Work Phone	-1234
709	185	DisplayOrder	5	-1234
710	185	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
711	185	SupportedByDefault	true	-1234
712	185	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.work	-1234
713	185	DisplayName	Phone Numbers - Work Phone Number	-1234
714	186	Description	Zip code or postal code component.	-1234
715	186	MappedLocalClaim	http://wso2.org/claims/postalcode	-1234
716	186	DisplayName	Postal Code	-1234
717	187	Description	Work Email	-1234
718	187	DisplayOrder	5	-1234
719	187	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
720	187	SupportedByDefault	true	-1234
721	187	MappedLocalClaim	http://wso2.org/claims/emails.work	-1234
722	187	DisplayName	Emails - Work Email	-1234
723	188	Description	Address - Country	-1234
724	188	DisplayOrder	5	-1234
725	188	SupportedByDefault	true	-1234
726	188	MappedLocalClaim	http://wso2.org/claims/country	-1234
727	188	DisplayName	Address - Country	-1234
728	189	Description	List of group names that have been assigned to the principal. This typically will require a mapping at the application container level to application deployment roles.	-1234
729	189	DisplayOrder	12	-1234
730	189	SupportedByDefault	true	-1234
731	189	MappedLocalClaim	http://wso2.org/claims/role	-1234
732	189	DisplayName	User Groups	-1234
733	190	Description	Other Email	-1234
734	190	DisplayOrder	5	-1234
735	190	SupportedByDefault	true	-1234
736	190	MappedLocalClaim	http://wso2.org/claims/emails.other	-1234
737	190	DisplayName	Emails - Other Email	-1234
738	191	Description	True if the End-User's phone number has been verified; otherwise false. 	-1234
739	191	MappedLocalClaim	http://wso2.org/claims/addresses	-1234
740	191	DisplayName	Address	-1234
741	192	Description	Work Phone	-1234
742	192	DisplayOrder	5	-1234
743	192	SupportedByDefault	true	-1234
744	192	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.work	-1234
745	192	DisplayName	Phone Numbers - Work Phone Number	-1234
746	193	Description	Last Name	-1234
747	193	Required	true	-1234
748	193	SupportedByDefault	true	-1234
749	193	MappedLocalClaim	http://wso2.org/claims/lastname	-1234
750	193	DisplayName	Last Name	-1234
751	194	Description	PPID	-1234
752	194	Required	true	-1234
753	194	SupportedByDefault	true	-1234
754	194	MappedLocalClaim	http://wso2.org/claims/im	-1234
755	194	DisplayName	0	-1234
756	195	Description	Mobile Number	-1234
757	195	DisplayOrder	5	-1234
758	195	SupportedByDefault	true	-1234
759	195	MappedLocalClaim	http://wso2.org/claims/mobile	-1234
760	195	DisplayName	Phone Numbers - Mobile Number	-1234
761	196	Description	EU Identifier	-1234
762	196	DisplayOrder	1	-1234
763	196	Required	true	-1234
764	196	SupportedByDefault	true	-1234
765	196	MappedLocalClaim	http://wso2.org/claims/externalid	-1234
766	196	DisplayName	EU Identifier	-1234
767	197	Description	Active	-1234
768	197	DisplayOrder	2	-1234
769	197	Required	true	-1234
770	197	SupportedByDefault	true	-1234
771	197	MappedLocalClaim	http://wso2.org/claims/active	-1234
772	197	DisplayName	Active	-1234
773	198	Description	Country name component	-1234
774	198	MappedLocalClaim	http://wso2.org/claims/country	-1234
775	198	DisplayName	Country	-1234
776	199	Description	Meta - Location	-1234
777	199	DisplayOrder	1	-1234
778	199	Required	true	-1234
779	199	SupportedByDefault	true	-1234
780	199	MappedLocalClaim	http://wso2.org/claims/location	-1234
781	199	DisplayName	Meta - Location	-1234
782	200	Description	Manager - home	-1234
783	200	DisplayOrder	1	-1234
784	200	Required	true	-1234
785	200	SupportedByDefault	true	-1234
786	200	MappedLocalClaim	http://wso2.org/claims/extendedRef	-1234
787	200	DisplayName	Manager - home	-1234
788	201	Description	Temporary claim to invoke email verified feature	-1234
789	201	DisplayOrder	1	-1234
790	201	Required	true	-1234
791	201	SupportedByDefault	true	-1234
792	201	MappedLocalClaim	http://wso2.org/claims/identity/verifyEmail	-1234
793	201	DisplayName	Verify Email	-1234
794	202	Description	Address	-1234
795	202	DisplayOrder	5	-1234
796	202	SupportedByDefault	true	-1234
797	202	MappedLocalClaim	http://wso2.org/claims/addresses	-1234
798	202	DisplayName	Address	-1234
799	203	Description	Temporary claim to invoke email ask Password feature	-1234
800	203	DisplayOrder	1	-1234
801	203	Required	true	-1234
802	203	SupportedByDefault	true	-1234
803	203	MappedLocalClaim	http://wso2.org/claims/identity/askPassword	-1234
804	203	DisplayName	Ask Password	-1234
805	204	Description	Full mailing address, formatted for display or use on a mailing label. This field MAY contain multiple lines, separated by newlines.	-1234
806	204	MappedLocalClaim	http://wso2.org/claims/addresses.formatted	-1234
807	204	DisplayName	Address Formatted	-1234
808	205	Description	Manager - home	-1234
809	205	DisplayOrder	1	-1234
810	205	Required	true	-1234
811	205	SupportedByDefault	true	-1234
812	205	MappedLocalClaim	http://wso2.org/claims/stateorprovince	-1234
813	205	DisplayName	Manager - home	-1234
814	206	Description	System for Exchange of Excise Data Identifier	-1234
815	206	DisplayOrder	1	-1234
816	206	Required	true	-1234
817	206	SupportedByDefault	true	-1234
818	206	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
819	206	DisplayName	System for Exchange of Excise Data Identifier	-1234
820	207	Description	Postalcode	-1234
821	207	SupportedByDefault	true	-1234
822	207	MappedLocalClaim	http://wso2.org/claims/postalcode	-1234
823	207	DisplayName	Postalcode	-1234
824	208	Description	End-User's preferred e-mail address.	-1234
825	208	MappedLocalClaim	http://wso2.org/claims/emailaddress	-1234
826	208	DisplayName	Email	-1234
827	209	Description	Photo - Thumbnail	-1234
828	209	DisplayOrder	5	-1234
829	209	SupportedByDefault	true	-1234
830	209	MappedLocalClaim	http://wso2.org/claims/thumbnail	-1234
831	209	DisplayName	Photo - Thumbnail	-1234
832	210	Description	Nick Name	-1234
833	210	DisplayOrder	1	-1234
834	210	Required	true	-1234
835	210	SupportedByDefault	true	-1234
836	210	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
837	210	DisplayName	Nick Name	-1234
838	211	Description	The user principal name	-1234
839	211	DisplayOrder	11	-1234
840	211	SupportedByDefault	true	-1234
841	211	MappedLocalClaim	http://wso2.org/claims/userprincipal	-1234
842	211	DisplayName	User Principal	-1234
843	212	Description	Honoric Prefix	-1234
844	212	DisplayOrder	2	-1234
845	212	Required	true	-1234
846	212	SupportedByDefault	true	-1234
847	212	MappedLocalClaim	http://wso2.org/claims/honorificPrefix	-1234
848	212	DisplayName	Name - Honoric Prefix	-1234
849	213	Description	Locality	-1234
850	213	DisplayOrder	2	-1234
851	213	Required	true	-1234
852	213	SupportedByDefault	true	-1234
853	213	MappedLocalClaim	http://wso2.org/claims/local	-1234
854	213	DisplayName	Locality	-1234
855	214	Description	IM - Gtalk	-1234
856	214	DisplayOrder	5	-1234
857	214	SupportedByDefault	true	-1234
858	214	MappedLocalClaim	http://wso2.org/claims/gtalk	-1234
859	214	DisplayName	IM - Gtalk	-1234
860	215	Description	Address - Postal Code	-1234
861	215	DisplayOrder	5	-1234
862	215	SupportedByDefault	true	-1234
863	215	MappedLocalClaim	http://wso2.org/claims/postalcode	-1234
864	215	DisplayName	Address - Postal Code	-1234
865	216	Description	Person Identifier	-1234
866	216	DisplayOrder	1	-1234
867	216	Required	true	-1234
868	216	SupportedByDefault	true	-1234
869	216	MappedLocalClaim	http://wso2.org/claims/userid	-1234
870	216	DisplayName	Person Identifier	-1234
871	217	Description	Given name(s) or first name(s) of the End-User. Note that in some cultures, people can have multiple given names; all can be present, with the names being separated by space characters.	-1234
872	217	DisplayOrder	3	-1234
873	217	SupportedByDefault	true	-1234
874	217	MappedLocalClaim	http://wso2.org/claims/givenname	-1234
875	217	DisplayName	Given Name	-1234
876	218	Description	First Name	-1234
877	218	Required	true	-1234
878	218	SupportedByDefault	true	-1234
879	218	MappedLocalClaim	http://wso2.org/claims/givenname	-1234
880	218	DisplayName	First Name	-1234
881	219	Description	City or locality component.	-1234
882	219	MappedLocalClaim	http://wso2.org/claims/locality	-1234
883	219	DisplayName	Locality	-1234
884	220	Description	Preferred Language	-1234
885	220	DisplayOrder	2	-1234
886	220	Required	true	-1234
887	220	SupportedByDefault	true	-1234
888	220	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	-1234
889	220	DisplayName	Preferred Language	-1234
890	221	Description	Gender	-1234
891	221	SupportedByDefault	true	-1234
892	221	MappedLocalClaim	http://wso2.org/claims/gender	-1234
893	221	DisplayName	Gender	-1234
894	222	Description	Time Zone	-1234
895	222	MappedLocalClaim	http://wso2.org/claims/timeZone	-1234
896	222	DisplayName	Time Zone	-1234
897	223	Description	Title	-1234
898	223	DisplayOrder	2	-1234
899	223	Required	true	-1234
900	223	SupportedByDefault	true	-1234
901	223	MappedLocalClaim	http://wso2.org/claims/title	-1234
902	223	DisplayName	Title	-1234
903	224	Description	Home Phone	-1234
904	224	SupportedByDefault	true	-1234
905	224	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	-1234
906	224	DisplayName	Home Phone	-1234
907	225	Description	State, province, prefecture, or region component.	-1234
908	225	MappedLocalClaim	http://wso2.org/claims/region	-1234
909	225	DisplayName	One Time Password	-1234
910	226	Description	Surname(s) or last name(s) of the End-User. Note that in some cultures, people can have multiple family names or no family name; all can be present, with the names being separated by space characters.	-1234
911	226	DisplayOrder	4	-1234
912	226	SupportedByDefault	true	-1234
913	226	MappedLocalClaim	http://wso2.org/claims/lastname	-1234
914	226	DisplayName	Surname	-1234
915	227	Description	Other Email	-1234
916	227	DisplayOrder	5	-1234
917	227	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
918	227	SupportedByDefault	true	-1234
919	227	MappedLocalClaim	http://wso2.org/claims/emails.other	-1234
920	227	DisplayName	Emails - Other Email	-1234
921	228	Description	Cost Center	-1234
922	228	DisplayOrder	1	-1234
923	228	Required	true	-1234
924	228	SupportedByDefault	true	-1234
925	228	MappedLocalClaim	http://wso2.org/claims/costCenter	-1234
926	228	DisplayName	Cost Center	-1234
927	229	Description	Other Phone Number	-1234
928	229	DisplayOrder	5	-1234
929	229	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
930	229	SupportedByDefault	true	-1234
931	229	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.other	-1234
932	229	DisplayName	Phone Numbers - Other	-1234
933	230	Description	Address - Street	-1234
934	230	DisplayOrder	5	-1234
935	230	SupportedByDefault	true	-1234
936	230	MappedLocalClaim	http://wso2.org/claims/streetaddress	-1234
937	230	DisplayName	Address - Street	-1234
938	231	Description	Id	-1234
939	231	DisplayOrder	1	-1234
940	231	Required	true	-1234
941	231	SupportedByDefault	true	-1234
942	231	MappedLocalClaim	http://wso2.org/claims/userid	-1234
943	231	DisplayName	Id	-1234
944	232	Description	Photo	-1234
945	232	DisplayOrder	5	-1234
946	232	SupportedByDefault	true	-1234
947	232	MappedLocalClaim	http://wso2.org/claims/photos	-1234
948	232	DisplayName	Photo	-1234
949	233	Description	Groups	-1234
950	233	DisplayOrder	5	-1234
951	233	SupportedByDefault	true	-1234
952	233	MappedLocalClaim	http://wso2.org/claims/groups	-1234
953	233	DisplayName	Groups	-1234
954	234	Description	Address - Home	-1234
955	234	DisplayOrder	5	-1234
956	234	SupportedByDefault	true	-1234
957	234	MappedLocalClaim	http://wso2.org/claims/addresses.locality	-1234
958	234	DisplayName	Address - Home	-1234
959	235	Description	Honoric Prefix	-1234
960	235	DisplayOrder	2	-1234
961	235	Required	true	-1234
962	235	SupportedByDefault	true	-1234
963	235	MappedLocalClaim	http://wso2.org/claims/honorificPrefix	-1234
964	235	DisplayName	Name - Honoric Prefix	-1234
965	236	Description	Address - Locality	-1234
966	236	DisplayOrder	5	-1234
967	236	SupportedByDefault	true	-1234
968	236	MappedLocalClaim	http://wso2.org/claims/addresses.locality	-1234
969	236	DisplayName	Address - Locality	-1234
970	237	Description	Middle Name	-1234
971	237	DisplayOrder	2	-1234
972	237	Required	true	-1234
973	237	SupportedByDefault	true	-1234
974	237	MappedLocalClaim	http://wso2.org/claims/middleName	-1234
975	237	DisplayName	Name - Middle Name	-1234
976	238	Description	Entitlements	-1234
977	238	DisplayOrder	5	-1234
978	238	SupportedByDefault	true	-1234
979	238	MappedLocalClaim	http://wso2.org/claims/entitlements	-1234
980	238	DisplayName	Entitlements	-1234
981	239	Description	Display Name	-1234
982	239	DisplayOrder	2	-1234
983	239	Required	true	-1234
984	239	SupportedByDefault	true	-1234
985	239	MappedLocalClaim	http://wso2.org/claims/displayName	-1234
986	239	DisplayName	Display Name	-1234
987	240	Description	Meta - Created	-1234
988	240	DisplayOrder	1	-1234
989	240	Required	true	-1234
990	240	SupportedByDefault	true	-1234
991	240	MappedLocalClaim	http://wso2.org/claims/created	-1234
992	240	DisplayName	Meta - Created	-1234
993	241	Description	End-User's full name in displayable form including all name parts, possibly including titles and suffixes, ordered according to the End-User's locale and preferences	-1234
994	241	DisplayOrder	2	-1234
995	241	Required	true	-1234
996	241	SupportedByDefault	true	-1234
997	241	MappedLocalClaim	http://wso2.org/claims/fullname	-1234
998	241	DisplayName	Full Name	-1234
999	242	Description	End-User's locale, For example, en-US or fr-CA, en_US	-1234
1000	242	MappedLocalClaim	http://wso2.org/claims/local	-1234
1001	242	DisplayName	Locale	-1234
1002	243	Description	VAT Registration Number	-1234
1003	243	DisplayOrder	1	-1234
1004	243	Required	true	-1234
1005	243	SupportedByDefault	true	-1234
1006	243	MappedLocalClaim	http://wso2.org/claims/im	-1234
1007	243	DisplayName	VAT Registration Number	-1234
1008	244	Description	IM - Gtalk	-1234
1009	244	DisplayOrder	5	-1234
1010	244	SupportedByDefault	true	-1234
1011	244	MappedLocalClaim	http://wso2.org/claims/gtalk	-1234
1012	244	DisplayName	IM - Gtalk	-1234
1013	245	Description	Email Address	-1234
1014	245	Required	true	-1234
1015	245	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
1016	245	SupportedByDefault	true	-1234
1017	245	MappedLocalClaim	http://wso2.org/claims/emailaddress	-1234
1018	245	DisplayName	Email	-1234
1019	246	Description	Other Phone	-1234
1020	246	MappedLocalClaim	http://wso2.org/claims/otherphone	-1234
1021	246	DisplayName	Other Phone	-1234
1022	247	Description	Date of birth	-1234
1023	247	DisplayOrder	1	-1234
1024	247	Required	true	-1234
1025	247	SupportedByDefault	true	-1234
1026	247	MappedLocalClaim	http://wso2.org/claims/dob	-1234
1027	247	DisplayName	Date of birth	-1234
1028	248	Description	User Type	-1234
1029	248	DisplayOrder	2	-1234
1030	248	Required	true	-1234
1031	248	SupportedByDefault	true	-1234
1032	248	MappedLocalClaim	http://wso2.org/claims/userType	-1234
1033	248	DisplayName	User Type	-1234
1034	249	Description	Manager - Display Name	-1234
1035	249	DisplayOrder	1	-1234
1036	249	Required	true	-1234
1037	249	SupportedByDefault	true	-1234
1038	249	MappedLocalClaim	http://wso2.org/claims/extendedDisplayName	-1234
1039	249	DisplayName	Manager - Display Name	-1234
1040	250	Description	Full street address component, which MAY include house number, street name, Post Office Box, and multi-line extended street address information.	-1234
1041	250	MappedLocalClaim	http://wso2.org/claims/streetaddress	-1234
1042	250	DisplayName	Street Address	-1234
1043	251	Description	Nick Name	-1234
1044	251	DisplayOrder	2	-1234
1045	251	Required	true	-1234
1046	251	SupportedByDefault	true	-1234
1047	251	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
1048	251	DisplayName	Nick Name	-1234
1049	252	Description	URL of the End-User's Web page or blog. This Web page SHOULD contain information published by the End-User or an organization that the End-User is affiliated with.	-1234
1050	252	DisplayOrder	10	-1234
1051	252	SupportedByDefault	true	-1234
1052	252	MappedLocalClaim	http://wso2.org/claims/url	-1234
1053	252	DisplayName	URL	-1234
1054	253	Description	Roles	-1234
1055	253	DisplayOrder	5	-1234
1056	253	SupportedByDefault	true	-1234
1057	253	MappedLocalClaim	http://wso2.org/claims/role	-1234
1058	253	DisplayName	Roles	-1234
1059	254	Description	Email Addresses	-1234
1060	254	DisplayOrder	3	-1234
1061	254	Required	true	-1234
1062	254	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
1063	254	SupportedByDefault	true	-1234
1064	254	MappedLocalClaim	http://wso2.org/claims/emailaddress	-1234
1065	254	DisplayName	Emails	-1234
1066	255	Description	Work Email	-1234
1067	255	DisplayOrder	5	-1234
1068	255	SupportedByDefault	true	-1234
1069	255	MappedLocalClaim	http://wso2.org/claims/emails.work	-1234
1070	255	DisplayName	Emails - Work Email	-1234
1071	256	Description	End-User's preferred telephone number. For example, +1 (425) 555-1212 or +56 (2) 687 2400., +1 (604) 555-1234;ext=5678.	-1234
1072	256	MappedLocalClaim	http://wso2.org/claims/telephone	-1234
1073	256	DisplayName	Phone Number	-1234
1074	257	Description	Gender	-1234
1075	257	DisplayOrder	1	-1234
1076	257	Required	true	-1234
1077	257	SupportedByDefault	true	-1234
1078	257	MappedLocalClaim	http://wso2.org/claims/gender	-1234
1079	257	DisplayName	Gender	-1234
1080	258	Description	Claim to store newly updated email address until the new email address is verified	-1234
1081	258	DisplayOrder	1	-1234
1082	258	Required	true	-1234
1083	258	SupportedByDefault	true	-1234
1084	258	MappedLocalClaim	http://wso2.org/claims/identity/emailaddress.pendingValue	-1234
1085	258	DisplayName	Verification Pending Email	-1234
1086	259	Description	Id	-1234
1087	259	DisplayOrder	1	-1234
1088	259	Required	true	-1234
1089	259	SupportedByDefault	true	-1234
1090	259	MappedLocalClaim	http://wso2.org/claims/userid	-1234
1091	259	DisplayName	Id	-1234
1092	260	Description	Nick Name	-1234
1093	260	DisplayOrder	2	-1234
1094	260	Required	true	-1234
1095	260	SupportedByDefault	true	-1234
1171	275	Description	Local	1
1096	260	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
1097	260	DisplayName	Nick Name	-1234
1098	261	Description	Organization -department	-1234
1099	261	DisplayOrder	1	-1234
1100	261	Required	true	-1234
1101	261	SupportedByDefault	true	-1234
1102	261	MappedLocalClaim	http://wso2.org/claims/department	-1234
1103	261	DisplayName	Organization -department	-1234
1104	262	Description	Home Phone	-1234
1105	262	DisplayOrder	5	-1234
1106	262	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
1107	262	SupportedByDefault	true	-1234
1108	262	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	-1234
1109	262	DisplayName	Phone Numbers - Home Phone Number	-1234
1110	263	Description	End-User's birthday, represented as an ISO 8601:2004 [ISO8601-2004] YYYY-MM-DD format. The year MAY be 0000, indicating that it is omitted. To represent only the year, YYYY format is allowed.	-1234
1111	263	MappedLocalClaim	http://wso2.org/claims/dob	-1234
1112	263	DisplayName	Birth Date	-1234
1113	264	Description	Current Address	-1234
1114	264	DisplayOrder	1	-1234
1115	264	Required	true	-1234
1116	264	SupportedByDefault	true	-1234
1117	264	MappedLocalClaim	http://wso2.org/claims/addresses	-1234
1118	264	DisplayName	Current Address	-1234
1119	265	Description	Standard Industrial Classification	-1234
1120	265	DisplayOrder	1	-1234
1121	265	Required	true	-1234
1122	265	SupportedByDefault	true	-1234
1123	265	MappedLocalClaim	http://wso2.org/claims/nickname	-1234
1124	265	DisplayName	Standard Industrial Classification	-1234
1125	266	Description	Given Name	-1234
1126	266	DisplayOrder	1	-1234
1127	266	Required	true	-1234
1128	266	SupportedByDefault	true	-1234
1129	266	MappedLocalClaim	http://wso2.org/claims/givenname	-1234
1130	266	DisplayName	Name - Given Name	-1234
1131	267	Description	Email Address	-1234
1132	267	Required	true	-1234
1133	267	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	-1234
1134	267	SupportedByDefault	true	-1234
1135	267	MappedLocalClaim	http://wso2.org/claims/emailaddress	-1234
1136	267	DisplayName	Email	-1234
1137	268	Description	Mobile Number	-1234
1138	268	DisplayOrder	5	-1234
1139	268	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	-1234
1140	268	SupportedByDefault	true	-1234
1141	268	MappedLocalClaim	http://wso2.org/claims/mobile	-1234
1142	268	DisplayName	Phone Numbers - Mobile Number	-1234
1143	269	Description	Country	-1234
1144	269	SupportedByDefault	true	-1234
1145	269	MappedLocalClaim	http://wso2.org/claims/country	-1234
1146	269	DisplayName	Country	-1234
1147	270	Description	Date of Birth	-1234
1148	270	SupportedByDefault	true	-1234
1149	270	MappedLocalClaim	http://wso2.org/claims/dob	-1234
1150	270	DisplayName	DOB	-1234
1151	271	Description	Time the End-User's information was last updated. Its value is a JSON number representing the number of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time.	-1234
1152	271	MappedLocalClaim	http://wso2.org/claims/modified	-1234
1153	271	DisplayName	Updated At	-1234
1154	272	Description	Profile URL	-1234
1155	272	DisplayOrder	2	-1234
1156	272	Required	true	-1234
1157	272	SupportedByDefault	true	-1234
1158	272	MappedLocalClaim	http://wso2.org/claims/url	-1234
1159	272	DisplayName	Profile URL	-1234
1160	273	Description	Formatted Name	-1234
1161	273	DisplayOrder	2	-1234
1162	273	Required	true	-1234
1163	273	SupportedByDefault	true	-1234
1164	273	MappedLocalClaim	http://wso2.org/claims/formattedName	-1234
1165	273	DisplayName	Name - Formatted Name	-1234
1166	274	Description	Fax Number	-1234
1167	274	DisplayOrder	5	-1234
1168	274	SupportedByDefault	true	-1234
1169	274	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.fax	-1234
1170	274	DisplayName	Phone Numbers - Fax Number	-1234
1172	275	DisplayName	Local	1
1173	276	Description	Locality	1
1174	276	DisplayName	Locality	1
1175	277	Description	Country	1
1176	277	DisplayOrder	5	1
1177	277	SupportedByDefault	true	1
1178	277	DisplayName	Country	1
1179	278	Description	Display Name	1
1180	278	DisplayName	Display Name	1
1181	279	Description	Failed Lockout Count	1
1182	279	DisplayName	Failed Lockout Count	1
1183	280	Description	Mobile	1
1184	280	DisplayOrder	8	1
1185	280	SupportedByDefault	true	1
1186	280	DisplayName	Mobile	1
1187	281	Description	Nick Name	1
1188	281	DisplayName	Nick Name	1
1189	282	Description	Locked Reason	1
1190	282	DisplayName	Locked Reason	1
1191	283	Description	Full Name	1
1192	283	DisplayName	Full Name	1
1193	284	Description	Honoric Prefix	1
1194	284	DisplayName	Name - Honoric Prefix	1
1195	285	Description	IM - Skype	1
1196	285	DisplayName	IM - Skype	1
1197	286	Description	Address	1
1198	286	DisplayName	Address	1
1199	287	Description	Region	1
1200	287	DisplayName	Region	1
1201	288	Description	Middle Name	1
1202	288	DisplayName	Middle Name	1
1203	289	Description	Last Logon Time	1
1204	289	DisplayName	Last Logon	1
1205	290	Description	Username	1
1206	290	DisplayName	Username	1
1207	291	Description	Preferred Language	1
1208	291	DisplayName	Preferred Language	1
1209	292	Description	Extended External ID	1
1210	292	DisplayName	Extended External ID	1
1211	293	Description	Time Zone	1
1212	293	DisplayName	Time Zone	1
1213	294	Description	Last Name	1
1214	294	DisplayOrder	2	1
1215	294	Required	true	1
1216	294	SupportedByDefault	true	1
1217	294	DisplayName	Last Name	1
1218	295	Description	Last Login Time	1
1219	295	DisplayName	Last Login	1
1220	296	Description	Preferred Notification Channel	1
1221	296	DisplayName	Preferred Channel	1
1222	297	Description	Cost Center	1
1223	297	DisplayName	Cost Center	1
1224	298	Description	Location	1
1225	298	DisplayName	Location	1
1226	299	Description	Challenge Question	1
1227	299	DisplayName	Challenge Question	1
1228	300	Description	Claim to disable EmailOTP	1
1229	300	DisplayName	Disable EmailOTP	1
1230	301	Description	Account Locked	1
1231	301	DisplayName	Account Locked	1
1232	302	Description	Other Email	1
1233	302	DisplayName	Emails - Other Email	1
1234	303	ReadOnly	true	1
1235	303	Description	Department	1
1236	303	SupportedByDefault	true	1
1237	303	DisplayName	Department	1
1238	304	Description	Photo URL	1
1239	304	DisplayName	Photo URIL	1
1240	305	Description	IM	1
1241	305	DisplayOrder	9	1
1242	305	SupportedByDefault	true	1
1243	305	DisplayName	IM	1
1244	306	Description	Postal Code	1
1245	306	DisplayName	Postal Code	1
1246	307	Description	Email Address	1
1247	307	DisplayOrder	6	1
1248	307	Required	true	1
1249	307	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
1250	307	SupportedByDefault	true	1
1251	307	DisplayName	Email	1
1252	308	Description	Organization	1
1253	308	DisplayOrder	3	1
1254	308	SupportedByDefault	true	1
1255	308	DisplayName	Organization	1
1256	309	Description	Work Email	1
1257	309	DisplayName	Emails - Work Email	1
1258	310	Description	URL	1
1259	310	DisplayOrder	10	1
1260	310	SupportedByDefault	true	1
1261	310	DisplayName	URL	1
1262	311	Description	Address - Locality	1
1263	311	DisplayName	Address - Locality	1
1264	312	Description	Unlock Time	1
1265	312	DisplayName	Unlock Time	1
1266	313	Description	Work Phone	1
1267	313	DisplayName	Phone Numbers - Work Phone Number	1
1268	314	Description	Honoric Suffix	1
1269	314	DisplayName	Name - Honoric Suffix	1
1270	315	Description	Phone Verified	1
1271	315	DisplayName	Phone Verified	1
1272	316	Description	Primary Challenge Question	1
1273	316	DisplayName	Primary Challenge Question	1
1274	317	Description	Address - Formatted	1
1275	317	DisplayName	Address - Formatted	1
1276	318	Description	Claim to store the secret key	1
1277	318	DisplayName	Secret Key	1
1278	319	Description	Temporary claim to invoke email verified feature	1
1279	319	DisplayName	Verify Email	1
1280	320	ReadOnly	true	1
1281	320	Description	Unique ID of the user used in external systems	1
1282	320	DisplayName	External User ID	1
1283	321	Description	Challenge Question1	1
1284	321	DisplayName	Challenge Question1	1
1285	322	Description	Fax Number	1
1286	322	DisplayName	Phone Numbers - Fax Number	1
1287	323	ReadOnly	true	1
1288	323	Description	Last Modified timestamp of the user	1
1289	323	DisplayName	Last Modified Time	1
1290	324	Description	Formatted Name	1
1291	324	DisplayName	Name - Formatted Name	1
1292	325	Description	Claim to disable SMSOTP	1
1293	325	DisplayName	Disable SMSOTP	1
1294	326	Description	Temporary claim to invoke email ask Password feature	1
1295	326	DisplayName	Ask Password	1
1296	327	Description	Challenge Question2	1
1297	327	DisplayName	Challenge Question2	1
1298	328	Description	Home Phone	1
1299	328	DisplayName	Phone Numbers - Home Phone Number	1
1300	329	Description	Number of consecutive failed attempts done for password recovery	1
1301	329	DisplayName	Failed Password Recovery Attempts	1
1302	330	ReadOnly	true	1
1303	330	Description	Unique ID of the user	1
1304	330	DisplayName	User ID	1
1305	331	Description	Address - Street	1
1306	331	DisplayOrder	5	1
1307	331	DisplayName	Address - Street	1
1308	332	Description	Telephone	1
1309	332	DisplayOrder	7	1
1310	332	SupportedByDefault	true	1
1311	332	DisplayName	Telephone	1
1312	333	ReadOnly	true	1
1313	333	Description	Claim to store newly updated email address until the new email address is verified.	1
1314	333	DisplayName	Verification Pending Email	1
1315	334	Description	Account Disabled	1
1430	374	DisplayOrder	1	1
1316	334	DisplayName	Account Disabled	1
1317	335	Description	First Name	1
1318	335	DisplayOrder	1	1
1319	335	Required	true	1
1320	335	SupportedByDefault	true	1
1321	335	DisplayName	First Name	1
1322	336	Description	Extended Display Name	1
1323	336	DisplayName	Extended Display Name	1
1324	337	Description	Failed Login Attempts	1
1325	337	DisplayName	Failed Login Attempts	1
1326	338	Description	IM - Gtalk	1
1327	338	DisplayName	IM - Gtalk	1
1328	339	Description	Groups	1
1329	339	DisplayName	Groups	1
1330	340	Description	X509Certificates	1
1331	340	DisplayName	X509Certificates	1
1332	341	Description	Photo	1
1333	341	DisplayName	Photo	1
1334	342	Description	Entitlements	1
1335	342	DisplayName	Entitlements	1
1336	343	Description	Last Password Update Time	1
1337	343	DisplayName	Last Password Update	1
1338	344	Description	Failed Attempts Before Success	1
1339	344	DisplayName	Failed Attempts Before Success	1
1340	345	Description	Phone Numbers	1
1341	345	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
1342	345	DisplayName	Phone Numbers	1
1343	346	Description	Birth Date	1
1344	346	DisplayName	Birth Date	1
1345	347	Description	Other Phone Number	1
1346	347	DisplayName	Phone Numbers - Other	1
1347	348	Description	User Principal	1
1348	348	DisplayName	User Principal	1
1349	349	Description	Home Email	1
1350	349	DisplayName	Emails - Home Email	1
1351	350	Description	State	1
1352	350	DisplayName	State	1
1353	351	Description	One Time Password	1
1354	351	DisplayName	One Time Password	1
1355	352	Description	Status of the account	1
1356	352	DisplayName	Active	1
1357	353	Description	Resource Type	1
1358	353	DisplayName	Resource Type	1
1359	354	Description	Email Verified	1
1360	354	DisplayName	Email Verified	1
1361	355	Description	User Type	1
1362	355	DisplayName	User Type	1
1363	356	Description	Other Phone	1
1364	356	DisplayName	Other Phone	1
1365	357	Description	Photo - Thumbnail	1
1366	357	DisplayName	Photo - Thumbnail	1
1367	358	Description	Temporary claim to invoke email force password feature	1
1368	358	DisplayName	Force Password Reset	1
1369	359	Description	Title	1
1370	359	DisplayName	Title	1
1371	360	Description	Extended Ref	1
1372	360	DisplayName	Extended Ref	1
1373	361	ReadOnly	true	1
1374	361	Description	Created timestamp of the user	1
1375	361	DisplayName	Created Time	1
1376	362	Description	Gender	1
1377	362	DisplayName	Gender	1
1378	363	ReadOnly	true	1
1379	363	Description	Role	1
1380	363	SupportedByDefault	true	1
1381	363	DisplayName	Role	1
1382	364	ReadOnly	true	1
1383	364	Description	Account State	1
1384	364	DisplayName	Account State	1
1385	365	Description	Pager Number	1
1386	365	DisplayName	Phone Numbers - Pager Number	1
1387	366	ReadOnly	true	1
1388	366	Description	End-User's gender. Values defined by this specification are female and male. Other values MAY be used when neither of the defined values are applicable.	1
1389	366	SupportedByDefault	true	1
1390	366	MappedLocalClaim	http://wso2.org/claims/gender	1
1391	366	DisplayName	Gender	1
1392	367	Description	Street Address	1
1393	367	MappedLocalClaim	http://wso2.org/claims/streetaddress	1
1394	367	DisplayName	Street Address	1
1395	368	Description	Time Zone	1
1396	368	DisplayOrder	2	1
1397	368	Required	true	1
1398	368	SupportedByDefault	true	1
1399	368	MappedLocalClaim	http://wso2.org/claims/timeZone	1
1400	368	DisplayName	Time Zone	1
1401	369	Description	Manager - home	1
1402	369	DisplayOrder	1	1
1403	369	Required	true	1
1404	369	SupportedByDefault	true	1
1405	369	MappedLocalClaim	http://wso2.org/claims/gender	1
1406	369	DisplayName	Manager - home	1
1407	370	Description	Photo	1
1408	370	DisplayOrder	5	1
1409	370	SupportedByDefault	true	1
1410	370	MappedLocalClaim	http://wso2.org/claims/photourl	1
1411	370	DisplayName	Photo	1
1412	371	Description	Date of Birth	1
1413	371	DisplayOrder	6	1
1414	371	SupportedByDefault	true	1
1415	371	MappedLocalClaim	http://wso2.org/claims/dob	1
1416	371	DisplayName	DOB	1
1417	372	Description	Locality	1
1418	372	DisplayOrder	2	1
1419	372	Required	true	1
1420	372	SupportedByDefault	true	1
1421	372	MappedLocalClaim	http://wso2.org/claims/local	1
1422	372	DisplayName	Locality	1
1423	373	Description	Legal Person Name	1
1424	373	DisplayOrder	1	1
1425	373	Required	true	1
1426	373	SupportedByDefault	true	1
1427	373	MappedLocalClaim	http://wso2.org/claims/extendedDisplayName	1
1428	373	DisplayName	Legal Person Name	1
1429	374	Description	Given Name	1
1431	374	Required	true	1
1432	374	SupportedByDefault	true	1
1433	374	MappedLocalClaim	http://wso2.org/claims/givenname	1
1434	374	DisplayName	Name - Given Name	1
1435	375	Description	Place of Birth	1
1436	375	DisplayOrder	1	1
1437	375	Required	true	1
1438	375	SupportedByDefault	true	1
1439	375	MappedLocalClaim	http://wso2.org/claims/country	1
1440	375	DisplayName	Place of Birth	1
1441	376	Description	Phone Numbers	1
1442	376	DisplayOrder	3	1
1443	376	Required	true	1
1444	376	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
1445	376	SupportedByDefault	true	1
1446	376	MappedLocalClaim	http://wso2.org/claims/phoneNumbers	1
1447	376	DisplayName	Phone Numbers	1
1448	377	Description	Organization -division	1
1449	377	DisplayOrder	1	1
1450	377	Required	true	1
1451	377	SupportedByDefault	true	1
1452	377	MappedLocalClaim	http://wso2.org/claims/organization	1
1453	377	DisplayName	Organization -division	1
1454	378	Description	Postalcode	1
1455	378	DisplayOrder	4	1
1456	378	SupportedByDefault	true	1
1457	378	MappedLocalClaim	http://wso2.org/claims/postalcode	1
1458	378	DisplayName	Postalcode	1
1459	379	Description	Economic Operator Registration and Identification	1
1460	379	DisplayOrder	1	1
1461	379	Required	true	1
1462	379	SupportedByDefault	true	1
1463	379	MappedLocalClaim	http://wso2.org/claims/department	1
1464	379	DisplayName	Economic Operator Registration and Identification	1
1465	380	Description	Email Addresses	1
1466	380	DisplayOrder	5	1
1467	380	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
1468	380	SupportedByDefault	true	1
1469	380	MappedLocalClaim	http://wso2.org/claims/emailaddress	1
1470	380	DisplayName	Emails	1
1471	381	Description	Phone Numbers	1
1472	381	DisplayOrder	5	1
1473	381	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
1474	381	SupportedByDefault	true	1
1475	381	MappedLocalClaim	http://wso2.org/claims/phoneNumbers	1
1476	381	DisplayName	Phone Numbers	1
1477	382	Description	Family Name	1
1478	382	DisplayOrder	2	1
1479	382	Required	true	1
1480	382	SupportedByDefault	true	1
1481	382	MappedLocalClaim	http://wso2.org/claims/lastname	1
1482	382	DisplayName	Name - Family Name	1
1483	383	Description	Photo	1
1484	383	DisplayOrder	5	1
1485	383	SupportedByDefault	true	1
1486	383	MappedLocalClaim	http://wso2.org/claims/photourl	1
1487	383	DisplayName	Photo	1
1488	384	Description	Photo - Thumbnail	1
1489	384	DisplayOrder	5	1
1490	384	SupportedByDefault	true	1
1491	384	MappedLocalClaim	http://wso2.org/claims/thumbnail	1
1492	384	DisplayName	Photo - Thumbnail	1
1493	385	Description	Country	1
1494	385	DisplayOrder	5	1
1495	385	SupportedByDefault	true	1
1496	385	MappedLocalClaim	http://wso2.org/claims/country	1
1497	385	DisplayName	Country	1
1498	386	Description	True if the End-User's e-mail address has been verified; otherwise false. 	1
1499	386	MappedLocalClaim	http://wso2.org/claims/identity/emailVerified	1
1500	386	DisplayName	Email Verified	1
1501	387	Description	Title	1
1502	387	DisplayOrder	2	1
1503	387	Required	true	1
1504	387	SupportedByDefault	true	1
1505	387	MappedLocalClaim	http://wso2.org/claims/title	1
1506	387	DisplayName	Title	1
1507	388	Description	User Type	1
1508	388	DisplayOrder	2	1
1509	388	Required	true	1
1510	388	SupportedByDefault	true	1
1511	388	MappedLocalClaim	http://wso2.org/claims/userType	1
1512	388	DisplayName	User Type	1
1513	389	Description	Home Email	1
1514	389	DisplayOrder	5	1
1515	389	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
1516	389	SupportedByDefault	true	1
1517	389	MappedLocalClaim	http://wso2.org/claims/emails.home	1
1518	389	DisplayName	Emails - Home Email	1
1519	390	Description	Active	1
1520	390	DisplayOrder	2	1
1521	390	Required	true	1
1522	390	SupportedByDefault	true	1
1523	390	MappedLocalClaim	http://wso2.org/claims/active	1
1524	390	DisplayName	Active	1
1525	391	Description	Pager Number	1
1526	391	DisplayOrder	5	1
1527	391	SupportedByDefault	true	1
1528	391	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.pager	1
1529	391	DisplayName	Phone Numbers - Pager Number	1
1530	392	Description	String from zoneinfo time zone database representing the End-User's time zone. For example, Europe/Paris or America/Los_Angeles.	1
1531	392	MappedLocalClaim	http://wso2.org/claims/timeZone	1
1532	392	DisplayName	Zone Info	1
1533	393	Description	Honoric Suffix	1
1534	393	DisplayOrder	2	1
1535	393	Required	true	1
1536	393	SupportedByDefault	true	1
1537	393	MappedLocalClaim	http://wso2.org/claims/honorificSuffix	1
1538	393	DisplayName	Name - Honoric Suffix	1
1539	394	Description	Photo	1
1540	394	DisplayOrder	5	1
1541	394	SupportedByDefault	true	1
1542	394	MappedLocalClaim	http://wso2.org/claims/photos	1
1543	394	DisplayName	Photo	1
1544	395	Description	Display Name	1
1545	395	DisplayOrder	2	1
1546	395	Required	true	1
1547	395	SupportedByDefault	true	1
1548	395	MappedLocalClaim	http://wso2.org/claims/displayName	1
1549	395	DisplayName	Display Name	1
1550	396	Description	URL of the End-User's profile picture. This URL MUST refer to an image file (for example, a PNG, JPEG, or GIF image file)	1
1551	396	DisplayOrder	9	1
1552	396	SupportedByDefault	true	1
1553	396	MappedLocalClaim	http://wso2.org/claims/photourl	1
1554	396	DisplayName	Picture	1
1555	397	Description	Meta - Last Modified	1
1556	397	DisplayOrder	1	1
1557	397	Required	true	1
1558	397	SupportedByDefault	true	1
1559	397	MappedLocalClaim	http://wso2.org/claims/modified	1
1560	397	DisplayName	Meta - Last Modified	1
1561	398	Description	X509Certificates	1
1562	398	DisplayOrder	5	1
1563	398	SupportedByDefault	true	1
1564	398	MappedLocalClaim	http://wso2.org/claims/x509Certificates	1
1565	398	DisplayName	X509Certificates	1
1566	399	Description	Formatted Name	1
1567	399	DisplayOrder	2	1
1568	399	Required	true	1
1569	399	SupportedByDefault	true	1
1570	399	MappedLocalClaim	http://wso2.org/claims/formattedName	1
1571	399	DisplayName	Name - Formatted Name	1
1572	400	Description	Groups	1
1573	400	DisplayOrder	5	1
1574	400	SupportedByDefault	true	1
1575	400	MappedLocalClaim	http://wso2.org/claims/groups	1
1576	400	DisplayName	Groups	1
1577	401	Description	Home Phone	1
1578	401	DisplayOrder	5	1
1579	401	SupportedByDefault	true	1
1580	401	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	1
1581	401	DisplayName	Phone Numbers - Home Phone Number	1
1582	402	Description	External Id	1
1583	402	DisplayOrder	1	1
1584	402	Required	true	1
1585	402	SupportedByDefault	true	1
1586	402	MappedLocalClaim	http://wso2.org/claims/externalid	1
1587	402	DisplayName	External Id	1
1588	403	Description	Roles	1
1589	403	DisplayOrder	5	1
1590	403	SupportedByDefault	true	1
1591	403	MappedLocalClaim	http://wso2.org/claims/role	1
1592	403	DisplayName	Roles	1
1593	404	Description	Entitlements	1
1594	404	DisplayOrder	5	1
1595	404	SupportedByDefault	true	1
1596	404	MappedLocalClaim	http://wso2.org/claims/entitlements	1
1597	404	DisplayName	Entitlements	1
1598	405	Description	Locality	1
1599	405	MappedLocalClaim	http://wso2.org/claims/locality	1
1600	405	DisplayName	Locality	1
1601	406	Description	Time Zone	1
1602	406	DisplayOrder	9	1
1603	406	SupportedByDefault	true	1
1604	406	MappedLocalClaim	http://wso2.org/claims/timeZone	1
1605	406	DisplayName	Time Zone	1
1606	407	Description	Address	1
1607	407	DisplayOrder	5	1
1608	407	SupportedByDefault	true	1
1609	407	MappedLocalClaim	http://wso2.org/claims/addresses	1
1610	407	DisplayName	Address	1
1611	408	Description	Current Family Name	1
1612	408	DisplayOrder	1	1
1613	408	Required	true	1
1614	408	SupportedByDefault	true	1
1615	408	MappedLocalClaim	http://wso2.org/claims/lastname	1
1616	408	DisplayName	Current Family Name	1
1617	409	Description	Shorthand name by which the End-User wishes to be referred to at the RP, such as janedoe or j.doe.	1
1618	409	DisplayOrder	7	1
1619	409	SupportedByDefault	true	1
1620	409	MappedLocalClaim	http://wso2.org/claims/displayName	1
1621	409	DisplayName	Preferred Username	1
1622	410	Description	Middle name(s) of the End-User. Note that in some cultures, people can have multiple middle names; all can be present, with the names being separated by space characters. Also note that in some cultures, middle names are not used.	1
1623	410	DisplayOrder	5	1
1624	410	SupportedByDefault	true	1
1625	410	MappedLocalClaim	http://wso2.org/claims/middleName	1
1626	410	DisplayName	Middle Name	1
1627	411	Description	Employee Number	1
1628	411	DisplayOrder	1	1
1629	411	Required	true	1
1630	411	SupportedByDefault	true	1
1631	411	MappedLocalClaim	http://wso2.org/claims/extendedExternalId	1
1632	411	DisplayName	Employee Number	1
1633	412	Description	External Id	1
1634	412	DisplayOrder	1	1
1635	412	Required	true	1
1636	412	SupportedByDefault	true	1
1637	412	MappedLocalClaim	http://wso2.org/claims/externalid	1
1638	412	DisplayName	External Id	1
1639	413	Description	Language	1
1640	413	SupportedByDefault	true	1
1641	413	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	1
1642	413	DisplayName	Language	1
1643	414	Description	Address - Formatted	1
1644	414	DisplayOrder	5	1
1645	414	SupportedByDefault	true	1
1646	414	MappedLocalClaim	http://wso2.org/claims/addresses.formatted	1
1647	414	DisplayName	Address - Formatted	1
1648	415	Description	User Name	1
1649	415	DisplayOrder	2	1
1650	415	Required	true	1
1651	415	SupportedByDefault	true	1
1652	415	MappedLocalClaim	http://wso2.org/claims/username	1
1653	415	DisplayName	User Name	1
1654	416	Description	Time Zone	1
1655	416	DisplayOrder	2	1
1656	416	Required	true	1
1657	416	SupportedByDefault	true	1
1658	416	MappedLocalClaim	http://wso2.org/claims/timeZone	1
1659	416	DisplayName	Time Zone	1
1660	417	Description	Honoric Suffix	1
1661	417	DisplayOrder	2	1
1662	417	Required	true	1
1663	417	SupportedByDefault	true	1
1664	417	MappedLocalClaim	http://wso2.org/claims/honorificSuffix	1
1665	417	DisplayName	Name - Honoric Suffix	1
1666	418	Description	State	1
1667	418	MappedLocalClaim	http://wso2.org/claims/stateorprovince	1
1668	418	DisplayName	State	1
1669	419	Description	Legal Person Identifier	1
1670	419	DisplayOrder	1	1
1671	419	Required	true	1
1672	419	SupportedByDefault	true	1
1673	419	MappedLocalClaim	http://wso2.org/claims/extendedExternalId	1
1674	419	DisplayName	Legal Person Identifier	1
1675	420	Description	Gender	1
1676	420	DisplayOrder	8	1
1677	420	SupportedByDefault	true	1
1678	420	MappedLocalClaim	http://wso2.org/claims/gender	1
1679	420	DisplayName	Gender	1
1680	421	Description	Language	1
1681	421	DisplayOrder	7	1
1682	421	SupportedByDefault	true	1
1683	421	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	1
1684	421	DisplayName	Language	1
1685	422	Description	Identifier for the End-User at the Issuer	1
1686	422	DisplayOrder	1	1
1687	422	Required	true	1
1688	422	SupportedByDefault	true	1
1689	422	MappedLocalClaim	http://wso2.org/claims/username	1
1690	422	DisplayName	Subject	1
1691	423	Description	User Name	1
1692	423	DisplayOrder	2	1
1693	423	Required	true	1
1694	423	SupportedByDefault	true	1
1695	423	MappedLocalClaim	http://wso2.org/claims/username	1
1696	423	DisplayName	User Name	1
1697	424	Description	Nick Name	1
1698	424	MappedLocalClaim	http://wso2.org/claims/nickname	1
1699	424	DisplayName	Nick Name	1
1700	425	Description	Casual name of the End-User that may or may not be the same as the given_name. For instance, a nickname value of Mike might be returned alongside a given_name value of Michael.	1
1701	425	DisplayOrder	6	1
1702	425	Required	true	1
1703	425	SupportedByDefault	true	1
1704	425	MappedLocalClaim	http://wso2.org/claims/nickname	1
1705	425	DisplayName	Nickname	1
1706	426	Description	Tax Reference	1
1707	426	DisplayOrder	1	1
1708	426	Required	true	1
1709	426	SupportedByDefault	true	1
1710	426	MappedLocalClaim	http://wso2.org/claims/postalcode	1
1711	426	DisplayName	Tax Reference	1
1712	427	Description	Postalcode	1
1713	427	SupportedByDefault	true	1
1714	427	MappedLocalClaim	http://wso2.org/claims/postalcode	1
1715	427	DisplayName	Postalcode	1
1716	428	Description	Meta - Location	1
1717	428	DisplayOrder	1	1
1718	428	Required	true	1
1719	428	SupportedByDefault	true	1
1720	428	MappedLocalClaim	http://wso2.org/claims/resourceType	1
1721	428	DisplayName	Meta - Location	1
1722	429	Description	IM - Skype	1
1723	429	DisplayOrder	5	1
1724	429	SupportedByDefault	true	1
1725	429	MappedLocalClaim	http://wso2.org/claims/skype	1
1726	429	DisplayName	IM - Skype	1
1727	430	Description	Preferred Language	1
1728	430	DisplayOrder	2	1
1729	430	Required	true	1
1730	430	SupportedByDefault	true	1
1731	430	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	1
1732	430	DisplayName	Preferred Language	1
1733	431	Description	Other Phone Number	1
1734	431	DisplayOrder	5	1
1735	431	SupportedByDefault	true	1
1736	431	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.other	1
1737	431	DisplayName	Phone Numbers - Other	1
1738	432	Description	Current Given Name	1
1739	432	DisplayOrder	1	1
1740	432	Required	true	1
1741	432	SupportedByDefault	true	1
1742	432	MappedLocalClaim	http://wso2.org/claims/givenname	1
1743	432	DisplayName	Current Given Name	1
1744	433	Description	Middle Name	1
1745	433	DisplayOrder	2	1
1746	433	Required	true	1
1747	433	SupportedByDefault	true	1
1748	433	MappedLocalClaim	http://wso2.org/claims/middleName	1
1749	433	DisplayName	Name - Middle Name	1
1750	434	Description	Address - Work	1
1751	434	DisplayOrder	5	1
1752	434	SupportedByDefault	true	1
1753	434	MappedLocalClaim	http://wso2.org/claims/region	1
1754	434	DisplayName	Address - Work	1
1755	435	Description	Mobile	1
1756	435	MappedLocalClaim	http://wso2.org/claims/mobile	1
1757	435	DisplayName	Mobile	1
1758	436	Description	Full Name	1
1759	436	DisplayOrder	2	1
1760	436	Required	true	1
1761	436	SupportedByDefault	true	1
1762	436	MappedLocalClaim	http://wso2.org/claims/fullname	1
1763	436	DisplayName	Full Name	1
1764	437	Description	Last Name	1
1765	437	Required	true	1
1766	437	SupportedByDefault	true	1
1767	437	MappedLocalClaim	http://wso2.org/claims/lastname	1
1768	437	DisplayName	Last Name	1
1769	438	Description	First Name	1
1770	438	Required	true	1
1771	438	SupportedByDefault	true	1
1772	438	MappedLocalClaim	http://wso2.org/claims/givenname	1
1773	438	DisplayName	First Name	1
1774	439	Description	Email Address	1
1775	439	DisplayOrder	3	1
1776	439	Required	true	1
1777	439	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
1778	439	SupportedByDefault	true	1
1779	439	MappedLocalClaim	http://wso2.org/claims/emailaddress	1
1780	439	DisplayName	Email	1
1781	440	Description	Gender	1
1782	440	MappedLocalClaim	http://wso2.org/claims/gender	1
1783	440	DisplayName	Gender	1
1784	441	Description	Address - Region	1
1785	441	DisplayOrder	5	1
1786	441	SupportedByDefault	true	1
1787	441	MappedLocalClaim	http://wso2.org/claims/region	1
1788	441	DisplayName	Address - Region	1
1789	442	Description	Meta - Location	1
1790	442	DisplayOrder	1	1
1791	442	Required	true	1
1792	442	SupportedByDefault	true	1
1793	442	MappedLocalClaim	http://wso2.org/claims/location	1
1794	442	DisplayName	Meta - Location	1
1795	443	Description	Home Email	1
1796	443	DisplayOrder	5	1
1797	443	SupportedByDefault	true	1
1798	443	MappedLocalClaim	http://wso2.org/claims/emails.home	1
1799	443	DisplayName	Emails - Home Email	1
1800	444	Description	Meta - Last Modified	1
1801	444	DisplayOrder	1	1
1802	444	Required	true	1
1803	444	SupportedByDefault	true	1
1804	444	MappedLocalClaim	http://wso2.org/claims/modified	1
1805	444	DisplayName	Meta - Last Modified	1
1806	445	Description	Profile URL	1
1807	445	DisplayOrder	2	1
1808	445	Required	true	1
1809	445	SupportedByDefault	true	1
1810	445	MappedLocalClaim	http://wso2.org/claims/url	1
1811	445	DisplayName	Profile URL	1
1812	446	Description	Date of Birth	1
1813	446	MappedLocalClaim	http://wso2.org/claims/dob	1
1814	446	DisplayName	DOB	1
1815	447	Description	Family Name	1
1816	447	DisplayOrder	2	1
1817	447	Required	true	1
1818	447	SupportedByDefault	true	1
1819	447	MappedLocalClaim	http://wso2.org/claims/lastname	1
1820	447	DisplayName	Name - Family Name	1
1821	448	Description	Meta - Version	1
1822	448	DisplayOrder	1	1
1823	448	Required	true	1
1824	448	SupportedByDefault	true	1
1825	448	MappedLocalClaim	http://wso2.org/claims/im	1
1826	448	DisplayName	Meta - Version	1
1827	449	Description	X509Certificates	1
1828	449	DisplayOrder	5	1
1829	449	SupportedByDefault	true	1
1830	449	MappedLocalClaim	http://wso2.org/claims/x509Certificates	1
1831	449	DisplayName	X509Certificates	1
1832	450	Description	IM	1
1833	450	DisplayOrder	5	1
1834	450	SupportedByDefault	true	1
1835	450	MappedLocalClaim	http://wso2.org/claims/im	1
1836	450	DisplayName	IMS	1
1837	451	Description	URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.	1
1838	451	DisplayOrder	8	1
1839	451	SupportedByDefault	true	1
1840	451	MappedLocalClaim	http://wso2.org/claims/url	1
1841	451	DisplayName	Profile	1
1842	452	Description	Meta - Created	1
1843	452	DisplayOrder	1	1
1844	452	Required	true	1
1845	452	SupportedByDefault	true	1
1846	452	MappedLocalClaim	http://wso2.org/claims/created	1
1847	452	DisplayName	Meta - Created	1
1848	453	Description	True if the End-User's phone number has been verified; otherwise false.	1
1849	453	MappedLocalClaim	http://wso2.org/claims/identity/phoneVerified	1
1850	453	DisplayName	Phone Number Verified	1
1851	454	Description	Legal Person Address	1
1852	454	DisplayOrder	1	1
1853	454	Required	true	1
1854	454	SupportedByDefault	true	1
1855	454	MappedLocalClaim	http://wso2.org/claims/addresses	1
1856	454	DisplayName	Legal Person Address	1
1857	455	Description	IM - Skype	1
1858	455	DisplayOrder	5	1
1859	455	SupportedByDefault	true	1
1860	455	MappedLocalClaim	http://wso2.org/claims/skype	1
1861	455	DisplayName	IM - Skype	1
1862	456	Description	Birth Name	1
1863	456	DisplayOrder	1	1
1864	456	Required	true	1
1865	456	SupportedByDefault	true	1
1866	456	MappedLocalClaim	http://wso2.org/claims/username	1
1867	456	DisplayName	Birth Name	1
1868	457	Description	Country	1
1869	457	SupportedByDefault	true	1
2081	500	DisplayOrder	4	1
1870	457	MappedLocalClaim	http://wso2.org/claims/country	1
1871	457	DisplayName	Country	1
1872	458	Description	LEI	1
1873	458	DisplayOrder	1	1
1874	458	Required	true	1
1875	458	SupportedByDefault	true	1
1876	458	MappedLocalClaim	http://wso2.org/claims/extendedRef	1
1877	458	DisplayName	LEI	1
1878	459	Description	Work Phone	1
1879	459	DisplayOrder	5	1
1880	459	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
1881	459	SupportedByDefault	true	1
1882	459	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.work	1
1883	459	DisplayName	Phone Numbers - Work Phone Number	1
1884	460	Description	Zip code or postal code component.	1
1885	460	MappedLocalClaim	http://wso2.org/claims/postalcode	1
1886	460	DisplayName	Postal Code	1
1887	461	Description	Work Email	1
1888	461	DisplayOrder	5	1
1889	461	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
1890	461	SupportedByDefault	true	1
1891	461	MappedLocalClaim	http://wso2.org/claims/emails.work	1
1892	461	DisplayName	Emails - Work Email	1
1893	462	Description	Address - Country	1
1894	462	DisplayOrder	5	1
1895	462	SupportedByDefault	true	1
1896	462	MappedLocalClaim	http://wso2.org/claims/country	1
1897	462	DisplayName	Address - Country	1
1898	463	Description	List of group names that have been assigned to the principal. This typically will require a mapping at the application container level to application deployment roles.	1
1899	463	DisplayOrder	12	1
1900	463	SupportedByDefault	true	1
1901	463	MappedLocalClaim	http://wso2.org/claims/role	1
1902	463	DisplayName	User Groups	1
1903	464	Description	Other Email	1
1904	464	DisplayOrder	5	1
1905	464	SupportedByDefault	true	1
1906	464	MappedLocalClaim	http://wso2.org/claims/emails.other	1
1907	464	DisplayName	Emails - Other Email	1
1908	465	Description	True if the End-User's phone number has been verified; otherwise false. 	1
1909	465	MappedLocalClaim	http://wso2.org/claims/addresses	1
1910	465	DisplayName	Address	1
1911	466	Description	Work Phone	1
1912	466	DisplayOrder	5	1
1913	466	SupportedByDefault	true	1
1914	466	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.work	1
1915	466	DisplayName	Phone Numbers - Work Phone Number	1
1916	467	Description	Last Name	1
1917	467	Required	true	1
1918	467	SupportedByDefault	true	1
1919	467	MappedLocalClaim	http://wso2.org/claims/lastname	1
1920	467	DisplayName	Last Name	1
1921	468	Description	PPID	1
1922	468	Required	true	1
1923	468	SupportedByDefault	true	1
1924	468	MappedLocalClaim	http://wso2.org/claims/im	1
1925	468	DisplayName	0	1
1926	469	Description	Mobile Number	1
1927	469	DisplayOrder	5	1
1928	469	SupportedByDefault	true	1
1929	469	MappedLocalClaim	http://wso2.org/claims/mobile	1
1930	469	DisplayName	Phone Numbers - Mobile Number	1
1931	470	Description	EU Identifier	1
1932	470	DisplayOrder	1	1
1933	470	Required	true	1
1934	470	SupportedByDefault	true	1
1935	470	MappedLocalClaim	http://wso2.org/claims/externalid	1
1936	470	DisplayName	EU Identifier	1
1937	471	Description	Active	1
1938	471	DisplayOrder	2	1
1939	471	Required	true	1
1940	471	SupportedByDefault	true	1
1941	471	MappedLocalClaim	http://wso2.org/claims/active	1
1942	471	DisplayName	Active	1
1943	472	Description	Country name component	1
1944	472	MappedLocalClaim	http://wso2.org/claims/country	1
1945	472	DisplayName	Country	1
1946	473	Description	Meta - Location	1
1947	473	DisplayOrder	1	1
1948	473	Required	true	1
1949	473	SupportedByDefault	true	1
1950	473	MappedLocalClaim	http://wso2.org/claims/location	1
1951	473	DisplayName	Meta - Location	1
1952	474	Description	Manager - home	1
1953	474	DisplayOrder	1	1
1954	474	Required	true	1
1955	474	SupportedByDefault	true	1
1956	474	MappedLocalClaim	http://wso2.org/claims/extendedRef	1
1957	474	DisplayName	Manager - home	1
1958	475	Description	Temporary claim to invoke email verified feature	1
1959	475	DisplayOrder	1	1
1960	475	Required	true	1
1961	475	SupportedByDefault	true	1
1962	475	MappedLocalClaim	http://wso2.org/claims/identity/verifyEmail	1
1963	475	DisplayName	Verify Email	1
1964	476	Description	Address	1
1965	476	DisplayOrder	5	1
1966	476	SupportedByDefault	true	1
1967	476	MappedLocalClaim	http://wso2.org/claims/addresses	1
1968	476	DisplayName	Address	1
1969	477	Description	Temporary claim to invoke email ask Password feature	1
1970	477	DisplayOrder	1	1
1971	477	Required	true	1
1972	477	SupportedByDefault	true	1
1973	477	MappedLocalClaim	http://wso2.org/claims/identity/askPassword	1
1974	477	DisplayName	Ask Password	1
1975	478	Description	Full mailing address, formatted for display or use on a mailing label. This field MAY contain multiple lines, separated by newlines.	1
1976	478	MappedLocalClaim	http://wso2.org/claims/addresses.formatted	1
1977	478	DisplayName	Address Formatted	1
1978	479	Description	Manager - home	1
1979	479	DisplayOrder	1	1
1980	479	Required	true	1
1981	479	SupportedByDefault	true	1
1982	479	MappedLocalClaim	http://wso2.org/claims/stateorprovince	1
1983	479	DisplayName	Manager - home	1
1984	480	Description	System for Exchange of Excise Data Identifier	1
1985	480	DisplayOrder	1	1
1986	480	Required	true	1
1987	480	SupportedByDefault	true	1
1988	480	MappedLocalClaim	http://wso2.org/claims/nickname	1
1989	480	DisplayName	System for Exchange of Excise Data Identifier	1
1990	481	Description	Postalcode	1
1991	481	SupportedByDefault	true	1
1992	481	MappedLocalClaim	http://wso2.org/claims/postalcode	1
1993	481	DisplayName	Postalcode	1
1994	482	Description	End-User's preferred e-mail address.	1
1995	482	MappedLocalClaim	http://wso2.org/claims/emailaddress	1
1996	482	DisplayName	Email	1
1997	483	Description	Photo - Thumbnail	1
1998	483	DisplayOrder	5	1
1999	483	SupportedByDefault	true	1
2000	483	MappedLocalClaim	http://wso2.org/claims/thumbnail	1
2001	483	DisplayName	Photo - Thumbnail	1
2002	484	Description	Nick Name	1
2003	484	DisplayOrder	1	1
2004	484	Required	true	1
2005	484	SupportedByDefault	true	1
2006	484	MappedLocalClaim	http://wso2.org/claims/nickname	1
2007	484	DisplayName	Nick Name	1
2008	485	Description	The user principal name	1
2009	485	DisplayOrder	11	1
2010	485	SupportedByDefault	true	1
2011	485	MappedLocalClaim	http://wso2.org/claims/userprincipal	1
2012	485	DisplayName	User Principal	1
2013	486	Description	Honoric Prefix	1
2014	486	DisplayOrder	2	1
2015	486	Required	true	1
2016	486	SupportedByDefault	true	1
2017	486	MappedLocalClaim	http://wso2.org/claims/honorificPrefix	1
2018	486	DisplayName	Name - Honoric Prefix	1
2019	487	Description	Locality	1
2020	487	DisplayOrder	2	1
2021	487	Required	true	1
2022	487	SupportedByDefault	true	1
2023	487	MappedLocalClaim	http://wso2.org/claims/local	1
2024	487	DisplayName	Locality	1
2025	488	Description	IM - Gtalk	1
2026	488	DisplayOrder	5	1
2027	488	SupportedByDefault	true	1
2028	488	MappedLocalClaim	http://wso2.org/claims/gtalk	1
2029	488	DisplayName	IM - Gtalk	1
2030	489	Description	Address - Postal Code	1
2031	489	DisplayOrder	5	1
2032	489	SupportedByDefault	true	1
2033	489	MappedLocalClaim	http://wso2.org/claims/postalcode	1
2034	489	DisplayName	Address - Postal Code	1
2035	490	Description	Person Identifier	1
2036	490	DisplayOrder	1	1
2037	490	Required	true	1
2038	490	SupportedByDefault	true	1
2039	490	MappedLocalClaim	http://wso2.org/claims/userid	1
2040	490	DisplayName	Person Identifier	1
2041	491	Description	Given name(s) or first name(s) of the End-User. Note that in some cultures, people can have multiple given names; all can be present, with the names being separated by space characters.	1
2042	491	DisplayOrder	3	1
2043	491	SupportedByDefault	true	1
2044	491	MappedLocalClaim	http://wso2.org/claims/givenname	1
2045	491	DisplayName	Given Name	1
2046	492	Description	First Name	1
2047	492	Required	true	1
2048	492	SupportedByDefault	true	1
2049	492	MappedLocalClaim	http://wso2.org/claims/givenname	1
2050	492	DisplayName	First Name	1
2051	493	Description	City or locality component.	1
2052	493	MappedLocalClaim	http://wso2.org/claims/locality	1
2053	493	DisplayName	Locality	1
2054	494	Description	Preferred Language	1
2055	494	DisplayOrder	2	1
2056	494	Required	true	1
2057	494	SupportedByDefault	true	1
2058	494	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	1
2059	494	DisplayName	Preferred Language	1
2060	495	Description	Gender	1
2061	495	SupportedByDefault	true	1
2062	495	MappedLocalClaim	http://wso2.org/claims/gender	1
2063	495	DisplayName	Gender	1
2064	496	Description	Time Zone	1
2065	496	MappedLocalClaim	http://wso2.org/claims/timeZone	1
2066	496	DisplayName	Time Zone	1
2067	497	Description	Title	1
2068	497	DisplayOrder	2	1
2069	497	Required	true	1
2070	497	SupportedByDefault	true	1
2071	497	MappedLocalClaim	http://wso2.org/claims/title	1
2072	497	DisplayName	Title	1
2073	498	Description	Home Phone	1
2074	498	SupportedByDefault	true	1
2075	498	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	1
2076	498	DisplayName	Home Phone	1
2077	499	Description	State, province, prefecture, or region component.	1
2078	499	MappedLocalClaim	http://wso2.org/claims/region	1
2079	499	DisplayName	One Time Password	1
2080	500	Description	Surname(s) or last name(s) of the End-User. Note that in some cultures, people can have multiple family names or no family name; all can be present, with the names being separated by space characters.	1
2082	500	SupportedByDefault	true	1
2083	500	MappedLocalClaim	http://wso2.org/claims/lastname	1
2084	500	DisplayName	Surname	1
2085	501	Description	Other Email	1
2086	501	DisplayOrder	5	1
2087	501	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
2088	501	SupportedByDefault	true	1
2089	501	MappedLocalClaim	http://wso2.org/claims/emails.other	1
2090	501	DisplayName	Emails - Other Email	1
2091	502	Description	Cost Center	1
2092	502	DisplayOrder	1	1
2093	502	Required	true	1
2094	502	SupportedByDefault	true	1
2095	502	MappedLocalClaim	http://wso2.org/claims/costCenter	1
2096	502	DisplayName	Cost Center	1
2097	503	Description	Other Phone Number	1
2098	503	DisplayOrder	5	1
2099	503	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
2100	503	SupportedByDefault	true	1
2101	503	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.other	1
2102	503	DisplayName	Phone Numbers - Other	1
2103	504	Description	Address - Street	1
2104	504	DisplayOrder	5	1
2105	504	SupportedByDefault	true	1
2106	504	MappedLocalClaim	http://wso2.org/claims/streetaddress	1
2107	504	DisplayName	Address - Street	1
2108	505	Description	Id	1
2109	505	DisplayOrder	1	1
2110	505	Required	true	1
2111	505	SupportedByDefault	true	1
2112	505	MappedLocalClaim	http://wso2.org/claims/userid	1
2113	505	DisplayName	Id	1
2114	506	Description	Photo	1
2115	506	DisplayOrder	5	1
2116	506	SupportedByDefault	true	1
2117	506	MappedLocalClaim	http://wso2.org/claims/photos	1
2118	506	DisplayName	Photo	1
2119	507	Description	Groups	1
2120	507	DisplayOrder	5	1
2121	507	SupportedByDefault	true	1
2122	507	MappedLocalClaim	http://wso2.org/claims/groups	1
2123	507	DisplayName	Groups	1
2124	508	Description	Address - Home	1
2125	508	DisplayOrder	5	1
2126	508	SupportedByDefault	true	1
2127	508	MappedLocalClaim	http://wso2.org/claims/addresses.locality	1
2128	508	DisplayName	Address - Home	1
2129	509	Description	Honoric Prefix	1
2130	509	DisplayOrder	2	1
2131	509	Required	true	1
2132	509	SupportedByDefault	true	1
2133	509	MappedLocalClaim	http://wso2.org/claims/honorificPrefix	1
2134	509	DisplayName	Name - Honoric Prefix	1
2135	510	Description	Address - Locality	1
2136	510	DisplayOrder	5	1
2137	510	SupportedByDefault	true	1
2138	510	MappedLocalClaim	http://wso2.org/claims/addresses.locality	1
2139	510	DisplayName	Address - Locality	1
2140	511	Description	Middle Name	1
2141	511	DisplayOrder	2	1
2142	511	Required	true	1
2143	511	SupportedByDefault	true	1
2144	511	MappedLocalClaim	http://wso2.org/claims/middleName	1
2145	511	DisplayName	Name - Middle Name	1
2146	512	Description	Entitlements	1
2147	512	DisplayOrder	5	1
2148	512	SupportedByDefault	true	1
2149	512	MappedLocalClaim	http://wso2.org/claims/entitlements	1
2150	512	DisplayName	Entitlements	1
2151	513	Description	Display Name	1
2152	513	DisplayOrder	2	1
2153	513	Required	true	1
2154	513	SupportedByDefault	true	1
2155	513	MappedLocalClaim	http://wso2.org/claims/displayName	1
2156	513	DisplayName	Display Name	1
2157	514	Description	Meta - Created	1
2158	514	DisplayOrder	1	1
2159	514	Required	true	1
2160	514	SupportedByDefault	true	1
2161	514	MappedLocalClaim	http://wso2.org/claims/created	1
2162	514	DisplayName	Meta - Created	1
2163	515	Description	End-User's full name in displayable form including all name parts, possibly including titles and suffixes, ordered according to the End-User's locale and preferences	1
2164	515	DisplayOrder	2	1
2165	515	Required	true	1
2166	515	SupportedByDefault	true	1
2167	515	MappedLocalClaim	http://wso2.org/claims/fullname	1
2168	515	DisplayName	Full Name	1
2169	516	Description	End-User's locale, For example, en-US or fr-CA, en_US	1
2170	516	MappedLocalClaim	http://wso2.org/claims/local	1
2171	516	DisplayName	Locale	1
2172	517	Description	VAT Registration Number	1
2173	517	DisplayOrder	1	1
2174	517	Required	true	1
2175	517	SupportedByDefault	true	1
2176	517	MappedLocalClaim	http://wso2.org/claims/im	1
2177	517	DisplayName	VAT Registration Number	1
2178	518	Description	IM - Gtalk	1
2179	518	DisplayOrder	5	1
2180	518	SupportedByDefault	true	1
2181	518	MappedLocalClaim	http://wso2.org/claims/gtalk	1
2182	518	DisplayName	IM - Gtalk	1
2183	519	Description	Email Address	1
2184	519	Required	true	1
2185	519	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
2186	519	SupportedByDefault	true	1
2187	519	MappedLocalClaim	http://wso2.org/claims/emailaddress	1
2188	519	DisplayName	Email	1
2189	520	Description	Other Phone	1
2190	520	MappedLocalClaim	http://wso2.org/claims/otherphone	1
2191	520	DisplayName	Other Phone	1
2192	521	Description	Date of birth	1
2193	521	DisplayOrder	1	1
2194	521	Required	true	1
2195	521	SupportedByDefault	true	1
2196	521	MappedLocalClaim	http://wso2.org/claims/dob	1
2197	521	DisplayName	Date of birth	1
2198	522	Description	User Type	1
2199	522	DisplayOrder	2	1
2200	522	Required	true	1
2201	522	SupportedByDefault	true	1
2202	522	MappedLocalClaim	http://wso2.org/claims/userType	1
2203	522	DisplayName	User Type	1
2204	523	Description	Manager - Display Name	1
2205	523	DisplayOrder	1	1
2206	523	Required	true	1
2207	523	SupportedByDefault	true	1
2208	523	MappedLocalClaim	http://wso2.org/claims/extendedDisplayName	1
2209	523	DisplayName	Manager - Display Name	1
2210	524	Description	Full street address component, which MAY include house number, street name, Post Office Box, and multi-line extended street address information.	1
2211	524	MappedLocalClaim	http://wso2.org/claims/streetaddress	1
2212	524	DisplayName	Street Address	1
2213	525	Description	Nick Name	1
2214	525	DisplayOrder	2	1
2215	525	Required	true	1
2216	525	SupportedByDefault	true	1
2217	525	MappedLocalClaim	http://wso2.org/claims/nickname	1
2218	525	DisplayName	Nick Name	1
2219	526	Description	URL of the End-User's Web page or blog. This Web page SHOULD contain information published by the End-User or an organization that the End-User is affiliated with.	1
2220	526	DisplayOrder	10	1
2221	526	SupportedByDefault	true	1
2222	526	MappedLocalClaim	http://wso2.org/claims/url	1
2223	526	DisplayName	URL	1
2224	527	Description	Roles	1
2225	527	DisplayOrder	5	1
2226	527	SupportedByDefault	true	1
2227	527	MappedLocalClaim	http://wso2.org/claims/role	1
2228	527	DisplayName	Roles	1
2229	528	Description	Email Addresses	1
2230	528	DisplayOrder	3	1
2231	528	Required	true	1
2232	528	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
2233	528	SupportedByDefault	true	1
2234	528	MappedLocalClaim	http://wso2.org/claims/emailaddress	1
2235	528	DisplayName	Emails	1
2236	529	Description	Work Email	1
2237	529	DisplayOrder	5	1
2238	529	SupportedByDefault	true	1
2239	529	MappedLocalClaim	http://wso2.org/claims/emails.work	1
2240	529	DisplayName	Emails - Work Email	1
2241	530	Description	End-User's preferred telephone number. For example, +1 (425) 555-1212 or +56 (2) 687 2400., +1 (604) 555-1234;ext=5678.	1
2242	530	MappedLocalClaim	http://wso2.org/claims/telephone	1
2243	530	DisplayName	Phone Number	1
2244	531	Description	Gender	1
2245	531	DisplayOrder	1	1
2246	531	Required	true	1
2247	531	SupportedByDefault	true	1
2248	531	MappedLocalClaim	http://wso2.org/claims/gender	1
2249	531	DisplayName	Gender	1
2250	532	Description	Claim to store newly updated email address until the new email address is verified	1
2251	532	DisplayOrder	1	1
2252	532	Required	true	1
2253	532	SupportedByDefault	true	1
2254	532	MappedLocalClaim	http://wso2.org/claims/identity/emailaddress.pendingValue	1
2255	532	DisplayName	Verification Pending Email	1
2256	533	Description	Id	1
2257	533	DisplayOrder	1	1
2258	533	Required	true	1
2259	533	SupportedByDefault	true	1
2260	533	MappedLocalClaim	http://wso2.org/claims/userid	1
2261	533	DisplayName	Id	1
2262	534	Description	Nick Name	1
2263	534	DisplayOrder	2	1
2264	534	Required	true	1
2265	534	SupportedByDefault	true	1
2266	534	MappedLocalClaim	http://wso2.org/claims/nickname	1
2267	534	DisplayName	Nick Name	1
2268	535	Description	Organization -department	1
2269	535	DisplayOrder	1	1
2270	535	Required	true	1
2271	535	SupportedByDefault	true	1
2272	535	MappedLocalClaim	http://wso2.org/claims/department	1
2273	535	DisplayName	Organization -department	1
2274	536	Description	Home Phone	1
2275	536	DisplayOrder	5	1
2276	536	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
2277	536	SupportedByDefault	true	1
2278	536	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	1
2279	536	DisplayName	Phone Numbers - Home Phone Number	1
2280	537	Description	End-User's birthday, represented as an ISO 8601:2004 [ISO8601-2004] YYYY-MM-DD format. The year MAY be 0000, indicating that it is omitted. To represent only the year, YYYY format is allowed.	1
2281	537	MappedLocalClaim	http://wso2.org/claims/dob	1
2282	537	DisplayName	Birth Date	1
2283	538	Description	Current Address	1
2284	538	DisplayOrder	1	1
2285	538	Required	true	1
2286	538	SupportedByDefault	true	1
2287	538	MappedLocalClaim	http://wso2.org/claims/addresses	1
2288	538	DisplayName	Current Address	1
2289	539	Description	Standard Industrial Classification	1
2290	539	DisplayOrder	1	1
2291	539	Required	true	1
2292	539	SupportedByDefault	true	1
2293	539	MappedLocalClaim	http://wso2.org/claims/nickname	1
2407	577	DisplayName	Department	2
2294	539	DisplayName	Standard Industrial Classification	1
2295	540	Description	Given Name	1
2296	540	DisplayOrder	1	1
2297	540	Required	true	1
2298	540	SupportedByDefault	true	1
2299	540	MappedLocalClaim	http://wso2.org/claims/givenname	1
2300	540	DisplayName	Name - Given Name	1
2301	541	Description	Email Address	1
2302	541	Required	true	1
2303	541	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	1
2304	541	SupportedByDefault	true	1
2305	541	MappedLocalClaim	http://wso2.org/claims/emailaddress	1
2306	541	DisplayName	Email	1
2307	542	Description	Mobile Number	1
2308	542	DisplayOrder	5	1
2309	542	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	1
2310	542	SupportedByDefault	true	1
2311	542	MappedLocalClaim	http://wso2.org/claims/mobile	1
2312	542	DisplayName	Phone Numbers - Mobile Number	1
2313	543	Description	Country	1
2314	543	SupportedByDefault	true	1
2315	543	MappedLocalClaim	http://wso2.org/claims/country	1
2316	543	DisplayName	Country	1
2317	544	Description	Date of Birth	1
2318	544	SupportedByDefault	true	1
2319	544	MappedLocalClaim	http://wso2.org/claims/dob	1
2320	544	DisplayName	DOB	1
2321	545	Description	Time the End-User's information was last updated. Its value is a JSON number representing the number of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time.	1
2322	545	MappedLocalClaim	http://wso2.org/claims/modified	1
2323	545	DisplayName	Updated At	1
2324	546	Description	Profile URL	1
2325	546	DisplayOrder	2	1
2326	546	Required	true	1
2327	546	SupportedByDefault	true	1
2328	546	MappedLocalClaim	http://wso2.org/claims/url	1
2329	546	DisplayName	Profile URL	1
2330	547	Description	Formatted Name	1
2331	547	DisplayOrder	2	1
2332	547	Required	true	1
2333	547	SupportedByDefault	true	1
2334	547	MappedLocalClaim	http://wso2.org/claims/formattedName	1
2335	547	DisplayName	Name - Formatted Name	1
2336	548	Description	Fax Number	1
2337	548	DisplayOrder	5	1
2338	548	SupportedByDefault	true	1
2339	548	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.fax	1
2340	548	DisplayName	Phone Numbers - Fax Number	1
2341	549	Description	Local	2
2342	549	DisplayName	Local	2
2343	550	Description	Locality	2
2344	550	DisplayName	Locality	2
2345	551	Description	Country	2
2346	551	DisplayOrder	5	2
2347	551	SupportedByDefault	true	2
2348	551	DisplayName	Country	2
2349	552	Description	Display Name	2
2350	552	DisplayName	Display Name	2
2351	553	Description	Failed Lockout Count	2
2352	553	DisplayName	Failed Lockout Count	2
2353	554	Description	Mobile	2
2354	554	DisplayOrder	8	2
2355	554	SupportedByDefault	true	2
2356	554	DisplayName	Mobile	2
2357	555	Description	Nick Name	2
2358	555	DisplayName	Nick Name	2
2359	556	Description	Locked Reason	2
2360	556	DisplayName	Locked Reason	2
2361	557	Description	Full Name	2
2362	557	DisplayName	Full Name	2
2363	558	Description	Honoric Prefix	2
2364	558	DisplayName	Name - Honoric Prefix	2
2365	559	Description	IM - Skype	2
2366	559	DisplayName	IM - Skype	2
2367	560	Description	Address	2
2368	560	DisplayName	Address	2
2369	561	Description	Region	2
2370	561	DisplayName	Region	2
2371	562	Description	Middle Name	2
2372	562	DisplayName	Middle Name	2
2373	563	Description	Last Logon Time	2
2374	563	DisplayName	Last Logon	2
2375	564	Description	Username	2
2376	564	DisplayName	Username	2
2377	565	Description	Preferred Language	2
2378	565	DisplayName	Preferred Language	2
2379	566	Description	Extended External ID	2
2380	566	DisplayName	Extended External ID	2
2381	567	Description	Time Zone	2
2382	567	DisplayName	Time Zone	2
2383	568	Description	Last Name	2
2384	568	DisplayOrder	2	2
2385	568	Required	true	2
2386	568	SupportedByDefault	true	2
2387	568	DisplayName	Last Name	2
2388	569	Description	Last Login Time	2
2389	569	DisplayName	Last Login	2
2390	570	Description	Preferred Notification Channel	2
2391	570	DisplayName	Preferred Channel	2
2392	571	Description	Cost Center	2
2393	571	DisplayName	Cost Center	2
2394	572	Description	Location	2
2395	572	DisplayName	Location	2
2396	573	Description	Challenge Question	2
2397	573	DisplayName	Challenge Question	2
2398	574	Description	Claim to disable EmailOTP	2
2399	574	DisplayName	Disable EmailOTP	2
2400	575	Description	Account Locked	2
2401	575	DisplayName	Account Locked	2
2402	576	Description	Other Email	2
2403	576	DisplayName	Emails - Other Email	2
2404	577	ReadOnly	true	2
2405	577	Description	Department	2
2406	577	SupportedByDefault	true	2
2408	578	Description	Photo URL	2
2409	578	DisplayName	Photo URIL	2
2410	579	Description	IM	2
2411	579	DisplayOrder	9	2
2412	579	SupportedByDefault	true	2
2413	579	DisplayName	IM	2
2414	580	Description	Postal Code	2
2415	580	DisplayName	Postal Code	2
2416	581	Description	Email Address	2
2417	581	DisplayOrder	6	2
2418	581	Required	true	2
2419	581	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
2420	581	SupportedByDefault	true	2
2421	581	DisplayName	Email	2
2422	582	Description	Organization	2
2423	582	DisplayOrder	3	2
2424	582	SupportedByDefault	true	2
2425	582	DisplayName	Organization	2
2426	583	Description	Work Email	2
2427	583	DisplayName	Emails - Work Email	2
2428	584	Description	URL	2
2429	584	DisplayOrder	10	2
2430	584	SupportedByDefault	true	2
2431	584	DisplayName	URL	2
2432	585	Description	Address - Locality	2
2433	585	DisplayName	Address - Locality	2
2434	586	Description	Unlock Time	2
2435	586	DisplayName	Unlock Time	2
2436	587	Description	Work Phone	2
2437	587	DisplayName	Phone Numbers - Work Phone Number	2
2438	588	Description	Honoric Suffix	2
2439	588	DisplayName	Name - Honoric Suffix	2
2440	589	Description	Phone Verified	2
2441	589	DisplayName	Phone Verified	2
2442	590	Description	Primary Challenge Question	2
2443	590	DisplayName	Primary Challenge Question	2
2444	591	Description	Address - Formatted	2
2445	591	DisplayName	Address - Formatted	2
2446	592	Description	Claim to store the secret key	2
2447	592	DisplayName	Secret Key	2
2448	593	Description	Temporary claim to invoke email verified feature	2
2449	593	DisplayName	Verify Email	2
2450	594	ReadOnly	true	2
2451	594	Description	Unique ID of the user used in external systems	2
2452	594	DisplayName	External User ID	2
2453	595	Description	Challenge Question1	2
2454	595	DisplayName	Challenge Question1	2
2455	596	Description	Fax Number	2
2456	596	DisplayName	Phone Numbers - Fax Number	2
2457	597	ReadOnly	true	2
2458	597	Description	Last Modified timestamp of the user	2
2459	597	DisplayName	Last Modified Time	2
2460	598	Description	Formatted Name	2
2461	598	DisplayName	Name - Formatted Name	2
2462	599	Description	Claim to disable SMSOTP	2
2463	599	DisplayName	Disable SMSOTP	2
2464	600	Description	Temporary claim to invoke email ask Password feature	2
2465	600	DisplayName	Ask Password	2
2466	601	Description	Challenge Question2	2
2467	601	DisplayName	Challenge Question2	2
2468	602	Description	Home Phone	2
2469	602	DisplayName	Phone Numbers - Home Phone Number	2
2470	603	Description	Number of consecutive failed attempts done for password recovery	2
2471	603	DisplayName	Failed Password Recovery Attempts	2
2472	604	ReadOnly	true	2
2473	604	Description	Unique ID of the user	2
2474	604	DisplayName	User ID	2
2475	605	Description	Address - Street	2
2476	605	DisplayOrder	5	2
2477	605	DisplayName	Address - Street	2
2478	606	Description	Telephone	2
2479	606	DisplayOrder	7	2
2480	606	SupportedByDefault	true	2
2481	606	DisplayName	Telephone	2
2482	607	ReadOnly	true	2
2483	607	Description	Claim to store newly updated email address until the new email address is verified.	2
2484	607	DisplayName	Verification Pending Email	2
2485	608	Description	Account Disabled	2
2486	608	DisplayName	Account Disabled	2
2487	609	Description	First Name	2
2488	609	DisplayOrder	1	2
2489	609	Required	true	2
2490	609	SupportedByDefault	true	2
2491	609	DisplayName	First Name	2
2492	610	Description	Extended Display Name	2
2493	610	DisplayName	Extended Display Name	2
2494	611	Description	Failed Login Attempts	2
2495	611	DisplayName	Failed Login Attempts	2
2496	612	Description	IM - Gtalk	2
2497	612	DisplayName	IM - Gtalk	2
2498	613	Description	Groups	2
2499	613	DisplayName	Groups	2
2500	614	Description	X509Certificates	2
2501	614	DisplayName	X509Certificates	2
2502	615	Description	Photo	2
2503	615	DisplayName	Photo	2
2504	616	Description	Entitlements	2
2505	616	DisplayName	Entitlements	2
2506	617	Description	Last Password Update Time	2
2507	617	DisplayName	Last Password Update	2
2508	618	Description	Failed Attempts Before Success	2
2509	618	DisplayName	Failed Attempts Before Success	2
2510	619	Description	Phone Numbers	2
2511	619	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
2512	619	DisplayName	Phone Numbers	2
2513	620	Description	Birth Date	2
2514	620	DisplayName	Birth Date	2
2515	621	Description	Other Phone Number	2
2516	621	DisplayName	Phone Numbers - Other	2
2517	622	Description	User Principal	2
2518	622	DisplayName	User Principal	2
2519	623	Description	Home Email	2
2520	623	DisplayName	Emails - Home Email	2
2521	624	Description	State	2
2522	624	DisplayName	State	2
2523	625	Description	One Time Password	2
2524	625	DisplayName	One Time Password	2
2525	626	Description	Status of the account	2
2526	626	DisplayName	Active	2
2527	627	Description	Resource Type	2
2528	627	DisplayName	Resource Type	2
2529	628	Description	Email Verified	2
2530	628	DisplayName	Email Verified	2
2531	629	Description	User Type	2
2532	629	DisplayName	User Type	2
2533	630	Description	Other Phone	2
2534	630	DisplayName	Other Phone	2
2535	631	Description	Photo - Thumbnail	2
2536	631	DisplayName	Photo - Thumbnail	2
2537	632	Description	Temporary claim to invoke email force password feature	2
2538	632	DisplayName	Force Password Reset	2
2539	633	Description	Title	2
2540	633	DisplayName	Title	2
2541	634	Description	Extended Ref	2
2542	634	DisplayName	Extended Ref	2
2543	635	ReadOnly	true	2
2544	635	Description	Created timestamp of the user	2
2545	635	DisplayName	Created Time	2
2546	636	Description	Gender	2
2547	636	DisplayName	Gender	2
2548	637	ReadOnly	true	2
2549	637	Description	Role	2
2550	637	SupportedByDefault	true	2
2551	637	DisplayName	Role	2
2552	638	ReadOnly	true	2
2553	638	Description	Account State	2
2554	638	DisplayName	Account State	2
2555	639	Description	Pager Number	2
2556	639	DisplayName	Phone Numbers - Pager Number	2
2557	640	ReadOnly	true	2
2558	640	Description	End-User's gender. Values defined by this specification are female and male. Other values MAY be used when neither of the defined values are applicable.	2
2559	640	SupportedByDefault	true	2
2560	640	MappedLocalClaim	http://wso2.org/claims/gender	2
2561	640	DisplayName	Gender	2
2562	641	Description	Street Address	2
2563	641	MappedLocalClaim	http://wso2.org/claims/streetaddress	2
2564	641	DisplayName	Street Address	2
2565	642	Description	Time Zone	2
2566	642	DisplayOrder	2	2
2567	642	Required	true	2
2568	642	SupportedByDefault	true	2
2569	642	MappedLocalClaim	http://wso2.org/claims/timeZone	2
2570	642	DisplayName	Time Zone	2
2571	643	Description	Manager - home	2
2572	643	DisplayOrder	1	2
2573	643	Required	true	2
2574	643	SupportedByDefault	true	2
2575	643	MappedLocalClaim	http://wso2.org/claims/gender	2
2576	643	DisplayName	Manager - home	2
2577	644	Description	Photo	2
2578	644	DisplayOrder	5	2
2579	644	SupportedByDefault	true	2
2580	644	MappedLocalClaim	http://wso2.org/claims/photourl	2
2581	644	DisplayName	Photo	2
2582	645	Description	Date of Birth	2
2583	645	DisplayOrder	6	2
2584	645	SupportedByDefault	true	2
2585	645	MappedLocalClaim	http://wso2.org/claims/dob	2
2586	645	DisplayName	DOB	2
2587	646	Description	Locality	2
2588	646	DisplayOrder	2	2
2589	646	Required	true	2
2590	646	SupportedByDefault	true	2
2591	646	MappedLocalClaim	http://wso2.org/claims/local	2
2592	646	DisplayName	Locality	2
2593	647	Description	Legal Person Name	2
2594	647	DisplayOrder	1	2
2595	647	Required	true	2
2596	647	SupportedByDefault	true	2
2597	647	MappedLocalClaim	http://wso2.org/claims/extendedDisplayName	2
2598	647	DisplayName	Legal Person Name	2
2599	648	Description	Given Name	2
2600	648	DisplayOrder	1	2
2601	648	Required	true	2
2602	648	SupportedByDefault	true	2
2603	648	MappedLocalClaim	http://wso2.org/claims/givenname	2
2604	648	DisplayName	Name - Given Name	2
2605	649	Description	Place of Birth	2
2606	649	DisplayOrder	1	2
2607	649	Required	true	2
2608	649	SupportedByDefault	true	2
2609	649	MappedLocalClaim	http://wso2.org/claims/country	2
2610	649	DisplayName	Place of Birth	2
2611	650	Description	Phone Numbers	2
2612	650	DisplayOrder	3	2
2613	650	Required	true	2
2614	650	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
2615	650	SupportedByDefault	true	2
2616	650	MappedLocalClaim	http://wso2.org/claims/phoneNumbers	2
2617	650	DisplayName	Phone Numbers	2
2618	651	Description	Organization -division	2
2619	651	DisplayOrder	1	2
2620	651	Required	true	2
2621	651	SupportedByDefault	true	2
2622	651	MappedLocalClaim	http://wso2.org/claims/organization	2
2623	651	DisplayName	Organization -division	2
2624	652	Description	Postalcode	2
2625	652	DisplayOrder	4	2
2626	652	SupportedByDefault	true	2
2627	652	MappedLocalClaim	http://wso2.org/claims/postalcode	2
2628	652	DisplayName	Postalcode	2
2629	653	Description	Economic Operator Registration and Identification	2
2630	653	DisplayOrder	1	2
2631	653	Required	true	2
2632	653	SupportedByDefault	true	2
2633	653	MappedLocalClaim	http://wso2.org/claims/department	2
2634	653	DisplayName	Economic Operator Registration and Identification	2
2635	654	Description	Email Addresses	2
2636	654	DisplayOrder	5	2
2637	654	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
2638	654	SupportedByDefault	true	2
2639	654	MappedLocalClaim	http://wso2.org/claims/emailaddress	2
2640	654	DisplayName	Emails	2
2641	655	Description	Phone Numbers	2
2642	655	DisplayOrder	5	2
2643	655	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
2644	655	SupportedByDefault	true	2
2645	655	MappedLocalClaim	http://wso2.org/claims/phoneNumbers	2
2646	655	DisplayName	Phone Numbers	2
2647	656	Description	Family Name	2
2648	656	DisplayOrder	2	2
2649	656	Required	true	2
2650	656	SupportedByDefault	true	2
2651	656	MappedLocalClaim	http://wso2.org/claims/lastname	2
2652	656	DisplayName	Name - Family Name	2
2653	657	Description	Photo	2
2654	657	DisplayOrder	5	2
2655	657	SupportedByDefault	true	2
2656	657	MappedLocalClaim	http://wso2.org/claims/photourl	2
2657	657	DisplayName	Photo	2
2658	658	Description	Photo - Thumbnail	2
2659	658	DisplayOrder	5	2
2660	658	SupportedByDefault	true	2
2661	658	MappedLocalClaim	http://wso2.org/claims/thumbnail	2
2662	658	DisplayName	Photo - Thumbnail	2
2663	659	Description	Country	2
2664	659	DisplayOrder	5	2
2665	659	SupportedByDefault	true	2
2666	659	MappedLocalClaim	http://wso2.org/claims/country	2
2667	659	DisplayName	Country	2
2668	660	Description	True if the End-User's e-mail address has been verified; otherwise false. 	2
2669	660	MappedLocalClaim	http://wso2.org/claims/identity/emailVerified	2
2670	660	DisplayName	Email Verified	2
2671	661	Description	Title	2
2672	661	DisplayOrder	2	2
2673	661	Required	true	2
2674	661	SupportedByDefault	true	2
2675	661	MappedLocalClaim	http://wso2.org/claims/title	2
2676	661	DisplayName	Title	2
2677	662	Description	User Type	2
2678	662	DisplayOrder	2	2
2679	662	Required	true	2
2680	662	SupportedByDefault	true	2
2681	662	MappedLocalClaim	http://wso2.org/claims/userType	2
2682	662	DisplayName	User Type	2
2683	663	Description	Home Email	2
2684	663	DisplayOrder	5	2
2685	663	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
2686	663	SupportedByDefault	true	2
2687	663	MappedLocalClaim	http://wso2.org/claims/emails.home	2
2688	663	DisplayName	Emails - Home Email	2
2689	664	Description	Active	2
2690	664	DisplayOrder	2	2
2691	664	Required	true	2
2692	664	SupportedByDefault	true	2
2693	664	MappedLocalClaim	http://wso2.org/claims/active	2
2694	664	DisplayName	Active	2
2695	665	Description	Pager Number	2
2696	665	DisplayOrder	5	2
2697	665	SupportedByDefault	true	2
2698	665	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.pager	2
2699	665	DisplayName	Phone Numbers - Pager Number	2
2700	666	Description	String from zoneinfo time zone database representing the End-User's time zone. For example, Europe/Paris or America/Los_Angeles.	2
2701	666	MappedLocalClaim	http://wso2.org/claims/timeZone	2
2702	666	DisplayName	Zone Info	2
2703	667	Description	Honoric Suffix	2
2704	667	DisplayOrder	2	2
2705	667	Required	true	2
2706	667	SupportedByDefault	true	2
2707	667	MappedLocalClaim	http://wso2.org/claims/honorificSuffix	2
2708	667	DisplayName	Name - Honoric Suffix	2
2709	668	Description	Photo	2
2710	668	DisplayOrder	5	2
2711	668	SupportedByDefault	true	2
2712	668	MappedLocalClaim	http://wso2.org/claims/photos	2
2713	668	DisplayName	Photo	2
2714	669	Description	Display Name	2
2715	669	DisplayOrder	2	2
2716	669	Required	true	2
2717	669	SupportedByDefault	true	2
2718	669	MappedLocalClaim	http://wso2.org/claims/displayName	2
2719	669	DisplayName	Display Name	2
2720	670	Description	URL of the End-User's profile picture. This URL MUST refer to an image file (for example, a PNG, JPEG, or GIF image file)	2
2721	670	DisplayOrder	9	2
2722	670	SupportedByDefault	true	2
2723	670	MappedLocalClaim	http://wso2.org/claims/photourl	2
2724	670	DisplayName	Picture	2
2725	671	Description	Meta - Last Modified	2
2726	671	DisplayOrder	1	2
2727	671	Required	true	2
2728	671	SupportedByDefault	true	2
2729	671	MappedLocalClaim	http://wso2.org/claims/modified	2
2730	671	DisplayName	Meta - Last Modified	2
2731	672	Description	X509Certificates	2
2732	672	DisplayOrder	5	2
2733	672	SupportedByDefault	true	2
2734	672	MappedLocalClaim	http://wso2.org/claims/x509Certificates	2
2735	672	DisplayName	X509Certificates	2
2736	673	Description	Formatted Name	2
2737	673	DisplayOrder	2	2
2738	673	Required	true	2
2739	673	SupportedByDefault	true	2
2740	673	MappedLocalClaim	http://wso2.org/claims/formattedName	2
2741	673	DisplayName	Name - Formatted Name	2
2742	674	Description	Groups	2
2743	674	DisplayOrder	5	2
2744	674	SupportedByDefault	true	2
2745	674	MappedLocalClaim	http://wso2.org/claims/groups	2
2746	674	DisplayName	Groups	2
2747	675	Description	Home Phone	2
2748	675	DisplayOrder	5	2
2749	675	SupportedByDefault	true	2
2750	675	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	2
2751	675	DisplayName	Phone Numbers - Home Phone Number	2
2752	676	Description	External Id	2
2753	676	DisplayOrder	1	2
2754	676	Required	true	2
2755	676	SupportedByDefault	true	2
2756	676	MappedLocalClaim	http://wso2.org/claims/externalid	2
2757	676	DisplayName	External Id	2
2758	677	Description	Roles	2
2759	677	DisplayOrder	5	2
2760	677	SupportedByDefault	true	2
2761	677	MappedLocalClaim	http://wso2.org/claims/role	2
2762	677	DisplayName	Roles	2
2763	678	Description	Entitlements	2
2764	678	DisplayOrder	5	2
2765	678	SupportedByDefault	true	2
2766	678	MappedLocalClaim	http://wso2.org/claims/entitlements	2
2767	678	DisplayName	Entitlements	2
2768	679	Description	Locality	2
2769	679	MappedLocalClaim	http://wso2.org/claims/locality	2
2770	679	DisplayName	Locality	2
2771	680	Description	Time Zone	2
2772	680	DisplayOrder	9	2
2773	680	SupportedByDefault	true	2
2774	680	MappedLocalClaim	http://wso2.org/claims/timeZone	2
2775	680	DisplayName	Time Zone	2
2776	681	Description	Address	2
2777	681	DisplayOrder	5	2
2778	681	SupportedByDefault	true	2
2779	681	MappedLocalClaim	http://wso2.org/claims/addresses	2
2780	681	DisplayName	Address	2
2781	682	Description	Current Family Name	2
2782	682	DisplayOrder	1	2
2783	682	Required	true	2
2784	682	SupportedByDefault	true	2
2785	682	MappedLocalClaim	http://wso2.org/claims/lastname	2
2786	682	DisplayName	Current Family Name	2
2787	683	Description	Shorthand name by which the End-User wishes to be referred to at the RP, such as janedoe or j.doe.	2
2788	683	DisplayOrder	7	2
2789	683	SupportedByDefault	true	2
2790	683	MappedLocalClaim	http://wso2.org/claims/displayName	2
2791	683	DisplayName	Preferred Username	2
2792	684	Description	Middle name(s) of the End-User. Note that in some cultures, people can have multiple middle names; all can be present, with the names being separated by space characters. Also note that in some cultures, middle names are not used.	2
2793	684	DisplayOrder	5	2
2794	684	SupportedByDefault	true	2
2795	684	MappedLocalClaim	http://wso2.org/claims/middleName	2
2796	684	DisplayName	Middle Name	2
2797	685	Description	Employee Number	2
2798	685	DisplayOrder	1	2
2799	685	Required	true	2
2800	685	SupportedByDefault	true	2
2801	685	MappedLocalClaim	http://wso2.org/claims/extendedExternalId	2
2802	685	DisplayName	Employee Number	2
2803	686	Description	External Id	2
2804	686	DisplayOrder	1	2
2805	686	Required	true	2
2806	686	SupportedByDefault	true	2
2807	686	MappedLocalClaim	http://wso2.org/claims/externalid	2
2808	686	DisplayName	External Id	2
2809	687	Description	Language	2
2810	687	SupportedByDefault	true	2
2811	687	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	2
2812	687	DisplayName	Language	2
2813	688	Description	Address - Formatted	2
2814	688	DisplayOrder	5	2
2815	688	SupportedByDefault	true	2
2816	688	MappedLocalClaim	http://wso2.org/claims/addresses.formatted	2
2817	688	DisplayName	Address - Formatted	2
2818	689	Description	User Name	2
2819	689	DisplayOrder	2	2
2820	689	Required	true	2
2821	689	SupportedByDefault	true	2
2822	689	MappedLocalClaim	http://wso2.org/claims/username	2
2823	689	DisplayName	User Name	2
2824	690	Description	Time Zone	2
2825	690	DisplayOrder	2	2
2826	690	Required	true	2
2827	690	SupportedByDefault	true	2
2828	690	MappedLocalClaim	http://wso2.org/claims/timeZone	2
2829	690	DisplayName	Time Zone	2
2830	691	Description	Honoric Suffix	2
2831	691	DisplayOrder	2	2
2832	691	Required	true	2
2833	691	SupportedByDefault	true	2
2834	691	MappedLocalClaim	http://wso2.org/claims/honorificSuffix	2
2835	691	DisplayName	Name - Honoric Suffix	2
2836	692	Description	State	2
2837	692	MappedLocalClaim	http://wso2.org/claims/stateorprovince	2
2838	692	DisplayName	State	2
2839	693	Description	Legal Person Identifier	2
2840	693	DisplayOrder	1	2
2841	693	Required	true	2
2842	693	SupportedByDefault	true	2
2843	693	MappedLocalClaim	http://wso2.org/claims/extendedExternalId	2
2844	693	DisplayName	Legal Person Identifier	2
2845	694	Description	Gender	2
2846	694	DisplayOrder	8	2
2847	694	SupportedByDefault	true	2
2848	694	MappedLocalClaim	http://wso2.org/claims/gender	2
2849	694	DisplayName	Gender	2
2850	695	Description	Language	2
2851	695	DisplayOrder	7	2
2852	695	SupportedByDefault	true	2
2853	695	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	2
2854	695	DisplayName	Language	2
2855	696	Description	Identifier for the End-User at the Issuer	2
2856	696	DisplayOrder	1	2
2857	696	Required	true	2
2858	696	SupportedByDefault	true	2
2859	696	MappedLocalClaim	http://wso2.org/claims/username	2
2860	696	DisplayName	Subject	2
2861	697	Description	User Name	2
2862	697	DisplayOrder	2	2
2863	697	Required	true	2
2864	697	SupportedByDefault	true	2
2865	697	MappedLocalClaim	http://wso2.org/claims/username	2
2866	697	DisplayName	User Name	2
2867	698	Description	Nick Name	2
2868	698	MappedLocalClaim	http://wso2.org/claims/nickname	2
2869	698	DisplayName	Nick Name	2
2870	699	Description	Casual name of the End-User that may or may not be the same as the given_name. For instance, a nickname value of Mike might be returned alongside a given_name value of Michael.	2
2871	699	DisplayOrder	6	2
2872	699	Required	true	2
2873	699	SupportedByDefault	true	2
2874	699	MappedLocalClaim	http://wso2.org/claims/nickname	2
2875	699	DisplayName	Nickname	2
2876	700	Description	Tax Reference	2
2877	700	DisplayOrder	1	2
2878	700	Required	true	2
2879	700	SupportedByDefault	true	2
2880	700	MappedLocalClaim	http://wso2.org/claims/postalcode	2
2881	700	DisplayName	Tax Reference	2
2882	701	Description	Postalcode	2
2883	701	SupportedByDefault	true	2
2884	701	MappedLocalClaim	http://wso2.org/claims/postalcode	2
2885	701	DisplayName	Postalcode	2
2886	702	Description	Meta - Location	2
2887	702	DisplayOrder	1	2
2888	702	Required	true	2
2889	702	SupportedByDefault	true	2
2890	702	MappedLocalClaim	http://wso2.org/claims/resourceType	2
2891	702	DisplayName	Meta - Location	2
2892	703	Description	IM - Skype	2
2893	703	DisplayOrder	5	2
2894	703	SupportedByDefault	true	2
2895	703	MappedLocalClaim	http://wso2.org/claims/skype	2
2896	703	DisplayName	IM - Skype	2
2897	704	Description	Preferred Language	2
2898	704	DisplayOrder	2	2
2899	704	Required	true	2
2900	704	SupportedByDefault	true	2
2901	704	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	2
2902	704	DisplayName	Preferred Language	2
2903	705	Description	Other Phone Number	2
2904	705	DisplayOrder	5	2
2905	705	SupportedByDefault	true	2
2906	705	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.other	2
2907	705	DisplayName	Phone Numbers - Other	2
2908	706	Description	Current Given Name	2
2909	706	DisplayOrder	1	2
2910	706	Required	true	2
2911	706	SupportedByDefault	true	2
2912	706	MappedLocalClaim	http://wso2.org/claims/givenname	2
2913	706	DisplayName	Current Given Name	2
2914	707	Description	Middle Name	2
2915	707	DisplayOrder	2	2
2916	707	Required	true	2
2917	707	SupportedByDefault	true	2
2918	707	MappedLocalClaim	http://wso2.org/claims/middleName	2
2919	707	DisplayName	Name - Middle Name	2
2920	708	Description	Address - Work	2
2921	708	DisplayOrder	5	2
2922	708	SupportedByDefault	true	2
2923	708	MappedLocalClaim	http://wso2.org/claims/region	2
2924	708	DisplayName	Address - Work	2
2925	709	Description	Mobile	2
2926	709	MappedLocalClaim	http://wso2.org/claims/mobile	2
2927	709	DisplayName	Mobile	2
2928	710	Description	Full Name	2
2929	710	DisplayOrder	2	2
2930	710	Required	true	2
2931	710	SupportedByDefault	true	2
2932	710	MappedLocalClaim	http://wso2.org/claims/fullname	2
2933	710	DisplayName	Full Name	2
2934	711	Description	Last Name	2
2935	711	Required	true	2
2936	711	SupportedByDefault	true	2
2937	711	MappedLocalClaim	http://wso2.org/claims/lastname	2
2938	711	DisplayName	Last Name	2
2939	712	Description	First Name	2
2940	712	Required	true	2
2941	712	SupportedByDefault	true	2
2942	712	MappedLocalClaim	http://wso2.org/claims/givenname	2
2943	712	DisplayName	First Name	2
2944	713	Description	Email Address	2
2945	713	DisplayOrder	3	2
2946	713	Required	true	2
2947	713	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
2948	713	SupportedByDefault	true	2
2949	713	MappedLocalClaim	http://wso2.org/claims/emailaddress	2
2950	713	DisplayName	Email	2
2951	714	Description	Gender	2
2952	714	MappedLocalClaim	http://wso2.org/claims/gender	2
2953	714	DisplayName	Gender	2
2954	715	Description	Address - Region	2
2955	715	DisplayOrder	5	2
2956	715	SupportedByDefault	true	2
2957	715	MappedLocalClaim	http://wso2.org/claims/region	2
2958	715	DisplayName	Address - Region	2
2959	716	Description	Meta - Location	2
2960	716	DisplayOrder	1	2
2961	716	Required	true	2
2962	716	SupportedByDefault	true	2
2963	716	MappedLocalClaim	http://wso2.org/claims/location	2
2964	716	DisplayName	Meta - Location	2
2965	717	Description	Home Email	2
2966	717	DisplayOrder	5	2
2967	717	SupportedByDefault	true	2
2968	717	MappedLocalClaim	http://wso2.org/claims/emails.home	2
2969	717	DisplayName	Emails - Home Email	2
2970	718	Description	Meta - Last Modified	2
2971	718	DisplayOrder	1	2
2972	718	Required	true	2
2973	718	SupportedByDefault	true	2
2974	718	MappedLocalClaim	http://wso2.org/claims/modified	2
2975	718	DisplayName	Meta - Last Modified	2
2976	719	Description	Profile URL	2
2977	719	DisplayOrder	2	2
2978	719	Required	true	2
2979	719	SupportedByDefault	true	2
2980	719	MappedLocalClaim	http://wso2.org/claims/url	2
2981	719	DisplayName	Profile URL	2
2982	720	Description	Date of Birth	2
2983	720	MappedLocalClaim	http://wso2.org/claims/dob	2
2984	720	DisplayName	DOB	2
2985	721	Description	Family Name	2
2986	721	DisplayOrder	2	2
2987	721	Required	true	2
2988	721	SupportedByDefault	true	2
2989	721	MappedLocalClaim	http://wso2.org/claims/lastname	2
2990	721	DisplayName	Name - Family Name	2
2991	722	Description	Meta - Version	2
2992	722	DisplayOrder	1	2
2993	722	Required	true	2
2994	722	SupportedByDefault	true	2
2995	722	MappedLocalClaim	http://wso2.org/claims/im	2
2996	722	DisplayName	Meta - Version	2
2997	723	Description	X509Certificates	2
2998	723	DisplayOrder	5	2
2999	723	SupportedByDefault	true	2
3000	723	MappedLocalClaim	http://wso2.org/claims/x509Certificates	2
3001	723	DisplayName	X509Certificates	2
3002	724	Description	IM	2
3003	724	DisplayOrder	5	2
3004	724	SupportedByDefault	true	2
3005	724	MappedLocalClaim	http://wso2.org/claims/im	2
3006	724	DisplayName	IMS	2
3007	725	Description	URL of the End-User's profile page. The contents of this Web page SHOULD be about the End-User.	2
3008	725	DisplayOrder	8	2
3009	725	SupportedByDefault	true	2
3010	725	MappedLocalClaim	http://wso2.org/claims/url	2
3011	725	DisplayName	Profile	2
3012	726	Description	Meta - Created	2
3013	726	DisplayOrder	1	2
3014	726	Required	true	2
3015	726	SupportedByDefault	true	2
3016	726	MappedLocalClaim	http://wso2.org/claims/created	2
3017	726	DisplayName	Meta - Created	2
3018	727	Description	True if the End-User's phone number has been verified; otherwise false.	2
3019	727	MappedLocalClaim	http://wso2.org/claims/identity/phoneVerified	2
3020	727	DisplayName	Phone Number Verified	2
3021	728	Description	Legal Person Address	2
3022	728	DisplayOrder	1	2
3023	728	Required	true	2
3024	728	SupportedByDefault	true	2
3025	728	MappedLocalClaim	http://wso2.org/claims/addresses	2
3026	728	DisplayName	Legal Person Address	2
3027	729	Description	IM - Skype	2
3028	729	DisplayOrder	5	2
3029	729	SupportedByDefault	true	2
3030	729	MappedLocalClaim	http://wso2.org/claims/skype	2
3031	729	DisplayName	IM - Skype	2
3032	730	Description	Birth Name	2
3033	730	DisplayOrder	1	2
3034	730	Required	true	2
3035	730	SupportedByDefault	true	2
3036	730	MappedLocalClaim	http://wso2.org/claims/username	2
3037	730	DisplayName	Birth Name	2
3038	731	Description	Country	2
3039	731	SupportedByDefault	true	2
3040	731	MappedLocalClaim	http://wso2.org/claims/country	2
3041	731	DisplayName	Country	2
3042	732	Description	LEI	2
3043	732	DisplayOrder	1	2
3044	732	Required	true	2
3045	732	SupportedByDefault	true	2
3046	732	MappedLocalClaim	http://wso2.org/claims/extendedRef	2
3047	732	DisplayName	LEI	2
3048	733	Description	Work Phone	2
3049	733	DisplayOrder	5	2
3050	733	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
3051	733	SupportedByDefault	true	2
3052	733	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.work	2
3053	733	DisplayName	Phone Numbers - Work Phone Number	2
3054	734	Description	Zip code or postal code component.	2
3055	734	MappedLocalClaim	http://wso2.org/claims/postalcode	2
3056	734	DisplayName	Postal Code	2
3057	735	Description	Work Email	2
3058	735	DisplayOrder	5	2
3059	735	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
3060	735	SupportedByDefault	true	2
3061	735	MappedLocalClaim	http://wso2.org/claims/emails.work	2
3062	735	DisplayName	Emails - Work Email	2
3063	736	Description	Address - Country	2
3064	736	DisplayOrder	5	2
3065	736	SupportedByDefault	true	2
3066	736	MappedLocalClaim	http://wso2.org/claims/country	2
3067	736	DisplayName	Address - Country	2
3175	758	SupportedByDefault	true	2
3176	758	MappedLocalClaim	http://wso2.org/claims/nickname	2
3068	737	Description	List of group names that have been assigned to the principal. This typically will require a mapping at the application container level to application deployment roles.	2
3069	737	DisplayOrder	12	2
3070	737	SupportedByDefault	true	2
3071	737	MappedLocalClaim	http://wso2.org/claims/role	2
3072	737	DisplayName	User Groups	2
3073	738	Description	Other Email	2
3074	738	DisplayOrder	5	2
3075	738	SupportedByDefault	true	2
3076	738	MappedLocalClaim	http://wso2.org/claims/emails.other	2
3077	738	DisplayName	Emails - Other Email	2
3078	739	Description	True if the End-User's phone number has been verified; otherwise false. 	2
3079	739	MappedLocalClaim	http://wso2.org/claims/addresses	2
3080	739	DisplayName	Address	2
3081	740	Description	Work Phone	2
3082	740	DisplayOrder	5	2
3083	740	SupportedByDefault	true	2
3084	740	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.work	2
3085	740	DisplayName	Phone Numbers - Work Phone Number	2
3086	741	Description	Last Name	2
3087	741	Required	true	2
3088	741	SupportedByDefault	true	2
3089	741	MappedLocalClaim	http://wso2.org/claims/lastname	2
3090	741	DisplayName	Last Name	2
3091	742	Description	PPID	2
3092	742	Required	true	2
3093	742	SupportedByDefault	true	2
3094	742	MappedLocalClaim	http://wso2.org/claims/im	2
3095	742	DisplayName	0	2
3096	743	Description	Mobile Number	2
3097	743	DisplayOrder	5	2
3098	743	SupportedByDefault	true	2
3099	743	MappedLocalClaim	http://wso2.org/claims/mobile	2
3100	743	DisplayName	Phone Numbers - Mobile Number	2
3101	744	Description	EU Identifier	2
3102	744	DisplayOrder	1	2
3103	744	Required	true	2
3104	744	SupportedByDefault	true	2
3105	744	MappedLocalClaim	http://wso2.org/claims/externalid	2
3106	744	DisplayName	EU Identifier	2
3107	745	Description	Active	2
3108	745	DisplayOrder	2	2
3109	745	Required	true	2
3110	745	SupportedByDefault	true	2
3111	745	MappedLocalClaim	http://wso2.org/claims/active	2
3112	745	DisplayName	Active	2
3113	746	Description	Country name component	2
3114	746	MappedLocalClaim	http://wso2.org/claims/country	2
3115	746	DisplayName	Country	2
3116	747	Description	Meta - Location	2
3117	747	DisplayOrder	1	2
3118	747	Required	true	2
3119	747	SupportedByDefault	true	2
3120	747	MappedLocalClaim	http://wso2.org/claims/location	2
3121	747	DisplayName	Meta - Location	2
3122	748	Description	Manager - home	2
3123	748	DisplayOrder	1	2
3124	748	Required	true	2
3125	748	SupportedByDefault	true	2
3126	748	MappedLocalClaim	http://wso2.org/claims/extendedRef	2
3127	748	DisplayName	Manager - home	2
3128	749	Description	Temporary claim to invoke email verified feature	2
3129	749	DisplayOrder	1	2
3130	749	Required	true	2
3131	749	SupportedByDefault	true	2
3132	749	MappedLocalClaim	http://wso2.org/claims/identity/verifyEmail	2
3133	749	DisplayName	Verify Email	2
3134	750	Description	Address	2
3135	750	DisplayOrder	5	2
3136	750	SupportedByDefault	true	2
3137	750	MappedLocalClaim	http://wso2.org/claims/addresses	2
3138	750	DisplayName	Address	2
3139	751	Description	Temporary claim to invoke email ask Password feature	2
3140	751	DisplayOrder	1	2
3141	751	Required	true	2
3142	751	SupportedByDefault	true	2
3143	751	MappedLocalClaim	http://wso2.org/claims/identity/askPassword	2
3144	751	DisplayName	Ask Password	2
3145	752	Description	Full mailing address, formatted for display or use on a mailing label. This field MAY contain multiple lines, separated by newlines.	2
3146	752	MappedLocalClaim	http://wso2.org/claims/addresses.formatted	2
3147	752	DisplayName	Address Formatted	2
3148	753	Description	Manager - home	2
3149	753	DisplayOrder	1	2
3150	753	Required	true	2
3151	753	SupportedByDefault	true	2
3152	753	MappedLocalClaim	http://wso2.org/claims/stateorprovince	2
3153	753	DisplayName	Manager - home	2
3154	754	Description	System for Exchange of Excise Data Identifier	2
3155	754	DisplayOrder	1	2
3156	754	Required	true	2
3157	754	SupportedByDefault	true	2
3158	754	MappedLocalClaim	http://wso2.org/claims/nickname	2
3159	754	DisplayName	System for Exchange of Excise Data Identifier	2
3160	755	Description	Postalcode	2
3161	755	SupportedByDefault	true	2
3162	755	MappedLocalClaim	http://wso2.org/claims/postalcode	2
3163	755	DisplayName	Postalcode	2
3164	756	Description	End-User's preferred e-mail address.	2
3165	756	MappedLocalClaim	http://wso2.org/claims/emailaddress	2
3166	756	DisplayName	Email	2
3167	757	Description	Photo - Thumbnail	2
3168	757	DisplayOrder	5	2
3169	757	SupportedByDefault	true	2
3170	757	MappedLocalClaim	http://wso2.org/claims/thumbnail	2
3171	757	DisplayName	Photo - Thumbnail	2
3172	758	Description	Nick Name	2
3173	758	DisplayOrder	1	2
3174	758	Required	true	2
3177	758	DisplayName	Nick Name	2
3178	759	Description	The user principal name	2
3179	759	DisplayOrder	11	2
3180	759	SupportedByDefault	true	2
3181	759	MappedLocalClaim	http://wso2.org/claims/userprincipal	2
3182	759	DisplayName	User Principal	2
3183	760	Description	Honoric Prefix	2
3184	760	DisplayOrder	2	2
3185	760	Required	true	2
3186	760	SupportedByDefault	true	2
3187	760	MappedLocalClaim	http://wso2.org/claims/honorificPrefix	2
3188	760	DisplayName	Name - Honoric Prefix	2
3189	761	Description	Locality	2
3190	761	DisplayOrder	2	2
3191	761	Required	true	2
3192	761	SupportedByDefault	true	2
3193	761	MappedLocalClaim	http://wso2.org/claims/local	2
3194	761	DisplayName	Locality	2
3195	762	Description	IM - Gtalk	2
3196	762	DisplayOrder	5	2
3197	762	SupportedByDefault	true	2
3198	762	MappedLocalClaim	http://wso2.org/claims/gtalk	2
3199	762	DisplayName	IM - Gtalk	2
3200	763	Description	Address - Postal Code	2
3201	763	DisplayOrder	5	2
3202	763	SupportedByDefault	true	2
3203	763	MappedLocalClaim	http://wso2.org/claims/postalcode	2
3204	763	DisplayName	Address - Postal Code	2
3205	764	Description	Person Identifier	2
3206	764	DisplayOrder	1	2
3207	764	Required	true	2
3208	764	SupportedByDefault	true	2
3209	764	MappedLocalClaim	http://wso2.org/claims/userid	2
3210	764	DisplayName	Person Identifier	2
3211	765	Description	Given name(s) or first name(s) of the End-User. Note that in some cultures, people can have multiple given names; all can be present, with the names being separated by space characters.	2
3212	765	DisplayOrder	3	2
3213	765	SupportedByDefault	true	2
3214	765	MappedLocalClaim	http://wso2.org/claims/givenname	2
3215	765	DisplayName	Given Name	2
3216	766	Description	First Name	2
3217	766	Required	true	2
3218	766	SupportedByDefault	true	2
3219	766	MappedLocalClaim	http://wso2.org/claims/givenname	2
3220	766	DisplayName	First Name	2
3221	767	Description	City or locality component.	2
3222	767	MappedLocalClaim	http://wso2.org/claims/locality	2
3223	767	DisplayName	Locality	2
3224	768	Description	Preferred Language	2
3225	768	DisplayOrder	2	2
3226	768	Required	true	2
3227	768	SupportedByDefault	true	2
3228	768	MappedLocalClaim	http://wso2.org/claims/preferredLanguage	2
3229	768	DisplayName	Preferred Language	2
3230	769	Description	Gender	2
3231	769	SupportedByDefault	true	2
3232	769	MappedLocalClaim	http://wso2.org/claims/gender	2
3233	769	DisplayName	Gender	2
3234	770	Description	Time Zone	2
3235	770	MappedLocalClaim	http://wso2.org/claims/timeZone	2
3236	770	DisplayName	Time Zone	2
3237	771	Description	Title	2
3238	771	DisplayOrder	2	2
3239	771	Required	true	2
3240	771	SupportedByDefault	true	2
3241	771	MappedLocalClaim	http://wso2.org/claims/title	2
3242	771	DisplayName	Title	2
3243	772	Description	Home Phone	2
3244	772	SupportedByDefault	true	2
3245	772	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	2
3246	772	DisplayName	Home Phone	2
3247	773	Description	State, province, prefecture, or region component.	2
3248	773	MappedLocalClaim	http://wso2.org/claims/region	2
3249	773	DisplayName	One Time Password	2
3250	774	Description	Surname(s) or last name(s) of the End-User. Note that in some cultures, people can have multiple family names or no family name; all can be present, with the names being separated by space characters.	2
3251	774	DisplayOrder	4	2
3252	774	SupportedByDefault	true	2
3253	774	MappedLocalClaim	http://wso2.org/claims/lastname	2
3254	774	DisplayName	Surname	2
3255	775	Description	Other Email	2
3256	775	DisplayOrder	5	2
3257	775	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
3258	775	SupportedByDefault	true	2
3259	775	MappedLocalClaim	http://wso2.org/claims/emails.other	2
3260	775	DisplayName	Emails - Other Email	2
3261	776	Description	Cost Center	2
3262	776	DisplayOrder	1	2
3263	776	Required	true	2
3264	776	SupportedByDefault	true	2
3265	776	MappedLocalClaim	http://wso2.org/claims/costCenter	2
3266	776	DisplayName	Cost Center	2
3267	777	Description	Other Phone Number	2
3268	777	DisplayOrder	5	2
3269	777	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
3270	777	SupportedByDefault	true	2
3271	777	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.other	2
3272	777	DisplayName	Phone Numbers - Other	2
3273	778	Description	Address - Street	2
3274	778	DisplayOrder	5	2
3275	778	SupportedByDefault	true	2
3276	778	MappedLocalClaim	http://wso2.org/claims/streetaddress	2
3277	778	DisplayName	Address - Street	2
3278	779	Description	Id	2
3279	779	DisplayOrder	1	2
3280	779	Required	true	2
3281	779	SupportedByDefault	true	2
3282	779	MappedLocalClaim	http://wso2.org/claims/userid	2
3283	779	DisplayName	Id	2
3284	780	Description	Photo	2
3285	780	DisplayOrder	5	2
3286	780	SupportedByDefault	true	2
3287	780	MappedLocalClaim	http://wso2.org/claims/photos	2
3288	780	DisplayName	Photo	2
3289	781	Description	Groups	2
3290	781	DisplayOrder	5	2
3291	781	SupportedByDefault	true	2
3292	781	MappedLocalClaim	http://wso2.org/claims/groups	2
3293	781	DisplayName	Groups	2
3294	782	Description	Address - Home	2
3295	782	DisplayOrder	5	2
3296	782	SupportedByDefault	true	2
3297	782	MappedLocalClaim	http://wso2.org/claims/addresses.locality	2
3298	782	DisplayName	Address - Home	2
3299	783	Description	Honoric Prefix	2
3300	783	DisplayOrder	2	2
3301	783	Required	true	2
3302	783	SupportedByDefault	true	2
3303	783	MappedLocalClaim	http://wso2.org/claims/honorificPrefix	2
3304	783	DisplayName	Name - Honoric Prefix	2
3305	784	Description	Address - Locality	2
3306	784	DisplayOrder	5	2
3307	784	SupportedByDefault	true	2
3308	784	MappedLocalClaim	http://wso2.org/claims/addresses.locality	2
3309	784	DisplayName	Address - Locality	2
3310	785	Description	Middle Name	2
3311	785	DisplayOrder	2	2
3312	785	Required	true	2
3313	785	SupportedByDefault	true	2
3314	785	MappedLocalClaim	http://wso2.org/claims/middleName	2
3315	785	DisplayName	Name - Middle Name	2
3316	786	Description	Entitlements	2
3317	786	DisplayOrder	5	2
3318	786	SupportedByDefault	true	2
3319	786	MappedLocalClaim	http://wso2.org/claims/entitlements	2
3320	786	DisplayName	Entitlements	2
3321	787	Description	Display Name	2
3322	787	DisplayOrder	2	2
3323	787	Required	true	2
3324	787	SupportedByDefault	true	2
3325	787	MappedLocalClaim	http://wso2.org/claims/displayName	2
3326	787	DisplayName	Display Name	2
3327	788	Description	Meta - Created	2
3328	788	DisplayOrder	1	2
3329	788	Required	true	2
3330	788	SupportedByDefault	true	2
3331	788	MappedLocalClaim	http://wso2.org/claims/created	2
3332	788	DisplayName	Meta - Created	2
3333	789	Description	End-User's full name in displayable form including all name parts, possibly including titles and suffixes, ordered according to the End-User's locale and preferences	2
3334	789	DisplayOrder	2	2
3335	789	Required	true	2
3336	789	SupportedByDefault	true	2
3337	789	MappedLocalClaim	http://wso2.org/claims/fullname	2
3338	789	DisplayName	Full Name	2
3339	790	Description	End-User's locale, For example, en-US or fr-CA, en_US	2
3340	790	MappedLocalClaim	http://wso2.org/claims/local	2
3341	790	DisplayName	Locale	2
3342	791	Description	VAT Registration Number	2
3343	791	DisplayOrder	1	2
3344	791	Required	true	2
3345	791	SupportedByDefault	true	2
3346	791	MappedLocalClaim	http://wso2.org/claims/im	2
3347	791	DisplayName	VAT Registration Number	2
3348	792	Description	IM - Gtalk	2
3349	792	DisplayOrder	5	2
3350	792	SupportedByDefault	true	2
3351	792	MappedLocalClaim	http://wso2.org/claims/gtalk	2
3352	792	DisplayName	IM - Gtalk	2
3353	793	Description	Email Address	2
3354	793	Required	true	2
3355	793	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
3356	793	SupportedByDefault	true	2
3357	793	MappedLocalClaim	http://wso2.org/claims/emailaddress	2
3358	793	DisplayName	Email	2
3359	794	Description	Other Phone	2
3360	794	MappedLocalClaim	http://wso2.org/claims/otherphone	2
3361	794	DisplayName	Other Phone	2
3362	795	Description	Date of birth	2
3363	795	DisplayOrder	1	2
3364	795	Required	true	2
3365	795	SupportedByDefault	true	2
3366	795	MappedLocalClaim	http://wso2.org/claims/dob	2
3367	795	DisplayName	Date of birth	2
3368	796	Description	User Type	2
3369	796	DisplayOrder	2	2
3370	796	Required	true	2
3371	796	SupportedByDefault	true	2
3372	796	MappedLocalClaim	http://wso2.org/claims/userType	2
3373	796	DisplayName	User Type	2
3374	797	Description	Manager - Display Name	2
3375	797	DisplayOrder	1	2
3376	797	Required	true	2
3377	797	SupportedByDefault	true	2
3378	797	MappedLocalClaim	http://wso2.org/claims/extendedDisplayName	2
3379	797	DisplayName	Manager - Display Name	2
3380	798	Description	Full street address component, which MAY include house number, street name, Post Office Box, and multi-line extended street address information.	2
3381	798	MappedLocalClaim	http://wso2.org/claims/streetaddress	2
3382	798	DisplayName	Street Address	2
3383	799	Description	Nick Name	2
3384	799	DisplayOrder	2	2
3385	799	Required	true	2
3386	799	SupportedByDefault	true	2
3387	799	MappedLocalClaim	http://wso2.org/claims/nickname	2
3388	799	DisplayName	Nick Name	2
3389	800	Description	URL of the End-User's Web page or blog. This Web page SHOULD contain information published by the End-User or an organization that the End-User is affiliated with.	2
3390	800	DisplayOrder	10	2
3391	800	SupportedByDefault	true	2
3392	800	MappedLocalClaim	http://wso2.org/claims/url	2
3393	800	DisplayName	URL	2
3394	801	Description	Roles	2
3395	801	DisplayOrder	5	2
3396	801	SupportedByDefault	true	2
3397	801	MappedLocalClaim	http://wso2.org/claims/role	2
3398	801	DisplayName	Roles	2
3399	802	Description	Email Addresses	2
3400	802	DisplayOrder	3	2
3401	802	Required	true	2
3402	802	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
3403	802	SupportedByDefault	true	2
3404	802	MappedLocalClaim	http://wso2.org/claims/emailaddress	2
3405	802	DisplayName	Emails	2
3406	803	Description	Work Email	2
3407	803	DisplayOrder	5	2
3408	803	SupportedByDefault	true	2
3409	803	MappedLocalClaim	http://wso2.org/claims/emails.work	2
3410	803	DisplayName	Emails - Work Email	2
3411	804	Description	End-User's preferred telephone number. For example, +1 (425) 555-1212 or +56 (2) 687 2400., +1 (604) 555-1234;ext=5678.	2
3412	804	MappedLocalClaim	http://wso2.org/claims/telephone	2
3413	804	DisplayName	Phone Number	2
3414	805	Description	Gender	2
3415	805	DisplayOrder	1	2
3416	805	Required	true	2
3417	805	SupportedByDefault	true	2
3418	805	MappedLocalClaim	http://wso2.org/claims/gender	2
3419	805	DisplayName	Gender	2
3420	806	Description	Claim to store newly updated email address until the new email address is verified	2
3421	806	DisplayOrder	1	2
3422	806	Required	true	2
3423	806	SupportedByDefault	true	2
3424	806	MappedLocalClaim	http://wso2.org/claims/identity/emailaddress.pendingValue	2
3425	806	DisplayName	Verification Pending Email	2
3426	807	Description	Id	2
3427	807	DisplayOrder	1	2
3428	807	Required	true	2
3429	807	SupportedByDefault	true	2
3430	807	MappedLocalClaim	http://wso2.org/claims/userid	2
3431	807	DisplayName	Id	2
3432	808	Description	Nick Name	2
3433	808	DisplayOrder	2	2
3434	808	Required	true	2
3435	808	SupportedByDefault	true	2
3436	808	MappedLocalClaim	http://wso2.org/claims/nickname	2
3437	808	DisplayName	Nick Name	2
3438	809	Description	Organization -department	2
3439	809	DisplayOrder	1	2
3440	809	Required	true	2
3441	809	SupportedByDefault	true	2
3442	809	MappedLocalClaim	http://wso2.org/claims/department	2
3443	809	DisplayName	Organization -department	2
3444	810	Description	Home Phone	2
3445	810	DisplayOrder	5	2
3446	810	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
3447	810	SupportedByDefault	true	2
3448	810	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.home	2
3449	810	DisplayName	Phone Numbers - Home Phone Number	2
3450	811	Description	End-User's birthday, represented as an ISO 8601:2004 [ISO8601-2004] YYYY-MM-DD format. The year MAY be 0000, indicating that it is omitted. To represent only the year, YYYY format is allowed.	2
3451	811	MappedLocalClaim	http://wso2.org/claims/dob	2
3452	811	DisplayName	Birth Date	2
3453	812	Description	Current Address	2
3454	812	DisplayOrder	1	2
3455	812	Required	true	2
3456	812	SupportedByDefault	true	2
3457	812	MappedLocalClaim	http://wso2.org/claims/addresses	2
3458	812	DisplayName	Current Address	2
3459	813	Description	Standard Industrial Classification	2
3460	813	DisplayOrder	1	2
3461	813	Required	true	2
3462	813	SupportedByDefault	true	2
3463	813	MappedLocalClaim	http://wso2.org/claims/nickname	2
3464	813	DisplayName	Standard Industrial Classification	2
3465	814	Description	Given Name	2
3466	814	DisplayOrder	1	2
3467	814	Required	true	2
3468	814	SupportedByDefault	true	2
3469	814	MappedLocalClaim	http://wso2.org/claims/givenname	2
3470	814	DisplayName	Name - Given Name	2
3471	815	Description	Email Address	2
3472	815	Required	true	2
3473	815	RegEx	^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$	2
3474	815	SupportedByDefault	true	2
3475	815	MappedLocalClaim	http://wso2.org/claims/emailaddress	2
3476	815	DisplayName	Email	2
3477	816	Description	Mobile Number	2
3478	816	DisplayOrder	5	2
3479	816	RegEx	^\\s*(?:\\+?(\\d{1,3}))?[-. (]*(\\d{3})[-. )]*(\\d{3})[-. ]*(\\d{4})(?: *x(\\d+))?\\s*$	2
3480	816	SupportedByDefault	true	2
3481	816	MappedLocalClaim	http://wso2.org/claims/mobile	2
3482	816	DisplayName	Phone Numbers - Mobile Number	2
3483	817	Description	Country	2
3484	817	SupportedByDefault	true	2
3485	817	MappedLocalClaim	http://wso2.org/claims/country	2
3486	817	DisplayName	Country	2
3487	818	Description	Date of Birth	2
3488	818	SupportedByDefault	true	2
3489	818	MappedLocalClaim	http://wso2.org/claims/dob	2
3490	818	DisplayName	DOB	2
3491	819	Description	Time the End-User's information was last updated. Its value is a JSON number representing the number of seconds from 1970-01-01T0:0:0Z as measured in UTC until the date/time.	2
3492	819	MappedLocalClaim	http://wso2.org/claims/modified	2
3493	819	DisplayName	Updated At	2
3494	820	Description	Profile URL	2
3495	820	DisplayOrder	2	2
3496	820	Required	true	2
3497	820	SupportedByDefault	true	2
3498	820	MappedLocalClaim	http://wso2.org/claims/url	2
3499	820	DisplayName	Profile URL	2
3500	821	Description	Formatted Name	2
3501	821	DisplayOrder	2	2
3502	821	Required	true	2
3503	821	SupportedByDefault	true	2
3504	821	MappedLocalClaim	http://wso2.org/claims/formattedName	2
3505	821	DisplayName	Name - Formatted Name	2
3506	822	Description	Fax Number	2
3507	822	DisplayOrder	5	2
3508	822	SupportedByDefault	true	2
3509	822	MappedLocalClaim	http://wso2.org/claims/phoneNumbers.fax	2
3510	822	DisplayName	Phone Numbers - Fax Number	2
\.


--
-- Data for Name: idn_fed_auth_session_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_fed_auth_session_mapping (idp_session_id, session_id, idp_name, authenticator_id, protocol_type, time_created) FROM stdin;
\.


--
-- Data for Name: idn_function_library; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_function_library (name, description, type, tenant_id, data) FROM stdin;
\.


--
-- Data for Name: idn_identity_meta_data; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_identity_meta_data (user_name, tenant_id, metadata_type, metadata, valid) FROM stdin;
\.


--
-- Data for Name: idn_identity_user_data; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_identity_user_data (tenant_id, user_name, data_key, data_value) FROM stdin;
-1234	adp_dev_userCS	http://wso2.org/claims/identity/preferredChannel	EMAIL
-1234	adp_dev_userEC	http://wso2.org/claims/identity/preferredChannel	EMAIL
-1234	adp_dev_userSC	http://wso2.org/claims/identity/preferredChannel	EMAIL
\.


--
-- Data for Name: idn_oauth1a_access_token; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth1a_access_token (access_token, access_token_secret, consumer_key_id, scope, authz_user, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth1a_request_token; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth1a_request_token (request_token, request_token_secret, consumer_key_id, callback_url, scope, authorized, oauth_verifier, authz_user, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_access_token; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_access_token (token_id, access_token, refresh_token, consumer_key_id, authz_user, tenant_id, user_domain, user_type, grant_type, time_created, refresh_token_time_created, validity_period, refresh_token_validity_period, token_scope_hash, token_state, token_state_id, subject_identifier, access_token_hash, refresh_token_hash, idp_id, token_binding_ref) FROM stdin;
34767695-d890-41d8-b137-c248027fba90	3cfe01b6-b8a6-33a3-a341-7481724dbd94	a397b9e6-55bb-33ef-9f1c-97a2d8d8adc6	1	admin	-1234	PRIMARY	APPLICATION_USER	password	2026-03-27 10:16:59.351	2026-03-27 10:16:59.351	3600000	86400000	f659334ac710157416af9ae9faf0e47b	ACTIVE	NONE	admin	{"hash":"6ff146d4d4a28b3ebaee1b8ff49c4d71904cb2f14cda8875e74d51ac7054c539","algorithm":"SHA-256"}	{"hash":"543ce57565457f7f4f91deedd00fe65df8b28e9a00c2997a35f22af1cc3db564","algorithm":"SHA-256"}	1	NONE
610b6de6-59cc-4cc6-abe5-8696793826eb	78e0429e-7f9a-32fe-a527-42fdf0d7d369	4306e605-3334-3656-bc35-e65374774460	2	adp_crt_user	-1234	PRIMARY	APPLICATION_USER	password	2026-03-27 10:17:07.713	2026-03-27 10:17:07.713	3600000	86400000	06f7bc770d6e885d470569360f100906	ACTIVE	NONE	adp_crt_user	{"hash":"12468bcad784bf155dabdb88691f0df3434d4b98b7e31736b9366526a207a97a","algorithm":"SHA-256"}	{"hash":"8e160c2ee3fd5d947f5a41a06a7ffa0809649b5b1dbd897be9485f0f1735ccf3","algorithm":"SHA-256"}	1	NONE
9fc73b84-f6b5-4eb7-9f00-233c3a615300	775beeb5-b0d5-3c5d-a444-87007a2869bb	5e242ad4-121e-33d5-b127-a80a3bb57ba3	3	adp_pub_user	-1234	PRIMARY	APPLICATION_USER	password	2026-03-27 10:17:08.92	2026-03-27 10:17:08.92	3600000	86400000	02374ed687089ee2263ed792460dec9e	ACTIVE	NONE	adp_pub_user	{"hash":"54022e27503a7486a7e73958156acaf37ce9518b80e02d46ab10ab7275402e13","algorithm":"SHA-256"}	{"hash":"91aefb5689dff57e5af493d0d17dce0dddbbd3c3b2a0245d9cf88aa2b794be54","algorithm":"SHA-256"}	1	NONE
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	8d644f0e-c8e1-3630-ab19-d19e069e1d31	c425828c-df9a-30fe-874f-807461c37aca	4	adp_sub_user	-1234	PRIMARY	APPLICATION_USER	password	2026-03-27 10:18:05.8	2026-03-27 10:18:05.8	3600000	86400000	0091899c0a61433ba55215b2e9e0be35	ACTIVE	NONE	adp_sub_user	{"hash":"d13f09569bd09744e481468297f2fcf0e0d96b37f53bc3596e0e1b27670c0131","algorithm":"SHA-256"}	{"hash":"2f033782ab3df6b293cdfa4ab59de86ffb91d6d3c9a6f9dee326c62e2d6c6816","algorithm":"SHA-256"}	1	NONE
20c397d2-40e6-4754-b73e-025b4dfb2441	6be0a4af-ae78-47d8-a48a-252401413399	b6246077-6c48-325f-afa7-66f4c72c3c94	5	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:18:13.038	2026-03-27 10:18:13.038	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@carbon.super	{"hash":"c88d51abf816e395ba7191ffb053e533129999751db5997d1673cb14db0c46c8","algorithm":"SHA-256"}	{"hash":"5356e238650e204427c2a1d2cdc2557eaa715f2eaea419850172d398ecc454f8","algorithm":"SHA-256"}	1	NONE
d213a962-b8ad-4344-9c91-feb06c04633a	88368595-4e9f-4294-bb5e-ac5b068c4cce	9905fd6a-8bb5-387a-bd9e-74f72459e05e	5	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:18:13.677	2026-03-27 10:18:13.677	3600000	86400000	e270439d7847cf03b11dd0ae08f49eaf	ACTIVE	NONE	apim_reserved_user@carbon.super	{"hash":"57a6fef4870cce80af53ef58b19675bd31383f92f973d288f511e4be0bbf9bb9","algorithm":"SHA-256"}	{"hash":"6b8a8ef3e932e99c57e351eb97cc8d75bf392664523351b9012ddd348fc69cf6","algorithm":"SHA-256"}	1	NONE
e0a53685-af96-4a2b-9e91-32287a378884	9ca9647b-c122-41a5-9ecd-80b4a1cf2c97	e1e1e7f6-83af-3db4-99a9-632cb4117472	6	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:18:25.642	2026-03-27 10:18:25.642	2147483646000	2147483646000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@carbon.super	{"hash":"a194590e330916d17d6fda7c3cdec6d449dae6c487c5ad3bdd8762899c2d7e70","algorithm":"SHA-256"}	{"hash":"fb7e9bf394e6346cf1102609c83ad50ffff046cd98f22f230c9b4ad15c921bdf","algorithm":"SHA-256"}	1	NONE
0efd6c87-491b-43e5-9295-832821d66f45	a82d1f7f-2889-384d-b4e8-aa0b9f509be5	626455ec-a48b-35fc-bfc2-4e147f470107	7	admin	1	PRIMARY	APPLICATION_USER	password	2026-03-27 10:18:42.227	2026-03-27 10:18:42.227	3600000	86400000	f659334ac710157416af9ae9faf0e47b	ACTIVE	NONE	admin	{"hash":"1b656231b0ab0ac92c0518dc9c08869bdb845c16fb636ad7eed235c5a19882d5","algorithm":"SHA-256"}	{"hash":"d4818e3b9225269b25852702daaee8f1a3786d196c5686391a07cb9f3b63def4","algorithm":"SHA-256"}	2	NONE
b106ccff-abc7-4aed-9deb-5950f476d2ed	65f3d49b-7391-33d6-8d53-2281941c4428	788a9c9f-d436-370c-bd32-f44e882ad4a2	8	adp_crt_user	1	PRIMARY	APPLICATION_USER	password	2026-03-27 10:18:48.671	2026-03-27 10:18:48.671	3600000	86400000	06f7bc770d6e885d470569360f100906	ACTIVE	NONE	adp_crt_user	{"hash":"71e2284ea31c7cccb7a6c5aba005eb6f655bfd7ea3fcccf4491c9cad0eea2a5f","algorithm":"SHA-256"}	{"hash":"b236b868d66e8dc193c8115506972fbefa747056c27b17d3b345f62f759dd27d","algorithm":"SHA-256"}	2	NONE
d06a860f-9081-4920-a036-0921f5b368f3	3ac4fa43-648e-38be-804e-0734fd7138e9	fc7e65ef-5125-3437-8d9e-24c448247581	9	adp_pub_user	1	PRIMARY	APPLICATION_USER	password	2026-03-27 10:18:49.862	2026-03-27 10:18:49.862	3600000	86400000	02374ed687089ee2263ed792460dec9e	ACTIVE	NONE	adp_pub_user	{"hash":"fdbf2b07d5cb2be5070d15c9df6ceb57ad8bbd1a9b97955d5cfcfccb043a3545","algorithm":"SHA-256"}	{"hash":"e133db6769b7e4ad891d358b50e5448ec95b1f8491453586e2f9f972ab60207a","algorithm":"SHA-256"}	2	NONE
79e79d21-a484-41ed-ad6e-3ac6bad08666	33f84ac8-52f4-38ca-bd4e-c5d830f17f60	11ab164f-9fdf-303c-a409-ddf1b7149ac7	10	adp_sub_user	1	PRIMARY	APPLICATION_USER	password	2026-03-27 10:19:46.344	2026-03-27 10:19:46.344	3600000	86400000	0091899c0a61433ba55215b2e9e0be35	ACTIVE	NONE	adp_sub_user	{"hash":"64671c5ee1e22bda51463e87c8b0670481330e28ebabedd86bf2ba0cd4ceb623","algorithm":"SHA-256"}	{"hash":"f8cf01376f5e0b7c9ac90f74c86cec29e81457581d1524f311b573f09d00dff1","algorithm":"SHA-256"}	2	NONE
39119485-3dd9-4d24-92ee-05968900bb68	fa752ebf-845e-49f5-97a8-4fd59c294822	ee317e41-2872-3ddf-95d6-ec4d34052858	11	apim_reserved_user	1	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:19:52.933	2026-03-27 10:19:52.933	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@adpexample.com	{"hash":"696122b58ba6629de8a19b84d2b724bc97c9107961b31b265d4f673f750c950f","algorithm":"SHA-256"}	{"hash":"7f5af6a3a91d93c5695a67049029480201d78232222249cf4e1f003a6c34523c","algorithm":"SHA-256"}	2	NONE
3992de80-1c5f-46d5-a56f-fec66421e1f7	523047da-536f-4d35-8b5f-92d07f48cd52	783df8e4-8345-336b-b1aa-ac02648b0db0	11	apim_reserved_user	1	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:19:53.546	2026-03-27 10:19:53.546	3600000	86400000	e270439d7847cf03b11dd0ae08f49eaf	ACTIVE	NONE	apim_reserved_user@adpexample.com	{"hash":"c5a7bbaa10b2b84344eda33b444b3b947861a454e5f7ed04a63b6fe2fe9607c5","algorithm":"SHA-256"}	{"hash":"0c04c9233f286f5813932361c82accd195a8a84d3f866afd7583dce7f6af7cfd","algorithm":"SHA-256"}	2	NONE
18864299-b6b5-468e-b170-d5d1ca2d8791	5349f31c-2c8c-4f33-9f73-23e2989aa73f	38650efb-025d-34e7-a284-397eff5daa06	12	apim_reserved_user	1	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:20:05.835	2026-03-27 10:20:05.835	2147483646000	2147483646000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@adpexample.com	{"hash":"ce881edef70edef4cc10bb961985f98cd729cc909600145b47d879576dbc7907","algorithm":"SHA-256"}	{"hash":"92b86d26611bc4b395d4c0f080851585fc3e7f3b652ddde06233889c7ceba841","algorithm":"SHA-256"}	2	NONE
58b49e23-7b42-4a8b-a23e-cfb0b74c3d85	b0ef3518-c2ed-43cf-a062-1ff60f3ec774	92d05e24-8e47-30e8-b323-04c009ef51bc	13	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:20:10.676	2026-03-27 10:20:10.676	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@carbon.super	{"hash":"ae34f5a2347f5e5aae30f5d701cccc46e0bc3d9e7687b216557de268fae56d00","algorithm":"SHA-256"}	{"hash":"8263cd3ef2c39472098cbfdb1f64a0313e12771de189afe47b7ca8c49bc3c8a9","algorithm":"SHA-256"}	1	NONE
cfece152-14b5-4c52-9906-7d288269cd41	c5361090-a111-3999-8722-b96af8c0bff5	3cf94208-c5ef-3d80-9296-d3bb5ddac533	14	admin	2	PRIMARY	APPLICATION_USER	password	2026-03-27 10:20:24.849	2026-03-27 10:20:24.849	3600000	86400000	f659334ac710157416af9ae9faf0e47b	ACTIVE	NONE	admin	{"hash":"62572d7ea9813cb8225ba62d3fb0a55a2d253f5992cd52b720789f153c82cd9f","algorithm":"SHA-256"}	{"hash":"8b6c24bd72dbf237ccbce1fb9da8291750f65003aee718f058c56b05bfbcb6ac","algorithm":"SHA-256"}	3	NONE
e421b271-49ab-4d6b-94cc-818cde51bef3	cd8b2a0c-48fe-36fd-b534-f48c333e50c1	d6d235a5-f454-3d66-9a47-fb35765c5421	15	adp_crt_user	2	PRIMARY	APPLICATION_USER	password	2026-03-27 10:20:31.187	2026-03-27 10:20:31.187	3600000	86400000	06f7bc770d6e885d470569360f100906	ACTIVE	NONE	adp_crt_user	{"hash":"e5635219c5db2a4c90e995f14bb33f9750c4255b2d9d39a0761fbf3b8ac4dea4","algorithm":"SHA-256"}	{"hash":"db39bb6f74e2c78dca855a25fac1f216c624345dca75d91a5ea7a11acfa4f3aa","algorithm":"SHA-256"}	3	NONE
4143400d-f381-4567-918a-f91239e64bdd	0748f0c5-57cd-3211-a7d1-fee24a7e8fc3	5106514a-6dc1-3d21-b351-0ac809d120b0	16	adp_pub_user	2	PRIMARY	APPLICATION_USER	password	2026-03-27 10:20:32.389	2026-03-27 10:20:32.389	3600000	86400000	02374ed687089ee2263ed792460dec9e	ACTIVE	NONE	adp_pub_user	{"hash":"de00e9b4199696c70953504859f5c2350ee033c5497f34ec8367a280b0c0bc77","algorithm":"SHA-256"}	{"hash":"5feeb4e8f422ab8977efea052aa2177aaa48b612bc17f912ffddbae0086f861d","algorithm":"SHA-256"}	3	NONE
fc511661-bda5-4f22-8fdc-1b64f451d144	19be7b80-bbf3-3a0e-a64e-b36323e23c92	4b4ccc28-b8fb-326c-b4f1-8135cbb42963	17	adp_sub_user	2	PRIMARY	APPLICATION_USER	password	2026-03-27 10:21:26.648	2026-03-27 10:21:26.648	3600000	86400000	0091899c0a61433ba55215b2e9e0be35	ACTIVE	NONE	adp_sub_user	{"hash":"33081cd113c71780ac3935d939bf0ef7d5a0df56826838a52c81bad3a2fd5b4f","algorithm":"SHA-256"}	{"hash":"7b10366815ba64591420bd89f1271872eb219070dbdce6aa8d94e9f374144294","algorithm":"SHA-256"}	3	NONE
38a8f15e-348e-4ba3-ba26-bcbd82a06378	cc96efae-ac28-458e-bc2a-4b4d23fd7e4f	d89f5e19-e418-3e30-ab73-03d602a17486	18	apim_reserved_user	2	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:21:33.278	2026-03-27 10:21:33.278	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@adpsample.com	{"hash":"fb57b7713a6033bf4f35ba14d13d4937f24da032968187d4855dfe1519fb8f75","algorithm":"SHA-256"}	{"hash":"fdab7fd10f8a9279a6525d4ccd296c82ea784ec52d4015a0f35069153cdaaca1","algorithm":"SHA-256"}	3	NONE
6484d9ed-ca12-4b4f-9e20-6067ab2058ae	b46b9433-4c85-4102-949f-d5f7cc2a039f	102dde09-76f8-3f29-bb53-eedfbcf282b2	18	apim_reserved_user	2	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:21:33.902	2026-03-27 10:21:33.902	3600000	86400000	e270439d7847cf03b11dd0ae08f49eaf	ACTIVE	NONE	apim_reserved_user@adpsample.com	{"hash":"02dbb8aa68b752cf4a84e89e4ab9960e4c409cde004ead23b67886058ac93649","algorithm":"SHA-256"}	{"hash":"c7240d115917df09cf92dd5235e3a5f85c2956193f001d89b0c03bd4f2f70bd4","algorithm":"SHA-256"}	3	NONE
5d65f1d6-2d37-44a0-9650-4ab24ed157b2	7f9745c9-9bae-4639-b7c1-f68a9f1efc86	748e47c3-bc37-3d78-8250-9e8158545f5f	19	apim_reserved_user	2	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:21:46.052	2026-03-27 10:21:46.052	2147483646000	2147483646000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@adpsample.com	{"hash":"af970f532445dc7b788b0ad79ef4cbec83e5c4686008172e21e69267503507e3","algorithm":"SHA-256"}	{"hash":"5abe12db92816c8b5c06156a0ad83ad8a7a5d4d0a8f6be0ce9aa61c3d95f5b51","algorithm":"SHA-256"}	3	NONE
4676b84f-0b1f-4cc8-9d29-4a2354d17b91	54f3378d-fb49-47f8-bcf3-ab4b06d44392	78e9a297-0a61-36c3-8347-8da7c73b5a70	20	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:21:50.922	2026-03-27 10:21:50.922	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	ACTIVE	NONE	apim_reserved_user@carbon.super	{"hash":"0765a977263a44f8527d669b9b28e21d754ffedd25feb8ecac13b1c7c24987b7","algorithm":"SHA-256"}	{"hash":"4d4f4e2479152fb9b77d2c6572578eccf0db9044e35ce8006385cf4db7b1da5a","algorithm":"SHA-256"}	1	NONE
\.


--
-- Data for Name: idn_oauth2_access_token_audit; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_access_token_audit (token_id, access_token, refresh_token, consumer_key_id, authz_user, tenant_id, user_domain, user_type, grant_type, time_created, refresh_token_time_created, validity_period, refresh_token_validity_period, token_scope_hash, token_state, token_state_id, subject_identifier, access_token_hash, refresh_token_hash, invalidated_time, idp_id) FROM stdin;
98b61605-7745-4244-8e5b-2c34a645a9af	8795e1e1-3931-4251-bc19-da1b916d61a8	68ee83f2-2fda-3239-9fcb-4c52d9f898cf	13	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:20:10.06	2026-03-27 10:20:10.06	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	REVOKED	f72e785a-8d4b-4774-abbb-64dfc065e5fe	apim_reserved_user@carbon.super	{"hash":"7a6427a7ef84e8ca4281153676719444c84c57432a7bf0cc3a72fe16b4a353e9","algorithm":"SHA-256"}	{"hash":"8d269a413225b0219039bcddcee5debae209249b7fb7dfd82bd8f0d255134f16","algorithm":"SHA-256"}	2026-03-27 15:50:10.667	1
12322f49-17a8-4acf-bc86-2432df24c477	c1e729f8-09bd-465d-a153-fd941342e2b2	4bb3d462-9f3e-3385-8ccf-3905ce6aaa23	20	apim_reserved_user	-1234	PRIMARY	APPLICATION	client_credentials	2026-03-27 10:21:50.335	2026-03-27 10:21:50.335	3600000	86400000	c21f969b5f03d33d43e04f8f136e7682	REVOKED	08ceaa64-690e-4931-986b-9250daebc633	apim_reserved_user@carbon.super	{"hash":"653a1b33ebdbcc002a6a26392859ee978684303e7f70c39a360564c0cac9d193","algorithm":"SHA-256"}	{"hash":"0199281dae3f29e24c0c611c481217e34fee946eb43f4cdb54a09b097f36e484","algorithm":"SHA-256"}	2026-03-27 15:51:50.918	1
\.


--
-- Data for Name: idn_oauth2_access_token_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_access_token_scope (token_id, token_scope, tenant_id) FROM stdin;
34767695-d890-41d8-b137-c248027fba90	apim:admin	-1234
34767695-d890-41d8-b137-c248027fba90	apim:admin_operations	-1234
34767695-d890-41d8-b137-c248027fba90	apim:api_create	-1234
34767695-d890-41d8-b137-c248027fba90	apim:api_import_export	-1234
34767695-d890-41d8-b137-c248027fba90	apim:api_publish	-1234
34767695-d890-41d8-b137-c248027fba90	apim:api_view	-1234
34767695-d890-41d8-b137-c248027fba90	apim:app_manage	-1234
34767695-d890-41d8-b137-c248027fba90	apim:document_create	-1234
34767695-d890-41d8-b137-c248027fba90	apim:mediation_policy_create	-1234
34767695-d890-41d8-b137-c248027fba90	apim:mediation_policy_view	-1234
34767695-d890-41d8-b137-c248027fba90	apim:scope_manage	-1234
34767695-d890-41d8-b137-c248027fba90	apim:shared_scope_manage	-1234
34767695-d890-41d8-b137-c248027fba90	apim:sub_manage	-1234
34767695-d890-41d8-b137-c248027fba90	apim:subscribe	-1234
34767695-d890-41d8-b137-c248027fba90	apim:subscription_view	-1234
34767695-d890-41d8-b137-c248027fba90	apim:tier_manage	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:api_create	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:api_view	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:client_certificates_add	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:client_certificates_update	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:client_certificates_view	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:document_create	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:document_manage	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:ep_certificates_add	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:ep_certificates_update	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:ep_certificates_view	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:mediation_policy_create	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:mediation_policy_manage	-1234
610b6de6-59cc-4cc6-abe5-8696793826eb	apim:mediation_policy_view	-1234
9fc73b84-f6b5-4eb7-9f00-233c3a615300	apim:api_publish	-1234
9fc73b84-f6b5-4eb7-9f00-233c3a615300	apim:api_view	-1234
9fc73b84-f6b5-4eb7-9f00-233c3a615300	apim:subscription_block	-1234
9fc73b84-f6b5-4eb7-9f00-233c3a615300	apim:subscription_view	-1234
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	apim:api_key	-1234
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	apim:app_manage	-1234
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	apim:store_settings	-1234
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	apim:sub_alert_manage	-1234
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	apim:sub_manage	-1234
31f1f376-ff92-4d14-a3f0-a5c898c4ca53	apim:subscribe	-1234
20c397d2-40e6-4754-b73e-025b4dfb2441	default	-1234
d213a962-b8ad-4344-9c91-feb06c04633a	adp-film-subscriber	-1234
d213a962-b8ad-4344-9c91-feb06c04633a	adp-local-scope-without-roles	-1234
d213a962-b8ad-4344-9c91-feb06c04633a	adp-shared-scope-without-roles	-1234
e0a53685-af96-4a2b-9e91-32287a378884	default	-1234
0efd6c87-491b-43e5-9295-832821d66f45	apim:admin	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:admin_operations	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:api_create	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:api_import_export	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:api_publish	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:api_view	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:app_manage	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:document_create	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:mediation_policy_create	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:mediation_policy_view	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:scope_manage	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:shared_scope_manage	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:sub_manage	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:subscribe	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:subscription_view	1
0efd6c87-491b-43e5-9295-832821d66f45	apim:tier_manage	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:api_create	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:api_view	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:client_certificates_add	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:client_certificates_update	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:client_certificates_view	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:document_create	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:document_manage	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:ep_certificates_add	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:ep_certificates_update	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:ep_certificates_view	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:mediation_policy_create	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:mediation_policy_manage	1
b106ccff-abc7-4aed-9deb-5950f476d2ed	apim:mediation_policy_view	1
d06a860f-9081-4920-a036-0921f5b368f3	apim:api_publish	1
d06a860f-9081-4920-a036-0921f5b368f3	apim:api_view	1
d06a860f-9081-4920-a036-0921f5b368f3	apim:subscription_block	1
d06a860f-9081-4920-a036-0921f5b368f3	apim:subscription_view	1
79e79d21-a484-41ed-ad6e-3ac6bad08666	apim:api_key	1
79e79d21-a484-41ed-ad6e-3ac6bad08666	apim:app_manage	1
79e79d21-a484-41ed-ad6e-3ac6bad08666	apim:store_settings	1
79e79d21-a484-41ed-ad6e-3ac6bad08666	apim:sub_alert_manage	1
79e79d21-a484-41ed-ad6e-3ac6bad08666	apim:sub_manage	1
79e79d21-a484-41ed-ad6e-3ac6bad08666	apim:subscribe	1
39119485-3dd9-4d24-92ee-05968900bb68	default	1
3992de80-1c5f-46d5-a56f-fec66421e1f7	adp-film-subscriber	1
3992de80-1c5f-46d5-a56f-fec66421e1f7	adp-local-scope-without-roles	1
3992de80-1c5f-46d5-a56f-fec66421e1f7	adp-shared-scope-without-roles	1
18864299-b6b5-468e-b170-d5d1ca2d8791	default	1
58b49e23-7b42-4a8b-a23e-cfb0b74c3d85	default	-1234
cfece152-14b5-4c52-9906-7d288269cd41	apim:admin	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:admin_operations	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:api_create	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:api_import_export	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:api_publish	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:api_view	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:app_manage	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:document_create	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:mediation_policy_create	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:mediation_policy_view	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:scope_manage	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:shared_scope_manage	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:sub_manage	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:subscribe	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:subscription_view	2
cfece152-14b5-4c52-9906-7d288269cd41	apim:tier_manage	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:api_create	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:api_view	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:client_certificates_add	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:client_certificates_update	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:client_certificates_view	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:document_create	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:document_manage	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:ep_certificates_add	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:ep_certificates_update	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:ep_certificates_view	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:mediation_policy_create	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:mediation_policy_manage	2
e421b271-49ab-4d6b-94cc-818cde51bef3	apim:mediation_policy_view	2
4143400d-f381-4567-918a-f91239e64bdd	apim:api_publish	2
4143400d-f381-4567-918a-f91239e64bdd	apim:api_view	2
4143400d-f381-4567-918a-f91239e64bdd	apim:subscription_block	2
4143400d-f381-4567-918a-f91239e64bdd	apim:subscription_view	2
fc511661-bda5-4f22-8fdc-1b64f451d144	apim:api_key	2
fc511661-bda5-4f22-8fdc-1b64f451d144	apim:app_manage	2
fc511661-bda5-4f22-8fdc-1b64f451d144	apim:store_settings	2
fc511661-bda5-4f22-8fdc-1b64f451d144	apim:sub_alert_manage	2
fc511661-bda5-4f22-8fdc-1b64f451d144	apim:sub_manage	2
fc511661-bda5-4f22-8fdc-1b64f451d144	apim:subscribe	2
38a8f15e-348e-4ba3-ba26-bcbd82a06378	default	2
6484d9ed-ca12-4b4f-9e20-6067ab2058ae	adp-film-subscriber	2
6484d9ed-ca12-4b4f-9e20-6067ab2058ae	adp-local-scope-without-roles	2
6484d9ed-ca12-4b4f-9e20-6067ab2058ae	adp-shared-scope-without-roles	2
5d65f1d6-2d37-44a0-9650-4ab24ed157b2	default	2
4676b84f-0b1f-4cc8-9d29-4a2354d17b91	default	-1234
\.


--
-- Data for Name: idn_oauth2_authorization_code; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_authorization_code (code_id, authorization_code, consumer_key_id, callback_url, scope, authz_user, tenant_id, user_domain, time_created, validity_period, state, token_id, subject_identifier, pkce_code_challenge, pkce_code_challenge_method, authorization_code_hash, idp_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_authz_code_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_authz_code_scope (code_id, scope, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_ciba_auth_code; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_ciba_auth_code (auth_code_key, auth_req_id, issued_time, consumer_key, last_polled_time, polling_interval, expires_in, authenticated_user_name, user_store_domain, tenant_id, auth_req_status, idp_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_ciba_request_scopes; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_ciba_request_scopes (auth_code_key, scope) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_device_flow; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_device_flow (code_id, device_code, user_code, consumer_key_id, last_poll_time, expiry_time, time_created, poll_time, status, authz_user, tenant_id, user_domain, idp_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_device_flow_scopes; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_device_flow_scopes (id, scope_id, scope) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_resource_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_resource_scope (resource_path, scope_id, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_scope (scope_id, name, display_name, description, tenant_id, scope_type) FROM stdin;
1	openid	openid	\N	-1234	OIDC
2	email	email	\N	-1234	OIDC
3	profile	profile	\N	-1234	OIDC
4	phone	phone	\N	-1234	OIDC
5	address	address	\N	-1234	OIDC
6	internal_feature_management	Feature Management	\N	-1234	OAUTH2
7	internal_modify_tenants	Modify Tenant	\N	-1234	OAUTH2
8	internal_list_tenants	List Tenant	\N	-1234	OAUTH2
9	internal_server_admin	Server Admin	\N	-1234	OAUTH2
10	internal_monitor_metrics	Monitor internal_monitor_metrics	\N	-1234	OAUTH2
11	internal_monitor_bpel	Monitor BPEL	\N	-1234	OAUTH2
12	internal_monitor_attachment	Monitor Attachments	\N	-1234	OAUTH2
13	internal_manage_passwords	Manage Passwords	\N	-1234	OAUTH2
14	internal_manage_profiles	Manage Profiles	\N	-1234	OAUTH2
15	internal_manage_users	Manage Users	\N	-1234	OAUTH2
16	internal_manage_provisining	Manager Provisioning	\N	-1234	OAUTH2
17	internal_configure_datasources	Configure datasources	\N	-1234	OAUTH2
18	internal_configure_themes	Configure Themes	\N	-1234	OAUTH2
19	internal_login	Login	\N	-1234	OAUTH2
20	internal_manage_event_streams	Event streams	\N	-1234	OAUTH2
21	internal_search_advanced	Advanced Search	\N	-1234	OAUTH2
22	internal_search_resouces	Basic Search	\N	-1234	OAUTH2
23	internal_bpel_instances	Manage BPEL Process Instances	\N	-1234	OAUTH2
24	internal_bpel_proceses	Manage BPEL Processes	\N	-1234	OAUTH2
25	internal_bpel_packages	Manage BPEL Packages	\N	-1234	OAUTH2
26	internal_add_bpel	Add BPEL	\N	-1234	OAUTH2
27	internal_resouces_browse	Browse Resouces	\N	-1234	OAUTH2
28	internal_resouces_notifications	Manage Resouces Notifications	\N	-1234	OAUTH2
29	internal_add_module	Add Modules	\N	-1234	OAUTH2
30	internal_add_services	Add Services	\N	-1234	OAUTH2
31	internal_add_webapp	Add WebApps	\N	-1234	OAUTH2
32	internal_add_attachements	Add Attachements	\N	-1234	OAUTH2
33	internal_add_extensions	Add Extensions	\N	-1234	OAUTH2
34	internal_list_extensions	List Extensions	\N	-1234	OAUTH2
35	internal_manage_pep	Manage PEP	\N	-1234	OAUTH2
36	internal_security_manage_update	Security Manage Update	\N	-1234	OAUTH2
37	internal_security_manage_view	Security Manage View	\N	-1234	OAUTH2
38	internal_security_manage_create	Security Manage Create	\N	-1234	OAUTH2
39	internal_security_manage_delete	Security Manage Delete	\N	-1234	OAUTH2
40	internal_session_view	View Sessions	\N	-1234	OAUTH2
41	internal_session_delete	Delete Sessions	\N	-1234	OAUTH2
42	internal_claim_meta_create	Create Claims Metadata	\N	-1234	OAUTH2
43	internal_claim_meta_delete	Delete Claims Metadata	\N	-1234	OAUTH2
44	internal_claim_meta_update	Update Cliams Metadata	\N	-1234	OAUTH2
45	internal_claim_meta_view	View Claims Metadata	\N	-1234	OAUTH2
46	internal_claim_manage_view	View Claims	\N	-1234	OAUTH2
47	internal_claim_manage_update	Update Claims	\N	-1234	OAUTH2
48	internal_claim_manage_create	Create Claims	\N	-1234	OAUTH2
49	internal_claim_manage_delete	Delete Claims	\N	-1234	OAUTH2
50	internal_userrole_ui_create	Show User Role Manage UI	\N	-1234	OAUTH2
51	internal_auth_seq_view	View Authentication Sequence	\N	-1234	OAUTH2
52	internal_auth_seq_update	Update Authentication Sequence	\N	-1234	OAUTH2
53	internal_auth_seq_create	Create Authentication Sequence	\N	-1234	OAUTH2
54	internal_auth_seq_delete	Delete Authentication Sequence	\N	-1234	OAUTH2
55	internal_pap_delete	Delete PAP	\N	-1234	OAUTH2
56	internal_pap_publish	Publish PAP	\N	-1234	OAUTH2
57	internal_pap_create	Create PAP	\N	-1234	OAUTH2
58	internal_pap_view	View PAP	\N	-1234	OAUTH2
59	internal_pap_rollback	Rolback PAP	\N	-1234	OAUTH2
60	internal_pap_order	Order PAP	\N	-1234	OAUTH2
61	internal_pap_demote	Demote PAP	\N	-1234	OAUTH2
62	internal_pap_update	Update PAP	\N	-1234	OAUTH2
63	internal_pap_enable	Enable PAP	\N	-1234	OAUTH2
64	internal_pap_list	List PAP	\N	-1234	OAUTH2
65	internal_pap_subscriber_update	Update PAP Subscriber	\N	-1234	OAUTH2
66	internal_pap_subscriber_view	View PAP Subscriber	\N	-1234	OAUTH2
67	internal_pap_subscriber_list	List PAP Subscriber	\N	-1234	OAUTH2
68	internal_pap_subscriber_create	Create PAP Subscriber	\N	-1234	OAUTH2
69	internal_pap_subscriber_delete	Delete PAP Subscriber	\N	-1234	OAUTH2
70	internal_pdp_view	View PDP	\N	-1234	OAUTH2
71	internal_pdp_manage	Manage PDP	\N	-1234	OAUTH2
72	internal_pdp_test	Test PDP	\N	-1234	OAUTH2
73	internal_pep_manage	Manage PEP	\N	-1234	OAUTH2
74	internal_idp_delete	Delete IDP	\N	-1234	OAUTH2
75	internal_idp_create	Create IDP	\N	-1234	OAUTH2
76	internal_idp_view	View IDP	\N	-1234	OAUTH2
77	internal_idp_update	Update IDP	\N	-1234	OAUTH2
78	internal_config_mgt_view	View Configs	\N	-1234	OAUTH2
79	internal_config_mgt_update	Update Configs	\N	-1234	OAUTH2
80	internal_config_mgt_delete	Delete Configs	\N	-1234	OAUTH2
81	internal_config_mgt_list	List Configs	\N	-1234	OAUTH2
82	internal_config_mgt_add	Add Configs	\N	-1234	OAUTH2
83	internal_keystore_view	View Keystore	\N	-1234	OAUTH2
84	internal_keystore_update	Update Keystore	\N	-1234	OAUTH2
85	internal_keystore_delete	Delete Keystore	\N	-1234	OAUTH2
86	internal_keystore_create	Create Keystore	\N	-1234	OAUTH2
87	internal_consent_mgt_list	List Consents	\N	-1234	OAUTH2
88	internal_consent_mgt_view	View Consents	\N	-1234	OAUTH2
89	internal_consent_mgt_add	Add Consents	\N	-1234	OAUTH2
90	internal_consent_mgt_delete	Delete Consents	\N	-1234	OAUTH2
91	internal_app_template_view	View Application Templates	\N	-1234	OAUTH2
92	internal_app_template_update	Update Application Templates	\N	-1234	OAUTH2
93	internal_app_template_delete	Delete Application Templates	\N	-1234	OAUTH2
94	internal_app_template_create	Create Application Templates	\N	-1234	OAUTH2
95	internal_identity_mgt_view	View IdentityMgt	\N	-1234	OAUTH2
96	internal_identity_mgt_update	Update IdentityMgt	\N	-1234	OAUTH2
97	internal_identity_mgt_create	Ceate IdentityMgt	\N	-1234	OAUTH2
98	internal_identity_mgt_delete	Delete IdentityMgt	\N	-1234	OAUTH2
99	internal_workflow_association_view	View Workflow Associations	\N	-1234	OAUTH2
100	internal_workflow_association_delete	Delete Workflow Associations	\N	-1234	OAUTH2
101	internal_workflow_association_create	Create Workflow Associations	\N	-1234	OAUTH2
102	internal_workflow_association_update	Update Workflow Associations	\N	-1234	OAUTH2
103	internal_workflow_def_create	Create Workflow Definition	\N	-1234	OAUTH2
104	internal_workflow_def_delete	Delete Workflow Definition	\N	-1234	OAUTH2
105	internal_workflow_def_view	View Workflow Definition	\N	-1234	OAUTH2
106	internal_workflow_def_update	Update Workflow Definition	\N	-1234	OAUTH2
107	internal_workflow_profile_view	View Workflow Profile	\N	-1234	OAUTH2
108	internal_workflow_profile_create	Create Workflow Profile	\N	-1234	OAUTH2
109	internal_workflow_profile_delete	Delete Workflow Profile	\N	-1234	OAUTH2
110	internal_workflow_profile_update	Update Workflow Profile	\N	-1234	OAUTH2
111	internal_workflow_monitor_view	View Workflow	\N	-1234	OAUTH2
112	internal_workflow_monitor_delete	Delete Workflow	\N	-1234	OAUTH2
113	internal_email_mgt_update	Update Email Configs	\N	-1234	OAUTH2
114	internal_email_mgt_create	Create Email Configs	\N	-1234	OAUTH2
115	internal_email_mgt_delete	Delete Email Configs	\N	-1234	OAUTH2
116	internal_email_mgt_view	View Email Configs	\N	-1234	OAUTH2
117	internal_manage_provisioning	Manage User Provisioning	\N	-1234	OAUTH2
118	internal_user_mgt_update	Update Users	\N	-1234	OAUTH2
119	internal_user_mgt_delete	Deleate Users	\N	-1234	OAUTH2
120	internal_user_mgt_list	List Users	\N	-1234	OAUTH2
121	internal_user_mgt_view	View Users	\N	-1234	OAUTH2
122	internal_user_mgt_create	Create Users	\N	-1234	OAUTH2
123	internal_user_association_view	View User Associations	\N	-1234	OAUTH2
124	internal_user_association_update	Update User Associations	\N	-1234	OAUTH2
125	internal_user_association_delete	Delete User Associations	\N	-1234	OAUTH2
126	internal_user_association_create	Create User Associations	\N	-1234	OAUTH2
127	internal_user_count_delete	Delete User Count	\N	-1234	OAUTH2
128	internal_user_count_view	View User Count	\N	-1234	OAUTH2
129	internal_user_count_create	Create User Count	\N	-1234	OAUTH2
130	internal_user_count_update	Update User Count	\N	-1234	OAUTH2
131	internal_userstore_delete	Delete User Stores	\N	-1234	OAUTH2
132	internal_userstore_create	Create User Stores	\N	-1234	OAUTH2
133	internal_userstore_update	Update User Stores	\N	-1234	OAUTH2
134	internal_userstore_view	View User Stores	\N	-1234	OAUTH2
135	internal_functional_lib_update	Update Functional Library	\N	-1234	OAUTH2
136	internal_functional_lib_create	Create Functional Library	\N	-1234	OAUTH2
137	internal_functional_lib_delete	Delete Functional Library	\N	-1234	OAUTH2
138	internal_functional_lib_view	View Functional Library	\N	-1234	OAUTH2
139	internal_application_mgt_delete	Delete Applications	\N	-1234	OAUTH2
140	internal_application_mgt_create	Create Applications	\N	-1234	OAUTH2
141	internal_application_mgt_update	Update Applications	\N	-1234	OAUTH2
142	internal_application_mgt_view	View Applications	\N	-1234	OAUTH2
143	internal_role_mgt_create	Create Roles	\N	-1234	OAUTH2
144	internal_role_mgt_delete	Delete Roles	\N	-1234	OAUTH2
145	internal_role_mgt_view	View Roles	\N	-1234	OAUTH2
146	internal_role_mgt_update	Update Roles	\N	-1234	OAUTH2
147	internal_challenge_questions_delete	Delete Challenge Questions	\N	-1234	OAUTH2
148	internal_challenge_questions_create	Create Challenge Questions	\N	-1234	OAUTH2
149	internal_challenge_questions_update	Update Challenge Questions	\N	-1234	OAUTH2
150	internal_challenge_questions_view	View Challenge Questions	\N	-1234	OAUTH2
151	internal_sts_mgt_create	Create STS Configs	\N	-1234	OAUTH2
152	internal_sts_mgt_delete	Delete STS Configs	\N	-1234	OAUTH2
153	internal_sts_mgt_view	View STS Configs	\N	-1234	OAUTH2
154	internal_sts_mgt_update	Update STS Configs	\N	-1234	OAUTH2
155	internal_template_mgt_view	View Template Management	\N	-1234	OAUTH2
156	internal_template_mgt_add	Add Template Management	\N	-1234	OAUTH2
157	internal_template_mgt_delete	Delete Template Management	\N	-1234	OAUTH2
158	internal_template_mgt_list	List Template Management	\N	-1234	OAUTH2
159	internal_user_profile_view	View User Profile	\N	-1234	OAUTH2
160	internal_user_profile_create	Create User Profile	\N	-1234	OAUTH2
161	internal_user_profile_delete	Delete User Profile	\N	-1234	OAUTH2
162	internal_user_profile_update	Update User Profile	\N	-1234	OAUTH2
163	internal_event_publish	Publish Events	\N	-1234	OAUTH2
164	internal_modify_module	Modify Modules	\N	-1234	OAUTH2
165	internal_modify_webapp	Modify WebApps	\N	-1234	OAUTH2
166	internal_modify_user_profile	Modify User Profile	\N	-1234	OAUTH2
167	internal_modify_service	Modify Services	\N	-1234	OAUTH2
168	internal_topic_delete	Delete Topic	\N	-1234	OAUTH2
169	internal_topic_browse	Browse Topic	\N	-1234	OAUTH2
170	internal_topic_purge	Purge Topic	\N	-1234	OAUTH2
171	internal_topic_add	Add Topic	\N	-1234	OAUTH2
172	internal_humantask_packages	Package Human Tasks	\N	-1234	OAUTH2
173	internal_humantask_view	View Human Tasks	\N	-1234	OAUTH2
174	internal_humantask_add	Add Human Tasks	\N	-1234	OAUTH2
175	internal_humantask_task	Manage Human Tasks	\N	-1234	OAUTH2
176	adp-shared-scope-with-roles	ADP Shared Scope with Roles	Shared scope with role mapping	-1234	OAUTH2
177	adp-shared-scope-without-roles	ADP Shared Scope without Roles	Shared scope without role mapping	-1234	OAUTH2
178	adp-local-scope-without-roles	adp-local-scope-without-roles	ADP local scope without roles	-1234	OAUTH2
179	adp-film-subscriber	adp-film-subscriber	ADP film subscriber	-1234	OAUTH2
180	adp-admin	adp-admin	ADP admin	-1234	OAUTH2
181	openid	openid	\N	1	OIDC
182	email	email	\N	1	OIDC
183	profile	profile	\N	1	OIDC
184	phone	phone	\N	1	OIDC
185	address	address	\N	1	OIDC
186	internal_feature_management	Feature Management	\N	1	OAUTH2
187	internal_modify_tenants	Modify Tenant	\N	1	OAUTH2
188	internal_list_tenants	List Tenant	\N	1	OAUTH2
189	internal_server_admin	Server Admin	\N	1	OAUTH2
190	internal_monitor_metrics	Monitor internal_monitor_metrics	\N	1	OAUTH2
191	internal_monitor_bpel	Monitor BPEL	\N	1	OAUTH2
192	internal_monitor_attachment	Monitor Attachments	\N	1	OAUTH2
193	internal_manage_passwords	Manage Passwords	\N	1	OAUTH2
194	internal_manage_profiles	Manage Profiles	\N	1	OAUTH2
195	internal_manage_users	Manage Users	\N	1	OAUTH2
196	internal_manage_provisining	Manager Provisioning	\N	1	OAUTH2
197	internal_configure_datasources	Configure datasources	\N	1	OAUTH2
198	internal_configure_themes	Configure Themes	\N	1	OAUTH2
199	internal_login	Login	\N	1	OAUTH2
200	internal_manage_event_streams	Event streams	\N	1	OAUTH2
201	internal_search_advanced	Advanced Search	\N	1	OAUTH2
202	internal_search_resouces	Basic Search	\N	1	OAUTH2
203	internal_bpel_instances	Manage BPEL Process Instances	\N	1	OAUTH2
204	internal_bpel_proceses	Manage BPEL Processes	\N	1	OAUTH2
205	internal_bpel_packages	Manage BPEL Packages	\N	1	OAUTH2
206	internal_add_bpel	Add BPEL	\N	1	OAUTH2
207	internal_resouces_browse	Browse Resouces	\N	1	OAUTH2
208	internal_resouces_notifications	Manage Resouces Notifications	\N	1	OAUTH2
209	internal_add_module	Add Modules	\N	1	OAUTH2
210	internal_add_services	Add Services	\N	1	OAUTH2
211	internal_add_webapp	Add WebApps	\N	1	OAUTH2
212	internal_add_attachements	Add Attachements	\N	1	OAUTH2
213	internal_add_extensions	Add Extensions	\N	1	OAUTH2
214	internal_list_extensions	List Extensions	\N	1	OAUTH2
215	internal_manage_pep	Manage PEP	\N	1	OAUTH2
216	internal_security_manage_update	Security Manage Update	\N	1	OAUTH2
217	internal_security_manage_view	Security Manage View	\N	1	OAUTH2
218	internal_security_manage_create	Security Manage Create	\N	1	OAUTH2
219	internal_security_manage_delete	Security Manage Delete	\N	1	OAUTH2
220	internal_session_view	View Sessions	\N	1	OAUTH2
221	internal_session_delete	Delete Sessions	\N	1	OAUTH2
222	internal_claim_meta_create	Create Claims Metadata	\N	1	OAUTH2
223	internal_claim_meta_delete	Delete Claims Metadata	\N	1	OAUTH2
224	internal_claim_meta_update	Update Cliams Metadata	\N	1	OAUTH2
225	internal_claim_meta_view	View Claims Metadata	\N	1	OAUTH2
226	internal_claim_manage_view	View Claims	\N	1	OAUTH2
227	internal_claim_manage_update	Update Claims	\N	1	OAUTH2
228	internal_claim_manage_create	Create Claims	\N	1	OAUTH2
229	internal_claim_manage_delete	Delete Claims	\N	1	OAUTH2
230	internal_userrole_ui_create	Show User Role Manage UI	\N	1	OAUTH2
231	internal_auth_seq_view	View Authentication Sequence	\N	1	OAUTH2
232	internal_auth_seq_update	Update Authentication Sequence	\N	1	OAUTH2
233	internal_auth_seq_create	Create Authentication Sequence	\N	1	OAUTH2
234	internal_auth_seq_delete	Delete Authentication Sequence	\N	1	OAUTH2
235	internal_pap_delete	Delete PAP	\N	1	OAUTH2
236	internal_pap_publish	Publish PAP	\N	1	OAUTH2
237	internal_pap_create	Create PAP	\N	1	OAUTH2
238	internal_pap_view	View PAP	\N	1	OAUTH2
239	internal_pap_rollback	Rolback PAP	\N	1	OAUTH2
240	internal_pap_order	Order PAP	\N	1	OAUTH2
241	internal_pap_demote	Demote PAP	\N	1	OAUTH2
242	internal_pap_update	Update PAP	\N	1	OAUTH2
243	internal_pap_enable	Enable PAP	\N	1	OAUTH2
244	internal_pap_list	List PAP	\N	1	OAUTH2
245	internal_pap_subscriber_update	Update PAP Subscriber	\N	1	OAUTH2
246	internal_pap_subscriber_view	View PAP Subscriber	\N	1	OAUTH2
247	internal_pap_subscriber_list	List PAP Subscriber	\N	1	OAUTH2
248	internal_pap_subscriber_create	Create PAP Subscriber	\N	1	OAUTH2
249	internal_pap_subscriber_delete	Delete PAP Subscriber	\N	1	OAUTH2
250	internal_pdp_view	View PDP	\N	1	OAUTH2
251	internal_pdp_manage	Manage PDP	\N	1	OAUTH2
252	internal_pdp_test	Test PDP	\N	1	OAUTH2
253	internal_pep_manage	Manage PEP	\N	1	OAUTH2
254	internal_idp_delete	Delete IDP	\N	1	OAUTH2
255	internal_idp_create	Create IDP	\N	1	OAUTH2
256	internal_idp_view	View IDP	\N	1	OAUTH2
257	internal_idp_update	Update IDP	\N	1	OAUTH2
258	internal_config_mgt_view	View Configs	\N	1	OAUTH2
259	internal_config_mgt_update	Update Configs	\N	1	OAUTH2
260	internal_config_mgt_delete	Delete Configs	\N	1	OAUTH2
261	internal_config_mgt_list	List Configs	\N	1	OAUTH2
262	internal_config_mgt_add	Add Configs	\N	1	OAUTH2
263	internal_keystore_view	View Keystore	\N	1	OAUTH2
264	internal_keystore_update	Update Keystore	\N	1	OAUTH2
265	internal_keystore_delete	Delete Keystore	\N	1	OAUTH2
266	internal_keystore_create	Create Keystore	\N	1	OAUTH2
267	internal_consent_mgt_list	List Consents	\N	1	OAUTH2
268	internal_consent_mgt_view	View Consents	\N	1	OAUTH2
269	internal_consent_mgt_add	Add Consents	\N	1	OAUTH2
270	internal_consent_mgt_delete	Delete Consents	\N	1	OAUTH2
271	internal_app_template_view	View Application Templates	\N	1	OAUTH2
272	internal_app_template_update	Update Application Templates	\N	1	OAUTH2
273	internal_app_template_delete	Delete Application Templates	\N	1	OAUTH2
274	internal_app_template_create	Create Application Templates	\N	1	OAUTH2
275	internal_identity_mgt_view	View IdentityMgt	\N	1	OAUTH2
276	internal_identity_mgt_update	Update IdentityMgt	\N	1	OAUTH2
277	internal_identity_mgt_create	Ceate IdentityMgt	\N	1	OAUTH2
278	internal_identity_mgt_delete	Delete IdentityMgt	\N	1	OAUTH2
279	internal_workflow_association_view	View Workflow Associations	\N	1	OAUTH2
280	internal_workflow_association_delete	Delete Workflow Associations	\N	1	OAUTH2
281	internal_workflow_association_create	Create Workflow Associations	\N	1	OAUTH2
282	internal_workflow_association_update	Update Workflow Associations	\N	1	OAUTH2
283	internal_workflow_def_create	Create Workflow Definition	\N	1	OAUTH2
284	internal_workflow_def_delete	Delete Workflow Definition	\N	1	OAUTH2
285	internal_workflow_def_view	View Workflow Definition	\N	1	OAUTH2
286	internal_workflow_def_update	Update Workflow Definition	\N	1	OAUTH2
287	internal_workflow_profile_view	View Workflow Profile	\N	1	OAUTH2
288	internal_workflow_profile_create	Create Workflow Profile	\N	1	OAUTH2
289	internal_workflow_profile_delete	Delete Workflow Profile	\N	1	OAUTH2
290	internal_workflow_profile_update	Update Workflow Profile	\N	1	OAUTH2
291	internal_workflow_monitor_view	View Workflow	\N	1	OAUTH2
292	internal_workflow_monitor_delete	Delete Workflow	\N	1	OAUTH2
293	internal_email_mgt_update	Update Email Configs	\N	1	OAUTH2
294	internal_email_mgt_create	Create Email Configs	\N	1	OAUTH2
295	internal_email_mgt_delete	Delete Email Configs	\N	1	OAUTH2
296	internal_email_mgt_view	View Email Configs	\N	1	OAUTH2
297	internal_manage_provisioning	Manage User Provisioning	\N	1	OAUTH2
298	internal_user_mgt_update	Update Users	\N	1	OAUTH2
299	internal_user_mgt_delete	Deleate Users	\N	1	OAUTH2
300	internal_user_mgt_list	List Users	\N	1	OAUTH2
301	internal_user_mgt_view	View Users	\N	1	OAUTH2
302	internal_user_mgt_create	Create Users	\N	1	OAUTH2
303	internal_user_association_view	View User Associations	\N	1	OAUTH2
304	internal_user_association_update	Update User Associations	\N	1	OAUTH2
305	internal_user_association_delete	Delete User Associations	\N	1	OAUTH2
306	internal_user_association_create	Create User Associations	\N	1	OAUTH2
307	internal_user_count_delete	Delete User Count	\N	1	OAUTH2
308	internal_user_count_view	View User Count	\N	1	OAUTH2
309	internal_user_count_create	Create User Count	\N	1	OAUTH2
310	internal_user_count_update	Update User Count	\N	1	OAUTH2
311	internal_userstore_delete	Delete User Stores	\N	1	OAUTH2
312	internal_userstore_create	Create User Stores	\N	1	OAUTH2
313	internal_userstore_update	Update User Stores	\N	1	OAUTH2
314	internal_userstore_view	View User Stores	\N	1	OAUTH2
315	internal_functional_lib_update	Update Functional Library	\N	1	OAUTH2
316	internal_functional_lib_create	Create Functional Library	\N	1	OAUTH2
317	internal_functional_lib_delete	Delete Functional Library	\N	1	OAUTH2
318	internal_functional_lib_view	View Functional Library	\N	1	OAUTH2
319	internal_application_mgt_delete	Delete Applications	\N	1	OAUTH2
320	internal_application_mgt_create	Create Applications	\N	1	OAUTH2
321	internal_application_mgt_update	Update Applications	\N	1	OAUTH2
322	internal_application_mgt_view	View Applications	\N	1	OAUTH2
323	internal_role_mgt_create	Create Roles	\N	1	OAUTH2
324	internal_role_mgt_delete	Delete Roles	\N	1	OAUTH2
325	internal_role_mgt_view	View Roles	\N	1	OAUTH2
326	internal_role_mgt_update	Update Roles	\N	1	OAUTH2
327	internal_challenge_questions_delete	Delete Challenge Questions	\N	1	OAUTH2
328	internal_challenge_questions_create	Create Challenge Questions	\N	1	OAUTH2
329	internal_challenge_questions_update	Update Challenge Questions	\N	1	OAUTH2
330	internal_challenge_questions_view	View Challenge Questions	\N	1	OAUTH2
331	internal_sts_mgt_create	Create STS Configs	\N	1	OAUTH2
332	internal_sts_mgt_delete	Delete STS Configs	\N	1	OAUTH2
333	internal_sts_mgt_view	View STS Configs	\N	1	OAUTH2
334	internal_sts_mgt_update	Update STS Configs	\N	1	OAUTH2
335	internal_template_mgt_view	View Template Management	\N	1	OAUTH2
336	internal_template_mgt_add	Add Template Management	\N	1	OAUTH2
337	internal_template_mgt_delete	Delete Template Management	\N	1	OAUTH2
338	internal_template_mgt_list	List Template Management	\N	1	OAUTH2
339	internal_user_profile_view	View User Profile	\N	1	OAUTH2
340	internal_user_profile_create	Create User Profile	\N	1	OAUTH2
341	internal_user_profile_delete	Delete User Profile	\N	1	OAUTH2
342	internal_user_profile_update	Update User Profile	\N	1	OAUTH2
343	internal_event_publish	Publish Events	\N	1	OAUTH2
344	internal_modify_module	Modify Modules	\N	1	OAUTH2
345	internal_modify_webapp	Modify WebApps	\N	1	OAUTH2
346	internal_modify_user_profile	Modify User Profile	\N	1	OAUTH2
347	internal_modify_service	Modify Services	\N	1	OAUTH2
348	internal_topic_delete	Delete Topic	\N	1	OAUTH2
349	internal_topic_browse	Browse Topic	\N	1	OAUTH2
350	internal_topic_purge	Purge Topic	\N	1	OAUTH2
351	internal_topic_add	Add Topic	\N	1	OAUTH2
352	internal_humantask_packages	Package Human Tasks	\N	1	OAUTH2
353	internal_humantask_view	View Human Tasks	\N	1	OAUTH2
354	internal_humantask_add	Add Human Tasks	\N	1	OAUTH2
355	internal_humantask_task	Manage Human Tasks	\N	1	OAUTH2
356	adp-shared-scope-with-roles	ADP Shared Scope with Roles	Shared scope with role mapping	1	OAUTH2
357	adp-shared-scope-without-roles	ADP Shared Scope without Roles	Shared scope without role mapping	1	OAUTH2
358	adp-local-scope-without-roles	adp-local-scope-without-roles	ADP local scope without roles	1	OAUTH2
359	adp-film-subscriber	adp-film-subscriber	ADP film subscriber	1	OAUTH2
360	adp-admin	adp-admin	ADP admin	1	OAUTH2
361	openid	openid	\N	2	OIDC
362	email	email	\N	2	OIDC
363	profile	profile	\N	2	OIDC
364	phone	phone	\N	2	OIDC
365	address	address	\N	2	OIDC
366	internal_feature_management	Feature Management	\N	2	OAUTH2
367	internal_modify_tenants	Modify Tenant	\N	2	OAUTH2
368	internal_list_tenants	List Tenant	\N	2	OAUTH2
369	internal_server_admin	Server Admin	\N	2	OAUTH2
370	internal_monitor_metrics	Monitor internal_monitor_metrics	\N	2	OAUTH2
371	internal_monitor_bpel	Monitor BPEL	\N	2	OAUTH2
372	internal_monitor_attachment	Monitor Attachments	\N	2	OAUTH2
373	internal_manage_passwords	Manage Passwords	\N	2	OAUTH2
374	internal_manage_profiles	Manage Profiles	\N	2	OAUTH2
375	internal_manage_users	Manage Users	\N	2	OAUTH2
376	internal_manage_provisining	Manager Provisioning	\N	2	OAUTH2
377	internal_configure_datasources	Configure datasources	\N	2	OAUTH2
378	internal_configure_themes	Configure Themes	\N	2	OAUTH2
379	internal_login	Login	\N	2	OAUTH2
380	internal_manage_event_streams	Event streams	\N	2	OAUTH2
381	internal_search_advanced	Advanced Search	\N	2	OAUTH2
382	internal_search_resouces	Basic Search	\N	2	OAUTH2
383	internal_bpel_instances	Manage BPEL Process Instances	\N	2	OAUTH2
384	internal_bpel_proceses	Manage BPEL Processes	\N	2	OAUTH2
385	internal_bpel_packages	Manage BPEL Packages	\N	2	OAUTH2
386	internal_add_bpel	Add BPEL	\N	2	OAUTH2
387	internal_resouces_browse	Browse Resouces	\N	2	OAUTH2
388	internal_resouces_notifications	Manage Resouces Notifications	\N	2	OAUTH2
389	internal_add_module	Add Modules	\N	2	OAUTH2
390	internal_add_services	Add Services	\N	2	OAUTH2
391	internal_add_webapp	Add WebApps	\N	2	OAUTH2
392	internal_add_attachements	Add Attachements	\N	2	OAUTH2
393	internal_add_extensions	Add Extensions	\N	2	OAUTH2
394	internal_list_extensions	List Extensions	\N	2	OAUTH2
395	internal_manage_pep	Manage PEP	\N	2	OAUTH2
396	internal_security_manage_update	Security Manage Update	\N	2	OAUTH2
397	internal_security_manage_view	Security Manage View	\N	2	OAUTH2
398	internal_security_manage_create	Security Manage Create	\N	2	OAUTH2
399	internal_security_manage_delete	Security Manage Delete	\N	2	OAUTH2
400	internal_session_view	View Sessions	\N	2	OAUTH2
401	internal_session_delete	Delete Sessions	\N	2	OAUTH2
402	internal_claim_meta_create	Create Claims Metadata	\N	2	OAUTH2
403	internal_claim_meta_delete	Delete Claims Metadata	\N	2	OAUTH2
404	internal_claim_meta_update	Update Cliams Metadata	\N	2	OAUTH2
405	internal_claim_meta_view	View Claims Metadata	\N	2	OAUTH2
406	internal_claim_manage_view	View Claims	\N	2	OAUTH2
407	internal_claim_manage_update	Update Claims	\N	2	OAUTH2
408	internal_claim_manage_create	Create Claims	\N	2	OAUTH2
409	internal_claim_manage_delete	Delete Claims	\N	2	OAUTH2
410	internal_userrole_ui_create	Show User Role Manage UI	\N	2	OAUTH2
411	internal_auth_seq_view	View Authentication Sequence	\N	2	OAUTH2
412	internal_auth_seq_update	Update Authentication Sequence	\N	2	OAUTH2
413	internal_auth_seq_create	Create Authentication Sequence	\N	2	OAUTH2
414	internal_auth_seq_delete	Delete Authentication Sequence	\N	2	OAUTH2
415	internal_pap_delete	Delete PAP	\N	2	OAUTH2
416	internal_pap_publish	Publish PAP	\N	2	OAUTH2
417	internal_pap_create	Create PAP	\N	2	OAUTH2
418	internal_pap_view	View PAP	\N	2	OAUTH2
419	internal_pap_rollback	Rolback PAP	\N	2	OAUTH2
420	internal_pap_order	Order PAP	\N	2	OAUTH2
421	internal_pap_demote	Demote PAP	\N	2	OAUTH2
422	internal_pap_update	Update PAP	\N	2	OAUTH2
423	internal_pap_enable	Enable PAP	\N	2	OAUTH2
424	internal_pap_list	List PAP	\N	2	OAUTH2
425	internal_pap_subscriber_update	Update PAP Subscriber	\N	2	OAUTH2
426	internal_pap_subscriber_view	View PAP Subscriber	\N	2	OAUTH2
427	internal_pap_subscriber_list	List PAP Subscriber	\N	2	OAUTH2
428	internal_pap_subscriber_create	Create PAP Subscriber	\N	2	OAUTH2
429	internal_pap_subscriber_delete	Delete PAP Subscriber	\N	2	OAUTH2
430	internal_pdp_view	View PDP	\N	2	OAUTH2
431	internal_pdp_manage	Manage PDP	\N	2	OAUTH2
432	internal_pdp_test	Test PDP	\N	2	OAUTH2
433	internal_pep_manage	Manage PEP	\N	2	OAUTH2
434	internal_idp_delete	Delete IDP	\N	2	OAUTH2
435	internal_idp_create	Create IDP	\N	2	OAUTH2
436	internal_idp_view	View IDP	\N	2	OAUTH2
437	internal_idp_update	Update IDP	\N	2	OAUTH2
438	internal_config_mgt_view	View Configs	\N	2	OAUTH2
439	internal_config_mgt_update	Update Configs	\N	2	OAUTH2
440	internal_config_mgt_delete	Delete Configs	\N	2	OAUTH2
441	internal_config_mgt_list	List Configs	\N	2	OAUTH2
442	internal_config_mgt_add	Add Configs	\N	2	OAUTH2
443	internal_keystore_view	View Keystore	\N	2	OAUTH2
444	internal_keystore_update	Update Keystore	\N	2	OAUTH2
445	internal_keystore_delete	Delete Keystore	\N	2	OAUTH2
446	internal_keystore_create	Create Keystore	\N	2	OAUTH2
447	internal_consent_mgt_list	List Consents	\N	2	OAUTH2
448	internal_consent_mgt_view	View Consents	\N	2	OAUTH2
449	internal_consent_mgt_add	Add Consents	\N	2	OAUTH2
450	internal_consent_mgt_delete	Delete Consents	\N	2	OAUTH2
451	internal_app_template_view	View Application Templates	\N	2	OAUTH2
452	internal_app_template_update	Update Application Templates	\N	2	OAUTH2
453	internal_app_template_delete	Delete Application Templates	\N	2	OAUTH2
454	internal_app_template_create	Create Application Templates	\N	2	OAUTH2
455	internal_identity_mgt_view	View IdentityMgt	\N	2	OAUTH2
456	internal_identity_mgt_update	Update IdentityMgt	\N	2	OAUTH2
457	internal_identity_mgt_create	Ceate IdentityMgt	\N	2	OAUTH2
458	internal_identity_mgt_delete	Delete IdentityMgt	\N	2	OAUTH2
459	internal_workflow_association_view	View Workflow Associations	\N	2	OAUTH2
460	internal_workflow_association_delete	Delete Workflow Associations	\N	2	OAUTH2
461	internal_workflow_association_create	Create Workflow Associations	\N	2	OAUTH2
462	internal_workflow_association_update	Update Workflow Associations	\N	2	OAUTH2
463	internal_workflow_def_create	Create Workflow Definition	\N	2	OAUTH2
464	internal_workflow_def_delete	Delete Workflow Definition	\N	2	OAUTH2
465	internal_workflow_def_view	View Workflow Definition	\N	2	OAUTH2
466	internal_workflow_def_update	Update Workflow Definition	\N	2	OAUTH2
467	internal_workflow_profile_view	View Workflow Profile	\N	2	OAUTH2
468	internal_workflow_profile_create	Create Workflow Profile	\N	2	OAUTH2
469	internal_workflow_profile_delete	Delete Workflow Profile	\N	2	OAUTH2
470	internal_workflow_profile_update	Update Workflow Profile	\N	2	OAUTH2
471	internal_workflow_monitor_view	View Workflow	\N	2	OAUTH2
472	internal_workflow_monitor_delete	Delete Workflow	\N	2	OAUTH2
473	internal_email_mgt_update	Update Email Configs	\N	2	OAUTH2
474	internal_email_mgt_create	Create Email Configs	\N	2	OAUTH2
475	internal_email_mgt_delete	Delete Email Configs	\N	2	OAUTH2
476	internal_email_mgt_view	View Email Configs	\N	2	OAUTH2
477	internal_manage_provisioning	Manage User Provisioning	\N	2	OAUTH2
478	internal_user_mgt_update	Update Users	\N	2	OAUTH2
479	internal_user_mgt_delete	Deleate Users	\N	2	OAUTH2
480	internal_user_mgt_list	List Users	\N	2	OAUTH2
481	internal_user_mgt_view	View Users	\N	2	OAUTH2
482	internal_user_mgt_create	Create Users	\N	2	OAUTH2
483	internal_user_association_view	View User Associations	\N	2	OAUTH2
484	internal_user_association_update	Update User Associations	\N	2	OAUTH2
485	internal_user_association_delete	Delete User Associations	\N	2	OAUTH2
486	internal_user_association_create	Create User Associations	\N	2	OAUTH2
487	internal_user_count_delete	Delete User Count	\N	2	OAUTH2
488	internal_user_count_view	View User Count	\N	2	OAUTH2
489	internal_user_count_create	Create User Count	\N	2	OAUTH2
490	internal_user_count_update	Update User Count	\N	2	OAUTH2
491	internal_userstore_delete	Delete User Stores	\N	2	OAUTH2
492	internal_userstore_create	Create User Stores	\N	2	OAUTH2
493	internal_userstore_update	Update User Stores	\N	2	OAUTH2
494	internal_userstore_view	View User Stores	\N	2	OAUTH2
495	internal_functional_lib_update	Update Functional Library	\N	2	OAUTH2
496	internal_functional_lib_create	Create Functional Library	\N	2	OAUTH2
497	internal_functional_lib_delete	Delete Functional Library	\N	2	OAUTH2
498	internal_functional_lib_view	View Functional Library	\N	2	OAUTH2
499	internal_application_mgt_delete	Delete Applications	\N	2	OAUTH2
500	internal_application_mgt_create	Create Applications	\N	2	OAUTH2
501	internal_application_mgt_update	Update Applications	\N	2	OAUTH2
502	internal_application_mgt_view	View Applications	\N	2	OAUTH2
503	internal_role_mgt_create	Create Roles	\N	2	OAUTH2
504	internal_role_mgt_delete	Delete Roles	\N	2	OAUTH2
505	internal_role_mgt_view	View Roles	\N	2	OAUTH2
506	internal_role_mgt_update	Update Roles	\N	2	OAUTH2
507	internal_challenge_questions_delete	Delete Challenge Questions	\N	2	OAUTH2
508	internal_challenge_questions_create	Create Challenge Questions	\N	2	OAUTH2
509	internal_challenge_questions_update	Update Challenge Questions	\N	2	OAUTH2
510	internal_challenge_questions_view	View Challenge Questions	\N	2	OAUTH2
511	internal_sts_mgt_create	Create STS Configs	\N	2	OAUTH2
512	internal_sts_mgt_delete	Delete STS Configs	\N	2	OAUTH2
513	internal_sts_mgt_view	View STS Configs	\N	2	OAUTH2
514	internal_sts_mgt_update	Update STS Configs	\N	2	OAUTH2
515	internal_template_mgt_view	View Template Management	\N	2	OAUTH2
516	internal_template_mgt_add	Add Template Management	\N	2	OAUTH2
517	internal_template_mgt_delete	Delete Template Management	\N	2	OAUTH2
518	internal_template_mgt_list	List Template Management	\N	2	OAUTH2
519	internal_user_profile_view	View User Profile	\N	2	OAUTH2
520	internal_user_profile_create	Create User Profile	\N	2	OAUTH2
521	internal_user_profile_delete	Delete User Profile	\N	2	OAUTH2
522	internal_user_profile_update	Update User Profile	\N	2	OAUTH2
523	internal_event_publish	Publish Events	\N	2	OAUTH2
524	internal_modify_module	Modify Modules	\N	2	OAUTH2
525	internal_modify_webapp	Modify WebApps	\N	2	OAUTH2
526	internal_modify_user_profile	Modify User Profile	\N	2	OAUTH2
527	internal_modify_service	Modify Services	\N	2	OAUTH2
528	internal_topic_delete	Delete Topic	\N	2	OAUTH2
529	internal_topic_browse	Browse Topic	\N	2	OAUTH2
530	internal_topic_purge	Purge Topic	\N	2	OAUTH2
531	internal_topic_add	Add Topic	\N	2	OAUTH2
532	internal_humantask_packages	Package Human Tasks	\N	2	OAUTH2
533	internal_humantask_view	View Human Tasks	\N	2	OAUTH2
534	internal_humantask_add	Add Human Tasks	\N	2	OAUTH2
535	internal_humantask_task	Manage Human Tasks	\N	2	OAUTH2
536	adp-shared-scope-with-roles	ADP Shared Scope with Roles	Shared scope with role mapping	2	OAUTH2
537	adp-shared-scope-without-roles	ADP Shared Scope without Roles	Shared scope without role mapping	2	OAUTH2
538	adp-local-scope-without-roles	adp-local-scope-without-roles	ADP local scope without roles	2	OAUTH2
539	adp-film-subscriber	adp-film-subscriber	ADP film subscriber	2	OAUTH2
540	adp-admin	adp-admin	ADP admin	2	OAUTH2
\.


--
-- Data for Name: idn_oauth2_scope_binding; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_scope_binding (scope_id, scope_binding, binding_type) FROM stdin;
6	/permission/protected/configure/components	PERMISSION
7	/permission/protected/manage/modify/tenants	PERMISSION
8	/permission/protected/manage/monitor/tenants/list	PERMISSION
9	/permission/protected/server-admin/homepage	PERMISSION
10	/permission/admin/monitor/metrics	PERMISSION
11	/permission/admin/monitor/bpel	PERMISSION
12	/permission/admin/monitor/attachment	PERMISSION
13	/permission/admin/configure/security/usermgt/passwords	PERMISSION
14	/permission/admin/configure/security/usermgt/profiles	PERMISSION
15	/permission/admin/configure/security/usermgt/users	PERMISSION
16	/permission/admin/configure/security/usermgt/provisioning	PERMISSION
17	/permission/admin/configure/datasources	PERMISSION
18	/permission/admin/configure/theme	PERMISSION
19	everyone_permission	PERMISSION
20	/permission/admin/manage/event-streams	PERMISSION
21	/permission/admin/manage/search/advanced-search	PERMISSION
22	/permission/admin/manage/search/resources	PERMISSION
23	/permission/admin/manage/bpel/instance	PERMISSION
24	/permission/admin/manage/bpel/processes	PERMISSION
25	/permission/admin/manage/bpel/packages	PERMISSION
26	/permission/admin/manage/bpel/add	PERMISSION
27	/permission/admin/manage/resources/browse	PERMISSION
28	/permission/admin/manage/resources/notifications	PERMISSION
29	/permission/admin/manage/add/module	PERMISSION
30	/permission/admin/manage/add/service	PERMISSION
31	/permission/admin/manage/add/webapp	PERMISSION
32	/permission/admin/manage/attachment	PERMISSION
33	/permission/admin/manage/extensions/add	PERMISSION
34	/permission/admin/manage/extensions/list	PERMISSION
35	/permission/admin/manage/identity/pep	PERMISSION
36	/permission/admin/manage/identity/securitymgt/update	PERMISSION
37	/permission/admin/manage/identity/securitymgt/view	PERMISSION
38	/permission/admin/manage/identity/securitymgt/create	PERMISSION
39	/permission/admin/manage/identity/securitymgt/delete	PERMISSION
40	/permission/admin/manage/identity/authentication/session/view	PERMISSION
41	/permission/admin/manage/identity/authentication/session/delete	PERMISSION
42	/permission/admin/manage/identity/claimmgt/metadata/create	PERMISSION
43	/permission/admin/manage/identity/claimmgt/metadata/delete	PERMISSION
44	/permission/admin/manage/identity/claimmgt/metadata/update	PERMISSION
45	/permission/admin/manage/identity/claimmgt/metadata/view	PERMISSION
46	/permission/admin/manage/identity/claimmgt/claim/view	PERMISSION
47	/permission/admin/manage/identity/claimmgt/claim/update	PERMISSION
48	/permission/admin/manage/identity/claimmgt/claim/create	PERMISSION
49	/permission/admin/manage/identity/claimmgt/claim/delete	PERMISSION
50	/permission/admin/manage/identity/userroleuimgt/create	PERMISSION
51	/permission/admin/manage/identity/defaultauthSeq/view	PERMISSION
52	/permission/admin/manage/identity/defaultauthSeq/update	PERMISSION
53	/permission/admin/manage/identity/defaultauthSeq/create	PERMISSION
54	/permission/admin/manage/identity/defaultauthSeq/delete	PERMISSION
55	/permission/admin/manage/identity/entitlement/pap/policy/delete	PERMISSION
56	/permission/admin/manage/identity/entitlement/pap/policy/publish	PERMISSION
57	/permission/admin/manage/identity/entitlement/pap/policy/create	PERMISSION
58	/permission/admin/manage/identity/entitlement/pap/policy/view	PERMISSION
59	/permission/admin/manage/identity/entitlement/pap/policy/rollback	PERMISSION
60	/permission/admin/manage/identity/entitlement/pap/policy/order	PERMISSION
61	/permission/admin/manage/identity/entitlement/pap/policy/demote	PERMISSION
62	/permission/admin/manage/identity/entitlement/pap/policy/update	PERMISSION
63	/permission/admin/manage/identity/entitlement/pap/policy/enable	PERMISSION
64	/permission/admin/manage/identity/entitlement/pap/policy/list	PERMISSION
65	/permission/admin/manage/identity/entitlement/pap/subscriber/update	PERMISSION
66	/permission/admin/manage/identity/entitlement/pap/subscriber/view	PERMISSION
67	/permission/admin/manage/identity/entitlement/pap/subscriber/list	PERMISSION
68	/permission/admin/manage/identity/entitlement/pap/subscriber/create	PERMISSION
69	/permission/admin/manage/identity/entitlement/pap/subscriber/delete	PERMISSION
70	/permission/admin/manage/identity/entitlement/pdp/view	PERMISSION
71	/permission/admin/manage/identity/entitlement/pdp/manage	PERMISSION
72	/permission/admin/manage/identity/entitlement/pdp/test	PERMISSION
73	/permission/admin/manage/identity/entitlement/pep	PERMISSION
74	/permission/admin/manage/identity/idpmgt/delete	PERMISSION
75	/permission/admin/manage/identity/idpmgt/create	PERMISSION
76	/permission/admin/manage/identity/idpmgt/view	PERMISSION
77	/permission/admin/manage/identity/idpmgt/update	PERMISSION
78	/permission/admin/manage/identity/configmgt/view	PERMISSION
79	/permission/admin/manage/identity/configmgt/update	PERMISSION
80	/permission/admin/manage/identity/configmgt/delete	PERMISSION
81	/permission/admin/manage/identity/configmgt/list	PERMISSION
82	/permission/admin/manage/identity/configmgt/add	PERMISSION
83	/permission/admin/manage/identity/keystoremgt/view	PERMISSION
84	/permission/admin/manage/identity/keystoremgt/update	PERMISSION
85	/permission/admin/manage/identity/keystoremgt/delete	PERMISSION
86	/permission/admin/manage/identity/keystoremgt/create	PERMISSION
87	/permission/admin/manage/identity/consentmgt/list	PERMISSION
88	/permission/admin/manage/identity/consentmgt/view	PERMISSION
89	/permission/admin/manage/identity/consentmgt/add	PERMISSION
90	/permission/admin/manage/identity/consentmgt/delete	PERMISSION
91	/permission/admin/manage/identity/apptemplatemgt/view	PERMISSION
92	/permission/admin/manage/identity/apptemplatemgt/update	PERMISSION
93	/permission/admin/manage/identity/apptemplatemgt/delete	PERMISSION
94	/permission/admin/manage/identity/apptemplatemgt/create	PERMISSION
95	/permission/admin/manage/identity/identitymgt/view	PERMISSION
96	/permission/admin/manage/identity/identitymgt/update	PERMISSION
97	/permission/admin/manage/identity/identitymgt/create	PERMISSION
98	/permission/admin/manage/identity/identitymgt/delete	PERMISSION
99	/permission/admin/manage/identity/workflow/association/view	PERMISSION
100	/permission/admin/manage/identity/workflow/association/delete	PERMISSION
101	/permission/admin/manage/identity/workflow/association/create	PERMISSION
102	/permission/admin/manage/identity/workflow/association/update	PERMISSION
103	/permission/admin/manage/identity/workflow/definition/create	PERMISSION
104	/permission/admin/manage/identity/workflow/definition/delete	PERMISSION
105	/permission/admin/manage/identity/workflow/definition/view	PERMISSION
106	/permission/admin/manage/identity/workflow/definition/update	PERMISSION
107	/permission/admin/manage/identity/workflow/profile/view	PERMISSION
108	/permission/admin/manage/identity/workflow/profile/create	PERMISSION
109	/permission/admin/manage/identity/workflow/profile/delete	PERMISSION
110	/permission/admin/manage/identity/workflow/profile/update	PERMISSION
111	/permission/admin/manage/identity/workflow/monitor/view	PERMISSION
112	/permission/admin/manage/identity/workflow/monitor/delete	PERMISSION
113	/permission/admin/manage/identity/emailmgt/update	PERMISSION
114	/permission/admin/manage/identity/emailmgt/create	PERMISSION
115	/permission/admin/manage/identity/emailmgt/delete	PERMISSION
116	/permission/admin/manage/identity/emailmgt/view	PERMISSION
117	/permission/admin/manage/identity/provisioning	PERMISSION
118	/permission/admin/manage/identity/usermgt/update	PERMISSION
119	/permission/admin/manage/identity/usermgt/delete	PERMISSION
120	/permission/admin/manage/identity/usermgt/list	PERMISSION
121	/permission/admin/manage/identity/usermgt/view	PERMISSION
122	/permission/admin/manage/identity/usermgt/create	PERMISSION
123	/permission/admin/manage/identity/user/association/view	PERMISSION
124	/permission/admin/manage/identity/user/association/update	PERMISSION
125	/permission/admin/manage/identity/user/association/delete	PERMISSION
126	/permission/admin/manage/identity/user/association/create	PERMISSION
127	/permission/admin/manage/identity/userstore/count/delete	PERMISSION
128	/permission/admin/manage/identity/userstore/count/view	PERMISSION
129	/permission/admin/manage/identity/userstore/count/create	PERMISSION
130	/permission/admin/manage/identity/userstore/count/update	PERMISSION
131	/permission/admin/manage/identity/userstore/config/delete	PERMISSION
132	/permission/admin/manage/identity/userstore/config/create	PERMISSION
133	/permission/admin/manage/identity/userstore/config/update	PERMISSION
134	/permission/admin/manage/identity/userstore/config/view	PERMISSION
135	/permission/admin/manage/identity/functionsLibrarymgt/update	PERMISSION
136	/permission/admin/manage/identity/functionsLibrarymgt/create	PERMISSION
137	/permission/admin/manage/identity/functionsLibrarymgt/delete	PERMISSION
138	/permission/admin/manage/identity/functionsLibrarymgt/view	PERMISSION
139	/permission/admin/manage/identity/applicationmgt/delete	PERMISSION
140	/permission/admin/manage/identity/applicationmgt/create	PERMISSION
141	/permission/admin/manage/identity/applicationmgt/update	PERMISSION
142	/permission/admin/manage/identity/applicationmgt/view	PERMISSION
143	/permission/admin/manage/identity/rolemgt/create	PERMISSION
144	/permission/admin/manage/identity/rolemgt/delete	PERMISSION
145	/permission/admin/manage/identity/rolemgt/view	PERMISSION
146	/permission/admin/manage/identity/rolemgt/update	PERMISSION
147	/permission/admin/manage/identity/challenge/delete	PERMISSION
148	/permission/admin/manage/identity/challenge/create	PERMISSION
149	/permission/admin/manage/identity/challenge/update	PERMISSION
150	/permission/admin/manage/identity/challenge/view	PERMISSION
151	/permission/admin/manage/identity/stsmgt/create	PERMISSION
152	/permission/admin/manage/identity/stsmgt/delete	PERMISSION
153	/permission/admin/manage/identity/stsmgt/view	PERMISSION
154	/permission/admin/manage/identity/stsmgt/update	PERMISSION
155	/permission/admin/manage/identity/template/mgt/view	PERMISSION
156	/permission/admin/manage/identity/template/mgt/add	PERMISSION
157	/permission/admin/manage/identity/template/mgt/delete	PERMISSION
158	/permission/admin/manage/identity/template/mgt/list	PERMISSION
159	/permission/admin/manage/identity/userprofile/view	PERMISSION
160	/permission/admin/manage/identity/userprofile/create	PERMISSION
161	/permission/admin/manage/identity/userprofile/delete	PERMISSION
162	/permission/admin/manage/identity/userprofile/update	PERMISSION
163	/permission/admin/manage/event-publish	PERMISSION
164	/permission/admin/manage/modify/module	PERMISSION
165	/permission/admin/manage/modify/webapp	PERMISSION
166	/permission/admin/manage/modify/user-profile	PERMISSION
167	/permission/admin/manage/modify/service	PERMISSION
168	/permission/admin/manage/topic/deleteTopic	PERMISSION
169	/permission/admin/manage/topic/browseTopic	PERMISSION
170	/permission/admin/manage/topic/purgeTopic	PERMISSION
171	/permission/admin/manage/topic/addTopic	PERMISSION
172	/permission/admin/manage/humantask/packages	PERMISSION
173	/permission/admin/manage/humantask/viewtasks	PERMISSION
174	/permission/admin/manage/humantask/add	PERMISSION
175	/permission/admin/manage/humantask/task	PERMISSION
176	ADP_CREATOR	DEFAULT
176	ADP_SUBSCRIBER	DEFAULT
180	admin	DEFAULT
186	/permission/protected/configure/components	PERMISSION
187	/permission/protected/manage/modify/tenants	PERMISSION
188	/permission/protected/manage/monitor/tenants/list	PERMISSION
189	/permission/protected/server-admin/homepage	PERMISSION
190	/permission/admin/monitor/metrics	PERMISSION
191	/permission/admin/monitor/bpel	PERMISSION
192	/permission/admin/monitor/attachment	PERMISSION
193	/permission/admin/configure/security/usermgt/passwords	PERMISSION
194	/permission/admin/configure/security/usermgt/profiles	PERMISSION
195	/permission/admin/configure/security/usermgt/users	PERMISSION
196	/permission/admin/configure/security/usermgt/provisioning	PERMISSION
197	/permission/admin/configure/datasources	PERMISSION
198	/permission/admin/configure/theme	PERMISSION
199	everyone_permission	PERMISSION
200	/permission/admin/manage/event-streams	PERMISSION
201	/permission/admin/manage/search/advanced-search	PERMISSION
202	/permission/admin/manage/search/resources	PERMISSION
203	/permission/admin/manage/bpel/instance	PERMISSION
204	/permission/admin/manage/bpel/processes	PERMISSION
205	/permission/admin/manage/bpel/packages	PERMISSION
206	/permission/admin/manage/bpel/add	PERMISSION
207	/permission/admin/manage/resources/browse	PERMISSION
208	/permission/admin/manage/resources/notifications	PERMISSION
209	/permission/admin/manage/add/module	PERMISSION
210	/permission/admin/manage/add/service	PERMISSION
211	/permission/admin/manage/add/webapp	PERMISSION
212	/permission/admin/manage/attachment	PERMISSION
213	/permission/admin/manage/extensions/add	PERMISSION
214	/permission/admin/manage/extensions/list	PERMISSION
215	/permission/admin/manage/identity/pep	PERMISSION
216	/permission/admin/manage/identity/securitymgt/update	PERMISSION
217	/permission/admin/manage/identity/securitymgt/view	PERMISSION
218	/permission/admin/manage/identity/securitymgt/create	PERMISSION
219	/permission/admin/manage/identity/securitymgt/delete	PERMISSION
220	/permission/admin/manage/identity/authentication/session/view	PERMISSION
221	/permission/admin/manage/identity/authentication/session/delete	PERMISSION
222	/permission/admin/manage/identity/claimmgt/metadata/create	PERMISSION
223	/permission/admin/manage/identity/claimmgt/metadata/delete	PERMISSION
224	/permission/admin/manage/identity/claimmgt/metadata/update	PERMISSION
225	/permission/admin/manage/identity/claimmgt/metadata/view	PERMISSION
226	/permission/admin/manage/identity/claimmgt/claim/view	PERMISSION
227	/permission/admin/manage/identity/claimmgt/claim/update	PERMISSION
228	/permission/admin/manage/identity/claimmgt/claim/create	PERMISSION
229	/permission/admin/manage/identity/claimmgt/claim/delete	PERMISSION
230	/permission/admin/manage/identity/userroleuimgt/create	PERMISSION
231	/permission/admin/manage/identity/defaultauthSeq/view	PERMISSION
232	/permission/admin/manage/identity/defaultauthSeq/update	PERMISSION
233	/permission/admin/manage/identity/defaultauthSeq/create	PERMISSION
234	/permission/admin/manage/identity/defaultauthSeq/delete	PERMISSION
235	/permission/admin/manage/identity/entitlement/pap/policy/delete	PERMISSION
236	/permission/admin/manage/identity/entitlement/pap/policy/publish	PERMISSION
237	/permission/admin/manage/identity/entitlement/pap/policy/create	PERMISSION
238	/permission/admin/manage/identity/entitlement/pap/policy/view	PERMISSION
239	/permission/admin/manage/identity/entitlement/pap/policy/rollback	PERMISSION
240	/permission/admin/manage/identity/entitlement/pap/policy/order	PERMISSION
241	/permission/admin/manage/identity/entitlement/pap/policy/demote	PERMISSION
242	/permission/admin/manage/identity/entitlement/pap/policy/update	PERMISSION
243	/permission/admin/manage/identity/entitlement/pap/policy/enable	PERMISSION
244	/permission/admin/manage/identity/entitlement/pap/policy/list	PERMISSION
245	/permission/admin/manage/identity/entitlement/pap/subscriber/update	PERMISSION
246	/permission/admin/manage/identity/entitlement/pap/subscriber/view	PERMISSION
247	/permission/admin/manage/identity/entitlement/pap/subscriber/list	PERMISSION
248	/permission/admin/manage/identity/entitlement/pap/subscriber/create	PERMISSION
249	/permission/admin/manage/identity/entitlement/pap/subscriber/delete	PERMISSION
250	/permission/admin/manage/identity/entitlement/pdp/view	PERMISSION
251	/permission/admin/manage/identity/entitlement/pdp/manage	PERMISSION
252	/permission/admin/manage/identity/entitlement/pdp/test	PERMISSION
253	/permission/admin/manage/identity/entitlement/pep	PERMISSION
254	/permission/admin/manage/identity/idpmgt/delete	PERMISSION
255	/permission/admin/manage/identity/idpmgt/create	PERMISSION
256	/permission/admin/manage/identity/idpmgt/view	PERMISSION
257	/permission/admin/manage/identity/idpmgt/update	PERMISSION
258	/permission/admin/manage/identity/configmgt/view	PERMISSION
259	/permission/admin/manage/identity/configmgt/update	PERMISSION
260	/permission/admin/manage/identity/configmgt/delete	PERMISSION
261	/permission/admin/manage/identity/configmgt/list	PERMISSION
262	/permission/admin/manage/identity/configmgt/add	PERMISSION
263	/permission/admin/manage/identity/keystoremgt/view	PERMISSION
264	/permission/admin/manage/identity/keystoremgt/update	PERMISSION
265	/permission/admin/manage/identity/keystoremgt/delete	PERMISSION
266	/permission/admin/manage/identity/keystoremgt/create	PERMISSION
267	/permission/admin/manage/identity/consentmgt/list	PERMISSION
268	/permission/admin/manage/identity/consentmgt/view	PERMISSION
269	/permission/admin/manage/identity/consentmgt/add	PERMISSION
270	/permission/admin/manage/identity/consentmgt/delete	PERMISSION
271	/permission/admin/manage/identity/apptemplatemgt/view	PERMISSION
272	/permission/admin/manage/identity/apptemplatemgt/update	PERMISSION
273	/permission/admin/manage/identity/apptemplatemgt/delete	PERMISSION
274	/permission/admin/manage/identity/apptemplatemgt/create	PERMISSION
275	/permission/admin/manage/identity/identitymgt/view	PERMISSION
276	/permission/admin/manage/identity/identitymgt/update	PERMISSION
277	/permission/admin/manage/identity/identitymgt/create	PERMISSION
278	/permission/admin/manage/identity/identitymgt/delete	PERMISSION
279	/permission/admin/manage/identity/workflow/association/view	PERMISSION
280	/permission/admin/manage/identity/workflow/association/delete	PERMISSION
281	/permission/admin/manage/identity/workflow/association/create	PERMISSION
282	/permission/admin/manage/identity/workflow/association/update	PERMISSION
283	/permission/admin/manage/identity/workflow/definition/create	PERMISSION
284	/permission/admin/manage/identity/workflow/definition/delete	PERMISSION
285	/permission/admin/manage/identity/workflow/definition/view	PERMISSION
286	/permission/admin/manage/identity/workflow/definition/update	PERMISSION
287	/permission/admin/manage/identity/workflow/profile/view	PERMISSION
288	/permission/admin/manage/identity/workflow/profile/create	PERMISSION
289	/permission/admin/manage/identity/workflow/profile/delete	PERMISSION
290	/permission/admin/manage/identity/workflow/profile/update	PERMISSION
291	/permission/admin/manage/identity/workflow/monitor/view	PERMISSION
292	/permission/admin/manage/identity/workflow/monitor/delete	PERMISSION
293	/permission/admin/manage/identity/emailmgt/update	PERMISSION
294	/permission/admin/manage/identity/emailmgt/create	PERMISSION
295	/permission/admin/manage/identity/emailmgt/delete	PERMISSION
296	/permission/admin/manage/identity/emailmgt/view	PERMISSION
297	/permission/admin/manage/identity/provisioning	PERMISSION
298	/permission/admin/manage/identity/usermgt/update	PERMISSION
299	/permission/admin/manage/identity/usermgt/delete	PERMISSION
300	/permission/admin/manage/identity/usermgt/list	PERMISSION
301	/permission/admin/manage/identity/usermgt/view	PERMISSION
302	/permission/admin/manage/identity/usermgt/create	PERMISSION
303	/permission/admin/manage/identity/user/association/view	PERMISSION
304	/permission/admin/manage/identity/user/association/update	PERMISSION
305	/permission/admin/manage/identity/user/association/delete	PERMISSION
306	/permission/admin/manage/identity/user/association/create	PERMISSION
307	/permission/admin/manage/identity/userstore/count/delete	PERMISSION
308	/permission/admin/manage/identity/userstore/count/view	PERMISSION
309	/permission/admin/manage/identity/userstore/count/create	PERMISSION
310	/permission/admin/manage/identity/userstore/count/update	PERMISSION
311	/permission/admin/manage/identity/userstore/config/delete	PERMISSION
312	/permission/admin/manage/identity/userstore/config/create	PERMISSION
313	/permission/admin/manage/identity/userstore/config/update	PERMISSION
314	/permission/admin/manage/identity/userstore/config/view	PERMISSION
315	/permission/admin/manage/identity/functionsLibrarymgt/update	PERMISSION
316	/permission/admin/manage/identity/functionsLibrarymgt/create	PERMISSION
317	/permission/admin/manage/identity/functionsLibrarymgt/delete	PERMISSION
318	/permission/admin/manage/identity/functionsLibrarymgt/view	PERMISSION
319	/permission/admin/manage/identity/applicationmgt/delete	PERMISSION
320	/permission/admin/manage/identity/applicationmgt/create	PERMISSION
321	/permission/admin/manage/identity/applicationmgt/update	PERMISSION
322	/permission/admin/manage/identity/applicationmgt/view	PERMISSION
323	/permission/admin/manage/identity/rolemgt/create	PERMISSION
324	/permission/admin/manage/identity/rolemgt/delete	PERMISSION
325	/permission/admin/manage/identity/rolemgt/view	PERMISSION
326	/permission/admin/manage/identity/rolemgt/update	PERMISSION
327	/permission/admin/manage/identity/challenge/delete	PERMISSION
328	/permission/admin/manage/identity/challenge/create	PERMISSION
329	/permission/admin/manage/identity/challenge/update	PERMISSION
330	/permission/admin/manage/identity/challenge/view	PERMISSION
331	/permission/admin/manage/identity/stsmgt/create	PERMISSION
332	/permission/admin/manage/identity/stsmgt/delete	PERMISSION
333	/permission/admin/manage/identity/stsmgt/view	PERMISSION
334	/permission/admin/manage/identity/stsmgt/update	PERMISSION
335	/permission/admin/manage/identity/template/mgt/view	PERMISSION
336	/permission/admin/manage/identity/template/mgt/add	PERMISSION
337	/permission/admin/manage/identity/template/mgt/delete	PERMISSION
338	/permission/admin/manage/identity/template/mgt/list	PERMISSION
339	/permission/admin/manage/identity/userprofile/view	PERMISSION
340	/permission/admin/manage/identity/userprofile/create	PERMISSION
341	/permission/admin/manage/identity/userprofile/delete	PERMISSION
342	/permission/admin/manage/identity/userprofile/update	PERMISSION
343	/permission/admin/manage/event-publish	PERMISSION
344	/permission/admin/manage/modify/module	PERMISSION
345	/permission/admin/manage/modify/webapp	PERMISSION
346	/permission/admin/manage/modify/user-profile	PERMISSION
347	/permission/admin/manage/modify/service	PERMISSION
348	/permission/admin/manage/topic/deleteTopic	PERMISSION
349	/permission/admin/manage/topic/browseTopic	PERMISSION
350	/permission/admin/manage/topic/purgeTopic	PERMISSION
351	/permission/admin/manage/topic/addTopic	PERMISSION
352	/permission/admin/manage/humantask/packages	PERMISSION
353	/permission/admin/manage/humantask/viewtasks	PERMISSION
354	/permission/admin/manage/humantask/add	PERMISSION
355	/permission/admin/manage/humantask/task	PERMISSION
356	ADP_CREATOR	DEFAULT
356	ADP_SUBSCRIBER	DEFAULT
360	admin	DEFAULT
366	/permission/protected/configure/components	PERMISSION
367	/permission/protected/manage/modify/tenants	PERMISSION
368	/permission/protected/manage/monitor/tenants/list	PERMISSION
369	/permission/protected/server-admin/homepage	PERMISSION
370	/permission/admin/monitor/metrics	PERMISSION
371	/permission/admin/monitor/bpel	PERMISSION
372	/permission/admin/monitor/attachment	PERMISSION
373	/permission/admin/configure/security/usermgt/passwords	PERMISSION
374	/permission/admin/configure/security/usermgt/profiles	PERMISSION
375	/permission/admin/configure/security/usermgt/users	PERMISSION
376	/permission/admin/configure/security/usermgt/provisioning	PERMISSION
377	/permission/admin/configure/datasources	PERMISSION
378	/permission/admin/configure/theme	PERMISSION
379	everyone_permission	PERMISSION
380	/permission/admin/manage/event-streams	PERMISSION
381	/permission/admin/manage/search/advanced-search	PERMISSION
382	/permission/admin/manage/search/resources	PERMISSION
383	/permission/admin/manage/bpel/instance	PERMISSION
384	/permission/admin/manage/bpel/processes	PERMISSION
385	/permission/admin/manage/bpel/packages	PERMISSION
386	/permission/admin/manage/bpel/add	PERMISSION
387	/permission/admin/manage/resources/browse	PERMISSION
388	/permission/admin/manage/resources/notifications	PERMISSION
389	/permission/admin/manage/add/module	PERMISSION
390	/permission/admin/manage/add/service	PERMISSION
391	/permission/admin/manage/add/webapp	PERMISSION
392	/permission/admin/manage/attachment	PERMISSION
393	/permission/admin/manage/extensions/add	PERMISSION
394	/permission/admin/manage/extensions/list	PERMISSION
395	/permission/admin/manage/identity/pep	PERMISSION
396	/permission/admin/manage/identity/securitymgt/update	PERMISSION
397	/permission/admin/manage/identity/securitymgt/view	PERMISSION
398	/permission/admin/manage/identity/securitymgt/create	PERMISSION
399	/permission/admin/manage/identity/securitymgt/delete	PERMISSION
400	/permission/admin/manage/identity/authentication/session/view	PERMISSION
401	/permission/admin/manage/identity/authentication/session/delete	PERMISSION
402	/permission/admin/manage/identity/claimmgt/metadata/create	PERMISSION
403	/permission/admin/manage/identity/claimmgt/metadata/delete	PERMISSION
404	/permission/admin/manage/identity/claimmgt/metadata/update	PERMISSION
405	/permission/admin/manage/identity/claimmgt/metadata/view	PERMISSION
406	/permission/admin/manage/identity/claimmgt/claim/view	PERMISSION
407	/permission/admin/manage/identity/claimmgt/claim/update	PERMISSION
408	/permission/admin/manage/identity/claimmgt/claim/create	PERMISSION
409	/permission/admin/manage/identity/claimmgt/claim/delete	PERMISSION
410	/permission/admin/manage/identity/userroleuimgt/create	PERMISSION
411	/permission/admin/manage/identity/defaultauthSeq/view	PERMISSION
412	/permission/admin/manage/identity/defaultauthSeq/update	PERMISSION
413	/permission/admin/manage/identity/defaultauthSeq/create	PERMISSION
414	/permission/admin/manage/identity/defaultauthSeq/delete	PERMISSION
415	/permission/admin/manage/identity/entitlement/pap/policy/delete	PERMISSION
416	/permission/admin/manage/identity/entitlement/pap/policy/publish	PERMISSION
417	/permission/admin/manage/identity/entitlement/pap/policy/create	PERMISSION
418	/permission/admin/manage/identity/entitlement/pap/policy/view	PERMISSION
419	/permission/admin/manage/identity/entitlement/pap/policy/rollback	PERMISSION
420	/permission/admin/manage/identity/entitlement/pap/policy/order	PERMISSION
421	/permission/admin/manage/identity/entitlement/pap/policy/demote	PERMISSION
422	/permission/admin/manage/identity/entitlement/pap/policy/update	PERMISSION
423	/permission/admin/manage/identity/entitlement/pap/policy/enable	PERMISSION
424	/permission/admin/manage/identity/entitlement/pap/policy/list	PERMISSION
425	/permission/admin/manage/identity/entitlement/pap/subscriber/update	PERMISSION
426	/permission/admin/manage/identity/entitlement/pap/subscriber/view	PERMISSION
427	/permission/admin/manage/identity/entitlement/pap/subscriber/list	PERMISSION
428	/permission/admin/manage/identity/entitlement/pap/subscriber/create	PERMISSION
429	/permission/admin/manage/identity/entitlement/pap/subscriber/delete	PERMISSION
430	/permission/admin/manage/identity/entitlement/pdp/view	PERMISSION
431	/permission/admin/manage/identity/entitlement/pdp/manage	PERMISSION
432	/permission/admin/manage/identity/entitlement/pdp/test	PERMISSION
433	/permission/admin/manage/identity/entitlement/pep	PERMISSION
434	/permission/admin/manage/identity/idpmgt/delete	PERMISSION
435	/permission/admin/manage/identity/idpmgt/create	PERMISSION
436	/permission/admin/manage/identity/idpmgt/view	PERMISSION
437	/permission/admin/manage/identity/idpmgt/update	PERMISSION
438	/permission/admin/manage/identity/configmgt/view	PERMISSION
439	/permission/admin/manage/identity/configmgt/update	PERMISSION
440	/permission/admin/manage/identity/configmgt/delete	PERMISSION
441	/permission/admin/manage/identity/configmgt/list	PERMISSION
442	/permission/admin/manage/identity/configmgt/add	PERMISSION
443	/permission/admin/manage/identity/keystoremgt/view	PERMISSION
444	/permission/admin/manage/identity/keystoremgt/update	PERMISSION
445	/permission/admin/manage/identity/keystoremgt/delete	PERMISSION
446	/permission/admin/manage/identity/keystoremgt/create	PERMISSION
447	/permission/admin/manage/identity/consentmgt/list	PERMISSION
448	/permission/admin/manage/identity/consentmgt/view	PERMISSION
449	/permission/admin/manage/identity/consentmgt/add	PERMISSION
450	/permission/admin/manage/identity/consentmgt/delete	PERMISSION
451	/permission/admin/manage/identity/apptemplatemgt/view	PERMISSION
452	/permission/admin/manage/identity/apptemplatemgt/update	PERMISSION
453	/permission/admin/manage/identity/apptemplatemgt/delete	PERMISSION
454	/permission/admin/manage/identity/apptemplatemgt/create	PERMISSION
455	/permission/admin/manage/identity/identitymgt/view	PERMISSION
456	/permission/admin/manage/identity/identitymgt/update	PERMISSION
457	/permission/admin/manage/identity/identitymgt/create	PERMISSION
458	/permission/admin/manage/identity/identitymgt/delete	PERMISSION
459	/permission/admin/manage/identity/workflow/association/view	PERMISSION
460	/permission/admin/manage/identity/workflow/association/delete	PERMISSION
461	/permission/admin/manage/identity/workflow/association/create	PERMISSION
462	/permission/admin/manage/identity/workflow/association/update	PERMISSION
463	/permission/admin/manage/identity/workflow/definition/create	PERMISSION
464	/permission/admin/manage/identity/workflow/definition/delete	PERMISSION
465	/permission/admin/manage/identity/workflow/definition/view	PERMISSION
466	/permission/admin/manage/identity/workflow/definition/update	PERMISSION
467	/permission/admin/manage/identity/workflow/profile/view	PERMISSION
468	/permission/admin/manage/identity/workflow/profile/create	PERMISSION
469	/permission/admin/manage/identity/workflow/profile/delete	PERMISSION
470	/permission/admin/manage/identity/workflow/profile/update	PERMISSION
471	/permission/admin/manage/identity/workflow/monitor/view	PERMISSION
472	/permission/admin/manage/identity/workflow/monitor/delete	PERMISSION
473	/permission/admin/manage/identity/emailmgt/update	PERMISSION
474	/permission/admin/manage/identity/emailmgt/create	PERMISSION
475	/permission/admin/manage/identity/emailmgt/delete	PERMISSION
476	/permission/admin/manage/identity/emailmgt/view	PERMISSION
477	/permission/admin/manage/identity/provisioning	PERMISSION
478	/permission/admin/manage/identity/usermgt/update	PERMISSION
479	/permission/admin/manage/identity/usermgt/delete	PERMISSION
480	/permission/admin/manage/identity/usermgt/list	PERMISSION
481	/permission/admin/manage/identity/usermgt/view	PERMISSION
482	/permission/admin/manage/identity/usermgt/create	PERMISSION
483	/permission/admin/manage/identity/user/association/view	PERMISSION
484	/permission/admin/manage/identity/user/association/update	PERMISSION
485	/permission/admin/manage/identity/user/association/delete	PERMISSION
486	/permission/admin/manage/identity/user/association/create	PERMISSION
487	/permission/admin/manage/identity/userstore/count/delete	PERMISSION
488	/permission/admin/manage/identity/userstore/count/view	PERMISSION
489	/permission/admin/manage/identity/userstore/count/create	PERMISSION
490	/permission/admin/manage/identity/userstore/count/update	PERMISSION
491	/permission/admin/manage/identity/userstore/config/delete	PERMISSION
492	/permission/admin/manage/identity/userstore/config/create	PERMISSION
493	/permission/admin/manage/identity/userstore/config/update	PERMISSION
494	/permission/admin/manage/identity/userstore/config/view	PERMISSION
495	/permission/admin/manage/identity/functionsLibrarymgt/update	PERMISSION
496	/permission/admin/manage/identity/functionsLibrarymgt/create	PERMISSION
497	/permission/admin/manage/identity/functionsLibrarymgt/delete	PERMISSION
498	/permission/admin/manage/identity/functionsLibrarymgt/view	PERMISSION
499	/permission/admin/manage/identity/applicationmgt/delete	PERMISSION
500	/permission/admin/manage/identity/applicationmgt/create	PERMISSION
501	/permission/admin/manage/identity/applicationmgt/update	PERMISSION
502	/permission/admin/manage/identity/applicationmgt/view	PERMISSION
503	/permission/admin/manage/identity/rolemgt/create	PERMISSION
504	/permission/admin/manage/identity/rolemgt/delete	PERMISSION
505	/permission/admin/manage/identity/rolemgt/view	PERMISSION
506	/permission/admin/manage/identity/rolemgt/update	PERMISSION
507	/permission/admin/manage/identity/challenge/delete	PERMISSION
508	/permission/admin/manage/identity/challenge/create	PERMISSION
509	/permission/admin/manage/identity/challenge/update	PERMISSION
510	/permission/admin/manage/identity/challenge/view	PERMISSION
511	/permission/admin/manage/identity/stsmgt/create	PERMISSION
512	/permission/admin/manage/identity/stsmgt/delete	PERMISSION
513	/permission/admin/manage/identity/stsmgt/view	PERMISSION
514	/permission/admin/manage/identity/stsmgt/update	PERMISSION
515	/permission/admin/manage/identity/template/mgt/view	PERMISSION
516	/permission/admin/manage/identity/template/mgt/add	PERMISSION
517	/permission/admin/manage/identity/template/mgt/delete	PERMISSION
518	/permission/admin/manage/identity/template/mgt/list	PERMISSION
519	/permission/admin/manage/identity/userprofile/view	PERMISSION
520	/permission/admin/manage/identity/userprofile/create	PERMISSION
521	/permission/admin/manage/identity/userprofile/delete	PERMISSION
522	/permission/admin/manage/identity/userprofile/update	PERMISSION
523	/permission/admin/manage/event-publish	PERMISSION
524	/permission/admin/manage/modify/module	PERMISSION
525	/permission/admin/manage/modify/webapp	PERMISSION
526	/permission/admin/manage/modify/user-profile	PERMISSION
527	/permission/admin/manage/modify/service	PERMISSION
528	/permission/admin/manage/topic/deleteTopic	PERMISSION
529	/permission/admin/manage/topic/browseTopic	PERMISSION
530	/permission/admin/manage/topic/purgeTopic	PERMISSION
531	/permission/admin/manage/topic/addTopic	PERMISSION
532	/permission/admin/manage/humantask/packages	PERMISSION
533	/permission/admin/manage/humantask/viewtasks	PERMISSION
534	/permission/admin/manage/humantask/add	PERMISSION
535	/permission/admin/manage/humantask/task	PERMISSION
536	ADP_CREATOR	DEFAULT
536	ADP_SUBSCRIBER	DEFAULT
540	admin	DEFAULT
\.


--
-- Data for Name: idn_oauth2_scope_validators; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_scope_validators (app_id, scope_validator) FROM stdin;
\.


--
-- Data for Name: idn_oauth2_token_binding; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth2_token_binding (token_id, token_binding_type, token_binding_ref, token_binding_value, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_oauth_consumer_apps; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oauth_consumer_apps (id, consumer_key, consumer_secret, username, tenant_id, user_domain, app_name, oauth_version, callback_url, grant_types, pkce_mandatory, pkce_support_plain, app_state, user_access_token_expire_time, app_access_token_expire_time, refresh_token_expire_time, id_token_expire_time) FROM stdin;
1	tfY6J6QOExf18A545h6gfQ2tZ8Ya	nbFHA6g4zNh7sYgfT_kE9V39RYMa	admin	-1234	PRIMARY	rest_api_admin	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
2	MTBiV2fK8ea8JCyVUL_TQuz_fWka	1FkDAUSq9x6UzLCnfbw6M4TLIBca	adp_crt_user	-1234	PRIMARY	rest_api_creator	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
3	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	zWsMYrQT1qFG10FqbONaBEr4GT0a	adp_pub_user	-1234	PRIMARY	rest_api_publisher	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
4	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	BWNSKok6UObheFCQJbs226iZH0Ya	adp_sub_user	-1234	PRIMARY	rest_api_subscriber	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
5	IjE3DFqMvXwVFKXf5XM5fMFwafMa	n6TjRXX2retn3FLV_xbsqlmlQA4a	apim_reserved_user	-1234	PRIMARY	adp_sub_user_ADPApplicationCS_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	3600	3600	86400	3600
6	w1ykQHnJIB8G5PpxI1TUiM7W05oa	BY6BjcISGKfqak83eoiq2ThpfDoa	apim_reserved_user	-1234	PRIMARY	adp_sub_user_CustomerApp_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	2147483646	2147483646	2147483646	2147483646
7	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	FylWIWxQrrx2BhvtfMifjyMziA4a	admin	1	PRIMARY	rest_api_admin	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
8	7QKYoz_shq4efQsLp_ImVoJAVfga	2_mfvTB8OGDgrqUpXnVvaNxkg2sa	adp_crt_user	1	PRIMARY	rest_api_creator	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
9	Iea_s33OIUf0zlC25l2gfEHAgbsa	piHNP2yHTBSINfldBv3fJgmrxEMa	adp_pub_user	1	PRIMARY	rest_api_publisher	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
10	MpESvRoYMBcNTXpVKwJFlKl6DHMa	BEv1bdm3rQ_fC5CNZJojcQnikGEa	adp_sub_user	1	PRIMARY	rest_api_subscriber	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
11	63GebUlqAMneJSCXJTFLcHrlZQka	x9PDUCKvV2kL4BOAF1GA5omx0Joa	apim_reserved_user	1	PRIMARY	adp_sub_user_ADPApplicationEC_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	3600	3600	86400	3600
12	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	v_LTROT56FN_er0oAzP1xcHblfga	apim_reserved_user	1	PRIMARY	adp_sub_user_CustomerApp_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	2147483646	2147483646	2147483646	2147483646
13	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	_QfK_bK68k0qR3W51IErj7_pmeoa	apim_reserved_user	-1234	PRIMARY	adp_sub_user_ADPCTSApplicationEC_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	3600	3600	86400	3600
14	AGVTdJnLVqt4aiHeQuPwli88Orca	HAMUIKUfGSV8A7hnWXYWyVNkUIYa	admin	2	PRIMARY	rest_api_admin	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
15	db7IatPG_pt47li7kZ_Wk7MmzdAa	fkQyF9Vp23wKnmfgZKCqRbEfAaMa	adp_crt_user	2	PRIMARY	rest_api_creator	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
16	ugckN838E9JYYyUVdlUI1GleVAwa	zGOIQoHOpYiiTzwcCsDa5BOIdLwa	adp_pub_user	2	PRIMARY	rest_api_publisher	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
17	FUU0l_k3877XrI08RlAN873RXOIa	YqpdAecrs6imMsOhiyGfAfvfRlAa	adp_sub_user	2	PRIMARY	rest_api_subscriber	OAuth-2.0	www.google.lk	client_credentials password refresh_token	0	0	ACTIVE	3600	3600	86400	3600
18	JmEzoL3bVKApBDegL1atV0cCfEoa	hCTm_Y38DO2Cbir_iVvzyuINoQsa	apim_reserved_user	2	PRIMARY	adp_sub_user_ADPApplicationSC_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	3600	3600	86400	3600
19	LrCi_oe6yaP1SeRfzzvccZIDECoa	RCJ6_gMnnRQ7o3faTJO0SpVxmN0a	apim_reserved_user	2	PRIMARY	adp_sub_user_CustomerApp_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	2147483646	2147483646	2147483646	2147483646
20	a6M8FWBd8aBCnoaYttsfjRGhVu0a	FGOMDC7EWAUynW5wd2iNaqXsirEa	apim_reserved_user	-1234	PRIMARY	adp_sub_user_ADPCTSApplicationSC_PRODUCTION	OAuth-2.0		refresh_token urn:ietf:params:oauth:grant-type:saml2-bearer password client_credentials iwa:ntlm urn:ietf:params:oauth:grant-type:jwt-bearer	0	0	ACTIVE	3600	3600	86400	3600
\.


--
-- Data for Name: idn_oidc_jti; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oidc_jti (jwt_id, exp_time, time_created) FROM stdin;
\.


--
-- Data for Name: idn_oidc_property; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oidc_property (id, tenant_id, consumer_key, property_key, property_value) FROM stdin;
1	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	requestObjectSigned	false
2	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	idTokenEncrypted	false
3	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	idTokenEncryptionAlgorithm	null
4	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	idTokenEncryptionMethod	null
5	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	backChannelLogoutURL	\N
6	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	frontchannelLogoutURL	\N
7	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	tokenType	Default
8	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	bypassClientCredentials	false
9	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	renewRefreshToken	\N
10	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	tokenBindingType	\N
11	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	tokenRevocationWithIDPSessionTermination	false
12	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	tokenBindingValidation	true
13	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	requestObjectSigned	false
14	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	idTokenEncrypted	false
15	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	idTokenEncryptionAlgorithm	null
16	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	idTokenEncryptionMethod	null
17	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	backChannelLogoutURL	\N
18	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	frontchannelLogoutURL	\N
19	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	tokenType	Default
20	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	bypassClientCredentials	false
21	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	renewRefreshToken	\N
22	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	tokenBindingType	\N
23	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	tokenRevocationWithIDPSessionTermination	false
24	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	tokenBindingValidation	true
25	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	requestObjectSigned	false
26	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	idTokenEncrypted	false
27	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	idTokenEncryptionAlgorithm	null
28	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	idTokenEncryptionMethod	null
29	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	backChannelLogoutURL	\N
30	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	frontchannelLogoutURL	\N
31	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	tokenType	Default
32	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	bypassClientCredentials	false
33	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	renewRefreshToken	\N
34	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	tokenBindingType	\N
35	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	tokenRevocationWithIDPSessionTermination	false
36	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	tokenBindingValidation	true
37	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	requestObjectSigned	false
38	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	idTokenEncrypted	false
39	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	idTokenEncryptionAlgorithm	null
40	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	idTokenEncryptionMethod	null
41	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	backChannelLogoutURL	\N
42	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	frontchannelLogoutURL	\N
43	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	tokenType	Default
44	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	bypassClientCredentials	false
45	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	renewRefreshToken	\N
46	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	tokenBindingType	\N
47	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	tokenRevocationWithIDPSessionTermination	false
48	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	tokenBindingValidation	true
49	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	requestObjectSigned	false
50	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	idTokenEncrypted	false
51	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	idTokenEncryptionAlgorithm	null
52	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	idTokenEncryptionMethod	null
53	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	backChannelLogoutURL	\N
54	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	frontchannelLogoutURL	\N
55	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	tokenType	JWT
56	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	bypassClientCredentials	false
57	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	renewRefreshToken	\N
58	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	tokenBindingType	\N
59	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	tokenRevocationWithIDPSessionTermination	false
60	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	tokenBindingValidation	true
61	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	requestObjectSigned	false
62	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	idTokenEncrypted	false
63	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	idTokenEncryptionAlgorithm	null
64	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	idTokenEncryptionMethod	null
65	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	backChannelLogoutURL	\N
66	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	frontchannelLogoutURL	\N
67	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	tokenType	JWT
68	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	bypassClientCredentials	false
69	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	renewRefreshToken	\N
70	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	tokenBindingType	\N
71	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	tokenRevocationWithIDPSessionTermination	false
72	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	tokenBindingValidation	true
73	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	requestObjectSigned	false
74	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	idTokenEncrypted	false
75	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	idTokenEncryptionAlgorithm	null
76	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	idTokenEncryptionMethod	null
77	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	backChannelLogoutURL	\N
78	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	frontchannelLogoutURL	\N
79	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	tokenType	Default
80	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	bypassClientCredentials	false
81	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	renewRefreshToken	\N
82	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	tokenBindingType	\N
83	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	tokenRevocationWithIDPSessionTermination	false
84	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	tokenBindingValidation	true
85	1	7QKYoz_shq4efQsLp_ImVoJAVfga	requestObjectSigned	false
86	1	7QKYoz_shq4efQsLp_ImVoJAVfga	idTokenEncrypted	false
87	1	7QKYoz_shq4efQsLp_ImVoJAVfga	idTokenEncryptionAlgorithm	null
88	1	7QKYoz_shq4efQsLp_ImVoJAVfga	idTokenEncryptionMethod	null
89	1	7QKYoz_shq4efQsLp_ImVoJAVfga	backChannelLogoutURL	\N
90	1	7QKYoz_shq4efQsLp_ImVoJAVfga	frontchannelLogoutURL	\N
91	1	7QKYoz_shq4efQsLp_ImVoJAVfga	tokenType	Default
92	1	7QKYoz_shq4efQsLp_ImVoJAVfga	bypassClientCredentials	false
93	1	7QKYoz_shq4efQsLp_ImVoJAVfga	renewRefreshToken	\N
94	1	7QKYoz_shq4efQsLp_ImVoJAVfga	tokenBindingType	\N
95	1	7QKYoz_shq4efQsLp_ImVoJAVfga	tokenRevocationWithIDPSessionTermination	false
96	1	7QKYoz_shq4efQsLp_ImVoJAVfga	tokenBindingValidation	true
97	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	requestObjectSigned	false
98	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	idTokenEncrypted	false
99	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	idTokenEncryptionAlgorithm	null
100	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	idTokenEncryptionMethod	null
101	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	backChannelLogoutURL	\N
102	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	frontchannelLogoutURL	\N
103	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	tokenType	Default
104	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	bypassClientCredentials	false
105	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	renewRefreshToken	\N
106	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	tokenBindingType	\N
107	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	tokenRevocationWithIDPSessionTermination	false
108	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	tokenBindingValidation	true
109	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	requestObjectSigned	false
110	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	idTokenEncrypted	false
111	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	idTokenEncryptionAlgorithm	null
112	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	idTokenEncryptionMethod	null
113	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	backChannelLogoutURL	\N
114	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	frontchannelLogoutURL	\N
115	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	tokenType	Default
116	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	bypassClientCredentials	false
117	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	renewRefreshToken	\N
118	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	tokenBindingType	\N
119	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	tokenRevocationWithIDPSessionTermination	false
120	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	tokenBindingValidation	true
121	1	63GebUlqAMneJSCXJTFLcHrlZQka	requestObjectSigned	false
122	1	63GebUlqAMneJSCXJTFLcHrlZQka	idTokenEncrypted	false
123	1	63GebUlqAMneJSCXJTFLcHrlZQka	idTokenEncryptionAlgorithm	null
124	1	63GebUlqAMneJSCXJTFLcHrlZQka	idTokenEncryptionMethod	null
125	1	63GebUlqAMneJSCXJTFLcHrlZQka	backChannelLogoutURL	\N
126	1	63GebUlqAMneJSCXJTFLcHrlZQka	frontchannelLogoutURL	\N
127	1	63GebUlqAMneJSCXJTFLcHrlZQka	tokenType	JWT
128	1	63GebUlqAMneJSCXJTFLcHrlZQka	bypassClientCredentials	false
129	1	63GebUlqAMneJSCXJTFLcHrlZQka	renewRefreshToken	\N
130	1	63GebUlqAMneJSCXJTFLcHrlZQka	tokenBindingType	\N
131	1	63GebUlqAMneJSCXJTFLcHrlZQka	tokenRevocationWithIDPSessionTermination	false
132	1	63GebUlqAMneJSCXJTFLcHrlZQka	tokenBindingValidation	true
133	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	requestObjectSigned	false
134	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	idTokenEncrypted	false
135	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	idTokenEncryptionAlgorithm	null
136	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	idTokenEncryptionMethod	null
137	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	backChannelLogoutURL	\N
138	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	frontchannelLogoutURL	\N
139	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	tokenType	JWT
140	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	bypassClientCredentials	false
141	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	renewRefreshToken	\N
142	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	tokenBindingType	\N
143	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	tokenRevocationWithIDPSessionTermination	false
144	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	tokenBindingValidation	true
145	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	requestObjectSigned	false
146	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	idTokenEncrypted	false
147	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	idTokenEncryptionAlgorithm	null
148	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	idTokenEncryptionMethod	null
149	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	backChannelLogoutURL	\N
150	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	frontchannelLogoutURL	\N
151	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	tokenType	JWT
152	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	bypassClientCredentials	false
153	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	renewRefreshToken	\N
154	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	tokenBindingType	\N
155	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	tokenRevocationWithIDPSessionTermination	false
156	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	tokenBindingValidation	true
157	2	AGVTdJnLVqt4aiHeQuPwli88Orca	requestObjectSigned	false
158	2	AGVTdJnLVqt4aiHeQuPwli88Orca	idTokenEncrypted	false
159	2	AGVTdJnLVqt4aiHeQuPwli88Orca	idTokenEncryptionAlgorithm	null
160	2	AGVTdJnLVqt4aiHeQuPwli88Orca	idTokenEncryptionMethod	null
161	2	AGVTdJnLVqt4aiHeQuPwli88Orca	backChannelLogoutURL	\N
162	2	AGVTdJnLVqt4aiHeQuPwli88Orca	frontchannelLogoutURL	\N
163	2	AGVTdJnLVqt4aiHeQuPwli88Orca	tokenType	Default
164	2	AGVTdJnLVqt4aiHeQuPwli88Orca	bypassClientCredentials	false
165	2	AGVTdJnLVqt4aiHeQuPwli88Orca	renewRefreshToken	\N
166	2	AGVTdJnLVqt4aiHeQuPwli88Orca	tokenBindingType	\N
167	2	AGVTdJnLVqt4aiHeQuPwli88Orca	tokenRevocationWithIDPSessionTermination	false
168	2	AGVTdJnLVqt4aiHeQuPwli88Orca	tokenBindingValidation	true
169	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	requestObjectSigned	false
170	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	idTokenEncrypted	false
171	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	idTokenEncryptionAlgorithm	null
172	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	idTokenEncryptionMethod	null
173	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	backChannelLogoutURL	\N
174	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	frontchannelLogoutURL	\N
175	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	tokenType	Default
176	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	bypassClientCredentials	false
177	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	renewRefreshToken	\N
178	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	tokenBindingType	\N
179	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	tokenRevocationWithIDPSessionTermination	false
180	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	tokenBindingValidation	true
181	2	ugckN838E9JYYyUVdlUI1GleVAwa	requestObjectSigned	false
182	2	ugckN838E9JYYyUVdlUI1GleVAwa	idTokenEncrypted	false
183	2	ugckN838E9JYYyUVdlUI1GleVAwa	idTokenEncryptionAlgorithm	null
184	2	ugckN838E9JYYyUVdlUI1GleVAwa	idTokenEncryptionMethod	null
185	2	ugckN838E9JYYyUVdlUI1GleVAwa	backChannelLogoutURL	\N
186	2	ugckN838E9JYYyUVdlUI1GleVAwa	frontchannelLogoutURL	\N
187	2	ugckN838E9JYYyUVdlUI1GleVAwa	tokenType	Default
188	2	ugckN838E9JYYyUVdlUI1GleVAwa	bypassClientCredentials	false
189	2	ugckN838E9JYYyUVdlUI1GleVAwa	renewRefreshToken	\N
190	2	ugckN838E9JYYyUVdlUI1GleVAwa	tokenBindingType	\N
191	2	ugckN838E9JYYyUVdlUI1GleVAwa	tokenRevocationWithIDPSessionTermination	false
192	2	ugckN838E9JYYyUVdlUI1GleVAwa	tokenBindingValidation	true
193	2	FUU0l_k3877XrI08RlAN873RXOIa	requestObjectSigned	false
194	2	FUU0l_k3877XrI08RlAN873RXOIa	idTokenEncrypted	false
195	2	FUU0l_k3877XrI08RlAN873RXOIa	idTokenEncryptionAlgorithm	null
196	2	FUU0l_k3877XrI08RlAN873RXOIa	idTokenEncryptionMethod	null
197	2	FUU0l_k3877XrI08RlAN873RXOIa	backChannelLogoutURL	\N
198	2	FUU0l_k3877XrI08RlAN873RXOIa	frontchannelLogoutURL	\N
199	2	FUU0l_k3877XrI08RlAN873RXOIa	tokenType	Default
200	2	FUU0l_k3877XrI08RlAN873RXOIa	bypassClientCredentials	false
201	2	FUU0l_k3877XrI08RlAN873RXOIa	renewRefreshToken	\N
202	2	FUU0l_k3877XrI08RlAN873RXOIa	tokenBindingType	\N
203	2	FUU0l_k3877XrI08RlAN873RXOIa	tokenRevocationWithIDPSessionTermination	false
204	2	FUU0l_k3877XrI08RlAN873RXOIa	tokenBindingValidation	true
205	2	JmEzoL3bVKApBDegL1atV0cCfEoa	requestObjectSigned	false
206	2	JmEzoL3bVKApBDegL1atV0cCfEoa	idTokenEncrypted	false
207	2	JmEzoL3bVKApBDegL1atV0cCfEoa	idTokenEncryptionAlgorithm	null
208	2	JmEzoL3bVKApBDegL1atV0cCfEoa	idTokenEncryptionMethod	null
209	2	JmEzoL3bVKApBDegL1atV0cCfEoa	backChannelLogoutURL	\N
210	2	JmEzoL3bVKApBDegL1atV0cCfEoa	frontchannelLogoutURL	\N
211	2	JmEzoL3bVKApBDegL1atV0cCfEoa	tokenType	JWT
212	2	JmEzoL3bVKApBDegL1atV0cCfEoa	bypassClientCredentials	false
213	2	JmEzoL3bVKApBDegL1atV0cCfEoa	renewRefreshToken	\N
214	2	JmEzoL3bVKApBDegL1atV0cCfEoa	tokenBindingType	\N
215	2	JmEzoL3bVKApBDegL1atV0cCfEoa	tokenRevocationWithIDPSessionTermination	false
216	2	JmEzoL3bVKApBDegL1atV0cCfEoa	tokenBindingValidation	true
217	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	requestObjectSigned	false
218	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	idTokenEncrypted	false
219	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	idTokenEncryptionAlgorithm	null
220	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	idTokenEncryptionMethod	null
221	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	backChannelLogoutURL	\N
222	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	frontchannelLogoutURL	\N
223	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	tokenType	JWT
224	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	bypassClientCredentials	false
225	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	renewRefreshToken	\N
226	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	tokenBindingType	\N
227	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	tokenRevocationWithIDPSessionTermination	false
228	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	tokenBindingValidation	true
229	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	requestObjectSigned	false
230	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	idTokenEncrypted	false
231	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	idTokenEncryptionAlgorithm	null
232	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	idTokenEncryptionMethod	null
233	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	backChannelLogoutURL	\N
234	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	frontchannelLogoutURL	\N
235	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	tokenType	JWT
236	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	bypassClientCredentials	false
237	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	renewRefreshToken	\N
238	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	tokenBindingType	\N
239	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	tokenRevocationWithIDPSessionTermination	false
240	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	tokenBindingValidation	true
\.


--
-- Data for Name: idn_oidc_req_obj_claim_values; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oidc_req_obj_claim_values (id, req_object_claims_id, claim_values) FROM stdin;
\.


--
-- Data for Name: idn_oidc_req_object_claims; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oidc_req_object_claims (id, req_object_id, claim_attribute, essential, value, is_userinfo) FROM stdin;
\.


--
-- Data for Name: idn_oidc_req_object_reference; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oidc_req_object_reference (id, consumer_key_id, code_id, token_id, session_data_key) FROM stdin;
\.


--
-- Data for Name: idn_oidc_scope_claim_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_oidc_scope_claim_mapping (id, scope_id, external_claim_id) FROM stdin;
1	1	148
2	1	250
3	1	198
4	1	118
5	1	263
6	1	92
7	1	204
8	1	135
9	1	242
10	1	271
11	1	151
12	1	208
13	1	252
14	1	112
15	1	191
16	1	177
17	1	219
18	1	189
19	1	179
20	1	217
21	1	136
22	1	122
23	1	211
24	1	241
25	1	256
26	1	186
27	1	225
28	1	226
29	2	112
30	2	208
31	3	252
32	3	118
33	3	263
34	3	92
35	3	177
36	3	135
37	3	217
38	3	136
39	3	242
40	3	122
41	3	271
42	3	241
43	3	151
44	3	226
45	4	179
46	4	256
47	5	250
48	5	191
49	181	422
50	181	524
51	181	472
52	181	392
53	181	537
54	181	366
55	181	478
56	181	409
57	181	516
58	181	545
59	181	425
60	181	482
61	181	526
62	181	386
63	181	465
64	181	451
65	181	493
66	181	463
67	181	453
68	181	491
69	181	410
70	181	396
71	181	485
72	181	515
73	181	530
74	181	460
75	181	499
76	181	500
77	182	386
78	182	482
79	183	526
80	183	392
81	183	537
82	183	366
83	183	451
84	183	409
85	183	491
86	183	410
87	183	516
88	183	396
89	183	545
90	183	515
91	183	425
92	183	500
93	184	453
94	184	530
95	185	524
96	185	465
97	361	696
98	361	798
99	361	746
100	361	666
101	361	811
102	361	640
103	361	752
104	361	683
105	361	790
106	361	819
107	361	699
108	361	756
109	361	800
110	361	660
111	361	739
112	361	725
113	361	767
114	361	737
115	361	727
116	361	765
117	361	684
118	361	670
119	361	759
120	361	789
121	361	804
122	361	734
123	361	773
124	361	774
125	362	660
126	362	756
127	363	800
128	363	666
129	363	811
130	363	640
131	363	725
132	363	683
133	363	765
134	363	684
135	363	790
136	363	670
137	363	819
138	363	789
139	363	699
140	363	774
141	364	727
142	364	804
143	365	798
144	365	739
\.


--
-- Data for Name: idn_openid_associations; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_openid_associations (handle, assoc_type, expire_in, mac_key, assoc_store, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_openid_remember_me; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_openid_remember_me (user_name, tenant_id, cookie_value, created_time) FROM stdin;
\.


--
-- Data for Name: idn_openid_user_rps; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_openid_user_rps (user_name, tenant_id, rp_url, trusted_always, last_visit, visit_count, default_profile_name) FROM stdin;
\.


--
-- Data for Name: idn_password_history_data; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_password_history_data (id, user_name, user_domain, tenant_id, salt_value, hash, time_created) FROM stdin;
\.


--
-- Data for Name: idn_recovery_data; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_recovery_data (user_name, user_domain, tenant_id, code, scenario, step, time_created, remaining_sets) FROM stdin;
\.


--
-- Data for Name: idn_saml2_artifact_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_saml2_artifact_store (id, source_id, message_handler, authn_req_dto, session_id, init_timestamp, exp_timestamp, assertion_id) FROM stdin;
\.


--
-- Data for Name: idn_saml2_assertion_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_saml2_assertion_store (id, saml2_id, saml2_issuer, saml2_subject, saml2_session_index, saml2_authn_context_class_ref, saml2_assertion, assertion) FROM stdin;
\.


--
-- Data for Name: idn_scim_group; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_scim_group (id, tenant_id, role_name, attr_name, attr_value) FROM stdin;
\.


--
-- Data for Name: idn_sts_store; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_sts_store (id, token_id, token_content, create_date, expire_date, state) FROM stdin;
\.


--
-- Data for Name: idn_thrift_session; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_thrift_session (session_id, user_name, created_time, last_modified_time, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_uma_permission_ticket; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_uma_permission_ticket (id, pt, time_created, expiry_time, ticket_state, tenant_id) FROM stdin;
\.


--
-- Data for Name: idn_uma_pt_resource; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_uma_pt_resource (id, pt_resource_id, pt_id) FROM stdin;
\.


--
-- Data for Name: idn_uma_pt_resource_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_uma_pt_resource_scope (id, pt_resource_id, pt_scope_id) FROM stdin;
\.


--
-- Data for Name: idn_uma_resource; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_uma_resource (id, resource_id, resource_name, time_created, resource_owner_name, client_id, tenant_id, user_domain) FROM stdin;
\.


--
-- Data for Name: idn_uma_resource_meta_data; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_uma_resource_meta_data (id, resource_identity, property_key, property_value) FROM stdin;
\.


--
-- Data for Name: idn_uma_resource_scope; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_uma_resource_scope (id, resource_identity, scope_name) FROM stdin;
\.


--
-- Data for Name: idn_user_account_association; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idn_user_account_association (association_key, tenant_id, domain_name, user_name) FROM stdin;
\.


--
-- Data for Name: idp; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp (id, tenant_id, name, is_enabled, is_primary, home_realm_id, image, certificate, alias, inbound_prov_enabled, inbound_prov_user_store_id, user_claim_uri, role_claim_uri, description, default_authenticator_name, default_pro_connector_name, provisioning_role, is_federation_hub, is_local_claim_dialect, display_name, image_url, uuid) FROM stdin;
1	-1234	LOCAL	1	1	localhost	\N	\\x5b5d	\N	0	\N	\N	\N	\N	\N	\N	\N	0	0	\N	\N	bc7dbfaf-82c7-43f4-8f56-286b6e5998c0
2	1	LOCAL	1	1	localhost	\N	\\x5b5d	\N	0	\N	\N	\N	\N	\N	\N	\N	0	0	\N	\N	ddc8183d-9546-4633-946b-cd3be743cbcc
3	2	LOCAL	1	1	localhost	\N	\\x5b5d	\N	0	\N	\N	\N	\N	\N	\N	\N	0	0	\N	\N	e9e85376-367f-4966-a48e-f1d2a6554a7b
\.


--
-- Data for Name: idp_authenticator; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_authenticator (id, tenant_id, idp_id, name, is_enabled, display_name) FROM stdin;
1	-1234	1	passivests	0	\N
2	-1234	1	samlsso	0	\N
3	-1234	1	openidconnect	0	\N
4	1	2	passivests	0	\N
5	1	2	samlsso	0	\N
6	1	2	openidconnect	0	\N
7	2	3	passivests	0	\N
8	2	3	samlsso	0	\N
9	2	3	openidconnect	0	\N
\.


--
-- Data for Name: idp_authenticator_property; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_authenticator_property (id, tenant_id, authenticator_id, property_key, property_value, is_secret) FROM stdin;
1	-1234	1	IdPEntityId	localhost	0
2	-1234	2	IdPEntityId	localhost	0
3	-1234	2	SAMLMetadataSigningEnabled	false	0
4	-1234	2	SAMLMetadataValidityPeriod	60	0
5	-1234	2	samlAuthnRequestsSigningEnabled	false	0
6	-1234	3	IdPEntityId	https://localhost:9443/oauth2/token	0
7	1	4	IdPEntityId	localhost	0
8	1	5	IdPEntityId	localhost	0
9	1	5	SAMLMetadataSigningEnabled	false	0
10	1	5	SAMLMetadataValidityPeriod	60	0
11	1	5	samlAuthnRequestsSigningEnabled	false	0
12	1	6	IdPEntityId	https://localhost:9443/oauth2/token	0
13	2	7	IdPEntityId	localhost	0
14	2	8	IdPEntityId	localhost	0
15	2	8	SAMLMetadataSigningEnabled	false	0
16	2	8	SAMLMetadataValidityPeriod	60	0
17	2	8	samlAuthnRequestsSigningEnabled	false	0
18	2	9	IdPEntityId	https://localhost:9443/oauth2/token	0
\.


--
-- Data for Name: idp_claim; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_claim (id, idp_id, tenant_id, claim) FROM stdin;
\.


--
-- Data for Name: idp_claim_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_claim_mapping (id, idp_claim_id, tenant_id, local_claim, default_value, is_requested) FROM stdin;
\.


--
-- Data for Name: idp_local_claim; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_local_claim (id, tenant_id, idp_id, claim_uri, default_value, is_requested) FROM stdin;
\.


--
-- Data for Name: idp_metadata; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_metadata (id, idp_id, name, value, display_name, tenant_id) FROM stdin;
1	1	RememberMeTimeout	20160	\N	-1234
2	1	SessionIdleTimeout	15	\N	-1234
3	1	PASSWORD_PROVISIONING_ENABLED	false	\N	-1234
4	1	MODIFY_USERNAME_ENABLED	false	\N	-1234
5	1	PROMPT_CONSENT_ENABLED	false	\N	-1234
6	2	RememberMeTimeout	20160	\N	1
7	2	SessionIdleTimeout	15	\N	1
8	2	PASSWORD_PROVISIONING_ENABLED	false	\N	1
9	2	MODIFY_USERNAME_ENABLED	false	\N	1
10	2	PROMPT_CONSENT_ENABLED	false	\N	1
11	3	RememberMeTimeout	20160	\N	2
12	3	SessionIdleTimeout	15	\N	2
13	3	PASSWORD_PROVISIONING_ENABLED	false	\N	2
14	3	MODIFY_USERNAME_ENABLED	false	\N	2
15	3	PROMPT_CONSENT_ENABLED	false	\N	2
\.


--
-- Data for Name: idp_prov_config_property; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_prov_config_property (id, tenant_id, provisioning_config_id, property_key, property_value, property_blob_value, property_type, is_secret) FROM stdin;
\.


--
-- Data for Name: idp_provisioning_config; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_provisioning_config (id, tenant_id, idp_id, provisioning_connector_type, is_enabled, is_blocking, is_rules_enabled) FROM stdin;
\.


--
-- Data for Name: idp_provisioning_entity; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_provisioning_entity (id, provisioning_config_id, entity_type, entity_local_userstore, entity_name, entity_value, tenant_id, entity_local_id) FROM stdin;
\.


--
-- Data for Name: idp_role; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_role (id, idp_id, tenant_id, role) FROM stdin;
\.


--
-- Data for Name: idp_role_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.idp_role_mapping (id, idp_role_id, tenant_id, user_store_id, local_role) FROM stdin;
\.


--
-- Data for Name: sp_app; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_app (id, tenant_id, app_name, user_store, username, description, role_claim, auth_type, provisioning_userstore_domain, is_local_claim_dialect, is_send_local_subject_id, is_send_auth_list_of_idps, is_use_tenant_domain_subject, is_use_user_domain_subject, enable_authorization, subject_claim_uri, is_saas_app, is_dumb_mode, uuid, image_url, access_url, is_discoverable) FROM stdin;
13	-1234	adp_sub_user_ADPCTSApplicationEC_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_ADPCTSApplicationEC_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	949980a4-c932-4714-9519-48e63604f64f	\N	\N	0
6	-1234	adp_sub_user_CustomerApp_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_CustomerApp_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	bfb0861e-5b6b-4e76-a058-5d2c9ae0f374	\N	\N	0
8	1	rest_api_creator	PRIMARY	adp_crt_user	Service Provider for application rest_api_creator	\N	default	\N	1	0	0	0	0	0	\N	1	0	69bd9f5c-7b70-40dd-8537-de3c42ad8d13	\N	\N	0
10	1	rest_api_subscriber	PRIMARY	adp_sub_user	Service Provider for application rest_api_subscriber	\N	default	\N	1	0	0	0	0	0	\N	1	0	2ecbc031-f09f-450a-9354-03ea15d36678	\N	\N	0
4	-1234	rest_api_subscriber	PRIMARY	adp_sub_user	Service Provider for application rest_api_subscriber	\N	default	\N	1	0	0	0	0	0	\N	1	0	8db2205a-473e-4f40-a8a3-68bad278c6f5	\N	\N	0
1	-1234	rest_api_admin	PRIMARY	admin	Service Provider for application rest_api_admin	\N	default	\N	1	0	0	0	0	0	\N	1	0	3f5f29b1-53dc-4eab-ba51-453a4eb44cdd	\N	\N	0
17	2	rest_api_subscriber	PRIMARY	adp_sub_user	Service Provider for application rest_api_subscriber	\N	default	\N	1	0	0	0	0	0	\N	1	0	55b998ba-a672-4fe5-a857-fcc4d762e3d6	\N	\N	0
12	1	adp_sub_user_CustomerApp_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_CustomerApp_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	bf128676-3842-4be3-af3e-30da984921fe	\N	\N	0
7	1	rest_api_admin	PRIMARY	admin	Service Provider for application rest_api_admin	\N	default	\N	1	0	0	0	0	0	\N	1	0	cc5d0e47-2232-4d6b-ab14-211f1c24a913	\N	\N	0
2	-1234	rest_api_creator	PRIMARY	adp_crt_user	Service Provider for application rest_api_creator	\N	default	\N	1	0	0	0	0	0	\N	1	0	dba50282-4dd0-4ee2-9e4f-a0c4b5fef93b	\N	\N	0
5	-1234	adp_sub_user_ADPApplicationCS_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_ADPApplicationCS_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	e5092be8-9667-4da1-8402-e8cda2b5b016	\N	\N	0
14	2	rest_api_admin	PRIMARY	admin	Service Provider for application rest_api_admin	\N	default	\N	1	0	0	0	0	0	\N	1	0	ce589f6d-eb8d-46de-a39f-f0e9db7890d2	\N	\N	0
9	1	rest_api_publisher	PRIMARY	adp_pub_user	Service Provider for application rest_api_publisher	\N	default	\N	1	0	0	0	0	0	\N	1	0	e7786414-aa23-41fb-9d1e-09ff55298715	\N	\N	0
11	1	adp_sub_user_ADPApplicationEC_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_ADPApplicationEC_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	55beacd3-cac5-4c5c-9bad-a0bccdfe70f3	\N	\N	0
3	-1234	rest_api_publisher	PRIMARY	adp_pub_user	Service Provider for application rest_api_publisher	\N	default	\N	1	0	0	0	0	0	\N	1	0	b84250ab-7e02-4af8-a0b1-93a4302ae3ee	\N	\N	0
19	2	adp_sub_user_CustomerApp_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_CustomerApp_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	da5f5479-7708-422c-acf3-44516218dc88	\N	\N	0
16	2	rest_api_publisher	PRIMARY	adp_pub_user	Service Provider for application rest_api_publisher	\N	default	\N	1	0	0	0	0	0	\N	1	0	340d2e24-3903-427e-9ea7-fba861950725	\N	\N	0
18	2	adp_sub_user_ADPApplicationSC_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_ADPApplicationSC_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	28c81c10-153b-45ee-929f-a04b1be29fec	\N	\N	0
20	-1234	adp_sub_user_ADPCTSApplicationSC_PRODUCTION	PRIMARY	apim_reserved_user	Service Provider for application adp_sub_user_ADPCTSApplicationSC_PRODUCTION	\N	default	\N	1	0	0	0	0	0	\N	1	0	b723ccdc-cdbc-487f-a8f6-3e0101b23b4f	\N	\N	0
15	2	rest_api_creator	PRIMARY	adp_crt_user	Service Provider for application rest_api_creator	\N	default	\N	1	0	0	0	0	0	\N	1	0	66008b5d-7d3a-47c4-b114-f261272a8f4a	\N	\N	0
\.


--
-- Data for Name: sp_auth_script; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_auth_script (id, tenant_id, app_id, type, content, is_enabled) FROM stdin;
\.


--
-- Data for Name: sp_auth_step; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_auth_step (id, tenant_id, step_order, app_id, is_subject_step, is_attribute_step) FROM stdin;
\.


--
-- Data for Name: sp_claim_dialect; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_claim_dialect (id, tenant_id, sp_dialect, app_id) FROM stdin;
\.


--
-- Data for Name: sp_claim_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_claim_mapping (id, tenant_id, idp_claim, sp_claim, app_id, is_requested, is_mandatory, default_value) FROM stdin;
\.


--
-- Data for Name: sp_federated_idp; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_federated_idp (id, tenant_id, authenticator_id) FROM stdin;
\.


--
-- Data for Name: sp_inbound_auth; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_inbound_auth (id, tenant_id, inbound_auth_key, inbound_auth_type, inbound_config_type, prop_name, prop_value, app_id) FROM stdin;
1	-1234	tfY6J6QOExf18A545h6gfQ2tZ8Ya	oauth2	standardAPP	oauthConsumerSecret	nbFHA6g4zNh7sYgfT_kE9V39RYMa	1
2	-1234	MTBiV2fK8ea8JCyVUL_TQuz_fWka	oauth2	standardAPP	oauthConsumerSecret	1FkDAUSq9x6UzLCnfbw6M4TLIBca	2
3	-1234	DCndMsfW7jWgJ4Gn2Bcq3HJf5Wga	oauth2	standardAPP	oauthConsumerSecret	zWsMYrQT1qFG10FqbONaBEr4GT0a	3
4	-1234	WwTjqb7JzAz7tz3b7D4CZx0PHvIa	oauth2	standardAPP	oauthConsumerSecret	BWNSKok6UObheFCQJbs226iZH0Ya	4
5	-1234	IjE3DFqMvXwVFKXf5XM5fMFwafMa	oauth2	standardAPP	oauthConsumerSecret	n6TjRXX2retn3FLV_xbsqlmlQA4a	5
6	-1234	w1ykQHnJIB8G5PpxI1TUiM7W05oa	oauth2	standardAPP	oauthConsumerSecret	BY6BjcISGKfqak83eoiq2ThpfDoa	6
7	1	5dbDl_wDaMtKZOtyuAtDuTjWWoMa	oauth2	standardAPP	oauthConsumerSecret	FylWIWxQrrx2BhvtfMifjyMziA4a	7
8	1	7QKYoz_shq4efQsLp_ImVoJAVfga	oauth2	standardAPP	oauthConsumerSecret	2_mfvTB8OGDgrqUpXnVvaNxkg2sa	8
9	1	Iea_s33OIUf0zlC25l2gfEHAgbsa	oauth2	standardAPP	oauthConsumerSecret	piHNP2yHTBSINfldBv3fJgmrxEMa	9
10	1	MpESvRoYMBcNTXpVKwJFlKl6DHMa	oauth2	standardAPP	oauthConsumerSecret	BEv1bdm3rQ_fC5CNZJojcQnikGEa	10
11	1	63GebUlqAMneJSCXJTFLcHrlZQka	oauth2	standardAPP	oauthConsumerSecret	x9PDUCKvV2kL4BOAF1GA5omx0Joa	11
12	1	TDqaZWZ6i2Qu3LE3EGjjPE3Z0rAa	oauth2	standardAPP	oauthConsumerSecret	v_LTROT56FN_er0oAzP1xcHblfga	12
13	-1234	7o8JYq5GLPcZofDm4m1xk7jAJ3Ia	oauth2	standardAPP	oauthConsumerSecret	_QfK_bK68k0qR3W51IErj7_pmeoa	13
14	2	AGVTdJnLVqt4aiHeQuPwli88Orca	oauth2	standardAPP	oauthConsumerSecret	HAMUIKUfGSV8A7hnWXYWyVNkUIYa	14
15	2	db7IatPG_pt47li7kZ_Wk7MmzdAa	oauth2	standardAPP	oauthConsumerSecret	fkQyF9Vp23wKnmfgZKCqRbEfAaMa	15
16	2	ugckN838E9JYYyUVdlUI1GleVAwa	oauth2	standardAPP	oauthConsumerSecret	zGOIQoHOpYiiTzwcCsDa5BOIdLwa	16
17	2	FUU0l_k3877XrI08RlAN873RXOIa	oauth2	standardAPP	oauthConsumerSecret	YqpdAecrs6imMsOhiyGfAfvfRlAa	17
18	2	JmEzoL3bVKApBDegL1atV0cCfEoa	oauth2	standardAPP	oauthConsumerSecret	hCTm_Y38DO2Cbir_iVvzyuINoQsa	18
19	2	LrCi_oe6yaP1SeRfzzvccZIDECoa	oauth2	standardAPP	oauthConsumerSecret	RCJ6_gMnnRQ7o3faTJO0SpVxmN0a	19
20	-1234	a6M8FWBd8aBCnoaYttsfjRGhVu0a	oauth2	standardAPP	oauthConsumerSecret	FGOMDC7EWAUynW5wd2iNaqXsirEa	20
\.


--
-- Data for Name: sp_metadata; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_metadata (id, sp_id, name, value, display_name, tenant_id) FROM stdin;
9	1	TokenType	DEFAULT	\N	-1234
10	1	skipLogoutConsent	true	Skip Logout Consent	-1234
11	1	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
12	1	DisplayName	rest_api_admin	\N	-1234
13	1	skipConsent	true	Skip Consent	-1234
106	10	TokenType	DEFAULT	\N	1
107	10	skipLogoutConsent	true	Skip Logout Consent	1
108	10	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	1
109	10	DisplayName	rest_api_subscriber	\N	1
110	10	skipConsent	true	Skip Consent	1
111	11	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	1
112	11	skipLogoutConsent	false	Skip Logout Consent	1
113	11	skipConsent	false	Skip Consent	1
22	2	TokenType	DEFAULT	\N	-1234
23	2	skipLogoutConsent	true	Skip Logout Consent	-1234
24	2	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
25	2	DisplayName	rest_api_creator	\N	-1234
26	2	skipConsent	true	Skip Consent	-1234
114	12	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	1
115	12	skipLogoutConsent	false	Skip Logout Consent	1
116	12	skipConsent	false	Skip Consent	1
117	13	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
118	13	skipLogoutConsent	false	Skip Logout Consent	-1234
119	13	skipConsent	false	Skip Consent	-1234
35	3	TokenType	DEFAULT	\N	-1234
36	3	skipLogoutConsent	true	Skip Logout Consent	-1234
37	3	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
38	3	DisplayName	rest_api_publisher	\N	-1234
39	3	skipConsent	true	Skip Consent	-1234
128	14	TokenType	DEFAULT	\N	2
129	14	skipLogoutConsent	true	Skip Logout Consent	2
48	4	TokenType	DEFAULT	\N	-1234
49	4	skipLogoutConsent	true	Skip Logout Consent	-1234
50	4	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
51	4	DisplayName	rest_api_subscriber	\N	-1234
52	4	skipConsent	true	Skip Consent	-1234
53	5	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
54	5	skipLogoutConsent	false	Skip Logout Consent	-1234
55	5	skipConsent	false	Skip Consent	-1234
56	6	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
57	6	skipLogoutConsent	false	Skip Logout Consent	-1234
58	6	skipConsent	false	Skip Consent	-1234
130	14	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	2
131	14	DisplayName	rest_api_admin	\N	2
132	14	skipConsent	true	Skip Consent	2
67	7	TokenType	DEFAULT	\N	1
68	7	skipLogoutConsent	true	Skip Logout Consent	1
69	7	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	1
70	7	DisplayName	rest_api_admin	\N	1
71	7	skipConsent	true	Skip Consent	1
141	15	TokenType	DEFAULT	\N	2
142	15	skipLogoutConsent	true	Skip Logout Consent	2
143	15	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	2
144	15	DisplayName	rest_api_creator	\N	2
145	15	skipConsent	true	Skip Consent	2
80	8	TokenType	DEFAULT	\N	1
81	8	skipLogoutConsent	true	Skip Logout Consent	1
82	8	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	1
83	8	DisplayName	rest_api_creator	\N	1
84	8	skipConsent	true	Skip Consent	1
93	9	TokenType	DEFAULT	\N	1
94	9	skipLogoutConsent	true	Skip Logout Consent	1
95	9	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	1
96	9	DisplayName	rest_api_publisher	\N	1
97	9	skipConsent	true	Skip Consent	1
154	16	TokenType	DEFAULT	\N	2
155	16	skipLogoutConsent	true	Skip Logout Consent	2
156	16	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	2
157	16	DisplayName	rest_api_publisher	\N	2
158	16	skipConsent	true	Skip Consent	2
167	17	TokenType	DEFAULT	\N	2
168	17	skipLogoutConsent	true	Skip Logout Consent	2
169	17	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	2
170	17	DisplayName	rest_api_subscriber	\N	2
171	17	skipConsent	true	Skip Consent	2
172	18	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	2
173	18	skipLogoutConsent	false	Skip Logout Consent	2
174	18	skipConsent	false	Skip Consent	2
175	19	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	2
176	19	skipLogoutConsent	false	Skip Logout Consent	2
177	19	skipConsent	false	Skip Consent	2
178	20	USE_DOMAIN_IN_ROLES	true	DOMAIN_IN_ROLES	-1234
179	20	skipLogoutConsent	false	Skip Logout Consent	-1234
180	20	skipConsent	false	Skip Consent	-1234
\.


--
-- Data for Name: sp_provisioning_connector; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_provisioning_connector (id, tenant_id, idp_name, connector_name, app_id, is_jit_enabled, blocking, rule_enabled) FROM stdin;
\.


--
-- Data for Name: sp_req_path_authenticator; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_req_path_authenticator (id, tenant_id, authenticator_name, app_id) FROM stdin;
\.


--
-- Data for Name: sp_role_mapping; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_role_mapping (id, tenant_id, idp_role, sp_role, app_id) FROM stdin;
\.


--
-- Data for Name: sp_template; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.sp_template (id, tenant_id, name, description, content) FROM stdin;
\.


--
-- Data for Name: wf_bps_profile; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_bps_profile (profile_name, host_url_manager, host_url_worker, username, password, callback_host, tenant_id) FROM stdin;
\.


--
-- Data for Name: wf_request; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_request (uuid, created_by, tenant_id, operation_type, created_at, updated_at, status, request) FROM stdin;
\.


--
-- Data for Name: wf_request_entity_relationship; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_request_entity_relationship (request_id, entity_name, entity_type, tenant_id) FROM stdin;
\.


--
-- Data for Name: wf_workflow; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_workflow (id, wf_name, description, template_id, impl_id, tenant_id) FROM stdin;
\.


--
-- Data for Name: wf_workflow_association; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_workflow_association (id, assoc_name, event_id, assoc_condition, workflow_id, is_enabled, tenant_id) FROM stdin;
\.


--
-- Data for Name: wf_workflow_config_param; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_workflow_config_param (workflow_id, param_name, param_value, param_qname, param_holder, tenant_id) FROM stdin;
\.


--
-- Data for Name: wf_workflow_request_relation; Type: TABLE DATA; Schema: public; Owner: apim_user
--

COPY public.wf_workflow_request_relation (relationship_id, workflow_id, request_id, updated_at, status, tenant_id) FROM stdin;
\.


--
-- Name: am_alert_emaillist_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_alert_emaillist_seq', 1, false);


--
-- Name: am_alert_types_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_alert_types_seq', 7, true);


--
-- Name: am_api_default_version_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_default_version_pk_seq', 3, true);


--
-- Name: am_api_lc_event_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_lc_event_sequence', 69, true);


--
-- Name: am_api_lc_publish_events_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_lc_publish_events_pk_seq', 1, false);


--
-- Name: am_api_product_mapping_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_product_mapping_sequence', 12, true);


--
-- Name: am_api_ratings_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_ratings_sequence', 1, false);


--
-- Name: am_api_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_sequence', 42, true);


--
-- Name: am_api_system_apps_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_system_apps_sequence', 1, false);


--
-- Name: am_api_throttle_policy_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_throttle_policy_seq', 15, true);


--
-- Name: am_api_url_mapping_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_api_url_mapping_sequence', 255, true);


--
-- Name: am_application_registration_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_application_registration_sequence', 8, true);


--
-- Name: am_application_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_application_sequence', 14, true);


--
-- Name: am_block_conditions_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_block_conditions_seq', 6, true);


--
-- Name: am_condition_group_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_condition_group_seq', 1, false);


--
-- Name: am_external_stores_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_external_stores_sequence', 1, false);


--
-- Name: am_header_field_condition_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_header_field_condition_seq', 1, false);


--
-- Name: am_ip_condition_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_ip_condition_seq', 1, false);


--
-- Name: am_jwt_claim_condition_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_jwt_claim_condition_seq', 1, false);


--
-- Name: am_policy_application_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_policy_application_seq', 15, true);


--
-- Name: am_policy_global_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_policy_global_seq', 1, false);


--
-- Name: am_policy_hard_throttling_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_policy_hard_throttling_seq', 1, false);


--
-- Name: am_policy_subscription_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_policy_subscription_seq', 18, true);


--
-- Name: am_query_parameter_condition_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_query_parameter_condition_seq', 1, false);


--
-- Name: am_scope_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_scope_pk_seq', 15, true);


--
-- Name: am_subscriber_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_subscriber_sequence', 3, true);


--
-- Name: am_subscription_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_subscription_sequence', 26, true);


--
-- Name: am_throttle_tier_permissions_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_throttle_tier_permissions_seq', 3, true);


--
-- Name: am_tier_permissions_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_tier_permissions_sequence', 1, false);


--
-- Name: am_workflows_sequence; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.am_workflows_sequence', 1, false);


--
-- Name: cm_pii_category_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.cm_pii_category_pk_seq', 1, false);


--
-- Name: cm_purpose_category_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.cm_purpose_category_pk_seq', 1, true);


--
-- Name: cm_purpose_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.cm_purpose_pk_seq', 1, true);


--
-- Name: cm_receipt_sp_assoc_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.cm_receipt_sp_assoc_pk_seq', 1, false);


--
-- Name: cm_sp_purpose_assoc_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.cm_sp_purpose_assoc_pk_seq', 1, false);


--
-- Name: idn_associated_id_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_associated_id_seq', 1, false);


--
-- Name: idn_auth_wait_status_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_auth_wait_status_seq', 1, false);


--
-- Name: idn_certificate_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_certificate_pk_seq', 1, false);


--
-- Name: idn_claim_dialect_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_claim_dialect_seq', 33, true);


--
-- Name: idn_claim_mapped_attribute_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_claim_mapped_attribute_seq', 273, true);


--
-- Name: idn_claim_mapping_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_claim_mapping_seq', 549, true);


--
-- Name: idn_claim_property_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_claim_property_seq', 3510, true);


--
-- Name: idn_claim_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_claim_seq', 822, true);


--
-- Name: idn_oauth2_device_flow_scopes_id_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oauth2_device_flow_scopes_id_seq', 1, false);


--
-- Name: idn_oauth2_scope_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oauth2_scope_pk_seq', 540, true);


--
-- Name: idn_oauth_consumer_apps_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oauth_consumer_apps_pk_seq', 20, true);


--
-- Name: idn_oidc_property_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oidc_property_seq', 240, true);


--
-- Name: idn_oidc_req_object_claim_values_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oidc_req_object_claim_values_seq', 1, false);


--
-- Name: idn_oidc_req_object_claims_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oidc_req_object_claims_seq', 1, false);


--
-- Name: idn_oidc_request_object_ref_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oidc_request_object_ref_seq', 1, false);


--
-- Name: idn_oidc_scope_claim_mapping_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_oidc_scope_claim_mapping_pk_seq', 144, true);


--
-- Name: idn_password_history_data_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_password_history_data_pk_seq', 1, false);


--
-- Name: idn_saml2_artifact_store_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_saml2_artifact_store_seq', 1, false);


--
-- Name: idn_saml2_assertion_store_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_saml2_assertion_store_seq', 1, false);


--
-- Name: idn_scim_group_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_scim_group_pk_seq', 1, false);


--
-- Name: idn_sts_store_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_sts_store_pk_seq', 1, false);


--
-- Name: idn_uma_permission_ticket_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_uma_permission_ticket_seq', 1, false);


--
-- Name: idn_uma_pt_resource_scope_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_uma_pt_resource_scope_seq', 1, false);


--
-- Name: idn_uma_pt_resource_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_uma_pt_resource_seq', 1, false);


--
-- Name: idn_uma_resource_meta_data_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_uma_resource_meta_data_seq', 1, false);


--
-- Name: idn_uma_resource_scope_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_uma_resource_scope_seq', 1, false);


--
-- Name: idn_uma_resource_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idn_uma_resource_seq', 1, false);


--
-- Name: idp_authenticator_prop_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_authenticator_prop_seq', 18, true);


--
-- Name: idp_authenticator_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_authenticator_seq', 9, true);


--
-- Name: idp_claim_mapping_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_claim_mapping_seq', 1, false);


--
-- Name: idp_claim_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_claim_seq', 1, false);


--
-- Name: idp_local_claim_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_local_claim_seq', 1, false);


--
-- Name: idp_metadata_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_metadata_seq', 15, true);


--
-- Name: idp_prov_config_prop_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_prov_config_prop_seq', 1, false);


--
-- Name: idp_prov_config_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_prov_config_seq', 1, false);


--
-- Name: idp_prov_entity_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_prov_entity_seq', 1, false);


--
-- Name: idp_role_mapping_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_role_mapping_seq', 1, false);


--
-- Name: idp_role_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_role_seq', 1, false);


--
-- Name: idp_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.idp_seq', 3, true);


--
-- Name: sp_app_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_app_seq', 20, true);


--
-- Name: sp_auth_script_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_auth_script_seq', 1, false);


--
-- Name: sp_auth_step_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_auth_step_seq', 1, false);


--
-- Name: sp_claim_dialect_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_claim_dialect_seq', 1, false);


--
-- Name: sp_claim_mapping_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_claim_mapping_seq', 1, false);


--
-- Name: sp_inbound_auth_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_inbound_auth_seq', 20, true);


--
-- Name: sp_metadata_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_metadata_seq', 180, true);


--
-- Name: sp_prov_connector_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_prov_connector_seq', 1, false);


--
-- Name: sp_req_path_auth_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_req_path_auth_seq', 1, false);


--
-- Name: sp_role_mapping_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_role_mapping_seq', 1, false);


--
-- Name: sp_template_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.sp_template_seq', 1, false);


--
-- Name: wf_workflow_association_pk_seq; Type: SEQUENCE SET; Schema: public; Owner: apim_user
--

SELECT pg_catalog.setval('public.wf_workflow_association_pk_seq', 1, false);


--
-- Name: am_alert_emaillist am_alert_emaillist_const; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_alert_emaillist
    ADD CONSTRAINT am_alert_emaillist_const UNIQUE (email_list_id, user_name, stake_holder);


--
-- Name: am_alert_emaillist_details am_alert_emaillist_details_const; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_alert_emaillist_details
    ADD CONSTRAINT am_alert_emaillist_details_const PRIMARY KEY (email_list_id, email);


--
-- Name: am_alert_emaillist am_alert_emaillist_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_alert_emaillist
    ADD CONSTRAINT am_alert_emaillist_pkey PRIMARY KEY (email_list_id);


--
-- Name: am_alert_types am_alert_types_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_alert_types
    ADD CONSTRAINT am_alert_types_pkey PRIMARY KEY (alert_type_id);


--
-- Name: am_alert_types_values am_alert_types_values_const; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_alert_types_values
    ADD CONSTRAINT am_alert_types_values_const PRIMARY KEY (alert_type_id, user_name, stake_holder);


--
-- Name: am_api am_api_api_provider_api_name_api_version_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api
    ADD CONSTRAINT am_api_api_provider_api_name_api_version_key UNIQUE (api_provider, api_name, api_version);


--
-- Name: am_api_categories am_api_categories_name_tenant_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_categories
    ADD CONSTRAINT am_api_categories_name_tenant_id_key UNIQUE (name, tenant_id);


--
-- Name: am_api_categories am_api_categories_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_categories
    ADD CONSTRAINT am_api_categories_pkey PRIMARY KEY (uuid);


--
-- Name: am_api_client_certificate am_api_client_certificate_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_client_certificate
    ADD CONSTRAINT am_api_client_certificate_pkey PRIMARY KEY (alias, tenant_id, removed);


--
-- Name: am_api_comments am_api_comments_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_comments
    ADD CONSTRAINT am_api_comments_pkey PRIMARY KEY (comment_id);


--
-- Name: am_api_default_version am_api_default_version_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_default_version
    ADD CONSTRAINT am_api_default_version_pkey PRIMARY KEY (default_version_id);


--
-- Name: am_api_lc_event am_api_lc_event_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_lc_event
    ADD CONSTRAINT am_api_lc_event_pkey PRIMARY KEY (event_id);


--
-- Name: am_api_lc_publish_events am_api_lc_publish_events_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_lc_publish_events
    ADD CONSTRAINT am_api_lc_publish_events_pkey PRIMARY KEY (id);


--
-- Name: am_api am_api_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api
    ADD CONSTRAINT am_api_pkey PRIMARY KEY (api_id);


--
-- Name: am_api_product_mapping am_api_product_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_product_mapping
    ADD CONSTRAINT am_api_product_mapping_pkey PRIMARY KEY (api_product_mapping_id);


--
-- Name: am_api_ratings am_api_ratings_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_ratings
    ADD CONSTRAINT am_api_ratings_pkey PRIMARY KEY (rating_id);


--
-- Name: am_api_resource_scope_mapping am_api_resource_scope_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_resource_scope_mapping
    ADD CONSTRAINT am_api_resource_scope_mapping_pkey PRIMARY KEY (scope_name, url_mapping_id);


--
-- Name: am_api_throttle_policy am_api_throttle_policy_name_tenant_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_throttle_policy
    ADD CONSTRAINT am_api_throttle_policy_name_tenant_id_key UNIQUE (name, tenant_id);


--
-- Name: am_api_throttle_policy am_api_throttle_policy_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_throttle_policy
    ADD CONSTRAINT am_api_throttle_policy_pkey PRIMARY KEY (policy_id);


--
-- Name: am_api_throttle_policy am_api_throttle_policy_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_throttle_policy
    ADD CONSTRAINT am_api_throttle_policy_uuid_key UNIQUE (uuid);


--
-- Name: am_api_url_mapping am_api_url_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_url_mapping
    ADD CONSTRAINT am_api_url_mapping_pkey PRIMARY KEY (url_mapping_id);


--
-- Name: am_app_key_domain_mapping am_app_key_domain_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_app_key_domain_mapping
    ADD CONSTRAINT am_app_key_domain_mapping_pkey PRIMARY KEY (consumer_key, authz_domain);


--
-- Name: am_application_attributes am_application_attributes_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_attributes
    ADD CONSTRAINT am_application_attributes_pkey PRIMARY KEY (application_id, name);


--
-- Name: am_application_group_mapping am_application_group_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_group_mapping
    ADD CONSTRAINT am_application_group_mapping_pkey PRIMARY KEY (application_id, group_id, tenant);


--
-- Name: am_application_key_mapping am_application_key_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_key_mapping
    ADD CONSTRAINT am_application_key_mapping_pkey PRIMARY KEY (application_id, key_type, key_manager);


--
-- Name: am_application am_application_name_subscriber_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application
    ADD CONSTRAINT am_application_name_subscriber_id_key UNIQUE (name, subscriber_id);


--
-- Name: am_application am_application_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application
    ADD CONSTRAINT am_application_pkey PRIMARY KEY (application_id);


--
-- Name: am_application_registration am_application_registration_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_registration
    ADD CONSTRAINT am_application_registration_pkey PRIMARY KEY (reg_id);


--
-- Name: am_application_registration am_application_registration_subscriber_id_app_id_token_type_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_registration
    ADD CONSTRAINT am_application_registration_subscriber_id_app_id_token_type_key UNIQUE (subscriber_id, app_id, token_type, key_manager);


--
-- Name: am_application am_application_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application
    ADD CONSTRAINT am_application_uuid_key UNIQUE (uuid);


--
-- Name: am_block_conditions am_block_conditions_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_block_conditions
    ADD CONSTRAINT am_block_conditions_pkey PRIMARY KEY (condition_id);


--
-- Name: am_block_conditions am_block_conditions_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_block_conditions
    ADD CONSTRAINT am_block_conditions_uuid_key UNIQUE (uuid);


--
-- Name: am_condition_group am_condition_group_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_condition_group
    ADD CONSTRAINT am_condition_group_pkey PRIMARY KEY (condition_group_id);


--
-- Name: am_correlation_configs am_correlation_configs_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_correlation_configs
    ADD CONSTRAINT am_correlation_configs_pkey PRIMARY KEY (component_name);


--
-- Name: am_correlation_properties am_correlation_properties_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_correlation_properties
    ADD CONSTRAINT am_correlation_properties_pkey PRIMARY KEY (property_name, component_name);


--
-- Name: am_external_stores am_external_stores_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_external_stores
    ADD CONSTRAINT am_external_stores_pkey PRIMARY KEY (apistore_id);


--
-- Name: am_graphql_complexity am_graphql_complexity_api_id_type_field_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_graphql_complexity
    ADD CONSTRAINT am_graphql_complexity_api_id_type_field_key UNIQUE (api_id, type, field);


--
-- Name: am_graphql_complexity am_graphql_complexity_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_graphql_complexity
    ADD CONSTRAINT am_graphql_complexity_pkey PRIMARY KEY (uuid);


--
-- Name: am_gw_api_artifacts am_gw_api_artifacts_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_gw_api_artifacts
    ADD CONSTRAINT am_gw_api_artifacts_pkey PRIMARY KEY (gateway_label, api_id);


--
-- Name: am_gw_published_api_details am_gw_published_api_details_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_gw_published_api_details
    ADD CONSTRAINT am_gw_published_api_details_pkey PRIMARY KEY (api_id);


--
-- Name: am_header_field_condition am_header_field_condition_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_header_field_condition
    ADD CONSTRAINT am_header_field_condition_pkey PRIMARY KEY (header_field_id);


--
-- Name: am_ip_condition am_ip_condition_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_ip_condition
    ADD CONSTRAINT am_ip_condition_pkey PRIMARY KEY (am_ip_condition_id);


--
-- Name: am_jwt_claim_condition am_jwt_claim_condition_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_jwt_claim_condition
    ADD CONSTRAINT am_jwt_claim_condition_pkey PRIMARY KEY (jwt_claim_id);


--
-- Name: am_key_manager am_key_manager_name_tenant_domain_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_key_manager
    ADD CONSTRAINT am_key_manager_name_tenant_domain_key UNIQUE (name, tenant_domain);


--
-- Name: am_key_manager am_key_manager_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_key_manager
    ADD CONSTRAINT am_key_manager_pkey PRIMARY KEY (uuid);


--
-- Name: am_label_urls am_label_urls_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_label_urls
    ADD CONSTRAINT am_label_urls_pkey PRIMARY KEY (label_id, access_url);


--
-- Name: am_labels am_labels_name_tenant_domain_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_labels
    ADD CONSTRAINT am_labels_name_tenant_domain_key UNIQUE (name, tenant_domain);


--
-- Name: am_labels am_labels_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_labels
    ADD CONSTRAINT am_labels_pkey PRIMARY KEY (label_id);


--
-- Name: am_monetization_usage am_monetization_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_monetization_usage
    ADD CONSTRAINT am_monetization_usage_pkey PRIMARY KEY (id);


--
-- Name: am_notification_subscriber am_notification_subscriber_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_notification_subscriber
    ADD CONSTRAINT am_notification_subscriber_pkey PRIMARY KEY (uuid, subscriber_address);


--
-- Name: am_policy_application am_policy_application_name_tenant_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_application
    ADD CONSTRAINT am_policy_application_name_tenant_id_key UNIQUE (name, tenant_id);


--
-- Name: am_policy_application am_policy_application_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_application
    ADD CONSTRAINT am_policy_application_pkey PRIMARY KEY (policy_id);


--
-- Name: am_policy_application am_policy_application_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_application
    ADD CONSTRAINT am_policy_application_uuid_key UNIQUE (uuid);


--
-- Name: am_policy_global am_policy_global_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_global
    ADD CONSTRAINT am_policy_global_pkey PRIMARY KEY (policy_id);


--
-- Name: am_policy_global am_policy_global_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_global
    ADD CONSTRAINT am_policy_global_uuid_key UNIQUE (uuid);


--
-- Name: am_policy_hard_throttling am_policy_hard_throttling_name_tenant_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_hard_throttling
    ADD CONSTRAINT am_policy_hard_throttling_name_tenant_id_key UNIQUE (name, tenant_id);


--
-- Name: am_policy_hard_throttling am_policy_hard_throttling_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_hard_throttling
    ADD CONSTRAINT am_policy_hard_throttling_pkey PRIMARY KEY (policy_id);


--
-- Name: am_policy_subscription am_policy_subscription_name_tenant_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_subscription
    ADD CONSTRAINT am_policy_subscription_name_tenant_id_key UNIQUE (name, tenant_id);


--
-- Name: am_policy_subscription am_policy_subscription_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_subscription
    ADD CONSTRAINT am_policy_subscription_pkey PRIMARY KEY (policy_id);


--
-- Name: am_policy_subscription am_policy_subscription_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_policy_subscription
    ADD CONSTRAINT am_policy_subscription_uuid_key UNIQUE (uuid);


--
-- Name: am_query_parameter_condition am_query_parameter_condition_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_query_parameter_condition
    ADD CONSTRAINT am_query_parameter_condition_pkey PRIMARY KEY (query_parameter_id);


--
-- Name: am_revoked_jwt am_revoked_jwt_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_revoked_jwt
    ADD CONSTRAINT am_revoked_jwt_pkey PRIMARY KEY (uuid);


--
-- Name: am_scope am_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_scope
    ADD CONSTRAINT am_scope_pkey PRIMARY KEY (scope_id);


--
-- Name: am_security_audit_uuid_mapping am_security_audit_uuid_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_security_audit_uuid_mapping
    ADD CONSTRAINT am_security_audit_uuid_mapping_pkey PRIMARY KEY (api_id);


--
-- Name: am_shared_scope am_shared_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_shared_scope
    ADD CONSTRAINT am_shared_scope_pkey PRIMARY KEY (uuid);


--
-- Name: am_subscriber am_subscriber_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscriber
    ADD CONSTRAINT am_subscriber_pkey PRIMARY KEY (subscriber_id);


--
-- Name: am_subscriber am_subscriber_tenant_id_user_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscriber
    ADD CONSTRAINT am_subscriber_tenant_id_user_id_key UNIQUE (tenant_id, user_id);


--
-- Name: am_subscription_key_mapping am_subscription_key_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscription_key_mapping
    ADD CONSTRAINT am_subscription_key_mapping_pkey PRIMARY KEY (subscription_id, access_token);


--
-- Name: am_subscription am_subscription_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscription
    ADD CONSTRAINT am_subscription_pkey PRIMARY KEY (subscription_id);


--
-- Name: am_subscription am_subscription_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscription
    ADD CONSTRAINT am_subscription_uuid_key UNIQUE (uuid);


--
-- Name: am_system_apps am_system_apps_consumer_key_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_system_apps
    ADD CONSTRAINT am_system_apps_consumer_key_key UNIQUE (consumer_key);


--
-- Name: am_system_apps am_system_apps_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_system_apps
    ADD CONSTRAINT am_system_apps_pkey PRIMARY KEY (id);


--
-- Name: am_tenant_themes am_tenant_themes_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_tenant_themes
    ADD CONSTRAINT am_tenant_themes_pkey PRIMARY KEY (tenant_id);


--
-- Name: am_throttle_tier_permissions am_throttle_tier_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_throttle_tier_permissions
    ADD CONSTRAINT am_throttle_tier_permissions_pkey PRIMARY KEY (throttle_tier_permissions_id);


--
-- Name: am_tier_permissions am_tier_permissions_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_tier_permissions
    ADD CONSTRAINT am_tier_permissions_pkey PRIMARY KEY (tier_permissions_id);


--
-- Name: am_usage_uploaded_files am_usage_uploaded_files_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_usage_uploaded_files
    ADD CONSTRAINT am_usage_uploaded_files_pkey PRIMARY KEY (tenant_domain, file_name, file_timestamp);


--
-- Name: am_user am_user_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_user
    ADD CONSTRAINT am_user_pkey PRIMARY KEY (user_id);


--
-- Name: am_workflows am_workflows_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_workflows
    ADD CONSTRAINT am_workflows_pkey PRIMARY KEY (wf_id);


--
-- Name: am_workflows am_workflows_wf_external_reference_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_workflows
    ADD CONSTRAINT am_workflows_wf_external_reference_key UNIQUE (wf_external_reference);


--
-- Name: sp_app application_name_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_app
    ADD CONSTRAINT application_name_constraint UNIQUE (app_name, tenant_id);


--
-- Name: sp_app application_uuid_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_app
    ADD CONSTRAINT application_uuid_constraint UNIQUE (uuid);


--
-- Name: idn_certificate certificate_unique_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_certificate
    ADD CONSTRAINT certificate_unique_key UNIQUE (name, tenant_id);


--
-- Name: idn_claim claim_uri_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim
    ADD CONSTRAINT claim_uri_constraint UNIQUE (dialect_id, claim_uri, tenant_id);


--
-- Name: cm_consent_receipt_property cm_consent_receipt_property_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_consent_receipt_property
    ADD CONSTRAINT cm_consent_receipt_property_cnt UNIQUE (consent_receipt_id, name);


--
-- Name: cm_pii_category cm_pii_category_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_pii_category
    ADD CONSTRAINT cm_pii_category_cnt UNIQUE (name, tenant_id);


--
-- Name: cm_pii_category cm_pii_category_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_pii_category
    ADD CONSTRAINT cm_pii_category_pkey PRIMARY KEY (id);


--
-- Name: cm_purpose_category cm_purpose_category_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_purpose_category
    ADD CONSTRAINT cm_purpose_category_cnt UNIQUE (name, tenant_id);


--
-- Name: cm_purpose_category cm_purpose_category_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_purpose_category
    ADD CONSTRAINT cm_purpose_category_pkey PRIMARY KEY (id);


--
-- Name: cm_purpose cm_purpose_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_purpose
    ADD CONSTRAINT cm_purpose_cnt UNIQUE (name, tenant_id, purpose_group, group_type);


--
-- Name: cm_purpose_pii_cat_assoc cm_purpose_pii_cat_assoc_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_purpose_pii_cat_assoc
    ADD CONSTRAINT cm_purpose_pii_cat_assoc_cnt UNIQUE (purpose_id, cm_pii_category_id);


--
-- Name: cm_purpose cm_purpose_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_purpose
    ADD CONSTRAINT cm_purpose_pkey PRIMARY KEY (id);


--
-- Name: cm_receipt cm_receipt_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_receipt
    ADD CONSTRAINT cm_receipt_pkey PRIMARY KEY (consent_receipt_id);


--
-- Name: cm_receipt_sp_assoc cm_receipt_sp_assoc_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_receipt_sp_assoc
    ADD CONSTRAINT cm_receipt_sp_assoc_cnt UNIQUE (consent_receipt_id, sp_name, sp_tenant_id);


--
-- Name: cm_receipt_sp_assoc cm_receipt_sp_assoc_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_receipt_sp_assoc
    ADD CONSTRAINT cm_receipt_sp_assoc_pkey PRIMARY KEY (id);


--
-- Name: cm_sp_purpose_assoc cm_sp_purpose_assoc_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_assoc
    ADD CONSTRAINT cm_sp_purpose_assoc_cnt UNIQUE (receipt_sp_assoc, purpose_id);


--
-- Name: cm_sp_purpose_assoc cm_sp_purpose_assoc_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_assoc
    ADD CONSTRAINT cm_sp_purpose_assoc_pkey PRIMARY KEY (id);


--
-- Name: cm_sp_purpose_pii_cat_assoc cm_sp_purpose_pii_cat_assoc_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_pii_cat_assoc
    ADD CONSTRAINT cm_sp_purpose_pii_cat_assoc_cnt UNIQUE (sp_purpose_assoc_id, pii_category_id);


--
-- Name: cm_sp_purpose_purpose_cat_assc cm_sp_purpose_purpose_cat_assc_cnt; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_purpose_cat_assc
    ADD CONSTRAINT cm_sp_purpose_purpose_cat_assc_cnt UNIQUE (sp_purpose_assoc_id, purpose_category_id);


--
-- Name: idn_oauth2_access_token con_app_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_access_token
    ADD CONSTRAINT con_app_key UNIQUE (consumer_key_id, authz_user, tenant_id, user_domain, user_type, token_scope_hash, token_state, token_state_id, idp_id, token_binding_ref);


--
-- Name: idn_oauth_consumer_apps consumer_key_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth_consumer_apps
    ADD CONSTRAINT consumer_key_constraint UNIQUE (consumer_key);


--
-- Name: idn_claim_dialect dialect_uri_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_dialect
    ADD CONSTRAINT dialect_uri_constraint UNIQUE (dialect_uri, tenant_id);


--
-- Name: idn_claim_mapping ext_to_loc_mapping_constrn; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapping
    ADD CONSTRAINT ext_to_loc_mapping_constrn UNIQUE (ext_claim_id, tenant_id);


--
-- Name: fido2_device_store fido2_device_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.fido2_device_store
    ADD CONSTRAINT fido2_device_store_pkey PRIMARY KEY (credential_id, user_handle);


--
-- Name: fido_device_store fido_device_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.fido_device_store
    ADD CONSTRAINT fido_device_store_pkey PRIMARY KEY (tenant_id, domain_name, user_name, key_handle);


--
-- Name: idn_associated_id idn_associated_id_idp_user_id_tenant_id_idp_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_associated_id
    ADD CONSTRAINT idn_associated_id_idp_user_id_tenant_id_idp_id_key UNIQUE (idp_user_id, tenant_id, idp_id);


--
-- Name: idn_associated_id idn_associated_id_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_associated_id
    ADD CONSTRAINT idn_associated_id_pkey PRIMARY KEY (id);


--
-- Name: idn_auth_session_app_info idn_auth_session_app_info_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_session_app_info
    ADD CONSTRAINT idn_auth_session_app_info_pkey PRIMARY KEY (session_id, subject, app_id, inbound_auth_type);


--
-- Name: idn_auth_session_meta_data idn_auth_session_meta_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_session_meta_data
    ADD CONSTRAINT idn_auth_session_meta_data_pkey PRIMARY KEY (session_id, property_type, value);


--
-- Name: idn_auth_session_store idn_auth_session_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_session_store
    ADD CONSTRAINT idn_auth_session_store_pkey PRIMARY KEY (session_id, session_type, time_created, operation);


--
-- Name: idn_auth_temp_session_store idn_auth_temp_session_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_temp_session_store
    ADD CONSTRAINT idn_auth_temp_session_store_pkey PRIMARY KEY (session_id, session_type, time_created, operation);


--
-- Name: idn_auth_user idn_auth_user_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_user
    ADD CONSTRAINT idn_auth_user_pkey PRIMARY KEY (user_id);


--
-- Name: idn_auth_wait_status idn_auth_wait_status_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_wait_status
    ADD CONSTRAINT idn_auth_wait_status_key UNIQUE (long_wait_key);


--
-- Name: idn_auth_wait_status idn_auth_wait_status_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_wait_status
    ADD CONSTRAINT idn_auth_wait_status_pkey PRIMARY KEY (id);


--
-- Name: idn_base_table idn_base_table_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_base_table
    ADD CONSTRAINT idn_base_table_pkey PRIMARY KEY (product_name);


--
-- Name: idn_certificate idn_certificate_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_certificate
    ADD CONSTRAINT idn_certificate_pkey PRIMARY KEY (id);


--
-- Name: idn_claim_dialect idn_claim_dialect_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_dialect
    ADD CONSTRAINT idn_claim_dialect_pkey PRIMARY KEY (id);


--
-- Name: idn_claim_mapped_attribute idn_claim_mapped_attribute_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapped_attribute
    ADD CONSTRAINT idn_claim_mapped_attribute_pkey PRIMARY KEY (id);


--
-- Name: idn_claim_mapping idn_claim_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapping
    ADD CONSTRAINT idn_claim_mapping_pkey PRIMARY KEY (id);


--
-- Name: idn_claim idn_claim_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim
    ADD CONSTRAINT idn_claim_pkey PRIMARY KEY (id);


--
-- Name: idn_claim_property idn_claim_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_property
    ADD CONSTRAINT idn_claim_property_pkey PRIMARY KEY (id);


--
-- Name: idn_fed_auth_session_mapping idn_fed_auth_session_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_fed_auth_session_mapping
    ADD CONSTRAINT idn_fed_auth_session_mapping_pkey PRIMARY KEY (idp_session_id);


--
-- Name: idn_function_library idn_function_library_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_function_library
    ADD CONSTRAINT idn_function_library_pkey PRIMARY KEY (tenant_id, name);


--
-- Name: idn_identity_meta_data idn_identity_meta_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_identity_meta_data
    ADD CONSTRAINT idn_identity_meta_data_pkey PRIMARY KEY (tenant_id, user_name, metadata_type, metadata);


--
-- Name: idn_identity_user_data idn_identity_user_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_identity_user_data
    ADD CONSTRAINT idn_identity_user_data_pkey PRIMARY KEY (tenant_id, user_name, data_key);


--
-- Name: idn_oauth1a_access_token idn_oauth1a_access_token_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth1a_access_token
    ADD CONSTRAINT idn_oauth1a_access_token_pkey PRIMARY KEY (access_token);


--
-- Name: idn_oauth1a_request_token idn_oauth1a_request_token_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth1a_request_token
    ADD CONSTRAINT idn_oauth1a_request_token_pkey PRIMARY KEY (request_token);


--
-- Name: idn_oauth2_access_token idn_oauth2_access_token_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_access_token
    ADD CONSTRAINT idn_oauth2_access_token_pkey PRIMARY KEY (token_id);


--
-- Name: idn_oauth2_access_token_scope idn_oauth2_access_token_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_access_token_scope
    ADD CONSTRAINT idn_oauth2_access_token_scope_pkey PRIMARY KEY (token_id, token_scope);


--
-- Name: idn_oauth2_authorization_code idn_oauth2_authorization_code_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_authorization_code
    ADD CONSTRAINT idn_oauth2_authorization_code_pkey PRIMARY KEY (code_id);


--
-- Name: idn_oauth2_authz_code_scope idn_oauth2_authz_code_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_authz_code_scope
    ADD CONSTRAINT idn_oauth2_authz_code_scope_pkey PRIMARY KEY (code_id, scope);


--
-- Name: idn_oauth2_ciba_auth_code idn_oauth2_ciba_auth_code_auth_req_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_ciba_auth_code
    ADD CONSTRAINT idn_oauth2_ciba_auth_code_auth_req_id_key UNIQUE (auth_req_id);


--
-- Name: idn_oauth2_ciba_auth_code idn_oauth2_ciba_auth_code_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_ciba_auth_code
    ADD CONSTRAINT idn_oauth2_ciba_auth_code_pkey PRIMARY KEY (auth_code_key);


--
-- Name: idn_oauth2_device_flow idn_oauth2_device_flow_code_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow
    ADD CONSTRAINT idn_oauth2_device_flow_code_id_key UNIQUE (code_id);


--
-- Name: idn_oauth2_device_flow idn_oauth2_device_flow_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow
    ADD CONSTRAINT idn_oauth2_device_flow_pkey PRIMARY KEY (device_code);


--
-- Name: idn_oauth2_device_flow_scopes idn_oauth2_device_flow_scopes_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow_scopes
    ADD CONSTRAINT idn_oauth2_device_flow_scopes_pkey PRIMARY KEY (id);


--
-- Name: idn_oauth2_device_flow idn_oauth2_device_flow_user_code_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow
    ADD CONSTRAINT idn_oauth2_device_flow_user_code_key UNIQUE (user_code);


--
-- Name: idn_oauth2_scope idn_oauth2_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_scope
    ADD CONSTRAINT idn_oauth2_scope_pkey PRIMARY KEY (scope_id);


--
-- Name: idn_oauth2_scope_validators idn_oauth2_scope_validators_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_scope_validators
    ADD CONSTRAINT idn_oauth2_scope_validators_pkey PRIMARY KEY (app_id, scope_validator);


--
-- Name: idn_oauth2_token_binding idn_oauth2_token_binding_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_token_binding
    ADD CONSTRAINT idn_oauth2_token_binding_pkey PRIMARY KEY (token_id);


--
-- Name: idn_oauth_consumer_apps idn_oauth_consumer_apps_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth_consumer_apps
    ADD CONSTRAINT idn_oauth_consumer_apps_pkey PRIMARY KEY (id);


--
-- Name: idn_oidc_jti idn_oidc_jti_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_jti
    ADD CONSTRAINT idn_oidc_jti_pkey PRIMARY KEY (jwt_id);


--
-- Name: idn_oidc_property idn_oidc_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_property
    ADD CONSTRAINT idn_oidc_property_pkey PRIMARY KEY (id);


--
-- Name: idn_oidc_req_obj_claim_values idn_oidc_req_obj_claim_values_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_obj_claim_values
    ADD CONSTRAINT idn_oidc_req_obj_claim_values_pkey PRIMARY KEY (id);


--
-- Name: idn_oidc_req_object_claims idn_oidc_req_object_claims_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_object_claims
    ADD CONSTRAINT idn_oidc_req_object_claims_pkey PRIMARY KEY (id);


--
-- Name: idn_oidc_req_object_reference idn_oidc_req_object_reference_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_object_reference
    ADD CONSTRAINT idn_oidc_req_object_reference_pkey PRIMARY KEY (id);


--
-- Name: idn_oidc_scope_claim_mapping idn_oidc_scope_claim_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_scope_claim_mapping
    ADD CONSTRAINT idn_oidc_scope_claim_mapping_pkey PRIMARY KEY (id);


--
-- Name: idn_oidc_scope_claim_mapping idn_oidc_scope_claim_mapping_scope_id_external_claim_id_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_scope_claim_mapping
    ADD CONSTRAINT idn_oidc_scope_claim_mapping_scope_id_external_claim_id_key UNIQUE (scope_id, external_claim_id);


--
-- Name: idn_openid_associations idn_openid_associations_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_openid_associations
    ADD CONSTRAINT idn_openid_associations_pkey PRIMARY KEY (handle);


--
-- Name: idn_openid_remember_me idn_openid_remember_me_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_openid_remember_me
    ADD CONSTRAINT idn_openid_remember_me_pkey PRIMARY KEY (user_name, tenant_id);


--
-- Name: idn_openid_user_rps idn_openid_user_rps_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_openid_user_rps
    ADD CONSTRAINT idn_openid_user_rps_pkey PRIMARY KEY (user_name, tenant_id, rp_url);


--
-- Name: idn_password_history_data idn_password_history_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_password_history_data
    ADD CONSTRAINT idn_password_history_data_pkey PRIMARY KEY (id);


--
-- Name: idn_password_history_data idn_password_history_data_user_name_user_domain_tenant_id_s_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_password_history_data
    ADD CONSTRAINT idn_password_history_data_user_name_user_domain_tenant_id_s_key UNIQUE (user_name, user_domain, tenant_id, salt_value, hash);


--
-- Name: idn_recovery_data idn_recovery_data_code_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_recovery_data
    ADD CONSTRAINT idn_recovery_data_code_key UNIQUE (code);


--
-- Name: idn_recovery_data idn_recovery_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_recovery_data
    ADD CONSTRAINT idn_recovery_data_pkey PRIMARY KEY (user_name, user_domain, tenant_id, scenario, step);


--
-- Name: idn_saml2_artifact_store idn_saml2_artifact_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_saml2_artifact_store
    ADD CONSTRAINT idn_saml2_artifact_store_pkey PRIMARY KEY (id);


--
-- Name: idn_saml2_assertion_store idn_saml2_assertion_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_saml2_assertion_store
    ADD CONSTRAINT idn_saml2_assertion_store_pkey PRIMARY KEY (id);


--
-- Name: idn_scim_group idn_scim_group_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_scim_group
    ADD CONSTRAINT idn_scim_group_pkey PRIMARY KEY (id);


--
-- Name: idn_sts_store idn_sts_store_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_sts_store
    ADD CONSTRAINT idn_sts_store_pkey PRIMARY KEY (id);


--
-- Name: idn_thrift_session idn_thrift_session_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_thrift_session
    ADD CONSTRAINT idn_thrift_session_pkey PRIMARY KEY (session_id);


--
-- Name: idn_uma_permission_ticket idn_uma_permission_ticket_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_permission_ticket
    ADD CONSTRAINT idn_uma_permission_ticket_pkey PRIMARY KEY (id);


--
-- Name: idn_uma_pt_resource idn_uma_pt_resource_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_pt_resource
    ADD CONSTRAINT idn_uma_pt_resource_pkey PRIMARY KEY (id);


--
-- Name: idn_uma_pt_resource_scope idn_uma_pt_resource_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_pt_resource_scope
    ADD CONSTRAINT idn_uma_pt_resource_scope_pkey PRIMARY KEY (id);


--
-- Name: idn_uma_resource_meta_data idn_uma_resource_meta_data_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_resource_meta_data
    ADD CONSTRAINT idn_uma_resource_meta_data_pkey PRIMARY KEY (id);


--
-- Name: idn_uma_resource idn_uma_resource_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_resource
    ADD CONSTRAINT idn_uma_resource_pkey PRIMARY KEY (id);


--
-- Name: idn_uma_resource_scope idn_uma_resource_scope_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_resource_scope
    ADD CONSTRAINT idn_uma_resource_scope_pkey PRIMARY KEY (id);


--
-- Name: idn_user_account_association idn_user_account_association_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_user_account_association
    ADD CONSTRAINT idn_user_account_association_pkey PRIMARY KEY (tenant_id, domain_name, user_name);


--
-- Name: idp_authenticator idp_authenticator_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_authenticator
    ADD CONSTRAINT idp_authenticator_pkey PRIMARY KEY (id);


--
-- Name: idp_authenticator_property idp_authenticator_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_authenticator_property
    ADD CONSTRAINT idp_authenticator_property_pkey PRIMARY KEY (id);


--
-- Name: idp_authenticator_property idp_authenticator_property_tenant_id_authenticator_id_prope_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_authenticator_property
    ADD CONSTRAINT idp_authenticator_property_tenant_id_authenticator_id_prope_key UNIQUE (tenant_id, authenticator_id, property_key);


--
-- Name: idp_authenticator idp_authenticator_tenant_id_idp_id_name_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_authenticator
    ADD CONSTRAINT idp_authenticator_tenant_id_idp_id_name_key UNIQUE (tenant_id, idp_id, name);


--
-- Name: idp_claim idp_claim_idp_id_claim_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_claim
    ADD CONSTRAINT idp_claim_idp_id_claim_key UNIQUE (idp_id, claim);


--
-- Name: idp_claim_mapping idp_claim_mapping_idp_claim_id_tenant_id_local_claim_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_claim_mapping
    ADD CONSTRAINT idp_claim_mapping_idp_claim_id_tenant_id_local_claim_key UNIQUE (idp_claim_id, tenant_id, local_claim);


--
-- Name: idp_claim_mapping idp_claim_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_claim_mapping
    ADD CONSTRAINT idp_claim_mapping_pkey PRIMARY KEY (id);


--
-- Name: idp_claim idp_claim_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_claim
    ADD CONSTRAINT idp_claim_pkey PRIMARY KEY (id);


--
-- Name: idp_local_claim idp_local_claim_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_local_claim
    ADD CONSTRAINT idp_local_claim_pkey PRIMARY KEY (id);


--
-- Name: idp_local_claim idp_local_claim_tenant_id_idp_id_claim_uri_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_local_claim
    ADD CONSTRAINT idp_local_claim_tenant_id_idp_id_claim_uri_key UNIQUE (tenant_id, idp_id, claim_uri);


--
-- Name: idp_metadata idp_metadata_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_metadata
    ADD CONSTRAINT idp_metadata_constraint UNIQUE (idp_id, name);


--
-- Name: idp_metadata idp_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_metadata
    ADD CONSTRAINT idp_metadata_pkey PRIMARY KEY (id);


--
-- Name: idp idp_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp
    ADD CONSTRAINT idp_pkey PRIMARY KEY (id);


--
-- Name: idp_prov_config_property idp_prov_config_property_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_prov_config_property
    ADD CONSTRAINT idp_prov_config_property_pkey PRIMARY KEY (id);


--
-- Name: idp_prov_config_property idp_prov_config_property_tenant_id_provisioning_config_id_p_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_prov_config_property
    ADD CONSTRAINT idp_prov_config_property_tenant_id_provisioning_config_id_p_key UNIQUE (tenant_id, provisioning_config_id, property_key);


--
-- Name: idp_provisioning_config idp_provisioning_config_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_config
    ADD CONSTRAINT idp_provisioning_config_pkey PRIMARY KEY (id);


--
-- Name: idp_provisioning_config idp_provisioning_config_tenant_id_idp_id_provisioning_conne_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_config
    ADD CONSTRAINT idp_provisioning_config_tenant_id_idp_id_provisioning_conne_key UNIQUE (tenant_id, idp_id, provisioning_connector_type);


--
-- Name: idp_provisioning_entity idp_provisioning_entity_entity_type_tenant_id_entity_local__key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_entity
    ADD CONSTRAINT idp_provisioning_entity_entity_type_tenant_id_entity_local__key UNIQUE (entity_type, tenant_id, entity_local_userstore, entity_name, provisioning_config_id);


--
-- Name: idp_provisioning_entity idp_provisioning_entity_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_entity
    ADD CONSTRAINT idp_provisioning_entity_pkey PRIMARY KEY (id);


--
-- Name: idp_provisioning_entity idp_provisioning_entity_provisioning_config_id_entity_type__key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_entity
    ADD CONSTRAINT idp_provisioning_entity_provisioning_config_id_entity_type__key UNIQUE (provisioning_config_id, entity_type, entity_value);


--
-- Name: idp_role idp_role_idp_id_role_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_role
    ADD CONSTRAINT idp_role_idp_id_role_key UNIQUE (idp_id, role);


--
-- Name: idp_role_mapping idp_role_mapping_idp_role_id_tenant_id_user_store_id_local__key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_role_mapping
    ADD CONSTRAINT idp_role_mapping_idp_role_id_tenant_id_user_store_id_local__key UNIQUE (idp_role_id, tenant_id, user_store_id, local_role);


--
-- Name: idp_role_mapping idp_role_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_role_mapping
    ADD CONSTRAINT idp_role_mapping_pkey PRIMARY KEY (id);


--
-- Name: idp_role idp_role_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_role
    ADD CONSTRAINT idp_role_pkey PRIMARY KEY (id);


--
-- Name: idp idp_tenant_id_name_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp
    ADD CONSTRAINT idp_tenant_id_name_key UNIQUE (tenant_id, name);


--
-- Name: idp idp_uuid_key; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp
    ADD CONSTRAINT idp_uuid_key UNIQUE (uuid);


--
-- Name: am_certificate_metadata pk_alias; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_certificate_metadata
    ADD CONSTRAINT pk_alias PRIMARY KEY (alias);


--
-- Name: idn_claim_property property_name_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_property
    ADD CONSTRAINT property_name_constraint UNIQUE (local_claim_id, property_name, tenant_id);


--
-- Name: sp_app sp_app_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_app
    ADD CONSTRAINT sp_app_pkey PRIMARY KEY (id);


--
-- Name: sp_auth_script sp_auth_script_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_auth_script
    ADD CONSTRAINT sp_auth_script_pkey PRIMARY KEY (id);


--
-- Name: sp_auth_step sp_auth_step_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_auth_step
    ADD CONSTRAINT sp_auth_step_pkey PRIMARY KEY (id);


--
-- Name: sp_claim_dialect sp_claim_dialect_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_claim_dialect
    ADD CONSTRAINT sp_claim_dialect_pkey PRIMARY KEY (id);


--
-- Name: sp_claim_mapping sp_claim_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_claim_mapping
    ADD CONSTRAINT sp_claim_mapping_pkey PRIMARY KEY (id);


--
-- Name: sp_federated_idp sp_federated_idp_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_federated_idp
    ADD CONSTRAINT sp_federated_idp_pkey PRIMARY KEY (id, authenticator_id);


--
-- Name: sp_inbound_auth sp_inbound_auth_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_inbound_auth
    ADD CONSTRAINT sp_inbound_auth_pkey PRIMARY KEY (id);


--
-- Name: sp_metadata sp_metadata_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_metadata
    ADD CONSTRAINT sp_metadata_constraint UNIQUE (sp_id, name);


--
-- Name: sp_metadata sp_metadata_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_metadata
    ADD CONSTRAINT sp_metadata_pkey PRIMARY KEY (id);


--
-- Name: sp_provisioning_connector sp_provisioning_connector_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_provisioning_connector
    ADD CONSTRAINT sp_provisioning_connector_pkey PRIMARY KEY (id);


--
-- Name: sp_req_path_authenticator sp_req_path_authenticator_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_req_path_authenticator
    ADD CONSTRAINT sp_req_path_authenticator_pkey PRIMARY KEY (id);


--
-- Name: sp_role_mapping sp_role_mapping_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_role_mapping
    ADD CONSTRAINT sp_role_mapping_pkey PRIMARY KEY (id);


--
-- Name: sp_template sp_template_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_template
    ADD CONSTRAINT sp_template_constraint UNIQUE (tenant_id, name);


--
-- Name: sp_template sp_template_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_template
    ADD CONSTRAINT sp_template_pkey PRIMARY KEY (id);


--
-- Name: idn_auth_user_session_mapping user_session_store_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_user_session_mapping
    ADD CONSTRAINT user_session_store_constraint UNIQUE (user_id, session_id);


--
-- Name: idn_auth_user user_store_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_auth_user
    ADD CONSTRAINT user_store_constraint UNIQUE (user_name, tenant_id, domain_name, idp_id);


--
-- Name: idn_claim_mapped_attribute user_store_domain_constraint; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapped_attribute
    ADD CONSTRAINT user_store_domain_constraint UNIQUE (local_claim_id, user_store_domain_name, tenant_id);


--
-- Name: wf_bps_profile wf_bps_profile_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_bps_profile
    ADD CONSTRAINT wf_bps_profile_pkey PRIMARY KEY (profile_name, tenant_id);


--
-- Name: wf_request_entity_relationship wf_request_entity_relationship_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_request_entity_relationship
    ADD CONSTRAINT wf_request_entity_relationship_pkey PRIMARY KEY (request_id, entity_name, entity_type, tenant_id);


--
-- Name: wf_request wf_request_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_request
    ADD CONSTRAINT wf_request_pkey PRIMARY KEY (uuid);


--
-- Name: wf_workflow_association wf_workflow_association_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_association
    ADD CONSTRAINT wf_workflow_association_pkey PRIMARY KEY (id);


--
-- Name: wf_workflow_config_param wf_workflow_config_param_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_config_param
    ADD CONSTRAINT wf_workflow_config_param_pkey PRIMARY KEY (workflow_id, param_name, param_qname, param_holder);


--
-- Name: wf_workflow wf_workflow_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow
    ADD CONSTRAINT wf_workflow_pkey PRIMARY KEY (id);


--
-- Name: wf_workflow_request_relation wf_workflow_request_relation_pkey; Type: CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_request_relation
    ADD CONSTRAINT wf_workflow_request_relation_pkey PRIMARY KEY (relationship_id);


--
-- Name: idx_aa_at_cb; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aa_at_cb ON public.am_application USING btree (application_tier, created_by);


--
-- Name: idx_aai_ctx; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aai_ctx ON public.am_api USING btree (context);


--
-- Name: idx_aakm_ck; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aakm_ck ON public.am_application_key_mapping USING btree (consumer_key);


--
-- Name: idx_aaprod_ai; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aaprod_ai ON public.am_api_product_mapping USING btree (api_id);


--
-- Name: idx_aatp_dqt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aatp_dqt ON public.am_api_throttle_policy USING btree (default_quota_type);


--
-- Name: idx_aaum_ai; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aaum_ai ON public.am_api_url_mapping USING btree (api_id);


--
-- Name: idx_aaum_tt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aaum_tt ON public.am_api_url_mapping USING btree (throttling_tier);


--
-- Name: idx_ac_ac_ckid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ac_ac_ckid ON public.idn_oauth2_authorization_code USING btree (authorization_code, consumer_key_id);


--
-- Name: idx_ac_ckid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ac_ckid ON public.idn_oauth2_authorization_code USING btree (consumer_key_id);


--
-- Name: idx_ac_tid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ac_tid ON public.idn_oauth2_authorization_code USING btree (token_id);


--
-- Name: idx_acg_qt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_acg_qt ON public.am_condition_group USING btree (quota_type);


--
-- Name: idx_ai_dn_un_ai; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ai_dn_un_ai ON public.idn_associated_id USING btree (domain_name, user_name, association_id);


--
-- Name: idx_apa_qt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_apa_qt ON public.am_policy_application USING btree (quota_type);


--
-- Name: idx_aps_qt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_aps_qt ON public.am_policy_subscription USING btree (quota_type);


--
-- Name: idx_as_aitiai; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_as_aitiai ON public.am_subscription USING btree (api_id, tier_id, application_id);


--
-- Name: idx_at_at; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_at ON public.idn_oauth2_access_token USING btree (access_token);


--
-- Name: idx_at_au_ckid_ts_ut; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_au_ckid_ts_ut ON public.idn_oauth2_access_token USING btree (authz_user, consumer_key_id, token_state, user_type);


--
-- Name: idx_at_au_tid_ud_ts_ckid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_au_tid_ud_ts_ckid ON public.idn_oauth2_access_token USING btree (authz_user, tenant_id, user_domain, token_state, consumer_key_id);


--
-- Name: idx_at_ck_au; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_ck_au ON public.idn_oauth2_access_token USING btree (consumer_key_id, authz_user, token_state, user_type);


--
-- Name: idx_at_ckid_au_tid_ud_tsh_ts; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_ckid_au_tid_ud_tsh_ts ON public.idn_oauth2_access_token USING btree (consumer_key_id, authz_user, tenant_id, user_domain, token_scope_hash, token_state);


--
-- Name: idx_at_rt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_rt ON public.idn_oauth2_access_token USING btree (refresh_token);


--
-- Name: idx_at_rth; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_rth ON public.idn_oauth2_access_token USING btree (refresh_token_hash);


--
-- Name: idx_at_si_eci; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_si_eci ON public.idn_oidc_scope_claim_mapping USING btree (scope_id, external_claim_id);


--
-- Name: idx_at_ti_ud; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_at_ti_ud ON public.idn_oauth2_access_token USING btree (authz_user, tenant_id, token_state, user_domain);


--
-- Name: idx_ath; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ath ON public.idn_oauth2_access_token USING btree (access_token_hash);


--
-- Name: idx_ats_tid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ats_tid ON public.idn_oauth2_access_token_scope USING btree (token_id);


--
-- Name: idx_auth_user_dn_tod; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_auth_user_dn_tod ON public.idn_auth_user USING btree (domain_name, tenant_id);


--
-- Name: idx_auth_user_un_tid_dn; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_auth_user_un_tid_dn ON public.idn_auth_user USING btree (user_name, tenant_id, domain_name);


--
-- Name: idx_authorization_code_au_ti; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_authorization_code_au_ti ON public.idn_oauth2_authorization_code USING btree (authz_user, tenant_id, user_domain, state);


--
-- Name: idx_authorization_code_hash; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_authorization_code_hash ON public.idn_oauth2_authorization_code USING btree (authorization_code_hash, consumer_key_id);


--
-- Name: idx_federated_auth_session_id; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_federated_auth_session_id ON public.idn_fed_auth_session_mapping USING btree (session_id);


--
-- Name: idx_fido2_str; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_fido2_str ON public.fido2_device_store USING btree (user_name, tenant_id, domain_name, credential_id, user_handle);


--
-- Name: idx_idn_auth_bind; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_idn_auth_bind ON public.idn_oauth2_token_binding USING btree (token_binding_ref);


--
-- Name: idx_idn_auth_session_time; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_idn_auth_session_time ON public.idn_auth_session_store USING btree (time_created);


--
-- Name: idx_idn_auth_tmp_session_time; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_idn_auth_tmp_session_time ON public.idn_auth_temp_session_store USING btree (time_created);


--
-- Name: idx_idn_scim_group_ti_rn; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_idn_scim_group_ti_rn ON public.idn_scim_group USING btree (tenant_id, role_name);


--
-- Name: idx_idn_scim_group_ti_rn_an; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_idn_scim_group_ti_rn_an ON public.idn_scim_group USING btree (tenant_id, role_name, attr_name);


--
-- Name: idx_ioat_ut; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_ioat_ut ON public.idn_oauth2_access_token USING btree (user_type);


--
-- Name: idx_iop_tid_ck; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_iop_tid_ck ON public.idn_oidc_property USING btree (tenant_id, consumer_key);


--
-- Name: idx_its_lmt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_its_lmt ON public.idn_thrift_session USING btree (last_modified_time);


--
-- Name: idx_oca_um_tid_ud_apn; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_oca_um_tid_ud_apn ON public.idn_oauth_consumer_apps USING btree (username, tenant_id, user_domain, app_name);


--
-- Name: idx_oror_tid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_oror_tid ON public.idn_oidc_req_object_reference USING btree (token_id);


--
-- Name: idx_pt; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_pt ON public.idn_uma_permission_ticket USING btree (pt);


--
-- Name: idx_rid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_rid ON public.idn_uma_resource USING btree (resource_id);


--
-- Name: idx_rs; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_rs ON public.idn_uma_resource_scope USING btree (scope_name);


--
-- Name: idx_sb_scpid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_sb_scpid ON public.idn_oauth2_scope_binding USING btree (scope_id);


--
-- Name: idx_sc_tid; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_sc_tid ON public.idn_oauth2_scope USING btree (tenant_id);


--
-- Name: idx_session_id; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_session_id ON public.idn_auth_user_session_mapping USING btree (session_id);


--
-- Name: idx_sp_template; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_sp_template ON public.sp_template USING btree (tenant_id, name);


--
-- Name: idx_spi_app; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_spi_app ON public.sp_inbound_auth USING btree (app_id);


--
-- Name: idx_sub_app_id; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_sub_app_id ON public.am_subscription USING btree (application_id, subscription_id);


--
-- Name: idx_tc; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_tc ON public.idn_oauth2_access_token USING btree (time_created);


--
-- Name: idx_user; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_user ON public.idn_uma_resource USING btree (resource_owner_name, user_domain);


--
-- Name: idx_user_id; Type: INDEX; Schema: public; Owner: apim_user
--

CREATE INDEX idx_user_id ON public.idn_auth_user_session_mapping USING btree (user_id);


--
-- Name: am_gw_api_artifacts time_stamp; Type: TRIGGER; Schema: public; Owner: apim_user
--

CREATE TRIGGER time_stamp AFTER UPDATE ON public.am_gw_api_artifacts FOR EACH ROW EXECUTE FUNCTION public.update_modified_column();


--
-- Name: am_api_client_certificate am_api_client_certificate_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_client_certificate
    ADD CONSTRAINT am_api_client_certificate_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON DELETE CASCADE;


--
-- Name: am_api_comments am_api_comments_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_comments
    ADD CONSTRAINT am_api_comments_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_api_lc_event am_api_lc_event_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_lc_event
    ADD CONSTRAINT am_api_lc_event_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_api_product_mapping am_api_product_mapping_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_product_mapping
    ADD CONSTRAINT am_api_product_mapping_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON DELETE CASCADE;


--
-- Name: am_api_product_mapping am_api_product_mapping_url_mapping_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_product_mapping
    ADD CONSTRAINT am_api_product_mapping_url_mapping_id_fkey FOREIGN KEY (url_mapping_id) REFERENCES public.am_api_url_mapping(url_mapping_id) ON DELETE CASCADE;


--
-- Name: am_api_ratings am_api_ratings_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_ratings
    ADD CONSTRAINT am_api_ratings_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_api_ratings am_api_ratings_subscriber_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_ratings
    ADD CONSTRAINT am_api_ratings_subscriber_id_fkey FOREIGN KEY (subscriber_id) REFERENCES public.am_subscriber(subscriber_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_api_resource_scope_mapping am_api_resource_scope_mapping_url_mapping_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_api_resource_scope_mapping
    ADD CONSTRAINT am_api_resource_scope_mapping_url_mapping_id_fkey FOREIGN KEY (url_mapping_id) REFERENCES public.am_api_url_mapping(url_mapping_id) ON DELETE CASCADE;


--
-- Name: am_application_attributes am_application_attributes_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_attributes
    ADD CONSTRAINT am_application_attributes_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.am_application(application_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_application_group_mapping am_application_group_mapping_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_group_mapping
    ADD CONSTRAINT am_application_group_mapping_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.am_application(application_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_application_key_mapping am_application_key_mapping_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_key_mapping
    ADD CONSTRAINT am_application_key_mapping_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.am_application(application_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_application_registration am_application_registration_app_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_registration
    ADD CONSTRAINT am_application_registration_app_id_fkey FOREIGN KEY (app_id) REFERENCES public.am_application(application_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_application_registration am_application_registration_subscriber_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application_registration
    ADD CONSTRAINT am_application_registration_subscriber_id_fkey FOREIGN KEY (subscriber_id) REFERENCES public.am_subscriber(subscriber_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_application am_application_subscriber_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_application
    ADD CONSTRAINT am_application_subscriber_id_fkey FOREIGN KEY (subscriber_id) REFERENCES public.am_subscriber(subscriber_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_condition_group am_condition_group_policy_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_condition_group
    ADD CONSTRAINT am_condition_group_policy_id_fkey FOREIGN KEY (policy_id) REFERENCES public.am_api_throttle_policy(policy_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_correlation_properties am_correlation_properties_component_name_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_correlation_properties
    ADD CONSTRAINT am_correlation_properties_component_name_fkey FOREIGN KEY (component_name) REFERENCES public.am_correlation_configs(component_name) ON DELETE CASCADE;


--
-- Name: am_external_stores am_external_stores_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_external_stores
    ADD CONSTRAINT am_external_stores_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_graphql_complexity am_graphql_complexity_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_graphql_complexity
    ADD CONSTRAINT am_graphql_complexity_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_gw_api_artifacts am_gw_api_artifacts_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_gw_api_artifacts
    ADD CONSTRAINT am_gw_api_artifacts_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_gw_published_api_details(api_id) ON UPDATE CASCADE;


--
-- Name: am_header_field_condition am_header_field_condition_condition_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_header_field_condition
    ADD CONSTRAINT am_header_field_condition_condition_group_id_fkey FOREIGN KEY (condition_group_id) REFERENCES public.am_condition_group(condition_group_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_ip_condition am_ip_condition_condition_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_ip_condition
    ADD CONSTRAINT am_ip_condition_condition_group_id_fkey FOREIGN KEY (condition_group_id) REFERENCES public.am_condition_group(condition_group_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_jwt_claim_condition am_jwt_claim_condition_condition_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_jwt_claim_condition
    ADD CONSTRAINT am_jwt_claim_condition_condition_group_id_fkey FOREIGN KEY (condition_group_id) REFERENCES public.am_condition_group(condition_group_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_label_urls am_label_urls_label_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_label_urls
    ADD CONSTRAINT am_label_urls_label_id_fkey FOREIGN KEY (label_id) REFERENCES public.am_labels(label_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_query_parameter_condition am_query_parameter_condition_condition_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_query_parameter_condition
    ADD CONSTRAINT am_query_parameter_condition_condition_group_id_fkey FOREIGN KEY (condition_group_id) REFERENCES public.am_condition_group(condition_group_id) ON UPDATE CASCADE ON DELETE CASCADE;


--
-- Name: am_scope_binding am_scope_binding_scope_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_scope_binding
    ADD CONSTRAINT am_scope_binding_scope_id_fkey FOREIGN KEY (scope_id) REFERENCES public.am_scope(scope_id) ON DELETE CASCADE;


--
-- Name: am_security_audit_uuid_mapping am_security_audit_uuid_mapping_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_security_audit_uuid_mapping
    ADD CONSTRAINT am_security_audit_uuid_mapping_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_subscription am_subscription_api_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscription
    ADD CONSTRAINT am_subscription_api_id_fkey FOREIGN KEY (api_id) REFERENCES public.am_api(api_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_subscription am_subscription_application_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscription
    ADD CONSTRAINT am_subscription_application_id_fkey FOREIGN KEY (application_id) REFERENCES public.am_application(application_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: am_subscription_key_mapping am_subscription_key_mapping_subscription_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.am_subscription_key_mapping
    ADD CONSTRAINT am_subscription_key_mapping_subscription_id_fkey FOREIGN KEY (subscription_id) REFERENCES public.am_subscription(subscription_id) ON UPDATE CASCADE ON DELETE RESTRICT;


--
-- Name: sp_inbound_auth application_id_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_inbound_auth
    ADD CONSTRAINT application_id_constraint FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: sp_auth_step application_id_constraint_step; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_auth_step
    ADD CONSTRAINT application_id_constraint_step FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: sp_claim_mapping claimid_appid_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_claim_mapping
    ADD CONSTRAINT claimid_appid_constraint FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: cm_consent_receipt_property cm_consent_receipt_prt_fk0; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_consent_receipt_property
    ADD CONSTRAINT cm_consent_receipt_prt_fk0 FOREIGN KEY (consent_receipt_id) REFERENCES public.cm_receipt(consent_receipt_id);


--
-- Name: cm_receipt_sp_assoc cm_receipt_sp_assoc_fk0; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_receipt_sp_assoc
    ADD CONSTRAINT cm_receipt_sp_assoc_fk0 FOREIGN KEY (consent_receipt_id) REFERENCES public.cm_receipt(consent_receipt_id);


--
-- Name: cm_sp_purpose_purpose_cat_assc cm_sp_p_p_cat_assoc_fk0; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_purpose_cat_assc
    ADD CONSTRAINT cm_sp_p_p_cat_assoc_fk0 FOREIGN KEY (sp_purpose_assoc_id) REFERENCES public.cm_sp_purpose_assoc(id);


--
-- Name: cm_sp_purpose_purpose_cat_assc cm_sp_p_p_cat_assoc_fk1; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_purpose_cat_assc
    ADD CONSTRAINT cm_sp_p_p_cat_assoc_fk1 FOREIGN KEY (purpose_category_id) REFERENCES public.cm_purpose_category(id);


--
-- Name: cm_sp_purpose_pii_cat_assoc cm_sp_p_pii_cat_assoc_fk0; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_pii_cat_assoc
    ADD CONSTRAINT cm_sp_p_pii_cat_assoc_fk0 FOREIGN KEY (sp_purpose_assoc_id) REFERENCES public.cm_sp_purpose_assoc(id);


--
-- Name: cm_sp_purpose_pii_cat_assoc cm_sp_p_pii_cat_assoc_fk1; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_pii_cat_assoc
    ADD CONSTRAINT cm_sp_p_pii_cat_assoc_fk1 FOREIGN KEY (pii_category_id) REFERENCES public.cm_pii_category(id);


--
-- Name: cm_sp_purpose_assoc cm_sp_purpose_assoc_fk0; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_assoc
    ADD CONSTRAINT cm_sp_purpose_assoc_fk0 FOREIGN KEY (receipt_sp_assoc) REFERENCES public.cm_receipt_sp_assoc(id);


--
-- Name: cm_sp_purpose_assoc cm_sp_purpose_assoc_fk1; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.cm_sp_purpose_assoc
    ADD CONSTRAINT cm_sp_purpose_assoc_fk1 FOREIGN KEY (purpose_id) REFERENCES public.cm_purpose(id);


--
-- Name: sp_claim_dialect dialectid_appid_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_claim_dialect
    ADD CONSTRAINT dialectid_appid_constraint FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: idn_associated_id idn_associated_id_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_associated_id
    ADD CONSTRAINT idn_associated_id_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idn_claim idn_claim_dialect_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim
    ADD CONSTRAINT idn_claim_dialect_id_fkey FOREIGN KEY (dialect_id) REFERENCES public.idn_claim_dialect(id) ON DELETE CASCADE;


--
-- Name: idn_claim_mapped_attribute idn_claim_mapped_attribute_local_claim_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapped_attribute
    ADD CONSTRAINT idn_claim_mapped_attribute_local_claim_id_fkey FOREIGN KEY (local_claim_id) REFERENCES public.idn_claim(id) ON DELETE CASCADE;


--
-- Name: idn_claim_mapping idn_claim_mapping_ext_claim_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapping
    ADD CONSTRAINT idn_claim_mapping_ext_claim_id_fkey FOREIGN KEY (ext_claim_id) REFERENCES public.idn_claim(id) ON DELETE CASCADE;


--
-- Name: idn_claim_mapping idn_claim_mapping_mapped_local_claim_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_mapping
    ADD CONSTRAINT idn_claim_mapping_mapped_local_claim_id_fkey FOREIGN KEY (mapped_local_claim_id) REFERENCES public.idn_claim(id) ON DELETE CASCADE;


--
-- Name: idn_claim_property idn_claim_property_local_claim_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_claim_property
    ADD CONSTRAINT idn_claim_property_local_claim_id_fkey FOREIGN KEY (local_claim_id) REFERENCES public.idn_claim(id) ON DELETE CASCADE;


--
-- Name: idn_oauth1a_access_token idn_oauth1a_access_token_consumer_key_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth1a_access_token
    ADD CONSTRAINT idn_oauth1a_access_token_consumer_key_id_fkey FOREIGN KEY (consumer_key_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oauth1a_request_token idn_oauth1a_request_token_consumer_key_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth1a_request_token
    ADD CONSTRAINT idn_oauth1a_request_token_consumer_key_id_fkey FOREIGN KEY (consumer_key_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_access_token idn_oauth2_access_token_consumer_key_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_access_token
    ADD CONSTRAINT idn_oauth2_access_token_consumer_key_id_fkey FOREIGN KEY (consumer_key_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_access_token_scope idn_oauth2_access_token_scope_token_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_access_token_scope
    ADD CONSTRAINT idn_oauth2_access_token_scope_token_id_fkey FOREIGN KEY (token_id) REFERENCES public.idn_oauth2_access_token(token_id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_authorization_code idn_oauth2_authorization_code_consumer_key_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_authorization_code
    ADD CONSTRAINT idn_oauth2_authorization_code_consumer_key_id_fkey FOREIGN KEY (consumer_key_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_authz_code_scope idn_oauth2_authz_code_scope_code_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_authz_code_scope
    ADD CONSTRAINT idn_oauth2_authz_code_scope_code_id_fkey FOREIGN KEY (code_id) REFERENCES public.idn_oauth2_authorization_code(code_id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_ciba_auth_code idn_oauth2_ciba_auth_code_consumer_key_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_ciba_auth_code
    ADD CONSTRAINT idn_oauth2_ciba_auth_code_consumer_key_fkey FOREIGN KEY (consumer_key) REFERENCES public.idn_oauth_consumer_apps(consumer_key) ON DELETE CASCADE;


--
-- Name: idn_oauth2_ciba_request_scopes idn_oauth2_ciba_request_scopes_auth_code_key_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_ciba_request_scopes
    ADD CONSTRAINT idn_oauth2_ciba_request_scopes_auth_code_key_fkey FOREIGN KEY (auth_code_key) REFERENCES public.idn_oauth2_ciba_auth_code(auth_code_key) ON DELETE CASCADE;


--
-- Name: idn_oauth2_device_flow idn_oauth2_device_flow_consumer_key_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow
    ADD CONSTRAINT idn_oauth2_device_flow_consumer_key_id_fkey FOREIGN KEY (consumer_key_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_device_flow_scopes idn_oauth2_device_flow_scopes_scope_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_device_flow_scopes
    ADD CONSTRAINT idn_oauth2_device_flow_scopes_scope_id_fkey FOREIGN KEY (scope_id) REFERENCES public.idn_oauth2_device_flow(code_id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_resource_scope idn_oauth2_resource_scope_scope_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_resource_scope
    ADD CONSTRAINT idn_oauth2_resource_scope_scope_id_fkey FOREIGN KEY (scope_id) REFERENCES public.idn_oauth2_scope(scope_id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_scope_binding idn_oauth2_scope_binding_scope_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_scope_binding
    ADD CONSTRAINT idn_oauth2_scope_binding_scope_id_fkey FOREIGN KEY (scope_id) REFERENCES public.idn_oauth2_scope(scope_id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_scope_validators idn_oauth2_scope_validators_app_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_scope_validators
    ADD CONSTRAINT idn_oauth2_scope_validators_app_id_fkey FOREIGN KEY (app_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oauth2_token_binding idn_oauth2_token_binding_token_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oauth2_token_binding
    ADD CONSTRAINT idn_oauth2_token_binding_token_id_fkey FOREIGN KEY (token_id) REFERENCES public.idn_oauth2_access_token(token_id) ON DELETE CASCADE;


--
-- Name: idn_oidc_property idn_oidc_property_consumer_key_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_property
    ADD CONSTRAINT idn_oidc_property_consumer_key_fkey FOREIGN KEY (consumer_key) REFERENCES public.idn_oauth_consumer_apps(consumer_key) ON DELETE CASCADE;


--
-- Name: idn_oidc_req_obj_claim_values idn_oidc_req_obj_claim_values_req_object_claims_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_obj_claim_values
    ADD CONSTRAINT idn_oidc_req_obj_claim_values_req_object_claims_id_fkey FOREIGN KEY (req_object_claims_id) REFERENCES public.idn_oidc_req_object_claims(id) ON DELETE CASCADE;


--
-- Name: idn_oidc_req_object_claims idn_oidc_req_object_claims_req_object_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_object_claims
    ADD CONSTRAINT idn_oidc_req_object_claims_req_object_id_fkey FOREIGN KEY (req_object_id) REFERENCES public.idn_oidc_req_object_reference(id) ON DELETE CASCADE;


--
-- Name: idn_oidc_req_object_reference idn_oidc_req_object_reference_code_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_object_reference
    ADD CONSTRAINT idn_oidc_req_object_reference_code_id_fkey FOREIGN KEY (code_id) REFERENCES public.idn_oauth2_authorization_code(code_id) ON DELETE CASCADE;


--
-- Name: idn_oidc_req_object_reference idn_oidc_req_object_reference_consumer_key_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_object_reference
    ADD CONSTRAINT idn_oidc_req_object_reference_consumer_key_id_fkey FOREIGN KEY (consumer_key_id) REFERENCES public.idn_oauth_consumer_apps(id) ON DELETE CASCADE;


--
-- Name: idn_oidc_req_object_reference idn_oidc_req_object_reference_token_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_req_object_reference
    ADD CONSTRAINT idn_oidc_req_object_reference_token_id_fkey FOREIGN KEY (token_id) REFERENCES public.idn_oauth2_access_token(token_id) ON DELETE CASCADE;


--
-- Name: idn_oidc_scope_claim_mapping idn_oidc_scope_claim_mapping_external_claim_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_scope_claim_mapping
    ADD CONSTRAINT idn_oidc_scope_claim_mapping_external_claim_id_fkey FOREIGN KEY (external_claim_id) REFERENCES public.idn_claim(id) ON DELETE CASCADE;


--
-- Name: idn_oidc_scope_claim_mapping idn_oidc_scope_claim_mapping_scope_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_oidc_scope_claim_mapping
    ADD CONSTRAINT idn_oidc_scope_claim_mapping_scope_id_fkey FOREIGN KEY (scope_id) REFERENCES public.idn_oauth2_scope(scope_id) ON DELETE CASCADE;


--
-- Name: idn_uma_pt_resource idn_uma_pt_resource_pt_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_pt_resource
    ADD CONSTRAINT idn_uma_pt_resource_pt_id_fkey FOREIGN KEY (pt_id) REFERENCES public.idn_uma_permission_ticket(id) ON DELETE CASCADE;


--
-- Name: idn_uma_pt_resource idn_uma_pt_resource_pt_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_pt_resource
    ADD CONSTRAINT idn_uma_pt_resource_pt_resource_id_fkey FOREIGN KEY (pt_resource_id) REFERENCES public.idn_uma_resource(id) ON DELETE CASCADE;


--
-- Name: idn_uma_pt_resource_scope idn_uma_pt_resource_scope_pt_resource_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_pt_resource_scope
    ADD CONSTRAINT idn_uma_pt_resource_scope_pt_resource_id_fkey FOREIGN KEY (pt_resource_id) REFERENCES public.idn_uma_pt_resource(id) ON DELETE CASCADE;


--
-- Name: idn_uma_pt_resource_scope idn_uma_pt_resource_scope_pt_scope_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_pt_resource_scope
    ADD CONSTRAINT idn_uma_pt_resource_scope_pt_scope_id_fkey FOREIGN KEY (pt_scope_id) REFERENCES public.idn_uma_resource_scope(id) ON DELETE CASCADE;


--
-- Name: idn_uma_resource_meta_data idn_uma_resource_meta_data_resource_identity_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_resource_meta_data
    ADD CONSTRAINT idn_uma_resource_meta_data_resource_identity_fkey FOREIGN KEY (resource_identity) REFERENCES public.idn_uma_resource(id) ON DELETE CASCADE;


--
-- Name: idn_uma_resource_scope idn_uma_resource_scope_resource_identity_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idn_uma_resource_scope
    ADD CONSTRAINT idn_uma_resource_scope_resource_identity_fkey FOREIGN KEY (resource_identity) REFERENCES public.idn_uma_resource(id) ON DELETE CASCADE;


--
-- Name: idp_authenticator idp_authenticator_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_authenticator
    ADD CONSTRAINT idp_authenticator_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idp_authenticator_property idp_authenticator_property_authenticator_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_authenticator_property
    ADD CONSTRAINT idp_authenticator_property_authenticator_id_fkey FOREIGN KEY (authenticator_id) REFERENCES public.idp_authenticator(id) ON DELETE CASCADE;


--
-- Name: idp_claim idp_claim_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_claim
    ADD CONSTRAINT idp_claim_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idp_claim_mapping idp_claim_mapping_idp_claim_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_claim_mapping
    ADD CONSTRAINT idp_claim_mapping_idp_claim_id_fkey FOREIGN KEY (idp_claim_id) REFERENCES public.idp_claim(id) ON DELETE CASCADE;


--
-- Name: idp_local_claim idp_local_claim_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_local_claim
    ADD CONSTRAINT idp_local_claim_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idp_metadata idp_metadata_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_metadata
    ADD CONSTRAINT idp_metadata_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idp_prov_config_property idp_prov_config_property_provisioning_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_prov_config_property
    ADD CONSTRAINT idp_prov_config_property_provisioning_config_id_fkey FOREIGN KEY (provisioning_config_id) REFERENCES public.idp_provisioning_config(id) ON DELETE CASCADE;


--
-- Name: idp_provisioning_config idp_provisioning_config_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_config
    ADD CONSTRAINT idp_provisioning_config_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idp_provisioning_entity idp_provisioning_entity_provisioning_config_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_provisioning_entity
    ADD CONSTRAINT idp_provisioning_entity_provisioning_config_id_fkey FOREIGN KEY (provisioning_config_id) REFERENCES public.idp_provisioning_config(id) ON DELETE CASCADE;


--
-- Name: idp_role idp_role_idp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_role
    ADD CONSTRAINT idp_role_idp_id_fkey FOREIGN KEY (idp_id) REFERENCES public.idp(id) ON DELETE CASCADE;


--
-- Name: idp_role_mapping idp_role_mapping_idp_role_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.idp_role_mapping
    ADD CONSTRAINT idp_role_mapping_idp_role_id_fkey FOREIGN KEY (idp_role_id) REFERENCES public.idp_role(id) ON DELETE CASCADE;


--
-- Name: sp_provisioning_connector pro_connector_appid_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_provisioning_connector
    ADD CONSTRAINT pro_connector_appid_constraint FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: sp_req_path_authenticator req_auth_appid_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_req_path_authenticator
    ADD CONSTRAINT req_auth_appid_constraint FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: sp_role_mapping roleid_appid_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_role_mapping
    ADD CONSTRAINT roleid_appid_constraint FOREIGN KEY (app_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: sp_metadata sp_metadata_sp_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_metadata
    ADD CONSTRAINT sp_metadata_sp_id_fkey FOREIGN KEY (sp_id) REFERENCES public.sp_app(id) ON DELETE CASCADE;


--
-- Name: sp_federated_idp step_id_constraint; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.sp_federated_idp
    ADD CONSTRAINT step_id_constraint FOREIGN KEY (id) REFERENCES public.sp_auth_step(id) ON DELETE CASCADE;


--
-- Name: wf_request_entity_relationship wf_request_entity_relationship_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_request_entity_relationship
    ADD CONSTRAINT wf_request_entity_relationship_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.wf_request(uuid) ON DELETE CASCADE;


--
-- Name: wf_workflow_association wf_workflow_association_workflow_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_association
    ADD CONSTRAINT wf_workflow_association_workflow_id_fkey FOREIGN KEY (workflow_id) REFERENCES public.wf_workflow(id) ON DELETE CASCADE;


--
-- Name: wf_workflow_config_param wf_workflow_config_param_workflow_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_config_param
    ADD CONSTRAINT wf_workflow_config_param_workflow_id_fkey FOREIGN KEY (workflow_id) REFERENCES public.wf_workflow(id) ON DELETE CASCADE;


--
-- Name: wf_workflow_request_relation wf_workflow_request_relation_request_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_request_relation
    ADD CONSTRAINT wf_workflow_request_relation_request_id_fkey FOREIGN KEY (request_id) REFERENCES public.wf_request(uuid) ON DELETE CASCADE;


--
-- Name: wf_workflow_request_relation wf_workflow_request_relation_workflow_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: apim_user
--

ALTER TABLE ONLY public.wf_workflow_request_relation
    ADD CONSTRAINT wf_workflow_request_relation_workflow_id_fkey FOREIGN KEY (workflow_id) REFERENCES public.wf_workflow(id) ON DELETE CASCADE;


--
-- PostgreSQL database dump complete
--

\unrestrict 9ths1bMTVM7pZenZgUHp77YVdBMkzogKl6urOnQiAfPvB38cZ1lZGpjvGHA35cZ

