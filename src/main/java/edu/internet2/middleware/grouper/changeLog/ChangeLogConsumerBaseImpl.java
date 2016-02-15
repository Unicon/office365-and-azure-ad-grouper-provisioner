package edu.internet2.middleware.grouper.changeLog;

import edu.internet2.middleware.grouper.*;
import edu.internet2.middleware.grouper.app.loader.GrouperLoaderConfig;
import edu.internet2.middleware.grouper.attr.AttributeDef;
import edu.internet2.middleware.grouper.attr.AttributeDefName;
import edu.internet2.middleware.grouper.attr.AttributeDefType;
import edu.internet2.middleware.grouper.attr.finder.AttributeDefFinder;
import edu.internet2.middleware.grouper.attr.finder.AttributeDefNameFinder;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBase;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogProcessorMetadata;
import edu.internet2.middleware.grouper.exception.GroupNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Default @ChangeLogConsumerBase implementation.
 */
public class ChangeLogConsumerBaseImpl extends ChangeLogConsumerBase {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeLogConsumerBaseImpl.class);

    /** Maps supported changeLogEntry category and action to methods */
    enum ChangeLogEventType {
        group_addGroup {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl changeLogConsumerBaseImpl) {
                // does this event pertain to us? was the group or one of its parent folders marked for sync
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name);
                if (changeLogConsumerBaseImpl.groupMarkedForSync(groupName)) {
                    changeLogConsumerBaseImpl.addGroupInternal(changeLogEntry);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                }
            }
        };

        /**
         * Placeholder method for mapping category_action to methods.
         * @param changeLogEntry
         * @param changeLogConsumerBaseImpl
         */
        public abstract void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl changeLogConsumerBaseImpl);
    }

    // If syncAttribute was applied to group or one of the parent folders return true
    private boolean groupMarkedForSync(String groupName) {
        try {
            Group group = GroupFinder.findByName(GrouperSession.staticGrouperSession(false), groupName, true);
            boolean groupMarkedForSync = group.getAttributeDelegate().retrieveAssignments(syncAttribute).isEmpty();
            return groupMarkedForSync || shouldSyncFolder(group.getParentStem());

        } catch (GroupNotFoundException gnfe) {
            // Group gone missing before we had a chance to sync?
            LOG.debug("changeLog.consumer.'{}': grouper group {} removed before we had a chance to sync", consumerName, groupName);
            return false;
        }
    }

    // If syncAttribute applied to folder or any parent folder(s) return true
    private boolean shouldSyncFolder(Stem folder) {
        boolean folderMarkedForSync = folder.getAttributeDelegate().retrieveAssignments(syncAttribute).isEmpty();
        return folderMarkedForSync || shouldSyncFolder(folder.getParentStem());
    }


    private String consumerName;

    /** Name of marker attribute defined in changeLog.consumer.<consumerName>.syncAttributeName */
    private String syncAttributeName;
    private AttributeDefName syncAttribute;

    public static String ATTRIBUTE_CONFIG_FOLDER_NAME = "etc:attribute";
    public static String CONFIG_FOLDER_NAME = "changeLogConsumer";

    /** Property name for marker attribute defined in changeLog.consumer.<consumerName>.syncAttributeName */
    public static String SYNC_ATTRIBUTE_NAME = "syncAttributeName";




    /**
     *
     * @param changeLogEntry
     */
    private void addGroupInternal(ChangeLogEntry changeLogEntry) {
        LOG.debug("changeLog.consumer.'{}': processing group add for {}.", consumerName, changeLogEntry.toStringDeep());
        try {
            addGroup(changeLogEntry);
        } catch(Exception e) {
            LOG.error("changeLog.consumer.'{}': error processing group add for {}");
        }
    }

    /**
     * expected to be overidden
     * @param changeLogEntry
     */
    protected void addGroup(ChangeLogEntry changeLogEntry) {
        LOG.error("ChangeLogConsumerBaseImpl expects to subclassed.");
    }

    /**
     * expected to be overidden
     * @param consumerName
     * @return
     */
    protected boolean isFullSyncRunning(String consumerName) {
        return false;
    }


    /**
     * Refreshes consumer configuration from grouper-loader.properties each time the consumer is run
     *
     * @param consumerName
     */
    private void initProperties(String consumerName) {
        final String consumerConfigPrefix = "changeLog.consumer." + consumerName + ".";

        GrouperLoaderConfig config = GrouperLoaderConfig.retrieveConfig();

        LOG.debug("changeLog.consumer.{}: setting properties...", consumerName);

        syncAttributeName = config.propertyValueStringRequired(consumerConfigPrefix + SYNC_ATTRIBUTE_NAME);

    }

    /**
     * Process the list of changeLogEntries since the last time this consumer was run.
     *
     * @param changeLogEntryList
     * @param changeLogProcessorMetadata
     * @return last processed changeLogEntry Id
     */
    @Override
    public long processChangeLogEntries(List<ChangeLogEntry> changeLogEntryList, ChangeLogProcessorMetadata changeLogProcessorMetadata) {

        // Name of the consumer configured in grouper-loader.properties that extends this class
        // e.g. changeLog.consumer.<consumerName>.class = edu.example.changeLogConsumer
        if (consumerName == null) {
            consumerName = changeLogProcessorMetadata.getConsumerName();
        }

        // Get config for the consumer from changeLog.consumer.<consumerName>.*
        initProperties(consumerName);

        // syncAttribute name configured in grouper-loader.properties
        // e.g. changeLog.consumer.<consumerName>.<syncAttributeName> = o365
        if (syncAttribute == null) {
            syncAttribute = AttributeDefNameFinder.findByName(ATTRIBUTE_CONFIG_FOLDER_NAME + ":" + CONFIG_FOLDER_NAME + ":" + syncAttributeName, false);
            if (syncAttribute == null) {
                // missing sync attribute, so let's create it
                // first check for etc:attribute:changeLogConsumer, and create if missing
                Stem configFolder = StemFinder.findByName(GrouperSession.staticGrouperSession(), ATTRIBUTE_CONFIG_FOLDER_NAME + ":" + CONFIG_FOLDER_NAME, false);
                if (configFolder == null) {
                    final Stem etcAttributeFolder = StemFinder.findByName(GrouperSession.staticGrouperSession(), ATTRIBUTE_CONFIG_FOLDER_NAME, false);
                    configFolder = etcAttributeFolder.addChildStem(CONFIG_FOLDER_NAME, CONFIG_FOLDER_NAME);
                }
                // next check for attribute definition etc:attribute:changeLogConsumer:<syncAttributeName>AttributeDef, and create if missing
                AttributeDef syncAttrDef = AttributeDefFinder.findByName(syncAttributeName + "Def", false);
                if (syncAttrDef == null) {
                    LOG.info("changeLog.consumer.{}: - attribute definition not found for {}, creating it now", consumerName, syncAttributeName + "Def");
                    syncAttrDef = configFolder.addChildAttributeDef(syncAttributeName + "Def", AttributeDefType.attr);
                    syncAttrDef.setAssignToGroup(true);
                    syncAttrDef.setAssignToStem(true);
                    syncAttrDef.setMultiAssignable(true);
                    syncAttrDef.store();
                }

                // finally create the attribute etc:attribute:changeLogConsumer:<syncAttributeName>. This is the marker attribute for the consumer.
                syncAttribute = configFolder.addChildAttributeDefName(syncAttrDef, syncAttributeName, syncAttributeName);
                LOG.info("changeLog.consumer.{}:  attribute name created for {}", consumerName, syncAttributeName);
            }
        }

        // change log sequence to return. will be updated in grouper db to keep track of processing progress.
        long changeLogEntrySequenceNumber = -1;

        // ContextId groups multiple changeLogEntries into a logical change set
        // tracking for performance purposes. is this really needed?
        String currentChangeLogEntryContextId = null;

        for (ChangeLogEntry changeLogEntry : changeLogEntryList) {

            // TODO bail out as soon as we can determine this change doesn't apply to us
            // is the earliest we can tell?
            // only supporting provisioing attributes on groups and folders (i.e. groups within folders)?
            // capture all group and membership create/updates/delete for marked groups and folders
            // is this a group or membership event?

            changeLogEntrySequenceNumber = changeLogEntry.getSequenceNumber();

            // If full sync is running abort this run and try again later
            if (isFullSyncRunning(consumerName)) {
                return changeLogEntrySequenceNumber - 1;
            }

            // if first time through or new contextID, update the contextId
            if (currentChangeLogEntryContextId == null || !changeLogEntry.getContextId().equals(currentChangeLogEntryContextId)) {
                currentChangeLogEntryContextId = changeLogEntry.getContextId();
            }

            try {
                // process the changeLogEntry
                processChangeLogEntry(changeLogEntry);
            } catch (Exception e){
                String message = "changeLog.consumer." + consumerName + ": an error occurred processing sequence number " + changeLogEntrySequenceNumber;
                LOG.error(message, e);
                changeLogProcessorMetadata.registerProblem(e, message, changeLogEntrySequenceNumber);
                changeLogProcessorMetadata.setHadProblem(true);
                changeLogProcessorMetadata.setRecordException(e);
                changeLogProcessorMetadata.setRecordExceptionSequence(changeLogEntrySequenceNumber);
                // TODO then break on retryOnError = false? or just keep processing and let full sync clean it up?
            }

        }

        if (changeLogEntrySequenceNumber == -1) {
            throw new RuntimeException("changeLog.consumer." + consumerName + ": unable to process any records");
        }

        LOG.debug("changeLog.consumer.{}: finished processing changeLogEntryList. Last sequence number processed was {}", consumerName, changeLogEntrySequenceNumber);

        // the last changeLogEntrySequence processed
        return changeLogEntrySequenceNumber;
    }

    /**
     * Call the method of the {@link ChangeLogEventType} enum which matches the {@link ChangeLogEntry} category and action (i.e. the change log event type).
     *
     * @param changeLogEntry the change log entry
     */
    private void processChangeLogEntry(ChangeLogEntry changeLogEntry) {

        // construct key from changeLogEntry in the form of <category>_<action>
        final String changeLogEventTypeKey = changeLogEntry.getChangeLogType().getChangeLogCategory() + "_"
                + changeLogEntry.getChangeLogType().getActionName();

        ChangeLogEventType changeLogEventType = null;

        // look up method to map to call it
        try {
            changeLogEventType = ChangeLogEventType.valueOf(changeLogEventTypeKey);
            LOG.info("changeLog.consumer.{}: start processing {} for {}", new Object[]{consumerName, changeLogEventTypeKey, changeLogEntry.toStringDeep()});
            changeLogEventType.process(changeLogEntry, this);
            LOG.info("changeLog.consumer.{}: end processing {} for {}", new Object[]{consumerName, changeLogEventTypeKey, changeLogEntry.toStringDeep()});
        } catch (IllegalArgumentException e) {
            LOG.debug("changeLog.consumer.{}: unsupported {} for {}", new Object[]{consumerName, changeLogEventTypeKey, changeLogEntry.toStringDeep()});
        }
    }

    // TODO implement change log type mapping to methods
    // TODO in implement class override methods for specific functionality?

}
