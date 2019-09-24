package SACEPICN;

public class DecentralizedNodeInformation {
    public String _nodeName;
    public String _iPAddress;
    public String _port;
    public int _hops;

    public DecentralizedNodeInformation(String nodeName, String iPAddress, String port, int hops) {
        this._nodeName = nodeName;
        this._iPAddress = iPAddress;
        this._port = port;
        this._hops = hops;
    }
}
