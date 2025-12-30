@echo off
echo ========================================
echo Rebuilding Project - Cleaning Target
echo ========================================
echo.

REM Check if target folder exists
if exist target (
    echo Deleting target folder...
    rmdir /s /q target
    echo Target folder deleted!
) else (
    echo Target folder does not exist, skipping...
)

echo.
echo ========================================
echo Done! Now do this in IntelliJ IDEA:
echo ========================================
echo 1. Build -^> Rebuild Project
echo 2. Stop application (red square)
echo 3. Start application (green play)
echo ========================================
pause
