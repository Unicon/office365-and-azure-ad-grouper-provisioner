package edu.internet2.middleware.grouper.changeLog.consumer;

import edu.internet2.middleware.grouper.Group;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBaseImpl;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabels;
import edu.internet2.middleware.grouper.changeLog.provisioning.GroupAndMembershipChangeLogConsumer;
import edu.internet2.middleware.grouper.pit.PITGroup;
import edu.internet2.middleware.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Example change log consumer based on ChangeLogConsumerBaseImpl. ChangeLogConsumerBaseImpl handles
 * the mapping of change log event to methods, the processing loop, and exception handling.
 */
public class PrintChangeLogConsumer extends ChangeLogConsumerBaseImpl {

    private static final Logger LOG = LoggerFactory.getLogger(PrintChangeLogConsumer.class);

    @Override
    protected void addGroup(Group group, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer) {
        // final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name);
        LOG.debug("{} add group {}.", consumer.getConsumerName(), group.getName());
    }

    @Override
    protected void addGroupAndMemberships(Group group, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer){
        // changeLogEntry type is attributeAssign_addAttributeAssign on group or folder
        LOG.debug("{} add group {} and memberships.", new Object[] {consumer.getConsumerName(), group.getName()});
    }

    @Override
    protected void updateGroup(Group group, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer){
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.name);
        LOG.debug("{} update group {}.", consumer.getConsumerName(), group.getName());
    }

    @Override
    protected void removeGroup(Group group, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer){
        // changeLogEntry type could be attribute_assignDelete or group_delete
        LOG.debug("{} remove group {} per change log entry {}.", new Object[] {consumer.getConsumerName(), group.getName(), changeLogEntry.getSequenceNumber()});
    }

    @Override
    protected void removeDeletedGroup(PITGroup pitGroup, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer){
        // changeLogEntry type could be attribute_assignDelete or group_delete
        LOG.debug("{} remove deleted group {} per change log entry {}.", new Object[] {consumer.getConsumerName(), pitGroup.getName(), changeLogEntry.getSequenceNumber()});
    }

    @Override
    protected void addMembership(Subject subject, Group group, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer){
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_ADD.groupName);
        final String subjectId = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_ADD.subjectId);
        // Group is marked for sync, but does it exist already in target? if not create it.
        LOG.debug("{} add subject {} to group {}.", new Object[] {consumer.getConsumerName(), subject.getName(), group.getName()});
    }

    @Override
    protected void removeMembership(Subject subject, Group group, ChangeLogEntry changeLogEntry, ChangeLogConsumerBaseImpl consumer){
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_DELETE.groupName);
        final String subjectId = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.MEMBERSHIP_DELETE.subjectId);
        LOG.debug("{} remove subject {} from group {}.", new Object[] {consumer.getConsumerName(), subject.getName(), group.getName()});
    }

    @Override
    protected boolean isFullSyncRunning(String consumerName){
        // TODO stub out fullsycn and check to see if running
        return false;
    }

}
