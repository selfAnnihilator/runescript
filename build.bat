@echo off
echo Building RuneScript...

REM Compile the source
javac src\RuneScript.java

if %errorlevel% equ 0 (
    REM Create JAR file
    jar cfm src\runescript.jar manifest.txt src\*.class
    del src\*.class  REM Clean up compiled files
    
    echo Build successful!
    echo Run with: java -jar src\runescript.jar [options] [file]
    echo.
    echo Examples:
    echo   java -jar src\runescript.jar examples\hello.rn
    echo   java -jar src\runescript.jar --emit-tokens examples\hello.rn
    echo   java -jar src\runescript.jar examples\arithmetic.rn
) else (
    echo Build failed!
    exit /b 1
)