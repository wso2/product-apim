CREATE OR REPLACE PROCEDURE add_index_if_not_exists (query IN VARCHAR2)
  IS
BEGIN
  execute immediate query;
  dbms_output.put_line(query);
exception WHEN OTHERS THEN
  dbms_output.put_line( 'Skipped ');
END;
/

CALL add_index_if_not_exists('CREATE INDEX REG_TAG_IND_BY_TAG_ID ON REG_RESOURCE_TAG(REG_TAG_ID, REG_TENANT_ID)');
/

CALL add_index_if_not_exists('CREATE INDEX REG_RESC_PROP_BY_REG_PROP_ID ON REG_RESOURCE_PROPERTY(REG_TENANT_ID,REG_PROPERTY_ID)');
/
