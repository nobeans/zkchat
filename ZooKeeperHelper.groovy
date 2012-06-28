@Grab(group='org.apache.zookeeper', module='zookeeper', version='3.4.3')
@GrabExclude('com.sun.jmx:jmxri')
@GrabExclude('javax.jms:jms')
@GrabExclude('com.sun.jdmk:jmxtools')
import org.apache.zookeeper.ZooKeeper
import org.apache.zookeeper.Watcher
import org.apache.zookeeper.Watcher.Event
import org.apache.zookeeper.Watcher.Event.EventType
import org.apache.zookeeper.Watcher.Event.KeeperState
import org.apache.zookeeper.WatchedEvent
import org.apache.zookeeper.KeeperException
import org.apache.zookeeper.KeeperException.Code

import groovy.util.logging.Log4j

@Log4j
class ZooKeeperHelper {

    def zk

    private handleNoneEvent = { event ->
        switch (event.state) {
            case KeeperState.SyncConnected:
                log.debug "Event: SyncConnected"
                break
            case KeeperState.Disconnected:
                log.debug "Event: Disconnected"
                break
            default:
                log.debug "Event: Unexpected state: ${event.state}"
                break
        }
    }

    private handleZnodeEvent = { event ->
        def znode = event.path
        log.debug "Event: ${event.type} on ${znode}"
        switch (event.type) {
            case EventType.NodeCreated:
            case EventType.NodeDataChanged:
                def data = zk.getData(znode, false, null)
                log.debug "New data of ${znode}: ${new String(data)}"
            case EventType.NodeDeleted:
                log.debug "Znode has been deleted: ${znode}"
            case EventType.NodeChildrenChanged:
                def children = zk.getChildren(znode, false)
                log.debug "Chidren of ${znode}: ${children}"
            default:
                assert false
        }
    }

    private defaultWatcher = { event ->
        log.debug "Processing event: ${event}"

        try {
            // 明示的にWatchしている対象に関する以外のイベント
            // ＝接続状態の変更に関するイベントと思えば良いみたい
            if (event.type == EventType.None) {
                return handleNoneEvent(event)
            }
            // Watch対象のznodeに対するイベント
            handleZnodeEvent(event)
        } catch (e) {
            // 別スレッドのため、出力しておかないとハマる
            e.printStackTrace()
        }

    } as Watcher

    ZooKeeperHelper(hostPort, timeout = 10000) {
        zk = new ZooKeeper(hostPort, timeout, defaultWatcher)
        log.debug "Connected to ZooKeeper server: ${zk}"
    }

    def withRetry(closure) {
        int count = 0
        while (true) {
            try {
                return closure.call(zk)
            } catch (KeeperException e) {
                log.debug "Error: ${e.code()}"
                log.debug "Retrying: ${++count} time(s)"
            }
        }
    }
}
