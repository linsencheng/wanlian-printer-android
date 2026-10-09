param([string]$JavaHome = 'C:\Program Files\Android\openjdk\jdk-21.0.8')
$ErrorActionPreference = 'Stop'
if (!(Test-Path -LiteralPath "$JavaHome\bin\java.exe")) { throw '请通过 -JavaHome 指定本机 JDK 目录' }
$buildDrive = @('W:', 'V:', 'U:', 'T:') | Where-Object { !(Test-Path "$_\") } | Select-Object -First 1
if (!$buildDrive) { throw '没有可用的临时构建盘符' }
$previousJava = $env:JAVA_HOME
$buildResult = 1
& subst.exe $buildDrive $PSScriptRoot
if ($LASTEXITCODE -ne 0) { throw '无法创建临时项目路径' }
Push-Location "$buildDrive\"
try {
    $env:JAVA_HOME = $JavaHome
    & .\gradlew.bat testDebugUnitTest lintDebug assembleDebug --console=plain
    $buildResult = $LASTEXITCODE
} finally {
    Pop-Location
    $env:JAVA_HOME = $previousJava
    & subst.exe $buildDrive /D
}
exit $buildResult
