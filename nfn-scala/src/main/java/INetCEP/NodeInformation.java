package INetCEP;

public class NodeInformation {
    public String _nodeName;
    public String _iPAddress;
    public String _port;

    public NodeInformation(String nodeName, String iPAddress, String port) {
        this._nodeName = nodeName;
        this._iPAddress = iPAddress;
        this._port = port;
    }
}
