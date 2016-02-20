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
import edu.internet2.middleware.grouper.pit.PITAttributeAssign;
import edu.internet2.middleware.grouper.pit.PITAttributeDefName;
import edu.internet2.middleware.grouper.pit.PITGroup;
import edu.internet2.middleware.grouper.pit.finder.PITAttributeAssignFinder;
import edu.internet2.middleware.grouper.pit.finder.PITAttributeDefNameFinder;
import edu.internet2.middleware.grouper.pit.finder.PITGroupFinder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Default @ChangeLogConsumerBase implementation. This class gets instantiated by grouper
 * for every run of processChangeLogEntries().
 */
public class ChangeLogConsumerBaseImpl extends ChangeLogConsumerBase {

    private static final Logger LOG = LoggerFactory.getLogger(ChangeLogConsumerBaseImpl.class);

    /**
     * Cache of previously seen grouper folders and groups names and if they are marked for provisioning.
     * folder or group name, marked or not marked
     **/
    private HashMap<String, String> markedFoldersAndGroups = new HashMap<String, String>(256);
    private static final String MARKED = "marked";
    private static final String NOT_MARKED = "not marked";
    private static final String FOLDER = "stem";
    private static final String GROUP = "group";

    /** Maps supported changeLogEntry category and action to methods */
    enum ChangeLogEventType {
        group_addGroup {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                // does this event pertain to us? was the group or one of its parent folders marked for sync
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name);
                if (consumer.isGroupMarkedForSync(groupName)) {
                    consumer.addGroup(changeLogEntry, consumer.consumerName);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                    LOG.debug("{} skipping addGroup since {} is not marked for sync", consumer.consumerName, groupName);
                }
            }
        },
        group_updateGroup {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.name);
                if (consumer.isGroupMarkedForSync(groupName)) {
                    consumer.updateGroup(changeLogEntry, consumer.consumerName);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                    LOG.debug("{} skipping updateGroup since {} is not marked for sync", consumer.consumerName, groupName);
                }
            }
        },
        group_deleteGroup {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_DELETE.name);
                if (consumer.isGroupMarkedForSync(groupName)) {
                    // case when marker is on parent folder of deleted group
                    consumer.deleteGroup(groupName, changeLogEntry, consumer.consumerName);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                    LOG.debug("{} skipping deleteGroup since {} is not marked for sync", consumer.consumerName, groupName);
                }
            }
        },
        membership_addMembership {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_ADD.groupName);
                if (consumer.isGroupMarkedForSync(groupName)) {
                    consumer.addMembership(changeLogEntry, consumer.consumerName);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                    LOG.debug("{} skipping addMembership since {} is not marked for sync", consumer.consumerName, groupName);
                }
            }
        },
        membership_deleteMembership {
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_DELETE.groupName);
                if (consumer.isGroupMarkedForSync(groupName)) {
                    consumer.deleteMembership(changeLogEntry, consumer.consumerName);
                } else {
                    // skipping changeLogEntry that doesn't pertain to us
                    LOG.debug("{} skipping deleteMembership since {} is not marked for sync", consumer.consumerName, groupName);
                }
            }
        },
        attributeAssign_addAttributeAssign {
            // on assignment of syncAttribute marker, create all the groups or group (if directly assigned), and add any memberships
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                // is this our syncAttribute?
                final String attributeDefNameName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.ATTRIBUTE_ASSIGN_ADD.attributeDefNameName);
                if (consumer.syncAttribute.getName().equals(attributeDefNameName)) {
                    // is it for a group? then create the group at the target
                    String assignType = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.ATTRIBUTE_ASSIGN_ADD.assignType);
                    String ownerId1 = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.ATTRIBUTE_ASSIGN_ADD.ownerId1);
                    if (GROUP.equals(assignType)){
                        Group markedGroup = GroupFinder.findByUuid(GrouperSession.staticGrouperSession(false), ownerId1, false );
                        if (markedGroup != null) {
                            consumer.createGroupAndMemberships(markedGroup, changeLogEntry, consumer.consumerName);
                        } // couldn't find group, already deleted?
                    } else if (FOLDER.equals(assignType)){
                        Stem markedFolder = StemFinder.findByUuid(GrouperSession.staticGrouperSession(false), ownerId1, false);
                        if (markedFolder != null) {
                            // get all the groups below this folder and sub folders and create them at the target
                            Set<Group> markedGroups = markedFolder.getChildGroups(Stem.Scope.SUB);
                            for( Group group : markedGroups) {
                                consumer.createGroupAndMemberships(group, changeLogEntry, consumer.consumerName);
                            }
                        } // couldn't find folder, already deleted?
                    }
                }
            }
        },
        attributeAssign_deleteAttributeAssign {
            /**
             * On the removal of the syncAttribute marker, delete all the groups or group (if directly assigned) at the target, unless
             * otherwise still marked by direct assignment or a parent folder.
             */
            public void process(ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
                // is this our syncAttribute? otherwise nothing to do.
                final String attributeDefNameName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.ATTRIBUTE_ASSIGN_DELETE.attributeDefNameName);
                if (consumer.syncAttribute.getName().equals(attributeDefNameName)) {
                    String assignType = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.ATTRIBUTE_ASSIGN_DELETE.assignType);
                    String ownerId1 = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.ATTRIBUTE_ASSIGN_DELETE.ownerId1);
                    // is it for a group? then delete the group at the target, unless a parent folder is also marked.
                    if (GROUP.equals(assignType)) {
                        // get the group and then check for a marked parent folder before deleting at target.
                        Group group = GroupFinder.findByUuid(GrouperSession.staticGrouperSession(false), ownerId1, false);
                        if (group != null){
                            // case when group had a direct syncAttribute marker assignment removed, does it still have a parent marker?
                            if (group.getAttributeDelegate().hasAttributeOrAncestorHasAttribute(consumer.syncAttribute.getName(), false)) {
                                LOG.debug("{} processed deleteAttributeAssign for group {}, but still marked by a parent folder.", consumer.consumerName, group.getName());
                            } else {
                                // marker syncAttribute removed from group and no other parent folder marked so delete at target
                                LOG.debug("{} processed deleteAttributeAssign for group {}, no other mark found so calling deleteGroup()", consumer.consumerName, group.getName());
                                consumer.deleteGroup(group.getName(), changeLogEntry, consumer.consumerName);
                            }
                        } else {
                            // case when a group which had a direct syncAttribute marker was deleted, always delete at target
                            PITGroup pitGroup = PITGroupFinder.findBySourceId(ownerId1, false).iterator().next();
                            if (pitGroup != null) {
                                String pitGroupName = pitGroup.getName();
                                // marker syncAttribute removed when deleting a group, always delete at target
                                LOG.debug("{} processed deleteAttributeAssign for deleted group {}, calling deleteGroup()", consumer.consumerName, pitGroupName);
                                consumer.deleteGroup(pitGroupName, changeLogEntry, consumer.consumerName);
                            } else {
                                // couldn't find group anywhere? so can't determine its name.
                                LOG.error("{} failed to find group when processing deleteAttributeAssign so can't determine the group name, let fullSync sort it out.", consumer.consumerName);
                            }
                        }
                    } else if (FOLDER.equals(assignType)) {
                        // is it a folder, then delete all the containing groups at the target, unless they are still marked otherwise (direct or indirect)
                        Stem unMarkedFolder = StemFinder.findByUuid(GrouperSession.staticGrouperSession(false), ownerId1, false);
                        if (unMarkedFolder != null) {
                            // get all the groups below this folder and sub folders and to see if they are still marked, otherwise delete them at the target
                            Set<Group> unMarkedGroups = unMarkedFolder.getChildGroups(Stem.Scope.SUB);
                            for (Group group : unMarkedGroups) {
                                // check that the group isn't marked directly or from some other parent folder
                                if (!consumer.isGroupMarkedForSync(group.getName())) {
                                    LOG.debug("{} processed deleteAttributeAssign for folder {}, no other mark found for {} so calling deleteGroup({})", new Object[]{consumer.consumerName, unMarkedFolder.getName(), group.getName(), group.getName()});
                                    consumer.deleteGroup(group.getName(), changeLogEntry, consumer.consumerName);
                                } else {
                                    LOG.debug("{} processed deleteAttributeAssign for folder {}, found mark for group {} so nothing to do.", new Object[]{consumer.consumerName, unMarkedFolder.getName(), group.getName()});
                                }
                            }
                        } else {
                            // couldn't find folder, already deleted? let fullSync sort it out.
                            // shouldn't get here...can't delete a folder without first deleting child objects, so there should be nothing to delete at the target
                            LOG.error("{} error processing deleteAttributeAssign for folder {}, let fullSync sort it out.", consumer.consumerName, unMarkedFolder.getName());
                        }
                    }
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


    /**
     * These methods are expected to be overriden in a subclass that is specific to a provisioning target. (e.g. Google Apps)
     */
    protected void addGroup(ChangeLogEntry changeLogEntry, String consumerName) {
        LOG.debug("{} addGroup dispatched but not implemented in subclass.", consumerName);
    }

    protected void updateGroup(ChangeLogEntry changeLogEntry, String consumerName) {
        LOG.debug("{} updateGroup dispatched but not implemented in subclass.", consumerName);
    }

    protected void deleteGroup(String groupName, ChangeLogEntry changeLogEntry, String consumerName) {
        LOG.debug("{} deleteGroup dispatched but not implemented in subclass.", consumerName);
    }

    protected void addMembership(ChangeLogEntry changeLogEntry, String consumerName) {
        LOG.debug("{} addMembership dispatched but not implemented in subclass.", consumerName);
    }

    protected void deleteMembership(ChangeLogEntry changeLogEntry, String consumerName) {
        LOG.debug("{} dispatched deleteMembership, but not implemented in subclass.", consumerName);
    }

    protected void createGroupAndMemberships(Group group, ChangeLogEntry changeLogEntry, String consumerName){
        LOG.debug("{} dispatched createGroupAndMemberships for {}, but method not implemented in subclass.", consumerName, group);
    }


    /**
     * If syncAttribute was applied to the group or one of the parent folders return true
     * Method keeps an internal cache of results per run in markedFoldersAndGroups
     * Will also check the PIT for recently deleted groups
     */
    private boolean isGroupMarkedForSync(String groupName) {

        // have we seen this group already in this run
        if (markedFoldersAndGroups.containsKey(groupName)) {
            return markedFoldersAndGroups.get(groupName).equals(MARKED);
        }

        boolean markedForSync;

        // looking for the group
        final Group group = GroupFinder.findByName(GrouperSession.staticGrouperSession(false), groupName, false);

        if (group != null) {
            // is it marked with the syncAttribute?
            markedForSync = group.getAttributeDelegate().hasAttributeOrAncestorHasAttribute(syncAttribute.getName(), false);
        } else {
            // looking for the deleted group in the PIT
            PITGroup pitGroup = PITGroupFinder.findMostRecentByName(groupName, false);
            if (pitGroup != null) {
                // looking for syncAttribute assignment in the PIT
                Set<PITAttributeDefName> pitSyncAttributes = PITAttributeDefNameFinder.findByName(syncAttribute.getName(), false, true);
                PITAttributeDefName pitSyncAttribute = pitSyncAttributes.iterator().next();
                Set<PITAttributeAssign> pitAttributeAssigns = PITAttributeAssignFinder.findByOwnerPITGroupAndPITAttributeDefName(pitGroup, pitSyncAttribute, pitGroup.getStartTime(), pitGroup.getEndTime());
                markedForSync = pitAttributeAssigns.isEmpty();
            } else {
                // couldn't find group anywhere including the PIT
                LOG.debug("{} checking for {} marker, but could not find group {} anywhere, including the PIT.", new Object[]{consumerName, syncAttributeName, groupName});
                markedForSync = false;
            }
        }

        // remember this for next time
        if(markedForSync) {
            markedFoldersAndGroups.put(groupName, MARKED);
            return true;
        } else {
            markedFoldersAndGroups.put(groupName, NOT_MARKED);
            return false;
        }
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
     * expected to be overidden
     * @param consumerName
     * @return
     */
    protected boolean isFullSyncRunning(String consumerName) {
        return false;
    }

    /**
     * Process the list of changeLogEntries since the last time this consumer was run.
     * This method will be called by grouper daemon (aka grouper loader).
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
            consumerName = "changeLog.consumer." + changeLogProcessorMetadata.getConsumerName();
        }


        GrouperLoaderConfig config = GrouperLoaderConfig.retrieveConfig();
        syncAttributeName = config.propertyValueStringRequired(consumerName + "." + SYNC_ATTRIBUTE_NAME);

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
                    LOG.info("{} attribute definition {} not found, creating it now.", consumerName, syncAttributeName + "Def");
                    syncAttrDef = configFolder.addChildAttributeDef(syncAttributeName + "Def", AttributeDefType.attr);
                    syncAttrDef.setAssignToGroup(true);
                    syncAttrDef.setAssignToStem(true);
                    syncAttrDef.setMultiAssignable(true);
                    syncAttrDef.store();
                }

                // finally create the attribute etc:attribute:changeLogConsumer:<syncAttributeName>. This is the marker attribute for the consumer.
                syncAttribute = configFolder.addChildAttributeDefName(syncAttrDef, syncAttributeName, syncAttributeName);
                LOG.info("{} created attribute name {}.", consumerName, syncAttributeName);
            }
        }

        // change log sequence to return. will be updated in grouper db to keep track of processing progress.
        long changeLogEntrySequenceNumber = -1;

        // ContextId groups multiple changeLogEntries into a logical change set.
        // Tracking this for performance purposes. is this really needed?
        String currentChangeLogEntryContextId = null;

        LOG.debug("{} ** starting processing run at change log entry {} ** ", consumerName, changeLogEntryList.get(0).getSequenceNumber());
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
                String message = consumerName + " threw an exception processing change log entry sequence number " + changeLogEntrySequenceNumber + ".";
                LOG.error(message, e);
                changeLogProcessorMetadata.registerProblem(e, message, changeLogEntrySequenceNumber);
                changeLogProcessorMetadata.setHadProblem(true);
                changeLogProcessorMetadata.setRecordException(e);
                changeLogProcessorMetadata.setRecordExceptionSequence(changeLogEntrySequenceNumber);
                // TODO then return previous seq number on retryOnError = true?
                // or just log and keep processing and let full sync clean it up?
            }

        }

        // TODO how would we get here?
        if (changeLogEntrySequenceNumber == -1) {
            throw new RuntimeException(consumerName + "was unable to dispatch any records.");
        }

        LOG.info("{} ** finished processing run at change log entry {} **", consumerName, changeLogEntrySequenceNumber);

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
            LOG.debug("{} dispatching change log event {} for change log {}.", new Object[]{consumerName, changeLogEventTypeKey, changeLogEntry.getSequenceNumber()});
            changeLogEventType.process(changeLogEntry, this);
        } catch (IllegalArgumentException e) {
            LOG.debug("{} unsupported event {} in change log {}.", new Object[]{consumerName, changeLogEventTypeKey, changeLogEntry.getSequenceNumber()});
        }
    }

    // TODO implement change log type mapping to methods
    // TODO in implement class override methods for specific functionality?

}
