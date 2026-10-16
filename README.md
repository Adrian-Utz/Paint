# Overview  
The purpose of this project is to do the following.  
1. Allow the user to draw with a red, green, blue, alpha schema. (rgba)
2. Allow the user to import images and edit them.
3. Allow the user to seperate different elements by adding layers.
  
**Latest Version**: v0.0.1  
**Written by**: AJ Utz  
**Written on**: 8/11/2026  
**Last Update**: 9/9/2026  

## Features

### Implemented
- RGBA Color selection.
- Zoom in and out, movement with the mouse.

### Planned
- **Free-Form Selection**: You can select a specific part, without using the default rectangle.

## Dependencies

This project uses JavaFX Controls and currently targets Java 25. JavaFX is not
included in the JDK, so install a JDK and download a JavaFX SDK separately.

### Set up JavaFX on Windows

1. Install a JDK 25 or newer.
2. Download the JavaFX SDK for your operating system and architecture from
   [Gluon's JavaFX downloads](https://gluonhq.com/products/javafx/), then extract
   it. Use an SDK version compatible with your JDK.
3. In PowerShell, set `$fx` to the SDK's `lib` directory. For example, if the
   SDK is extracted under your user `Tools` folder:

   ```powershell
   $fx = "$env:USERPROFILE\Tools\javafx-sdk-26.0.2\lib"
   ```

   Check that the path contains `javafx.controls.jar`:

   ```powershell
   Test-Path "$fx\javafx.controls.jar"
   ```

   This should print `True`. If not, update `$fx` to the correct `lib` folder.

### Compile and run from source

From the project root, compile the Java files and launch the application:

```powershell
New-Item -ItemType Directory -Force out | Out-Null
javac --release 25 --module-path $fx --add-modules javafx.controls -d out (Get-ChildItem -Recurse myPaint -Filter *.java).FullName
java --module-path $fx --add-modules javafx.controls -cp out myPaint.MainClass
```

`--module-path` points Java to the JavaFX SDK, and `--add-modules javafx.controls`
enables the controls module (which also brings in the graphics and base modules).

### Create and run an application JAR

The application JAR contains this project's compiled classes, not JavaFX itself.
Create it with `myPaint.MainClass` as its entry point:

```powershell
jar --create --file Paint.jar --main-class myPaint.MainClass -C out .
```

Run it with the JavaFX SDK available, using the same `$fx` path:

```powershell
java --module-path $fx --add-modules javafx.controls -jar Paint.jar
```

The JDK and a compatible JavaFX SDK must be installed on each computer that
runs this JAR. A plain `java -jar Paint.jar` or double-click will not supply
JavaFX automatically.

## File Orientation

## UI Design
**Base UI**

## 