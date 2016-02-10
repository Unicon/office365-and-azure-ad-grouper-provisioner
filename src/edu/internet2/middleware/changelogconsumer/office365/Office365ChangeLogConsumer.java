package edu.internet2.middleware.changelogconsumer.office365;

import edu.internet2.middleware.grouper.changeLog.ChangeLogConsumerBase;
import edu.internet2.middleware.grouper.changeLog.ChangeLogEntry;

/**
 * A {@link ChangeLogConsumer} to provision groups for Office 365.
 *
 * @author Bill Thompson, Unicon
 **/
public class Office365ChangeLogConsumer extends ChangeLogConsumerBase {

    /** Maps change log entry category and action (change log type) to methods. */
    enum EventType {

        /** Process the add attribute assign value change log entry type. */
        attributeAssign__addAttributeAssign {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processAttributeAssignAdd(consumer, changeLogEntry);
            }
        },

        /** Process the delete attribute assign value change log entry type. */
        attributeAssign__deleteAttributeAssign {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) {
                consumer.processAttributeAssignDelete(consumer, changeLogEntry);
            }
        },

        /** Process the add group change log entry type. */
        group__addGroup {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processGroupAdd(consumer, changeLogEntry);
            }
        },

        /** Process the delete group change log entry type. */
        group__deleteGroup {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processGroupDelete(consumer, changeLogEntry);
            }
        },

        /** Process the update group change log entry type. */
        group__updateGroup {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processGroupUpdate(consumer, changeLogEntry);
            }
        },

        /** Process the add membership change log entry type. */
        membership__addMembership {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processMembershipAdd(consumer, changeLogEntry);
            }
        },

        /** Process the delete membership change log entry type. */
        membership__deleteMembership {
            /** {@inheritDoc} */
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processMembershipDelete(consumer, changeLogEntry);
            }
        },

        privilege__addPrivilege {
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processPrivilegeAdd(consumer, changeLogEntry);
            }
        },

        privilege__deletePrivilege {
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processPrivilegeDelete(consumer, changeLogEntry);
            }
        },

        privilege__updatePrivilege {
            public void process(Office365ChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processPrivilegeUpdate(consumer, changeLogEntry);
            }
        },

        /** Process the delete stem change log entry type. */
        stem__deleteStem {
            /** {@inheritDoc} */
            public void process(GoogleAppsChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception {
                consumer.processStemDelete(consumer, changeLogEntry);
            }
        },
        ;

        /**
         * Process the change log entry.
         *
         * @param consumer the google change log consumer
         * @param changeLogEntry the change log entry
         * @throws Exception if any error occurs
         */
        public abstract void process(GoogleAppsChangeLogConsumer consumer, ChangeLogEntry changeLogEntry) throws Exception;
    }

    private static final Logger LOG = LoggerFactory.getLogger(Office365ChangeLogConsumer.class);

    /** The change log consumer name from the processor metadata. */
    private String consumerName;
    private AttributeDefName syncAttribute;
    private GoogleGrouperConnector connector;

    public Office365ChangeLogConsumer() {
        LOG.trace("Office365ChangeLogConsumer - new instance starting up");
        // connector = new Office365GrouperConnector();
    }

    /** {@inheritDoc} */
    @Override
    public long processChangeLogEntries(final List<ChangeLogEntry> changeLogEntryList,
                                        ChangeLogProcessorMetadata changeLogProcessorMetadata) {

        LOG.debug("Office365ChangeLogConsumer - waking up");

        // the change log sequence number to return
        long sequenceNumber = -1;

        // initialize this consumer's consumerName from the change log metadata
        if (consumerName == null) {
            consumerName = changeLogProcessorMetadata.getConsumerName();
            LOG.trace("Office365ChangeLogConsumer '{}' - Setting name.", consumerName);
        }

        // TODO
        /**
        GoogleAppsSyncProperties properties = new GoogleAppsSyncProperties(consumerName);

        try {
            connector.initialize(consumerName, properties);

            if (properties.getprefillGoogleCachesForConsumer()) {
                connector.populateGoogleCache();
            }

        } catch (Exception e) {
            LOG.error("Google Apps Consumer '{}' - This consumer failed to initialize: {}", consumerName, e.getMessage());
            return changeLogEntryList.get(0).getSequenceNumber() - 1;
        }
        **/

        GrouperSession grouperSession = null;
        try {

            grouperSession = GrouperSession.startRootSession();
            // TODO syncAttribute = connector.getGoogleSyncAttribute();
            // TODO connector.cacheSyncedGroupsAndStems();

            // time context processing
            final StopWatch stopWatch = new StopWatch();

            // the last change log sequence number processed
            String lastContextId = null;

            LOG.debug("Office365ChangeLogConsumer '{}' - Processing change log entry list size '{}'", consumerName, changeLogEntryList.size());

            // process each change log entry
            for (ChangeLogEntry changeLogEntry : changeLogEntryList) {

                // return the current change log sequence number
                sequenceNumber = changeLogEntry.getSequenceNumber();

                // if full sync is running, return the previous sequence number to process this entry on the next run
                // TODO
                /** boolean isFullSyncRunning = GoogleAppsFullSync.isFullSyncRunning(consumerName);

                if (isFullSyncRunning) {
                    LOG.info("Google Apps Consumer '{}' - Full sync is running, returning sequence number '{}'", consumerName,
                            sequenceNumber - 1);
                    return sequenceNumber - 1;
                }
                 */

                // if first run, start the stop watch and store the last sequence number
                if (lastContextId == null) {
                    stopWatch.start();
                    lastContextId = changeLogEntry.getContextId();
                }

                // whether or not an exception was thrown during processing of the change log entry
                boolean errorOccurred = false;

                try {
                    // process the change log entry
                    processChangeLogEntry(changeLogEntry);

                } catch (Exception e) {
                    errorOccurred = true;
                    String message =
                            "Office365ChangeLogConsumer '" + consumerName + "' - An error occurred processing sequence number " + sequenceNumber;
                    LOG.error(message, e);
                    changeLogProcessorMetadata.registerProblem(e, message, sequenceNumber);
                    changeLogProcessorMetadata.setHadProblem(true);
                    changeLogProcessorMetadata.setRecordException(e);
                    changeLogProcessorMetadata.setRecordExceptionSequence(sequenceNumber);
                }

                // if the change log context id has changed, log and restart stop watch
                if (!lastContextId.equals(changeLogEntry.getContextId())) {
                    stopWatch.stop();
                    LOG.debug("Office365ChangeLogConsumer '{}' - Processed change log context '{}' Elapsed time {}", new Object[] {consumerName,
                            lastContextId, stopWatch,});
                    stopWatch.reset();
                    stopWatch.start();
                }

                lastContextId = changeLogEntry.getContextId();

                // if an error occurs and retry on error is true, return the current sequence number minus 1
                /* Whether or not to retry a change log entry if an error occurs. */
                boolean retryOnError = properties.isRetryOnError();
                if (errorOccurred && retryOnError) {
                    sequenceNumber--;
                    break;
                }
            }

            // stop the timer and log
            stopWatch.stop();
            LOG.debug("Office365ChangeLogConsumer '{}' - Processed change log context '{}' Elapsed time {}", new Object[] {consumerName,
                    lastContextId, stopWatch,});

        } finally {
            GrouperSession.stopQuietly(grouperSession);
        }

        if (sequenceNumber == -1) {
            LOG.error("Office365ChangeLogConsumer '" + consumerName + "' - Unable to process any records.");
            throw new RuntimeException("Office365ChangeLogConsumer '" + consumerName + "' - Unable to process any records.");
        }

        LOG.debug("Office365ChangeLogConsumer '{}' - Finished processing change log entries. Last sequence number '{}'", consumerName,
                sequenceNumber);

        // return the sequence number
        return sequenceNumber;
    }

    /**
     * Call the method of the {@link EventType} enum which matches the {@link ChangeLogEntry} category and action (the
     * change log type).
     *
     * @param changeLogEntry the change log entry
     * @throws Exception if an error occurs processing the change log entry
     */
    protected void processChangeLogEntry(ChangeLogEntry changeLogEntry) throws Exception {
        try {
            // find the method to run via the enum
            final String enumKey = changeLogEntry.getChangeLogType().getChangeLogCategory() + "__"
                    + changeLogEntry.getChangeLogType().getActionName();

            final EventType eventType = EventType.valueOf(enumKey);

            if (eventType == null) {
                LOG.debug("Office365ChangeLogConsumer '{}' - Change log entry '{}' Unsupported category and action.", consumerName,
                        toString(changeLogEntry));
            } else {
                // process the change log event
                LOG.info("Office365ChangeLogConsumer '{}' - Change log entry '{}'", consumerName, toStringDeep(changeLogEntry));
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();

                eventType.process(this, changeLogEntry);

                stopWatch.stop();
                LOG.info("Office365ChangeLogConsumer '{}' - Change log entry '{}' Finished processing. Elapsed time {}",
                        new Object[] {consumerName, toString(changeLogEntry), stopWatch,});

            }

        } catch (IllegalArgumentException e) {
            LOG.debug("Office365ChangeLogConsumer '{}' - Change log entry '{}' Unsupported category and action.", consumerName,
                    toString(changeLogEntry));
        }
    }



}
