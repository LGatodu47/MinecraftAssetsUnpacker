package io.github.lgatodu47.asset_unpacker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Main class of Minecraft Asset Unpacker. As it name implies, it uses the asset index files to decode
 * the game assets and unpack them in an output directory.<br>
 * <strong>Note that this app doesn't download any kind of asset, it only uses existing files, therefore you will
 * need a valid Minecraft installation in order for it to work.</strong>
 */
public class MinecraftAssetUnpacker {
    /**
     * The path of the directory in which the jar is located. This is only used to generate the output directory
     * if not specified in production environment. Programmers should pass their own custom output directory.
     */
    static final Path RUNTIME_PATH = obtainRuntimePath();

    public void start(AssetUnpackerConfig config) {
        Path indexesFile = config.mcDir().resolve("assets/indexes/".concat(config.assetsIndex()).concat(".json")).toAbsolutePath();
        if (!Files.isRegularFile(indexesFile)) {
            System.err.println("No matching index file was found for " + config.assetsIndex() + " in '`mcDir`/assets/indexes'!");
            return;
        }
        Path objectsDir = config.mcDir().resolve("assets/objects").toAbsolutePath();
        if (!Files.isDirectory(objectsDir)) {
            System.err.println("No 'objects' directory was found! Searched in the following location: '`mcDir`/assets'!");
            return;
        }

        Path outputDir = config.outputDir().toAbsolutePath();
        if (Files.exists(outputDir)) {
            try {
                cleanDirectory(outputDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to clean output directory; consider doing it manually or specifying another one.", e);
            }
        } else {
            try {
                Files.createDirectories(outputDir);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create the output directory.", e);
            }
        }

        Map<String, String> indexes = new HashMap<>();
        String indexesFileContent;
        try {
            indexesFileContent = Files.readString(indexesFile);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read indexes file '" + indexesFile.getFileName().toString() + "'!", e);
        }
        JsonUtils.readIndexesJson(indexesFileContent, indexes);

        final int assetsAmount = indexes.size();
        System.out.println("Found " + assetsAmount + " assets to unpack in the indexes file!");
        System.out.println("Unpacking files...");
        long startTime = System.currentTimeMillis();

        int unpackedFileCount = 0;
        for (Map.Entry<String, String> asset : indexes.entrySet()) {
            String path = asset.getKey();
            String hash = asset.getValue();
            // Assets are stored in a folder named by the two-first letters of the hash
            Path objectPath = objectsDir.resolve(hash.substring(0, 2).concat("/").concat(hash));

            if(Files.notExists(objectPath)) {
                System.err.println("Found non-existing object in the indexes json! Check that your assets are up to date!");
                continue;
            }

            Path resultPath = outputDir.resolve(path);
            try {
                // The 'copy' function doesn't create the parent directories for us
                Files.createDirectories(resultPath.getParent());
                Files.copy(objectPath, resultPath);
                unpackedFileCount++;
            } catch (IOException e) {
                System.err.println("Failed to copy asset with path '" + path + "'!");
            }
        }

        System.out.println("Unpacked " + unpackedFileCount + "/" + assetsAmount + " files in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime) + " seconds!");
    }

    /**
     * Empties a directory without deleting it using recursion.<br>
     * If the given path is a file, it doesn't do anything.
     *
     * @param path The path of the directory to empty.
     * @throws IOException If an I/O error occurs.
     */
    private void cleanDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (Stream<Path> stream = Files.list(path)) {
                List<Path> paths = stream.toList();
                for (Path p : paths) {
                    // This won't do anything for files
                    cleanDirectory(p);
                    // But they're still deleted so it's ok
                    Files.delete(p);
                }
            }
        }
    }

    /**
     * @return The path of the directory containing the running jar file.
     */
    private static Path obtainRuntimePath() {
        try {
            Path path = Path.of(MinecraftAssetUnpacker.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            while (!Files.isDirectory(path)) {
                path = path.getParent();
            }
            return path;
        } catch (Exception e) {
            throw new RuntimeException("Failed to obtain runtime path. Please move the jar file and try again.", e);
        }
    }

    public static void main(String[] args) {
        try {
            new MinecraftAssetUnpacker().start(AssetUnpackerConfig.parseArgs(args));
        } catch (ConfigParsingError e) {
            String msg = e.getMessage();
            if (msg.equals("help")) {
                System.out.println("---Minecraft Asset Unpacker--- ** by LGatodu47");
                System.out.println("Argument scheme: -[arg]=[value], e.g. -abc=hello");
                System.out.println("-mcd=<Path> | A path to the minecraft directory. Will try to detect it if not specified");
                System.out.println("-ai=<Version> | A version for the minecraft asset index");
                System.out.println("-od=<Path> | A path to an output directory (defaults to `runDir`/unpacked/`indexesVersion`)");
                System.out.println("-reo=<Boolean> | If existing files in the output directory should be replaced or not (defaults to false)");
                System.out.println();
                return;
            }
            System.err.println("There are one or more missing or invalid arguments: " + msg);
            System.out.println("Run with 'help' for a list of the available arguments.");
        } catch (RuntimeException e) {
            e.printStackTrace();
        } catch (Throwable t) {
            throw new RuntimeException("Caught an exception when running Minecraft Asset Unpacker!", t);
        }
    }
}
