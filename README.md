Generates and publishes a record and entity sitemap for www.europeana.eu
 
The record sitemap is generated by connecting to a Mongo server and listing all records (with 
a minimum content tier and meta data tier).
The entity sitemap uses the search functionality of Entity-API to retrieve all entities used
on the Europeana website.
 
For both, the generated sitemap consists of:
 - multiple sitemap files containing record urls (45,000 resp. 20,000 per file)
 - a sitemap index file listing all the sitemap files
  
To make sure there is always a sitemap available, we use blue/green versions of the sitemap files and we
keep track which one is 'active'. At the start of the update process all files of the inactive 
blue/green version are deleted first. Then the new sitemap files are created and the active 
version is switched from blue to green or vice versa.

For more information about sitemaps in general see also https://support.google.com/webmasters/answer/183668?hl=en

**Run**

You can run the application directly in your IDE (select 'Run' on SitemapApplication class)

For debugging purposes you can use the following urls:

  - `/files` shows a list of stored files
  - `/file?name=x` shows the contents of the stored file with the name x
  
  - `/record/index.xml` and `/entity/index.xml` shows the contents of the sitemap index files  

Note that you can only run `/record/update` or `/entity/update` manually if you configure and provide an
administrator apikey e.g. `/record/update?wskey=<enter_adminkey_here>`