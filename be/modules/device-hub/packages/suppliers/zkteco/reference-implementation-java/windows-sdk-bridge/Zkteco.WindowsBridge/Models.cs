namespace Zkteco.WindowsBridge;
public record ConnectRequest(string Host, int Port = 4370, int MachineNumber = 1);
public record ProbeReply(bool Online, string? Serial, string? Firmware, string? Platform, string? Error);
public record AttendanceRow(string UserId, DateTime Timestamp, int VerifyMode, int InOutMode, int WorkCode);
public record UserRow(string UserId, string Name, string Password, int Privilege, bool Enabled, string? CardNumber);
public record PlcommRequest(string ConnectionString);
