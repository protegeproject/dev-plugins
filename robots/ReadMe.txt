
To run the robots do the following:

  * copy build.properties.template to build.properties and adjust for the server configuration.
  * copy the appropriate config files from the config directory.
  * set the PROTEGE_HOME environment variable
  * do "ant run" or "ant debug"
  
I am using rsync to copy directories to different machines.  (I have been aware of 
rsync for a long time but actually using it is a new trick for me and it is ridiculously
easy.)  For example, 

   rsync -av Protege smi-tredmond:/tmp
   rsync -av Robots  smi-tredmond:/tmp
   
Rsync naturally uses ssh so this just simply works.
