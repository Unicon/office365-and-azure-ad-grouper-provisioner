FROM unicon/grouper-demo

ADD provisioningTargetAttributes.gsh /

RUN set -x; \
    service mysql start \
    && service slapd start \
    && cd /opt/grouper.apiBinary-2.2.2/ \
    && bin/gsh /provisioningTargetAttributes.gsh

COPY custom/ /opt/grouper.apiBinary-2.2.2/lib/custom/
COPY grouper-loader.properties /opt/grouper.apiBinary-2.2.2/conf/
COPY googleServiceAccount.p12 /opt/grouper.apiBinary-2.2.2/conf/
COPY googleAppsFullSync /opt/grouper.apiBinary-2.2.2/bin/