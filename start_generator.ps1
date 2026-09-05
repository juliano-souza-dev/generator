$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot
$generatorUrl = 'http://127.0.0.1:8080'
$mutex = New-Object System.Threading.Mutex($false, 'Local\IHUB_GENERATOR_START')
$owned = $false
function Test-Generator {
    try { return (Invoke-RestMethod "$generatorUrl/api/state" -TimeoutSec 2).app.name -eq 'Media and Subtitle Generator' } catch { return $false }
}
try {
    $owned = $mutex.WaitOne(0)
    if (-not $owned) { exit }
    if (Test-Generator) { Start-Process $generatorUrl; exit }
    $python = Join-Path $PSScriptRoot '.venv\Scripts\python.exe'
    & $python -m pip install --disable-pip-version-check -r requirements.txt *> (Join-Path $PSScriptRoot 'dependencies.log')
    if ($LASTEXITCODE -ne 0) { throw 'Falha ao instalar dependencias. Consulte dependencies.log.' }
    if (@(Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue).Count) { throw 'A porta 8080 esta ocupada por outro programa.' }
    Start-Process -FilePath $python -ArgumentList 'run_launcher.py' -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -RedirectStandardOutput (Join-Path $PSScriptRoot 'launcher.stdout.log') -RedirectStandardError (Join-Path $PSScriptRoot 'launcher.stderr.log')
    $deadline = (Get-Date).AddSeconds(45)
    while ((Get-Date) -lt $deadline) {
        if (Test-Generator) { Start-Process $generatorUrl; exit }
        Start-Sleep -Milliseconds 500
    }
    throw 'Servidor nao iniciou. Consulte launcher.stderr.log.'
} catch {
    Add-Type -AssemblyName PresentationFramework
    [System.Windows.MessageBox]::Show($_.Exception.Message, 'IHUB Generator') | Out-Null
} finally {
    if ($owned) { $mutex.ReleaseMutex() }
    $mutex.Dispose()
}
