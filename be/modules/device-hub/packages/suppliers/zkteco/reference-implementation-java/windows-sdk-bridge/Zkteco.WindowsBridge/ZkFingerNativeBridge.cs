using System.Runtime.InteropServices;
namespace Zkteco.WindowsBridge;

public sealed class ZkFingerNativeBridge {
  public int DeviceCount(){ int rc=Native.ZKFPM_Init(); if(rc!=0)throw new InvalidOperationException("ZKFPM_Init failed: "+rc); try{return Native.ZKFPM_GetDeviceCount();}finally{Native.ZKFPM_Terminate();}}
  static class Native {
    [DllImport("libzkfp.dll",CallingConvention=CallingConvention.Cdecl)] internal static extern int ZKFPM_Init();
    [DllImport("libzkfp.dll",CallingConvention=CallingConvention.Cdecl)] internal static extern int ZKFPM_Terminate();
    [DllImport("libzkfp.dll",CallingConvention=CallingConvention.Cdecl)] internal static extern int ZKFPM_GetDeviceCount();
  }
}
