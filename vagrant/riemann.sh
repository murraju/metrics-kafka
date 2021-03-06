# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
# 
#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#!/bin/sh -Eux

#  Trap non-normal exit signals: 1/HUP, 2/INT, 3/QUIT, 15/TERM, ERR
trap founderror 1 2 3 15 ERR

founderror()
{
        exit 1
}

exitscript()
{
        #remove lock file
        #rm $lockfile
        exit 0
}

apt-get -y update
apt-get install -y software-properties-common python-software-properties
add-apt-repository -y ppa:webupd8team/java
apt-get -y update
/bin/echo debconf shared/accepted-oracle-license-v1-1 select true | /usr/bin/debconf-set-selections
apt-get -y install oracle-java7-installer oracle-java7-set-default

chown -R vagrant /opt

cd /opt

wget http://aphyr.com/riemann/riemann-0.2.4.tar.bz2
tar xvfj riemann-0.2.4.tar.bz2
cd riemann-0.2.4

/opt/riemann-0.2.4/bin/riemann /vagrant/config/riemann.config >> /tmp/riemann.log &

apt-add-repository -y ppa:brightbox/ruby-ng
apt-get -y update

apt-get -y install rubygems
apt-get -y install ruby1.9.3

gem1.9.3 install riemann-client riemann-tools riemann-dash

chown -R vagrant /var/lib/gems/

riemann-dash /vagrant/config/config.rb >> /tmp/riemann-dash.log &

exitscript