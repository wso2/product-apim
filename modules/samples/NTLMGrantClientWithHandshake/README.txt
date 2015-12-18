Pre-Reqesties
===============

1. This NTLM grant client has to run in Windows server in which NTLM authentication enabled.
2. Make sure 'ant' and 'java' installed in running Windows environment.

Instructions
=============

1. Configure build.xml with the information for the below properties.

   is.home= Path to AM/IS pack location [In a AM distributed setup, give the Keymgt node path]
   token_url= The token endpoint url
   consumer_key= Consumer key
   consumer_secret= Consumer secret

2. Start AM/IS server[In a AM distributed setup,it's the Keymgt node]  if its not already started.
3. Run "ant run".