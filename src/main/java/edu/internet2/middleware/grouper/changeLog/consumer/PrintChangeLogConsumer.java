package edu.internet2.middleware.grouper.changeLog.consumer;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBaseImpl;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example change log consumer based on ChangeLogConsumerBaseImpl. ChangeLogConsumerBaseImpl handles
 * the mapping of change log event to methods, the processing loop, and exception handling.
 */
public class PrintChangeLogConsumer extends ChangeLogConsumerBaseImpl {

    private static final Logger LOG = LoggerFactory.getLogger(PrintChangeLogConsumer.class);

    @Override
    protected void addGroup(ChangeLogEntry changeLogEntry, String consumerName) {
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name);
        LOG.debug("{} add group {}.", consumerName, groupName);
        // final edu.internet2.middleware.grouper.Group group = connector.fetchGrouperGroup(groupName);
    }

    @Override
    protected void updateGroup(ChangeLogEntry changeLogEntry, String consumerName){
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.name);
        LOG.debug("{} update group {}.", consumerName, groupName);
    }

    @Override
    protected void deleteGroup(String groupName, ChangeLogEntry changeLogEntry, String consumerName){
        // changeLogEntry type could be attribute_assignDelete or group_delete
        LOG.debug("{} delete group {} per change log entry {}.", new Object[] {consumerName, groupName, changeLogEntry.getSequenceNumber()});
    }

    @Override
    protected void addMembership(ChangeLogEntry changeLogEntry, String consumerName){
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_ADD.groupName);
        final String subjectId = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_ADD.subjectId);
        // Group is marked for sync, but does it exist already in target? if not create it.
        LOG.debug("{} add {} to group {}.", new Object[] {consumerName, subjectId, groupName});
    }

    @Override
    protected void deleteMembership(ChangeLogEntry changeLogEntry, String consumerName){
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_DELETE.groupName);
        final String subjectId = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_DELETE.subjectId);
        LOG.debug("{} remove {} from group {}.", new Object[] {consumerName, subjectId, groupName});
    }

}
