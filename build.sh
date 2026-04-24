#!/bin/bash
# Build script for RuneScript

echo "Building RuneScript..."

# Compile the source (output class files to src/)
javac -d src src/RuneScript.java

if [ $? -eq 0 ]; then
    # Create JAR file
    jar cfm src/runescript.jar manifest.txt -C src .
    find src -name "*.class" -delete  # Clean up compiled files
    
    echo "Build successful!"
    echo "Run with: java -jar src/runescript.jar [options] [file]"
    echo ""
    echo "Examples:"
    echo "  java -jar src/runescript.jar examples/hello.rn"
    echo "  java -jar src/runescript.jar --emit-tokens examples/hello.rn"
    echo "  java -jar src/runescript.jar examples/arithmetic.rn"
else
    echo "Build failed!"
    exit 1
fi