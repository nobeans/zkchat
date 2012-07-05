@Grab('org.codehaus.groovyfx:groovyfx:0.2')
@GrabExclude('com.sun.jmx:jmxri')
@GrabExclude('javax.jms:jms')
@GrabExclude('com.sun.jdmk:jmxtools')
import groovyx.javafx.GroovyFX
import groovyx.javafx.SceneGraphBuilder
import groovyx.javafx.beans.FXBindable
import javafx.event.EventHandler
import javafx.scene.input.KeyCode

GroovyFX.start { app ->
    SceneGraphBuilder builder = delegate

    def chatClient = new ChatClient()
    def stage = stage title: "ZkChat", visible: false, width: 400, height: 300
    def logonScene
    def mainScene

    logonScene = scene {
        def logonNotReady = { chatClient.server == '' || chatClient.nick == '' }
        def logon = {
            chatClient.connect()
            stage.scene = mainScene
            channel.requestFocus()
        }
        gridPane hgap: 5, vgap: 10, padding: 25, alignment: TOP_CENTER, {
            columnConstraints minWidth: 50, halignment: "right"
            columnConstraints prefWidth: 250, hgrow: 'always'

            label text: "Server", row: 0, column: 0
            textField id: 'server', row: 0, column: 1, text: bind(chatClient.server()), {
                onKeyPressed { event ->
                    if (event.code == KeyCode.ENTER && !logonNotReady()) { logon() }
                }
            }

            label text: "Nick", row: 1, column: 0
            textField id: 'nick', row: 1, column: 1, text: bind(chatClient.nick()), {
                onKeyPressed { event ->
                    if (event.code == KeyCode.ENTER && !logonNotReady()) { logon() }
                }
            }

            button "Logon", id: "logonButton", row: 2, column: 1, halignment: "right", onAction: logon, disable: bind(logonNotReady)
        }
    }

    mainScene = scene {
        def tabFactory = new ChannelTabFactory(chatClient:chatClient, builder:builder)
        def joinNotReady = { tabFactory.channelName == '' }
        vbox spacing: 5, padding: 5, {
            hbox spacing: 5, {
                label "Channel"
                textField id: 'channel', text: bind(tabFactory.channelName()), hgrow: 'always', {
                    onKeyPressed { event ->
                        if (event.code == KeyCode.ENTER && !joinNotReady()) { tabFactory.addTab() }
                    }
                }
                button "Join", onAction: tabFactory.addTab, disable: bind(joinNotReady)
            }
            tabFactory.channelTabPane = tabPane vgrow: 'always', {
                /* empty at first */
            }
        }
    }

    stage.scene = logonScene
    stage.show()
}

class ChatClient {
    static final DEFAULT_SERVER = '127.0.0.1:2181'

    def protocol
    @FXBindable String server = DEFAULT_SERVER
    @FXBindable String nick = ''

    def connect = {
        close()
        protocol = new ChatProtocol(server, nick)
    }
    def join = { String channelName ->
        new ChannelClient(chatClient:this, channelName:channelName).join()
    }
    def close = {
        protocol?.close()
    }
}

class ChannelTabFactory {
    def chatClient
    def builder
    def channelTabPane
    @FXBindable String channelName = ''

    def addTab = {
        // Add new tab for the channel
        def channelTab = createNewTab(channelName)
        channelTabPane.tabs.add(channelTab)

        channelName = ''

        // Focus joinned channel's message field
        channelTabPane.selectionModel.select(channelTab)
        channelTab.requestFocus()
    }

    private createNewTab = { String channelName ->
        def channelClient = chatClient.join(channelName)

        def inputMessageField
        def tab = builder.tab(text:channelName, {
            vbox spacing: 3, {
                textArea editable: false, text: bind(channelClient.receivedMessages()), hgrow: 'always', vgrow: 'always'
                hbox spacing: 5, {
                    inputMessageField = textField text: bind(channelClient.inputMessage()), hgrow: 'always', {
                        onKeyPressed { event ->
                            if (event.code == KeyCode.ENTER) { channelClient.postMessage() }
                        }
                    }
                    button "Post", {
                        onAction { channelClient.postMessage() }
                    }
                }
            }
        })
        tab.onClosed = { channelClient.part() } as EventHandler
        tab.metaClass.requestFocus = { inputMessageField.requestFocus() }
        return tab
    }
}

class ChannelClient {
    def chatClient
    String channelName
    @FXBindable String inputMessage = ''
    @FXBindable String receivedMessages = ''

    def postMessage = {
        chatClient.protocol.sendMessage(channelName, inputMessage)
        inputMessage = ''
        this
    }
    def join = {
        chatClient.protocol.join(channelName) { line ->
            def time = new Date().format('HH:mm')
            receivedMessages += "${time} ${line}\n"
        }
        this
    }
    def part = {
        chatClient.protocol.part(channelName)
        this
    }
}
