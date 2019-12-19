Installing ELK
============================================================================================
Install elasticsearch, kibana, and logstash as detailed here:
https://www.elastic.co/guide/en/elastic-stack-get-started/7.3/get-started-elastic-stack.html


Copy the files in this directory to following locations:
/etc/kibana/kibana.yml
/etc/logstash/logstash.yml
/etc/logstash/conf.d/ForeignBusCapture.conf
/etc/elasticsearc/elasticsearch.yml

Then restart the three services.

Logfiles for the services can be found in /var/log

Connect to kibana GUI using localhost:10000


Under Settings > Saved Objects, "Import" export.ndjson




