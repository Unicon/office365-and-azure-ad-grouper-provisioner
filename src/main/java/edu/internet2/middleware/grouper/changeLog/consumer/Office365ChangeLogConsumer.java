package edu.internet2.middleware.grouper.changeLog.consumer;


import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.GrouperSession;
import edu.internet2.middleware.grouper.app.loader.GrouperLoaderConfig;
import edu.internet2.middleware.grouper.attr.AttributeDefName;
import edu.internet2.middleware.grouper.attr.finder.AttributeDefNameFinder;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBaseImpl;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.consumer.model.OAuthTokenInfo;
import edu.internet2.middleware.grouper.changeLog.consumer.model.OdataIdContainer;
import edu.internet2.middleware.grouper.changeLog.consumer.model.User;
import edu.internet2.middleware.grouper.pit.PITGroup;
import edu.internet2.middleware.subject.Subject;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import org.apache.log4j.Logger;
import retrofit2.Retrofit;
import retrofit2.converter.moshi.MoshiConverterFactory;

import java.io.IOException;
import java.util.*;

/**
 * Created by jj on 5/30/16.
 */
public class Office365ChangeLogConsumer extends ChangeLogConsumerBaseImpl {
    private static final Logger logger = Logger.getLogger(Office365ChangeLogConsumer.class);

    private String token;
    private final String clientId;
    private final String clientSecret;

    private final Office365GraphApiService service;

    private final GrouperSession grouperSession;

    public Office365ChangeLogConsumer() {
        // TODO: this.getConsumerName() isn't working for some reason. track down
        String name = this.getConsumerName() != null ? this.getConsumerName() : "o365";
        this.clientId = GrouperLoaderConfig.retrieveConfig().propertyValueStringRequired("changeLog.consumer." + name + ".clientId");
        this.clientSecret = GrouperLoaderConfig.retrieveConfig().propertyValueStringRequired("changeLog.consumer." + name + ".clientSecret");

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        try {
            this.token = this.getToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        Request request = chain.request().newBuilder().header("Authorization", "Bearer " + token).build();
                        return chain.proceed(request);
                    }
                })
                .addInterceptor(loggingInterceptor)
                .build();
        Retrofit retrofit = new Retrofit
                .Builder()
                .baseUrl("https://graph.microsoft.com/v1.0/")
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build();

        this.service = retrofit.create(Office365GraphApiService.class);

        this.grouperSession = GrouperSession.startRootSession();
    }

    private String getToken() throws IOException {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://login.microsoftonline.com/MKE197365.onmicrosoft.com/")
                .addConverterFactory(MoshiConverterFactory.create())
                .build();
        Office365AuthApiService service = retrofit.create(Office365AuthApiService.class);
        OAuthTokenInfo info = service.getOauth2Token("client_credentials", this.clientId, this.clientSecret, "https://graph.microsoft.com").execute().body();
        return info.accessToken;
    }

    @Override
    protected void addGroup(Group group, ChangeLogEntry changeLogEntry) {
        logger.debug("Creating group " + group);
        try {
            logger.debug("**** ");
            // TODO: currently uses the mailNickname as an ID. need to fix this
            /*
            {
            "id": "faccfbe2-3270-4db6-9d61-cf95feae9faf",
            "createdDateTime": "2016-06-03T01:09:51Z",
            "description": null,
            "displayName": "test",
            "groupTypes": [],
            "mail": null,
            "mailEnabled": false,
            "mailNickname": "test",
            "onPremisesLastSyncDateTime": null,
            "onPremisesSecurityIdentifier": null,
            "onPremisesSyncEnabled": null,
            "proxyAddresses": [],
            "renewedDateTime": "2016-06-03T01:09:51Z",
            "securityEnabled": true,
            "visibility": null
        }
             */
            retrofit2.Response response = this.service.createGroup(
                    new edu.internet2.middleware.grouper.changeLog.consumer.model.Group(
                            null,
                            group.getName(),
                            false,
                            group.getUuid(),
                            true,
                            new ArrayList<String>(),
                            group.getId()
                    )
            ).execute();
            AttributeDefName attributeDefName = AttributeDefNameFinder.findByName("etc:attribute:office365:o365Id", false);
            group.getAttributeDelegate().assignAttribute(attributeDefName);
            group.getAttributeValueDelegate().assignValue("etc:attribute:office365:o365Id", ((edu.internet2.middleware.grouper.changeLog.consumer.model.Group)response.body()).id);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // TODO: find out how to induce and implement (if necessary)
    @Override
    protected void removeGroup(Group group, ChangeLogEntry changeLogEntry) {
        logger.debug("removing group " + group);
        String id = group.getAttributeValueDelegate().retrieveValuesString("etc:attribute:office365:o365Id").get(0);
        logger.debug("removing id: " + id);
    }

    @Override
    protected void removeDeletedGroup(PITGroup pitGroup, ChangeLogEntry changeLogEntry) {
        logger.debug("removing group " + pitGroup + ": " + pitGroup.getId());
        try {
            Map options = new TreeMap<>();
            options.put("$filter", "displayName eq '" + pitGroup.getName() + "'");
            edu.internet2.middleware.grouper.changeLog.consumer.model.Group group = (edu.internet2.middleware.grouper.changeLog.consumer.model.Group) this.service.getGroups(options).execute().body();
            this.service.deleteGroup(group.id).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void addMembership(Subject subject, Group group, ChangeLogEntry changeLogEntry) {
        logger.debug("adding " + subject + " to " + group);
        logger.debug("attributes: " + subject.getAttributes());

        String groupId = group.getAttributeValueDelegate().retrieveValueString("etc:attribute:office365:o365Id");
        logger.debug("groupId: " + groupId);

        try {
            this.service.addGroupMember(groupId, new OdataIdContainer("https://graph.microsoft.com/v1.0/users/" + subject.getAttributeValue("uid") + "@MKE197365.onmicrosoft.com")).execute();
        } catch (IOException e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    protected void removeMembership(Subject subject, Group group, ChangeLogEntry changeLogEntry) {
        logger.debug("removing " + subject + " from " + group);
        try {
            User user = this.service.getUserByUPN(subject.getAttributeValue("uid") + "@MKE197365.onmicrosoft.com").execute().body();
            String groupId = group.getAttributeValueDelegate().retrieveValueString("etc:attribute:office365:o365Id");
            this.service.removeGroupMember(groupId, user.id).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
