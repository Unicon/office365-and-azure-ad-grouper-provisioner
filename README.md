# office365-and-azure-ad-grouper-provisioner
This project is an Internet2 Grouper connector (full sync and changelog consumer) that synchronizes Grouper groups and users to Microsoft Azure Active Directory/Office 365.

Note that this currently only supports security groups. Support for other group types is planned.

# Running

1. build

    ```
    ./gradlew clean distZip
    ```

1. copy contents of file to grouper home

    ```
    unzip build/distributions/office-365-azure-ad-grouper-provisioner-1.0.0.zip -d /tmp
    cp /tmp/office-365-azure-ad-grouper-provisioner-1.0.0/*.jar /opt/grouper.apiBinary-2.3.0/lib/custom
    ```

1. Set up stem for provisioning and ID attribute

    ```
    GrouperSession grouperSession = GrouperSession.startRootSession();
    AttributeDef provisioningMarkerAttributeDef = new AttributeDefSave(grouperSession).assignCreateParentStemsIfNotExist(true).assignName("etc:attribute:office365:o365SyncDef").assignToStem(true).assignToGroup(true).save();
    AttributeDefName provisioningMarkerAttributeName = new AttributeDefNameSave(grouperSession, provisioningMarkerAttributeDef).assignName("etc:attribute:office365:o365Sync").save();

    rootStem = addStem("", "test", "test");
    rootStem.getAttributeDelegate().assignAttribute(provisioningMarkerAttributeName);

    AttributeDef o365Id = new AttributeDefSave(grouperSession).assignCreateParentStemsIfNotExist(true).assignName("etc:attribute:office365:o365IdDef").assignToGroup(true).assignValueType(AttributeDefValueType.string).save();
    AttributeDefName o365IdName = new AttributeDefNameSave(grouperSession, o365Id).assignName("etc:attribute:office365:o365Id").save();
    ```

2. Configure loader job in `grouper-loader.properties`. Note that you will need to set up an application with access to your domain.
See documentation at [http://graph.microsoft.io/en-us/docs].

    ```
    changeLog.consumer.o365.class = edu.internet2.middleware.grouper.changeLog.consumer.Office365ChangeLogConsumer
    # fire every 5 seconds
    changeLog.consumer.o365.quartzCron =  0,5,10,15,20,25,30,35,40,45,50,55 * * * * ?
    changeLog.consumer.o365.syncAttributeName = etc:attribute:office365:o365Sync
    changeLog.consumer.o365.retryOnError = true
    changeLog.consumer.o365.clientId = @o365.clientId@
    changeLog.consumer.o365.clientSecret = @o365.clientSecret@
    ```

    Replace `@o365.clientId@` and `@o365.clientSecret@` with appropriate values from the application configuration.
