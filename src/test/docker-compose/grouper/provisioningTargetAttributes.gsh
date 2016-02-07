// Start the grouper session...never gets old!?
gs = GrouperSession.startRootSession();

// prime affiliation groups
affiliationLoader = GroupFinder.findByName(gs, "loader:affiliationLoader", true);
loaderRunOneJob(affiliationLoader);

// Get the Google Sync Marker Attribute for changelog.consumer.google.*
googleSyncAttr = AttributeDefNameFinder.findByName("etc:attribute:googleProvisioner:syncToGooglegoogle", true);

// Get the “staff” group to mark
staff = GroupFinder.findByName(gs, "affiliations:staff", true);

// Add the marker to staff
staff.getAttributeDelegate().addAttribute(googleSyncAttr);
