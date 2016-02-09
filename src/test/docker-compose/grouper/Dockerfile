FROM unicon/grouper-demo

# Install Shell Wrappers for Grouper GSH
# https://github.com/wgthom/groovysh4grouper
RUN wget -O /opt/groovy.zip https://bintray.com/artifact/download/groovy/maven/groovy-binary-2.3.3.zip
WORKDIR /opt
RUN unzip groovy.zip
RUN mv groovy-2.3.3 groovy
RUN bash -c "ln -s /opt/groovy/bin/{grape,groovy,groovyConsole,groovyc,groovydoc,groovysh,java2groovy,startGroovy} /usr/local/bin/"
COPY groovysh4grouper/config /opt/grouper.apiBinary-2.2.2/conf/.groovy
COPY groovysh4grouper/bin/ggsh /opt/grouper.apiBinary-2.2.2/bin/

# Install Google Apps Provisioner
# https://github.com/Unicon/googleapps-grouper-provisioner
COPY custom/ /opt/grouper.apiBinary-2.2.2/lib/custom/
COPY grouper-loader.properties /opt/grouper.apiBinary-2.2.2/conf/
COPY googleServiceAccount.p12 /opt/grouper.apiBinary-2.2.2/conf/
COPY googleAppsFullSync /opt/grouper.apiBinary-2.2.2/bin/

# Add google changlog consumer to logging
COPY log4j.properties /opt/grouper.apiBinary-2.2.2/conf/

# Setup up Google Apps provisioning targets
# "googleAppsFullSync google --dry-run" creates etc:attribute:googleProvisioner folder and syncToGoogle<consumerName> attributes
# "gsh /provisioningTargetAttributes.gsh" marks groups for google provisioning
ADD provisioningTargetAttributes.gsh /
RUN set -x; \
    service mysql start \
    && service slapd start \
    && cd /opt/grouper.apiBinary-2.2.2/ \
    && bin/googleAppsFullSync google --dry-run \
    && bin/gsh /provisioningTargetAttributes.gsh

WORKDIR /opt/grouper.apiBinary-2.2.2/bin/