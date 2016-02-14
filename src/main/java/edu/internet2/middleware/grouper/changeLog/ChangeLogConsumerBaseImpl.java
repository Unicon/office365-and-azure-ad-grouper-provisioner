package edu.internet2.middleware.grouper.changeLog;

import edu.internet2.middleware.grouper.*;
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
    enum changeLogEventType {
        group_addGroup {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl changeLogConsumerBaseImpl) throws Exception {
                // does this event pertain to us? was the group or one of its parent folders marked for sync
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name);
                if (changeLogConsumerBaseImpl.groupMarkedForSync(groupName)) {
                    changeLogConsumerBaseImpl.addGroupInternal(changeLogEntry);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                }
            }
        }
    }

    // If syncAttribute was applied to group or one of the parent folders return true
    private boolean groupMarkedForSync(String groupName) {
        try {
            Group group = GroupFinder.findByName(GrouperSession.staticGrouperSession(false), groupName, true);
            boolean groupMarkedForSync = group.getAttributeDelegate().retrieveAssignments(syncAttribute).isEmpty();
            return groupMarkedForSync || shouldSyncFolder(group.getParentStem());

        } catch (GroupNotFoundException gnfe) {
            // Group gone missing before we had a chance to sync
            LOG.debug("changeLog.consumer.'{}': grouper group {} removed before we had a chance to sync", consumerName, groupName);
            return false;
        }
    }

    // If syncAttribute applied to folder or parent folder(s) return true
    private boolean shouldSyncFolder(Stem folder) {
        boolean folderMarkedForSync = folder.getAttributeDelegate().retrieveAssignments(syncAttribute).isEmpty();
        return folderMarkedForSync || shouldSyncFolder(folder.getParentStem());
    }


    private String consumerName;

    private AttributeDefName syncAttribute;

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

        // syncAttribute name configured in grouper-loader.properties
        // e.g. changeLog.consumer.<consumerName>.<syncAttributeName> = o365
        if (syncAttribute == null) {
            AttributeDefName attrDefName = AttributeDefNameFinder.findByName(consumerName + SYNC_ATTRIBUTE_NAME, false);
            if (attrDefName == null) {
                // missing sync attribute, so let's create it
                Stem syncConfigFolder = StemFinder.findByName(GrouperSession.staticGrouperSession(), SYNC_CONFIG_FOLDER);
                if (syncConfigFolder == null) {
                    final Stem etcAttributeStem = StemFinder.findByName(GrouperSession.staticGrouperSession(), ATTRIBUTE_CONFIG_STEM, false);
                    googleStem = etcAttributeStem.addChildStem(GOOGLE_PROVISIONER, GOOGLE_PROVISIONER);
                }

                AttributeDef syncAttrDef = AttributeDefFinder.findByName(SYNC_TO_GOOGLE_NAME + "Def", false);
                if (syncAttrDef == null) {
                    LOG.info("Google Apps Consumer '{}' - {} AttributeDef not found, creating it now", consumerName, SYNC_TO_GOOGLE + "Def");
                    syncAttrDef = googleStem.addChildAttributeDef(SYNC_TO_GOOGLE + "Def", AttributeDefType.attr);
                    syncAttrDef.setAssignToGroup(true);
                    syncAttrDef.setAssignToStem(true);
                    syncAttrDef.setMultiAssignable(true);
                    syncAttrDef.store();
                }

                LOG.info("Google Apps Consumer '{}' - {} attribute not found, creating it now", consumerName, SYNC_TO_GOOGLE_NAME + consumerName);
                attrDefName = googleStem.addChildAttributeDefName(syncAttrDef, SYNC_TO_GOOGLE + consumerName, SYNC_TO_GOOGLE + consumerName);
            }

            syncAttribute = attrDefName;

            return attrDefName;
                }
            }

        }

        // initConnector(consumerName)?

        try {
            for (ChangeLogEntry changeLogEntry : changeLogEntryList) {

                // TODO bail out as soon as we can determine this change doesn't apply to us
                // is the earliest we can tell?
                // only supporting provisioing attributes on groups and folders (i.e. groups within folders)?
                // capture all group and membership create/updates/delete for marked groups and folders

                // is this a group or membership event?



            }
        } catch (Exception e) {

        }


        long lastProcessedChangeLogEntryId = -1;










        return 0;
    }

    // TODO implement change log type mapping to methods
    // TODO in implement class override methods for specific functionality?
    // TODO or some poor man's injections for composing functionality?

}
