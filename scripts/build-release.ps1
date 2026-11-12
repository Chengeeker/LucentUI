param([string]$SigningProperties=$env:LUCENT_SIGNING_PROPERTIES,[switch]$Offline)
$ErrorActionPreference='Stop'
if(-not $SigningProperties -or -not (Test-Path -LiteralPath $SigningProperties)){throw 'Set LUCENT_SIGNING_PROPERTIES to your private signing properties file.'}
if(-not $env:JAVA_HOME){throw 'Set JAVA_HOME to a JDK 21 installation.'}
$projectRoot=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$env:LUCENT_SIGNING_PROPERTIES=(Resolve-Path -LiteralPath $SigningProperties).Path
$tempPath=Join-Path $projectRoot 'build/jvm-tmp'
New-Item -ItemType Directory -Force -Path $tempPath | Out-Null
$oldOptions=$env:JAVA_TOOL_OPTIONS
$env:JAVA_TOOL_OPTIONS="$oldOptions `"-Djava.io.tmpdir=$tempPath`" `"-Djdk.net.unixdomain.tmpdir=$tempPath`""
Push-Location $projectRoot
try {
    $buildArguments=@(':demo:testDebugUnitTest',':lucent-ui:testDebugUnitTest',':demo:lintRelease',':demo:assembleRelease','--no-daemon','--console=plain')
    if($Offline){$buildArguments+='--offline'}
    & ./gradlew.bat @buildArguments
    if($LASTEXITCODE -ne 0){throw 'Build or checks failed. The existing root APK was preserved.'}
    $source=Join-Path $projectRoot 'demo/build/outputs/apk/release/demo-release.apk'
    $metadata=Get-Content 'demo/build/outputs/apk/release/output-metadata.json' -Raw | ConvertFrom-Json
    if($metadata.applicationId -ne 'dev.chengeeker.lucent.demo'){throw 'Unexpected package identifier.'}
    $version=$metadata.elements[0].versionName
    $sdk=$env:ANDROID_HOME
    if(-not $sdk){$sdk=$env:ANDROID_SDK_ROOT}
    if(-not $sdk){$sdk=((Get-Content local.properties | Where-Object {$_ -like 'sdk.dir=*'}) -replace '^sdk.dir=','').Replace('\\','\')}
    $buildTools=Get-ChildItem -LiteralPath (Join-Path $sdk 'build-tools') -Directory | Where-Object {$_.Name -match '^\d+\.\d+\.\d+$'} | Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if(-not $buildTools){throw 'Android build-tools are required for APK verification.'}
    & (Join-Path $buildTools.FullName 'apksigner.bat') verify --verbose $source
    if($LASTEXITCODE -ne 0){throw 'APK signature verification failed.'}
    & (Join-Path $buildTools.FullName 'zipalign.exe') -c -P 16 4 $source
    if($LASTEXITCODE -ne 0){throw 'APK alignment verification failed.'}
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive=[IO.Compression.ZipFile]::OpenRead($source)
    try {if(@($archive.Entries | Where-Object {$_.FullName -match '^lib/' -and $_.FullName -notmatch '^lib/arm64-v8a/'}).Count){throw 'Unexpected native ABI in APK.'}}finally{$archive.Dispose()}
    $target=Join-Path $projectRoot "LucentUI_v${version}_arm64-v8a.apk"
    if(Test-Path -LiteralPath $target){$rollback=Join-Path $projectRoot ('build/rollback/'+(Get-Date -Format 'yyyyMMdd-HHmmss'));New-Item -ItemType Directory -Force -Path $rollback | Out-Null;Copy-Item -LiteralPath $target -Destination $rollback}
    Copy-Item -LiteralPath $source -Destination $target -Force
    $archiveRoot=[IO.Path]::GetFullPath((Join-Path $projectRoot 'build/rollback'))
    if(-not $archiveRoot.StartsWith($projectRoot+[IO.Path]::DirectorySeparatorChar,[StringComparison]::OrdinalIgnoreCase)){throw 'Invalid rollback path.'}
    New-Item -ItemType Directory -Force -Path $archiveRoot | Out-Null
    Get-ChildItem -LiteralPath $projectRoot -File -Filter 'LucentUI_v*_arm64-v8a.apk' | Where-Object {$_.FullName -ne $target} | ForEach-Object {
        $old=[IO.Path]::GetFullPath($_.FullName)
        if([IO.Path]::GetDirectoryName($old) -ne $projectRoot){throw 'Unexpected artifact location.'}
        $destination=Join-Path $archiveRoot ($_.BaseName+'-'+(Get-Date -Format 'yyyyMMdd-HHmmss')+'.apk')
        Move-Item -LiteralPath $old -Destination $destination
    }
    Get-FileHash -LiteralPath $target -Algorithm SHA256
} finally { Pop-Location; $env:JAVA_TOOL_OPTIONS=$oldOptions }
