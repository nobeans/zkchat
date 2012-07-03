Requirement
-----------

- ZooKeeper 3.4.3+
- Groovy 1.8.6 (2.0 isn't supported yet)
- JavaFX 2.0 or Java 7u2+ (for GUI client)

How to play
-----------

Server
^^^^^^

- Run ZooKeeper server

  If you use Mac, you can easily install via homebrew as follows::

    $ brew install zookeeper
    $ cp /usr/local/etc/zookeeper/zoo_sample.cfg /usr/local/etc/zookeeper/zoo.cfg
    $ zkServer start

  ZooKeeper server will be started on 127.0.0.1:2181.

CUI client
^^^^^^^^^^

- Invoke zkchat.groovy with proper arguments::

    usage: groovy zkchat.groovy <hostPort> <nick> <channel>

    e.g.
        $ groovy zkchat.groovy 127.0.0.1:2181 nobeans lounge

- Input some text and hit the ENTER key!

  And, You can use the following commands::

    /QUIT
    /PART
    /JOIN <channel>

GUI client
^^^^^^^^^^

- Invoke fxchat.groovy with proper arguments::

    usage: groovy -cp <JAVAFX_JAR_PATH>:lib/groovyfx-0.2.jar fxchat.groovy

    e.g.
        $ groovy -cp /Library/Java/JavaVirtualMachines/1.7.0.jdk/Contents/Home/jre/lib/jfxrt.jar:lib/groovyfx-0.2.jar fxchat.groovy


Let's enjoy ZooKeeper and JavaFX/GroovyFX!
