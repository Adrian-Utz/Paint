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

## Dependancies
### From Source:
- JavaFX

### Jar File
- To make your own .jar file to the following:
```powershell
$fx = "C:\Users\autz\Desktop\Tools\javafx-sdk-26.0.2\lib"
```
1. Compile
```powershell
javac --module-path $fx --add-modules javafx.controls -d out (Get-ChildItem -Recurse myPaint -Filter *.java).FullName
```
2. Package, using MainClass as the entry point. The JavaFX classes and native DLLs
   (extracted from the `javafx-*-win.jar` files into `build\classes`) must be bundled
   in the jar so it opens by double-clicking. Compile into `build\classes` instead of `out`:
```powershell
javac --release 25 --module-path $fx --add-modules javafx.controls -d build\classes (Get-ChildItem -Recurse myPaint -Filter *.java).FullName
jar --create --file Paint.jar --main-class myPaint.MainClass -C build\classes .
```
3. Run (or just double-click Paint.jar)
```powershell
java -jar Paint.jar
```

## File Orientation

## UI Design
**Base UI**

## 