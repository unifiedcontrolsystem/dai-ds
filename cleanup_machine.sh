#!/bin/bash

distributions=$(dirname "$0")
echo "*** distributions_dir: ${distributions}"

echo "*** Stop any local voltdb instance ***"
voltadmin shutdown

echo "*** Shutdown dai containers ***"
sudo chmod -R a+r /opt/dai-docker
for compose_file in $(find /opt/dai-docker/ -maxdepth 1 -name "*.yml")
do
  docker-compose -f $compose_file down
done

echo "*** Stop and remove any remaining dai containers"
for container in $(docker ps -aq -f name="dai-")
do
  docker stop $container
  docker rm $container
done

echo "*** Delete logstash configuration files ***"
sudo rm -f /etc/logstash/conf.d/*.conf

echo "*** Uninstall DAI ***"
for installer in $(find ${distributions} -maxdepth 1 -name "install-*.sh")
do
  sudo $installer -U
done

sudo systemctl daemon-reload

echo "*** Delete UCS directories"
sudo rm -rf /opt/dai-docker
sudo rm -rf /opt/ucs
