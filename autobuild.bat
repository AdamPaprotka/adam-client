@echo off
setlocal

set MODS_DIR=C:\Users\papro\AppData\Roaming\ModrinthApp\profiles\Meteor\mods

echo Building...
call gradlew.bat build
if errorlevel 1 (
    echo Build failed.
    pause
    exit /b 1
)

rem Find the plain jar (not -sources, not -dev)
set JAR=
for %%f in (build\libs\adam-client-1.0.0.jar) do (
    set "NAME=%%~nf"
    echo %%~nf | findstr /i "sources dev" >nul || set JAR=%%f
)

if "%JAR%"=="" (
    echo Could not find output jar.
    pause
    exit /b 1
)

echo Found: %JAR%

echo Closing Minecraft...
taskkill /f /im javaw.exe >nul 2>&1
timeout /t 2 /nobreak >nul

rem Remove any existing adam-client jar in the mods folder
for %%f in ("%MODS_DIR%\adam-client-*.jar") do (
    echo Removing old: %%f
    del "%%f"
)

echo Copying to %MODS_DIR%...
copy "%JAR%" "%MODS_DIR%\"
if errorlevel 1 (
    echo Copy failed.
    pause
    exit /b 1
)

echo Done.
pause
