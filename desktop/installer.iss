#define MyAppName "ImmersionHub Generator"
#define MyAppVersion "0.1.0"
#define MyAppPublisher "ImmersionHub"
#define MyAppExeName "ImmersionHub Generator.exe"

[Setup]
AppId={{4F32C0B7-2B58-4F4E-9E27-1D4B8AAB5C12}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={localappdata}\Programs\ImmersionHub Generator
DefaultGroupName={#MyAppName}
PrivilegesRequired=lowest
OutputDir=target\installer
OutputBaseFilename=ImmersionHub-Generator-Setup-0.1.0
Compression=lzma2
SolidCompression=yes
WizardStyle=modern
ArchitecturesAllowed=x64compatible
ArchitecturesInstallIn64BitMode=x64compatible
CloseApplications=yes
CloseApplicationsFilter=*.*
RestartApplications=no
UninstallDisplayName={#MyAppName}
UsePreviousAppDir=yes
DisableProgramGroupPage=yes

[InstallDelete]
Type: filesandordirs; Name: "{app}\runtime"
Type: filesandordirs; Name: "{app}\app"

[Files]
Source: "target\app-image\ImmersionHub Generator\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs

[Icons]
Name: "{autoprograms}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{autodesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Tasks]
Name: "desktopicon"; Description: "Criar atalho na área de trabalho"; GroupDescription: "Atalhos:"; Flags: unchecked

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "Abrir {#MyAppName}"; Flags: nowait postinstall skipifsilent

[Code]
function PrepareToInstall(var NeedsRestart: Boolean): String;
var
  ResultCode: Integer;
begin
  { Ensure an existing desktop instance cannot keep the packaged Java runtime locked. }
  Exec(
    ExpandConstant('{cmd}'),
    '/C taskkill /F /IM "ImmersionHub Generator.exe" >nul 2>&1',
    '',
    SW_HIDE,
    ewWaitUntilTerminated,
    ResultCode
  );
  Result := '';
end;
