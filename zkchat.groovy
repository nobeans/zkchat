import java.util.regex.Matcher

//----------------------------------
// Handle arguments

if (args.size() != 3) {
    System.err.println("usage: groovy zkchat.groovy <hostPort> <nick> <channel>")
    System.exit(2)
}
args = args.toList().reverse()
def hostPort = args.pop()
def nick = args.pop()
def channel = args.pop()

//----------------------------------
// Start chat session

def protocol = new ChatProtocol(hostPort, nick)

def callback = { line ->
    def time = new Date().format('HH:mm')
    println "> ${time} ${line}"
}

protocol.join(channel, callback)

def inputHandler = { line ->
    switch (line) {
        case '/QUIT':
            protocol.close()
            System.exit(0)
        case '/PART':
            protocol.part(channel)
            channel = null
            return
        case ~'^/JOIN (.*)$':
            def newChannel = Matcher.lastMatcher.group(1)
            if (channel) protocol.part(channel)
            channel = newChannel
            protocol.join(channel, callback)
            return
        default:
            if (channel) {
                protocol.sendMessage(channel, line)
            } else {
                println "ERROR: you must join to channel"
                println "usage:"
                println "  /QUIT"
                println "  /PART"
                println "  /JOIN <channel>"
            }
    }
}

println "Ready."
// Console.readLine() is generally useful for this purpose
// but it cannot be used on GroovyServ...
System.in.withReader { reader ->
    while (true) {
        def line = reader.readLine()
        inputHandler(line)
    }
}

