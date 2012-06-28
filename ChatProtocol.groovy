@Grab(group='org.apache.zookeeper', module='zookeeper', version='3.4.3')
@GrabExclude('com.sun.jmx:jmxri')
@GrabExclude('javax.jms:jms')
@GrabExclude('com.sun.jdmk:jmxtools')
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.CreateMode
import org.apache.zookeeper.ZooDefs.Ids
import groovy.util.logging.Log4j

@Log4j
class ChatProtocol {

    private static final ENCODING = 'UTF8'
    private static final ROOT = '/zkchat'
    private static final USERS = "${ROOT}/users"
    private static final CHANNELS = "${ROOT}/channels"

    def zkhelper
    def nick

    ChatProtocol(String hostPort, String nick) {
        zkhelper = new ZooKeeperHelper(hostPort)
        setupBasicStructures()

        this.nick = nick
        createExclusively "${USERS}/${nick}", CreateMode.EPHEMERAL
    }

    def join(String channel, Closure callback) {
        setupChannel(channel)

        def listenZnode = getChannelListenZnode(channel)
        createIfNotExist listenZnode, CreateMode.EPHEMERAL

        zkhelper.withRetry { zk ->
            def continuousWatcher
            continuousWatcher = { event ->
                // PART
                if (event.type == EventType.NodeDeleted) {
                    callback.call "Part from ${channel}. Bye."
                    return
                }
                // MESSAGE
                if (event.type == EventType.NodeDataChanged) {
                    // 2nd arg is to watch continously
                    def data = zk.getData(listenZnode, continuousWatcher, null)

                    def receivedMessage = "(${channel}) ${new String(data)}"
                    callback.call receivedMessage
                    return
                }
                log.debug "Unexpected event: ${event}"

            } as Watcher
            zk.exists(listenZnode, continuousWatcher)
        }
        callback.call "Joined to ${channel}"
    }

    def part(String channel) {
        def listenZnode = getChannelListenZnode(channel)
        deleteRecusivelyIfExist(listenZnode)
    }

    def sendMessage(String channel, String message) {
        if (!isJoinedChannel(channel)) {
            throw new RuntimeException("Not joined yet: ${channel}")
        }
        def joinedUsers = getAllJoinedUsers(channel)
        def channelZnode = getChannelZnode(channel)
        zkhelper.withRetry { zk ->
            joinedUsers.each { user ->
                zk.setData("${channelZnode}/users/${user}", "[${nick}] ${message}".getBytes(ENCODING), -1)
            }
        }
    }

    def close() {
        zkhelper.withRetry { zk -> zk.close() }
    }

    private setupBasicStructures() {
        createIfNotExist ROOT
        createIfNotExist USERS
        createIfNotExist CHANNELS
    }

    private getAllJoinedUsers(String channel) {
        return zkhelper.withRetry { zk ->
            def channelZnode = getChannelZnode(channel)
            return zk.getChildren("${channelZnode}/users", false)
        }
    }

    private isJoinedChannel(String channel) {
        return zkhelper.withRetry { zk ->
            def listenZnode = getChannelListenZnode(channel)
            return zk.exists(listenZnode, false)
        }
    }

    private getChannelZnode(String channel) {
        "${CHANNELS}/${channel}"
    }

    private getChannelListenZnode(String channel) {
        "${getChannelZnode(channel)}/users/${nick}"
    }

    private setupChannel(String name) {
        def channelZnode = getChannelZnode(name)
        createIfNotExist channelZnode
        createIfNotExist "${channelZnode}/topic"
        createIfNotExist "${channelZnode}/users"
    }

    private createIfNotExist(znode, mode = CreateMode.PERSISTENT) {
        zkhelper.withRetry { zk ->
            if (!zk.exists(znode, false)) {
                zk.create(znode, new byte[0], Ids.OPEN_ACL_UNSAFE, mode)
            }
        }
    }

    private createExclusively(znode, mode = CreateMode.PERSISTENT) {
        zkhelper.withRetry { zk ->
            if (zk.exists(znode, false)) {
                throw new RuntimeException("Already exists: ${znode}")
            }
            zk.create(znode, new byte[0], Ids.OPEN_ACL_UNSAFE, mode)
        }
    }

    private deleteRecusivelyIfExist(znode) {
        zkhelper.withRetry { zk ->
            zk.getChildren(znode, false).each { child ->
                deleteRecusivelyIfExist "${znode}/${child}"
            }
            if (zk.exists(znode, false)) {
                zk.delete(znode, -1)
            }
        }
    }
}
