package ags.assembly;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

/**
 * Assembly compiler that replaces the ANT-based ACME compilation
 * Uses the pure Java ACME cross-assembler instead of external native executable
 */
public class AssemblyCompiler {
    
    private static final List<String> ASSEMBLY_FILES = Arrays.asList(
        "init/init.a",
        "init/tinyloader.a",
        "rwts/rwts.a", 
        "rwts/minirwts.a",
        "rwts/c6rwts.a",
        "sos/sos.a",
        "sos/sos_himem.a",
        "compression/deflate_sos.a"
    );
    
    private static final int[] SLOTS = {1, 2, 4, 5, 6, 7};
    
    public static void main(String[] args) {
        System.out.println("Compiling assembly files with Java ACME...");
        
        AssemblyCompiler compiler = new AssemblyCompiler();
        try {
            compiler.compileAll();
            System.out.println("Assembly compilation completed successfully!");
        } catch (Exception e) {
            System.err.println("Assembly compilation failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public void compileAll() throws IOException {
        Path sourceDir = Paths.get("src/main/assembly");
        Path outputDir = Paths.get("target/classes/ags/asm");
        
        // Create output directory
        Files.createDirectories(outputDir);
        
        // Check if assembly source directory exists
        if (!Files.exists(sourceDir)) {
            System.out.println("No assembly source directory found at " + sourceDir + ", skipping assembly compilation");
            return;
        }
        
        // Compile files for each slot
        for (int slot : SLOTS) {
            System.out.println("Compiling for slot " + slot + "...");
            
            for (String asmFile : ASSEMBLY_FILES) {
                Path sourcePath = sourceDir.resolve(asmFile);
                
                if (Files.exists(sourcePath)) {
                    compileForSlot(sourcePath, slot, outputDir);
                } else {
                    System.out.println("Assembly file not found: " + sourcePath + ", skipping");
                }
            }
        }
        
        // Compile universal files (slot 0)
        compileUniversal(sourceDir, outputDir);
    }
    
    private void compileForSlot(Path sourceFile, int slot, Path outputDir) throws IOException {
        String baseName = getBaseName(sourceFile.getFileName().toString());
        
        // Compile for both SSC and GS (if slot 1 or 2)
        compileVariant(sourceFile, slot, "SSC", baseName + "_ssc_slot" + slot + ".o", outputDir);
        
        if (slot == 1 || slot == 2) {
            compileVariant(sourceFile, slot, "GS", baseName + "_gs_port" + slot + ".o", outputDir);
        }
    }
    
    private void compileVariant(Path sourceFile, int slot, String type, String outputName, Path outputDir) {
        try {
            System.out.println("  Compiling " + sourceFile.getFileName() + " for " + type + " slot " + slot);
            
            AcmeCompilerAdapter adapter = new AcmeCompilerAdapter();
            byte[] result = adapter.compile(sourceFile.toFile(), slot, type);
            
            if (result != null) {
                Path outputFile = outputDir.resolve(outputName);
                Files.write(outputFile, result);
                System.out.println("    Generated: " + outputFile);
            } else {
                System.err.println("    Failed to compile " + sourceFile.getFileName());
            }
            
        } catch (Exception e) {
            System.err.println("    Error compiling " + sourceFile.getFileName() + ": " + e.getMessage());
        }
    }
    
    private void compileUniversal(Path sourceDir, Path outputDir) throws IOException {
        Path deflateSource = sourceDir.resolve("compression/deflate_sos.a");
        
        if (Files.exists(deflateSource)) {
            System.out.println("Compiling universal deflate...");
            try {
                AcmeCompilerAdapter adapter = new AcmeCompilerAdapter();
                byte[] result = adapter.compileUniversal(deflateSource.toFile());
                
                if (result != null) {
                    Path outputFile = outputDir.resolve("deflate.o");
                    Files.write(outputFile, result);
                    System.out.println("  Generated: " + outputFile);
                } else {
                    System.err.println("  Failed to compile deflate");
                }
                
            } catch (Exception e) {
                System.err.println("  Error compiling deflate: " + e.getMessage());
            }
        }
    }
    
    private String getBaseName(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}