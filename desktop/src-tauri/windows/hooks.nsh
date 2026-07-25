!macro NSIS_HOOK_PREUNINSTALL
  ; Release the device-bound seat before files are removed. The command exits
  ; with code 2 when the online service cannot be reached, so the operator is
  ; explicitly told that the seat may need manual release.
  ExecWait '"$INSTDIR\bemo-hr-desktop.exe" --deactivate-license' $0
  ${If} $0 != 0
    MessageBox MB_ICONEXCLAMATION|MB_OK "The online license could not be released. Connect to the internet and release it from Settings before uninstalling, or release the device from the license service. / تعذر تحرير الترخيص عبر الإنترنت. حرره من الإعدادات قبل الإزالة أو من خدمة التراخيص."
  ${EndIf}
!macroend
