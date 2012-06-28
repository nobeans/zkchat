Requirement
-----------

- ZooKeeper 3.4.3+
- Groovy 1.8.6 (2.0 isn't supported yet)

How to play
-----------

- Run ZooKeeper server

  If you use Mac, you can easily install via homebrew as follows::

    $ brew install zookeeper
    $ cp /usr/local/etc/zookeeper/zoo_sample.cfg /usr/local/etc/zookeeper/zoo.cfg
    $ zkServer start

  ZooKeeper server will be started on 127.0.0.1:2181.

- Invoke zkchat.groovy with proper arguments::

    usage: groovy zkchat.groovy <hostPort> <nick> <channel>

    e.g.
        $ groovy zkchat.groovy 127.0.0.1:2181 nobeans lounge

- Input some text and hit the ENTER key!

  And, You can use the following commands::

    /QUIT
    /PART
    /JOIN <channel>


Let's enjoy ZooKeeper!
