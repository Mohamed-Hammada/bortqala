from .base import DirectTcpAdapter

class TcpTerminalAdapter(DirectTcpAdapter):
    route = 'tcp-terminal'
    kind = 'direct-protocol'
