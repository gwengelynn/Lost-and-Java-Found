@echo off
setlocal EnableExtensions
cd /d "%~dp0"

set "SILENT=0"
if /i "%~1"=="silent" set "SILENT=1"

if "%SILENT%"=="0" (
    title Lost and Java-Found
    echo.
    echo  ========================================
    echo   Lost and Java-Found
    echo   Starting without NetBeans...
    echo  ========================================
    echo.
)

rem Log file used in silent mode so errors are still visible later
set "LOG=%~dp0run-last.log"
if "%SILENT%"=="1" (
    > "%LOG%" echo Lost and Java-Found silent launch — %DATE% %TIME%
)

rem --- Prefer the JDK that NetBeans ships (Java 21+). PATH java is often old 1.8. ---
if exist "C:\Program Files\Apache NetBeans\jdk\bin\java.exe" (
    set "JAVA_HOME=C:\Program Files\Apache NetBeans\jdk"
) else if exist "%JAVA_HOME%\bin\java.exe" (
    rem keep existing JAVA_HOME
) else if exist "C:\Program Files\Eclipse Adoptium\jdk-21*\bin\java.exe" (
    for /d %%J in ("C:\Program Files\Eclipse Adoptium\jdk-21*") do set "JAVA_HOME=%%J"
) else if exist "C:\Program Files\Java\jdk-21*\bin\java.exe" (
    for /d %%J in ("C:\Program Files\Java\jdk-21*") do set "JAVA_HOME=%%J"
)

if not defined JAVA_HOME (
    if "%SILENT%"=="1" (
        >> "%LOG%" echo [ERROR] No suitable JDK found.
        mshta "javascript:var s=new ActiveXObject('WScript.Shell');s.Popup('No suitable JDK found.\nInstall JDK 21+ or keep Apache NetBeans installed.\n\nDetails: run-last.log',0,'Lost and Java-Found',16);close();"
    ) else (
        echo [ERROR] No suitable JDK found.
        echo Install JDK 21+ or open NetBeans once so its JDK is available.
        echo.
        pause
    )
    exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

rem --- Maven: PATH first, then NetBeans bundled Maven ---
set "MVN="
where mvn >nul 2>&1 && set "MVN=mvn"
if not defined MVN if exist "C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd" (
    set "MVN=C:\Program Files\Apache NetBeans\java\maven\bin\mvn.cmd"
)
if not defined MVN if exist "%MAVEN_HOME%\bin\mvn.cmd" (
    set "MVN=%MAVEN_HOME%\bin\mvn.cmd"
)

if not defined MVN (
    if "%SILENT%"=="1" (
        >> "%LOG%" echo [ERROR] Maven not found.
        mshta "javascript:var s=new ActiveXObject('WScript.Shell');s.Popup('Maven not found.\nKeep Apache NetBeans installed or install Maven.\n\nDetails: run-last.log',0,'Lost and Java-Found',16);close();"
    ) else (
        echo [ERROR] Maven not found.
        echo Install Maven or keep Apache NetBeans installed ^(it includes Maven^).
        echo.
        pause
    )
    exit /b 1
)

if "%SILENT%"=="0" (
    echo Using Java:
    "%JAVA_HOME%\bin\java.exe" -version
    echo.
    echo Using Maven: %MVN%
    echo.
    echo Building and launching... ^(first run may download dependencies^)
    echo.
)

rem Same goal NetBeans uses — sets up JavaFX correctly
if "%SILENT%"=="1" (
    >> "%LOG%" echo JAVA_HOME=%JAVA_HOME%
    >> "%LOG%" echo MVN=%MVN%
    call "%MVN%" -q javafx:run >> "%LOG%" 2>&1
) else (
    call "%MVN%" -q javafx:run
)
set "EXITCODE=%ERRORLEVEL%"

if not "%EXITCODE%"=="0" (
    if "%SILENT%"=="1" (
        >> "%LOG%" echo [ERROR] App exited with code %EXITCODE%.
        mshta "javascript:var s=new ActiveXObject('WScript.Shell');s.Popup('Could not start the app (code %EXITCODE%).\nIf MySQL is offline, start MySQL80.\n\nSee run-last.log in the project folder.',0,'Lost and Java-Found',16);close();"
    ) else (
        echo.
        echo [ERROR] App exited with code %EXITCODE%.
        echo If MySQL is offline, start the MySQL80 service and try again.
        echo.
        pause
    )
    exit /b %EXITCODE%
)

endlocal
exit /b 0
