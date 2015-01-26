Pre-Reqesties
===============

1. This NTLM grant client has to run in Windows server in which NTLM authentication enabled.
2. Make sure 'ant' and 'java' installed in running Windows environment.

Instructions
=============

1. Configure build.xml with the information for the below properties.

   is.home= Path to AM/IS pack location [In a AM distributed setup, give the Keymgt node path]   
   url= The token endpoint url
   auto= The flag to determine whther generate the ntlm token by this client or pass a ntlm token as an input
   nToken= NTLM token [Type1 token type]
   key= Consumer key  
   secret= Consumer secret   
   jna.dir= Path to JNA jar dependencies [Point this to full path of the "NTLMGrantClient/jna" sub-directory inside this client directory] 

2. Start AM/IS server[In a AM distributed setup,it's the Keymgt node]  if its not already started.
3. Run "ant run" to migrate. 


