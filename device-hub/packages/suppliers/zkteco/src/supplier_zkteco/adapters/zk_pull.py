from .base import DirectTcpAdapter

class ZkPullAdapter(DirectTcpAdapter):
    route = 'zk-pull'
    kind = 'direct-protocol'
    default_port = 4370
