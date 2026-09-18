!define APP_NAME "ImmersionHub Generator"
!define APP_VERSION "0.1.0"
!define APP_PUBLISHER "ImmersionHub"
!define APP_DIR "$LOCALAPPDATA\Programs\ImmersionHub Generator"
!define DATA_DIR "$LOCALAPPDATA\ImmersionHub Generator"

Unicode true
RequestExecutionLevel user
Name "${APP_NAME}"
OutFile "target/installer/ImmersionHub-Generator-Setup-0.1.0.exe"
InstallDir "${APP_DIR}"
ShowInstDetails show
ShowUninstDetails show
SetCompressor /SOLID lzma

Page directory
Page instfiles
UninstPage uninstConfirm
UninstPage instfiles

Section "Install"
  SetOutPath "$INSTDIR"
  File /r "target\package\*"

  CreateDirectory "$SMPROGRAMS\ImmersionHub Generator"
  CreateShortCut "$SMPROGRAMS\ImmersionHub Generator\ImmersionHub Generator.lnk"     "$INSTDIR\runtime\bin\javaw.exe"     '-cp "$INSTDIR\app\generator-desktop.jar;$INSTDIR\app\lib\*" br.com.immersionhub.generator.desktop.GeneratorLauncher'     "$INSTDIR\runtime\bin\javaw.exe"

  CreateShortCut "$DESKTOP\ImmersionHub Generator.lnk"     "$INSTDIR\runtime\bin\javaw.exe"     '-cp "$INSTDIR\app\generator-desktop.jar;$INSTDIR\app\lib\*" br.com.immersionhub.generator.desktop.GeneratorLauncher'     "$INSTDIR\runtime\bin\javaw.exe"

  WriteUninstaller "$INSTDIR\Uninstall.exe"

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "DisplayName" "${APP_NAME}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "DisplayVersion" "${APP_VERSION}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "Publisher" "${APP_PUBLISHER}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "InstallLocation" "$INSTDIR"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "QuietUninstallString" '"$INSTDIR\Uninstall.exe" /S'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator" "NoRepair" 1
SectionEnd

Section "Uninstall"
  Delete "$DESKTOP\ImmersionHub Generator.lnk"
  Delete "$SMPROGRAMS\ImmersionHub Generator\ImmersionHub Generator.lnk"
  RMDir "$SMPROGRAMS\ImmersionHub Generator"

  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\ImmersionHubGenerator"

  RMDir /r "$INSTDIR"

  ; Intentionally preserve ${DATA_DIR}:
  ; source-cache, projects, workspace, logs and settings survive uninstall.
SectionEnd
