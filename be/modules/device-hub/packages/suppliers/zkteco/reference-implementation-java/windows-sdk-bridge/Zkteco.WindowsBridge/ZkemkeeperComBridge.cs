using System.Reflection;
namespace Zkteco.WindowsBridge;

public sealed class ZkemkeeperComBridge : IDisposable {
    private object? _zk; private Type? _type; private int _machine=1;
    public bool IsAvailable => Type.GetTypeFromProgID("zkemkeeper.CZKEM") is not null;
    public bool Connect(string host,int port,int machine=1) {
        _type=Type.GetTypeFromProgID("zkemkeeper.CZKEM") ?? throw new InvalidOperationException("zkemkeeper COM component is not registered. Install the licensed ZKTeco Standalone SDK/runtime.");
        _zk=Activator.CreateInstance(_type) ?? throw new InvalidOperationException("Unable to create CZKEM COM object"); _machine=machine;
        return (bool)(Invoke("Connect_Net",host,port) ?? false);
    }
    public ProbeReply Probe() {
        Ensure(); string? serial=OutString("GetSerialNumber",_machine); string? firmware=OutString("GetFirmwareVersion",_machine); string? platform=OutString("GetPlatform",_machine);
        return new ProbeReply(true,serial,firmware,platform,null);
    }
    public List<UserRow> Users() {
        Ensure(); Invoke("ReadAllUserID",_machine); var rows=new List<UserRow>();
        while(true){object?[] a={_machine,"","","",0,true}; bool ok=(bool)(InvokeArgs("SSR_GetAllUserInfo",a)??false); if(!ok)break; rows.Add(new UserRow((string)a[1],(string)a[2],(string)a[3],Convert.ToInt32(a[4]),Convert.ToBoolean(a[5]),null));}
        return rows;
    }
    public List<AttendanceRow> Attendance() {
        Ensure(); Invoke("ReadGeneralLogData",_machine); var rows=new List<AttendanceRow>();
        while(true){object?[] a={_machine,"",0,0,0,0,0,0,0,0,0}; bool ok=(bool)(InvokeArgs("SSR_GetGeneralLogData",a)??false); if(!ok)break; var dt=new DateTime(Convert.ToInt32(a[5]),Convert.ToInt32(a[6]),Convert.ToInt32(a[7]),Convert.ToInt32(a[8]),Convert.ToInt32(a[9]),Convert.ToInt32(a[10])); rows.Add(new AttendanceRow((string)a[1],dt,Convert.ToInt32(a[2]),Convert.ToInt32(a[3]),Convert.ToInt32(a[4]))); }
        return rows;
    }
    public void Disconnect(){if(_zk!=null)try{Invoke("Disconnect");}catch{} _zk=null;}
    string? OutString(string method,int machine){object?[] a={machine,""}; try{var ok=(bool)(InvokeArgs(method,a)??false);return ok?(string?)a[1]:null;}catch{return null;}}
    object? Invoke(string name,params object?[] args)=>_type!.InvokeMember(name,BindingFlags.InvokeMethod,null,_zk,args);
    object? InvokeArgs(string name,object?[] args)=>_type!.InvokeMember(name,BindingFlags.InvokeMethod,null,_zk,args);
    void Ensure(){if(_zk is null)throw new InvalidOperationException("Not connected");}
    public void Dispose()=>Disconnect();
}
