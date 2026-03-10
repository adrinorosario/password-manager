#!/bin/bash
# Create bin directory
mkdir -p bin

# Copy resources (CSS) to bin so classloader can find them
cp style.css bin/

# Compile all Java files together
echo "Compiling Adrino System..."
javac --module-path lib \
      --add-modules javafx.controls \
      -cp "lib/*" \
      -d bin \
      *.java

# Check success
if [ $? -eq 0 ]; then
    echo "Compilation successful. Running..."
    # Run with native access enabled to suppress warnings
    java --module-path lib \
         --add-modules javafx.controls \
         --enable-native-access=javafx.graphics \
         -cp "bin:lib/*:." \
         com.adrino.passmanager.AdrinoPasswordManager
else
    echo "Compilation failed."
fi
