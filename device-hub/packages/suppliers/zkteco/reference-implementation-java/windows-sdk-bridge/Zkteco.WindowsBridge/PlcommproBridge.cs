using System.Runtime.InteropServices;
using System.Text;
namespace Zkteco.WindowsBridge;

public sealed class PlcommproBridge : IDisposable {
    IntPtr _handle=IntPtr.Zero;
    public bool IsConnected => _handle!=IntPtr.Zero && _handle!=new IntPtr(-1);
    public void Connect(string parameters){ _handle=Native.Connect(parameters); if(!IsConnected)throw new InvalidOperationException("plcommpro Connect failed. Check vendor DLL architecture, connection string and CommPwd."); }
    public string GetDeviceData(string table,string fields,string filter="",string options=""){
        Ensure(); var buffer=new byte[4*1024*1024]; int n=Native.GetDeviceData(_handle,buffer,buffer.Length,table,fields,filter,options); if(n<0)throw new InvalidOperationException("GetDeviceData failed: "+n); return Encoding.Default.GetString(buffer,0,Math.Min(n,buffer.Length)).TrimEnd('\0');
    }
    public int SetDeviceData(string table,string data,string options=""){Ensure();return Native.SetDeviceData(_handle,table,data,options);}
    public int ControlDevice(int operation,int param1,int param2,int param3,int param4,string options=""){Ensure();return Native.ControlDevice(_handle,operation,param1,param2,param3,param4,options);}
    void Ensure(){if(!IsConnected)throw new InvalidOperationException("Not connected");}
    public void Dispose(){if(IsConnected)Native.Disconnect(_handle);_handle=IntPtr.Zero;}
    static class Native {
      [DllImport("plcommpro.dll",CallingConvention=CallingConvention.Cdecl,CharSet=CharSet.Ansi)] internal static extern IntPtr Connect(string Parameters);
      [DllImport("plcommpro.dll",CallingConvention=CallingConvention.Cdecl)] internal static extern void Disconnect(IntPtr h);
      [DllImport("plcommpro.dll",CallingConvention=CallingConvention.Cdecl,CharSet=CharSet.Ansi)] internal static extern int GetDeviceData(IntPtr h,[Out] byte[] buffer,int bufferSize,string table,string fields,string filter,string options);
      [DllImport("plcommpro.dll",CallingConvention=CallingConvention.Cdecl,CharSet=CharSet.Ansi)] internal static extern int SetDeviceData(IntPtr h,string table,string data,string options);
      [DllImport("plcommpro.dll",CallingConvention=CallingConvention.Cdecl,CharSet=CharSet.Ansi)] internal static extern int ControlDevice(IntPtr h,int operation,int param1,int param2,int param3,int param4,string options);
    }
}
