package edu.internet2.middleware.grouper.changeLog.consumer;

import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBaseImpl;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by thompsow on 2/14/16.
 */
public class PrintChangeLogConsumer extends ChangeLogConsumerBaseImpl {

    private static final Logger LOG = LoggerFactory.getLogger(PrintChangeLogConsumer.class);

    @Override
    protected void addGroup(ChangeLogEntry changeLogEntry) {
        final String groupName = changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name);
        LOG.debug("changeLog.consumer.{}: addGroup {}", consumerName, groupName);
        // final edu.internet2.middleware.grouper.Group group = connector.fetchGrouperGroup(groupName);
    }

}
