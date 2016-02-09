// Start the grouper session...never gets old!?
gs = GrouperSession.startRootSession();

// Get the Google Sync Marker Attribute for changelog.consumer.google.*
googleSyncAttr = AttributeDefNameFinder.findByName("etc:attribute:googleProvisioner:syncToGooglegoogle", true);

// For more extensive testing, load groups with more members...
// prime affiliation groups
// affiliationLoader = GroupFinder.findByName(gs, "loader:affiliationLoader", true);
// loaderRunOneJob(affiliationLoader);
// Get the “staff” group to mark
// staff = GroupFinder.findByName(gs, "affiliations:staff", true);
// Add the marker to staff
// staff.getAttributeDelegate().addAttribute(googleSyncAttr);

// For quick prototype work, create a small test group to exercise Google Provisioner
test = addStem("", "test", "test")
testGroup = addGroup("test", "test-group", "test-group")
addMember("test:test-group", "bsmith")
addMember("test:test-group", "bthompson")
addMember("test:test-group", "dgasper")
// Add the marker to test-group
testGroup.getAttributeDelegate().addAttribute(googleSyncAttr);

