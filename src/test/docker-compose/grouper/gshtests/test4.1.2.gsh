print("Test 4.1.2 Membership delete to indirectly marked group (i.e. parent folder is marked");
// 1) Test 4.0.2
// 2) Remove member from indirectly marked group
// Outcome:
// 1) membership removed from target
// GSH:
// Test 4.1.2

BufferedReader in = new BufferedReader(new InputStreamReader(System.in));

gs = GrouperSession.startRootSession();

print("add test folder and parent folder");
testFolderName = "testFolder"
addStem("", testFolderName, testFolderName);
parentFolderExtension = "parentFolder";
addStem(testFolderName, parentFolderExtension, parentFolderExtension);

print("add syncAttribute mark to parent folder");
syncAttr = AttributeDefNameFinder.findByName("etc:attribute:changeLogConsumer:printSync", true);
parentFolder = StemFinder.findByName(gs, testFolderName + ":" + parentFolderExtension, true);
parentFolder.getAttributeDelegate().addAttribute(syncAttr);

print("add group1 to marked folder, expecting to be added to target");
group1Name = parentFolderName + ":group1";
group1 = new GroupSave(gs).assignName(group1Name).assignGroupNameToEdit(group1Name).assignSaveMode(SaveMode.INSERT_OR_UPDATE).assignCreateParentStemsIfNotExist(true).save();

print("wait for grouper_debug.log: changeLog.consumer.print add group testFolder:parentFolder:group1.");

print("add new membership, expect to add to target");
bill = "bbrown705";
addMember(group1Name, bill);

print("wait for grouper_debug.log: changeLog.consumer.print add subject Bill Brown to group testFolder:parentFolder:group1.");
print("hit return to continue);
in.readLine();

print("remove bill from marked group, expect to remove bill from target");
delMember(group1Name, bill);

print("wait for grouper_debug.log: changeLog.consumer.print remove subject Bill Brown from group testFolder:parentFolder:group1.");
print("end of Test 4.1.2");
print("hit return to teardown test");
in.readLine();

// Test 4.1.2 teardown
delGroup(group1Name);
delStem(parentFolderName);
delStem(testFolderName);
print("end of Test 4.1.2 teardown");

