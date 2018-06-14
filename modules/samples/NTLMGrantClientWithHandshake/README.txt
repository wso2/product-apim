Prerequisites
===============

1. This NTLM grant client has to run in the Windows server in which NTLM authentication is enabled.
2. Make sure 'ant' and 'java' are installed in a running Windows environment.

Instructions
=============

1. Configure build.xml with information for the properties given below.

   is.home= Path to API-M/IS pack location [in an API-M distributed setup, give the Keymgt node path]
   token_url= The token endpoint URL
   consumer_key= Consumer key
   consumer_secret= Consumer secret

2. Start the API-M/IS server [in an API-M distributed setup,it's the Keymgt node], if it's not already started.
3. Run "ant run".
