package ags.assembly;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for the ACME cross-assembler that handles Apple Game Server specific compilation
 * Replaces the ANT-based build system with pure Java compilation
 */
public class AcmeCompilerAdapter {
    
    private static final Logger LOGGER = Logger.getLogger(AcmeCompilerAdapter.class.getName());
    
    /**
     * Compile an assembly file for a specific slot and type
     */
    public byte[] compile(File sourceFile, int slot, String type) throws IOException {
        String sourceContent = Files.readString(sourceFile.toPath());
        
        // Add slot and type definitions like the original ANT build
        String modifiedSource = addDefines(sourceContent, slot, type);
        
        return compileSource(modifiedSource, sourceFile.getParentFile());
    }
    
    /**
     * Compile a universal assembly file (no slot/type specific defines)
     */
    public byte[] compileUniversal(File sourceFile) throws IOException {
        String sourceContent = Files.readString(sourceFile.toPath());
        
        // Add universal define using proper ACME syntax
        String[] lines = sourceContent.split("\n");
        StringBuilder result = new StringBuilder();
        
        boolean definesAdded = false;
        for (String line : lines) {
            result.append(line).append("\n");
            
            // After !cpu line, add our defines
            if (!definesAdded && line.trim().startsWith("!cpu")) {
                result.append("!set UNIVERSAL = 1\n");
                definesAdded = true;
            }
        }
        
        // If no !cpu found, add defines at the beginning
        String modifiedSource = definesAdded ? result.toString() : "!set UNIVERSAL = 1\n" + sourceContent;
        
        return compileSource(modifiedSource, sourceFile.getParentFile());
    }
    
    private String addDefines(String sourceContent, int slot, String type) {
        // ACME syntax: !set SYMBOL = value (for numeric values)
        StringBuilder defines = new StringBuilder();
        
        // Need to insert defines after !cpu but before everything else
        String[] lines = sourceContent.split("\n");
        StringBuilder result = new StringBuilder();
        
        boolean definesAdded = false;
        for (String line : lines) {
            result.append(line).append("\n");
            
            // After !cpu line, add our defines
            if (!definesAdded && line.trim().startsWith("!cpu")) {
                result.append("!set SLOT = ").append(slot).append("\n");
                result.append("!set ").append(type).append(" = 1\n");
                definesAdded = true;
            }
        }
        
        // If no !cpu found, add defines at the beginning
        if (!definesAdded) {
            return "!set SLOT = " + slot + "\n!set " + type + " = 1\n" + sourceContent;
        }
        
        return result.toString();
    }
    
    private static File acmeExecutable = null;
    
    private File findAssemblyRoot(File workingDirectory) {
        // Walk up the directory tree to find src/main/assembly
        File current = workingDirectory;
        while (current != null) {
            if (current.getName().equals("assembly") && 
                current.getParentFile() != null && current.getParentFile().getName().equals("main") &&
                current.getParentFile().getParentFile() != null && current.getParentFile().getParentFile().getName().equals("src")) {
                return current;
            }
            current = current.getParentFile();
        }
        
        // If we can't find it, try to find it relative to working directory
        File assemblyDir = new File(System.getProperty("user.dir"), "src/main/assembly");
        if (assemblyDir.exists() && assemblyDir.isDirectory()) {
            return assemblyDir;
        }
        
        return null;
    }
    
    private synchronized File getAcmeExecutable() throws IOException {
        if (acmeExecutable == null || !acmeExecutable.exists()) {
            // Determine platform-specific binary name
            String os = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            
            String platform;
            String suffix = os.contains("windows") ? ".exe" : "";
            if (os.contains("mac") || os.contains("darwin")) {
                platform = arch.contains("aarch64") || arch.contains("arm") ? "darwin-aarch64" : "darwin-x64";
            } else if (os.contains("linux")) {
                platform = arch.contains("aarch64") || arch.contains("arm") ? "linux-aarch64" : "linux-x64";
            } else if (os.contains("windows")) {
                platform = arch.contains("aarch64") || arch.contains("arm") ? "windows-aarch64" : "windows-x64";
            } else {
                throw new IOException("Unsupported platform: " + os + " " + arch);
            }
            
            String resourcePath = "/native/" + platform + "/acme" + suffix;
            try (InputStream acmeStream = getClass().getResourceAsStream(resourcePath)) {
                if (acmeStream == null) {
                    throw new IOException("ACME executable not found for platform: " + platform);
                }
                
                // Extract to temporary file
                Path tempPath = Files.createTempFile("acme", os.contains("windows") ? ".exe" : "");
                Files.copy(acmeStream, tempPath, StandardCopyOption.REPLACE_EXISTING);
                acmeExecutable = tempPath.toFile();
                acmeExecutable.setExecutable(true);
                acmeExecutable.deleteOnExit();
            }
        }
        return acmeExecutable;
    }
    
    private byte[] compileSource(String sourceContent, File workingDirectory) {
        try {
            // Create a temporary file for the source
            File tempSource = File.createTempFile("ags_asm_", ".a", workingDirectory);
            tempSource.deleteOnExit();
            Files.writeString(tempSource.toPath(), sourceContent);
            
            // Create output file
            File tempOutput = File.createTempFile("ags_out_", ".bin", workingDirectory);
            tempOutput.deleteOnExit();
            
            // Get ACME executable
            File acme = getAcmeExecutable();
            
            // Build command
            List<String> command = new ArrayList<>();
            command.add(acme.getAbsolutePath());
            command.add("--format");
            command.add("plain");  // Plain binary output
            command.add("--outfile");
            command.add(tempOutput.getAbsolutePath());
            command.add("--maxerrors");
            command.add("16");
            
            // Add include path to the assembly root directory
            // Find the assembly root (should be src/main/assembly)
            File assemblyRoot = findAssemblyRoot(workingDirectory);
            if (assemblyRoot != null) {
                command.add("-I");
                command.add(assemblyRoot.getAbsolutePath());
            }
            
            command.add(tempSource.getAbsolutePath());
            
            // Execute ACME with assembly root as working directory
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(assemblyRoot != null ? assemblyRoot : workingDirectory);
            pb.redirectErrorStream(true);
            
            Process process = pb.start();
            boolean finished = process.waitFor(30, TimeUnit.SECONDS);
            
            if (!finished) {
                process.destroyForcibly();
                LOGGER.warning("ACME compilation timed out");
                return null;
            }
            
            int exitCode = process.exitValue();
            
            if (exitCode == 0 && tempOutput.exists()) {
                // Read the compiled output
                byte[] compiled = Files.readAllBytes(tempOutput.toPath());
                tempOutput.delete();
                tempSource.delete();
                return compiled;
            } else {
                LOGGER.warning("ACME compilation failed with exit code: " + exitCode);
                
                // Log any error output
                try (var reader = process.inputReader()) {
                    reader.lines().forEach(line -> LOGGER.warning("ACME: " + line));
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to read ACME output", e);
                }
            }
            
            tempSource.delete();
            tempOutput.delete();
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during ACME compilation", e);
        }
        
        return null;
    }
}