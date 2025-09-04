<div align="center">

![Apple Game Server](logo_banner.png)

</div>

# Apple Game Server

Upload games to Apple // computers via serial connection.

A modernized Java application that serves a menu of Apple // games over serial connection, allowing you to browse and load games directly on your Apple // computer.

## Requirements

- **Java 23+** with preview features enabled
- **Maven 3.6+** for building
- Serial connection to Apple // computer (physical serial port, USB-to-serial adapter, or TCP connection for emulators)

## Building

### Standard JAR Build
```bash
mvn clean package
```

### Native Binary (GraalVM)
Requires GraalVM with native-image:
```bash
mvn clean package -Pnative
```

## Running

### From JAR
```bash
java --enable-preview -jar target/apple-game-server.jar
```

### From Maven
```bash
mvn exec:java
```

*Note: The exec plugin is pre-configured with preview features enabled and the correct main class.*

### HiDPI/4K Display Support
The application includes basic HiDPI scaling support for modern displays. The UI scale is set to 2.0x by default. To adjust for your display, you can modify the `sun.java2d.uiScale` property in the `Main.java` file or run with a custom scale:

```bash
java -Dsun.java2d.uiScale=1.5 --enable-preview -jar target/apple-game-server.jar
```

## Setup

### Apple // Computer Setup
1. Connect your Apple // to your computer via serial cable
2. At the BASIC prompt, type `IN#2` and press Return
3. Leave the Apple // at this prompt (it's now listening on the serial port)

**For Apple //gs users**: These settings assume port 2 (modem port) at standard settings (1200 baud, 8N1).

### Application Configuration
1. Launch the Apple Game Server application
2. Configure your connection settings:
   - **Serial Port**: Use system port names (e.g., `COM1` on Windows, `/dev/ttyUSB0` on Linux, `/dev/cu.usbserial` on macOS)
   - **TCP Port**: Use `6502` for AppleWin emulator
   - **Baud Rate**: Typically 1200 for //gs, 300-1200 for other models

## Game Menu Controls

Once the games menu appears on your Apple //:

### Navigation
- **A-Z**: Jump to games starting with that letter
- **Left/Right** or **[ ]**: Move half a screen up/down
- **Up/Down** (//e, //c, //gs): Move one line up/down
- **Left/Right** (original //): Move one line up/down

### Information
- **TAB**: View detailed game information
- **V**: View screenshots (from detail view)
- **ESC**: Switch between main menu and search screen
- **Return**: Load the selected game

## Technical Details

### Modern Architecture
- **Java 23** with preview features and modern APIs
- **Maven** build system with standardized project structure
- **jSerialComm** library for reliable cross-platform serial communication
- **Native ACME** cross-assembler integration for 6502 code compilation
- **GraalVM** native image support for fast startup and reduced memory usage

### Assembly Compilation
The project includes 6502 assembly code that is automatically compiled during the Maven build process:
- Uses the original ACME cross-assembler (bundled as native executable)
- Compiles variants for different slots (1, 2, 4, 5, 6, 7) and card types (SSC, GS)
- Generates optimized binary files for each configuration

### Cross-Platform Support
- **Windows**: Native executable or JAR
- **macOS**: Native executable or JAR (ARM64 and Intel)
- **Linux**: Native executable or JAR (ARM64 and x86_64)

## Development

### Project Structure
```
src/main/java/          # Java source code
src/main/resources/     # Resources and data files
src/main/assembly/      # 6502 assembly source files
target/classes/ags/asm/ # Compiled assembly objects
```

### Dependencies
- **jSerialComm 2.11.0**: Modern serial port communication
- **JAXB**: XML binding for game database
- **JUnit 5**: Testing framework

### Build Process
1. Compile Java sources (Java 23 with preview features)
2. Copy resources to target directory
3. Compile 6502 assembly files using native ACME
4. Generate assembly variants for all slot/card combinations
5. Package into executable JAR or native binary

## Legacy Notes

This is a modernized version of the original Apple Game Server. Key improvements:

- ✅ **Modern Java 23** (was Java 1.5+)
- ✅ **Maven build system** (was Apache ANT)
- ✅ **jSerialComm library** (was RXTX)
- ✅ **Native ACME integration** (was external dependency)
- ✅ **Cross-platform native binaries** (new feature)
- ✅ **Simplified dependencies** (no manual library installation)

## Support

**Original Author**: brendan.robert (a) gmail.com

**Community**: 
- Apple2Infinitum Slack: [https://prodos8.com/apple2infinitum/](https://prodos8.com/apple2infinitum/)
- Facebook Apple II Enthusiasts: [https://www.facebook.com/groups/5251478676](https://www.facebook.com/groups/5251478676)
- GitHub: [Apple Game Server](https://github.com/badvision/Apple-game-server)

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

```
Copyright 2024 Brendan Robert

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```