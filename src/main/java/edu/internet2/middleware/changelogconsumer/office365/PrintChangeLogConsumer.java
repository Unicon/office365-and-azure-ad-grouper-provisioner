package edu.internet2.middleware.changelogconsumer.office365;

import java.util.List;
import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBase;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;
import edu.internet2.middleware.grouper.changeLog.ChangeLogLabels;
import edu.internet2.middleware.grouper.changeLog.ChangeLogProcessorMetadata;
import edu.internet2.middleware.grouper.changeLog.ChangeLogTypeBuiltin;


/**
 * just print out some of the events
 */
public class PrintChangeLogConsumer extends ChangeLogConsumerBase {

    @Override
    public long processChangeLogEntries(List<ChangeLogEntry> changeLogEntryList, ChangeLogProcessorMetadata changeLogProcessorMetadata) {

        // sequence number for ChangeLogEntry
        // ChangeLogConsumerBase expects us to return the sequence number of the last processed ChangeLogEntry
        long changeLogEntryId = -1;

        //try catch so we can track that we made some progress
        try {
            for (ChangeLogEntry changeLogEntry : changeLogEntryList) {

                changeLogEntryId = changeLogEntry.getSequenceNumber();

                //if this is a group add action and category
                if (changeLogEntry.equalsCategoryAndAction(ChangeLogTypeBuiltin.GROUP_ADD)) {

                    //print the name from the entry
                    System.out.println("Group add, name: "
                            + changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_ADD.name));
                }

                if (changeLogEntry.equalsCategoryAndAction(ChangeLogTypeBuiltin.GROUP_DELETE)) {

                    //print the name from the entry
                    System.out.println("Group delete, name: "
                            + changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_DELETE.name));
                }

                if (changeLogEntry.equalsCategoryAndAction(ChangeLogTypeBuiltin.GROUP_UPDATE)) {

                    //print the name from the entry
                    System.out.println("Group update, name: "
                            + changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.name)
                            + ", property: " + changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.propertyChanged)
                            + ", from: '" + changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.propertyOldValue)
                            + "', to: '" + changeLogEntry.retrieveValueForLabel(ChangeLogLabels.GROUP_UPDATE.propertyNewValue) + "'");
                }

                //we successfully processed this record
            }
        } catch (Exception e) {
            changeLogProcessorMetadata.registerProblem(e, "Error processing record", changeLogEntryId);
            //we made it to this -1
            return changeLogEntryId-1;
        }
        if (changeLogEntryId == -1) {
            throw new RuntimeException("Couldn't process any records");
        }

        return changeLogEntryId;
    }

}