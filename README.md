Creates and publishes a new sitemap by connecting to a Mongo server and listing all records
 
A generated sitemap consists of:
 - multiple sitemap files containing record urls (45.000 per file)
 - a sitemap index file listing all the sitemap files
  
To make sure there is always a sitemap available, we use blue/green versions of the sitemap files. At the start of the
update process any old blue/green version left is deleted before the new sitemap files are created.

For more information about sitemaps in general see also https://support.google.com/webmasters/answer/183668?hl=en

**Run**

You can either run the application directly in your IDE (select 'Run' on SitemapApplication class) or
you can run `mvn clean package` and then start the provided docker-compose file (run `docker-compose up` in the 
application root folder)

For debugging purposes you can use the following urls:

  - `/list` shows a list of stored files
  - `/index` shows the contents of the sitemap index file
  - `/file?name=x` shows the contents of the stored file with the name x

Note that you can only run `/update` manually if you configure and provide an administrator apikey
e.g. `/update?wskey=<enter_adminkey_here>`