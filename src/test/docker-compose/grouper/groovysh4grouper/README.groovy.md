
Installing Groovy Shell Wrapper for Grouper
===========================================

* Copy and rename the `config` directory to `$GROUPER_HOME/grouper.apiBinary-2.2.0/conf/.groovy`
* Copy `bin/*` to `$GROUPER_HOME/grouper.apiBinary-2.2.0/bin/`.

Running
=======

First check that the regular gsh command is working.  If you are starting from scratch or want an isolated testbed use the [Grouper Installer](https://spaces.internet2.edu/display/Grouper/Grouper+Downloads) and load the sample database when prompted.

If gsh is working you should be able to run:
`$GROUPER_HOME/gsh.groovy` to start up groovy shell with grouper symbols loaded.

If you've loaded the sample data, try:

```
    groovy:000> getGroups "all students"
```

which should return

```
    ===> [Group[name=qsuob:all,uuid=30c2a93f-3494-4022-a2f5-b92682586482], Group[name=qsuob:all_students,uuid=7959b1fe-c63b-45de-a603-9dc0fdda8206]]
```

