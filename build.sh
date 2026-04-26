#!/bin/bash
# Build script for RuneScript

echo "Building RuneScript..."

BUILD_DIR="$(mktemp -d)"
cleanup() {
    rm -rf "$BUILD_DIR"
}
trap cleanup EXIT

# Compile the source into a temporary directory so the packaged JAR contains
# only class files and never embeds a previous src/runescript.jar.
javac -d "$BUILD_DIR" src/RuneScript.java

if [ $? -eq 0 ]; then
    # Create JAR file
    jar cfm src/runescript.jar manifest.txt -C "$BUILD_DIR" .
    
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
