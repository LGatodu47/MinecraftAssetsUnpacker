package io.github.lgatodu47.asset_unpacker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

/**
 * Legacy version of Minecraft Asset Unpacker I made a long time ago. Use at your own risk.<br>
 * Note that it requires <strong>Gson</strong> in order for it to work.
 */
@Deprecated
public class LegacyMCAssetsUnpacker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static String mcDirPath;
    private static String outputDirectoryPath;
    private static String indexesVersion;
    private static boolean replaceExistingResult;

    public static void main(String[] args) {
        if (isValidFile(getProjectDirectory().concat("config.json"))) {
            if (!readConfig())
                return;
        } else {
            int result = parseArgs(args);
            if (result == -1) {
                System.out.println("Invalid or missing arguments to run program! These are the following possible arguments:");
                System.out.println("\tconfig (generates a defaulted config file)");
                System.out.println("\t</.minecraft directory path> <indexes version> (runs the program with the given arguments if no config is found)");
                return;
            } else if (result == 0) {
                return;
            }
        }

        if (!isValidDir(mcDirPath)) {
            System.out.println("Failed to parse location of directory '" + mcDirPath + "'!");
            return;
        }

        String assetsPath = mcDirPath.concat("/assets");
        if (!isValidDir(assetsPath)) {
            System.out.println("Could not find assets directory with the following path '" + assetsPath + "'!");
            return;
        }

        String indexesPath = assetsPath.concat("/indexes");
        if (!isValidDir(indexesPath)) {
            System.out.println("Could not find indexes directory with following path '" + indexesPath + "'!");
            return;
        }

        String objectsPath = assetsPath.concat("/objects");
        if (!isValidDir(objectsPath)) {
            System.out.println("Could not find objects directory with following path '" + objectsPath + "'!");
            return;
        }

        String indexesFilePath = indexesPath.concat("/").concat(indexesVersion).concat(".json");
        if (!isValidFile(indexesFilePath)) {
            System.out.println("Could not find indexes file with following path '" + indexesFilePath + "'!");
            return;
        }

        Map<String, File> objectsMap = new HashMap<>();
        scanObjectsFiles(new File(objectsPath), objectsMap);

        File resultDir = new File(outputDirectoryPath.concat("/").concat(indexesVersion));

        if (resultDir.exists() && replaceExistingResult) {
            deleteDirectory(resultDir);
            System.out.println("Cleaned result directory.");
        }

        if (resultDir.mkdirs()) {
            Map<String, String> fileNameToHash = new HashMap<>();

            int readFileCount = 0;
            try {
                JsonReader reader = GSON.newJsonReader(new FileReader(indexesFilePath));
                readFileCount = readIndexes(reader, fileNameToHash);
            } catch (FileNotFoundException e) {
                System.err.println("Failed to create JsonReader!");
                e.printStackTrace();
            }

            System.out.println("Got " + readFileCount + " files to unpack...");

            int unpackedFilesCount = 0;

            for (Map.Entry<String, String> entry : fileNameToHash.entrySet()) {
                String filePath = entry.getKey();
                String hash = entry.getValue();
                File target = objectsMap.get(hash);

                if (target.exists()) {
                    try {
                        File resultFile = new File(resultDir.getPath().concat("/").concat(filePath));

                        if (!resultFile.getParentFile().exists()) {
                            resultFile.getParentFile().mkdirs();
                        }

                        if (resultFile.createNewFile()) {
                            FileOutputStream outputStream = new FileOutputStream(resultFile);
                            Files.copy(target.toPath(), outputStream);
                            outputStream.close();
                            unpackedFilesCount++;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            System.out.println("Unpacked " + unpackedFilesCount + "/" + readFileCount + " files.");
        } else {
            if (replaceExistingResult)
                System.out.println("Failed to create output directory!");
            else
                System.out.println("Result directory already exists!");
        }
    }

    private static void scanObjectsFiles(File objectsDir, Map<String, File> objectsMap) {
        File[] directories = objectsDir.listFiles();

        if (directories != null) {
            for (File dir : directories) {
                if (dir.isDirectory()) {
                    File[] files = dir.listFiles();
                    if (files != null) {
                        for (File file : files) {
                            if (file.isFile()) {
                                objectsMap.put(file.getName(), file);
                            }
                        }
                    }
                }
            }
        }
    }

    private static int readIndexes(JsonReader reader, Map<String, String> fileNameToHash) {
        try {
            int fileCount = 0;
            reader.beginObject();
            reader.nextName(); // "objects"
            reader.beginObject();
            while (reader.hasNext()) {
                String filePath = reader.nextName();
                reader.beginObject();
                // "hash"
                reader.nextName(); String hash = reader.nextString();
                // "size"
                reader.nextName(); reader.nextInt();
                reader.endObject();
                fileNameToHash.put(filePath, hash);
                fileCount++;
            }
            reader.endObject();
            reader.endObject();
            reader.close();
            return fileCount;
        } catch (IOException e) {
            System.err.println("There was an error trying to parse indexes file!");
            e.printStackTrace();
        }

        return 0;
    }

    private static boolean isValidDir(String path) {
        File file = new File(path);
        return file.exists() && file.isDirectory();
    }

    private static boolean isValidFile(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    private static int parseArgs(String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("config")) {
                writeDefaultConfig();
                return 0;
            } else if (args.length > 1) {
                LegacyMCAssetsUnpacker.mcDirPath = args[0];
                LegacyMCAssetsUnpacker.indexesVersion = args[1];
                outputDirectoryPath = getProjectDirectory().concat("/unpacked");
                replaceExistingResult = false;
                return 1;
            }
        }
        return -1;
    }

    private static boolean readConfig() {
        try {
            JsonReader reader = GSON.newJsonReader(new FileReader(getProjectDirectory().concat("config.json")));
            reader.beginObject();
            reader.nextName(); mcDirPath = reader.nextString();
            reader.nextName(); outputDirectoryPath = reader.nextString();
            reader.nextName(); indexesVersion = reader.nextString();
            reader.nextName(); replaceExistingResult = reader.nextBoolean();
            reader.endObject();
            reader.close();
            return true;
        } catch (IOException e) {
            System.out.println("Failed to read the config file!");
            e.printStackTrace();
        }
        return false;
    }

    private static void writeDefaultConfig() {
        try {
            JsonWriter writer = GSON.newJsonWriter(new FileWriter(getProjectDirectory().concat("config.json")));
            writer.beginObject();
            writer.name("mcDirPath"); writer.value(getDefaultMcDirPath());
            writer.name("outputDirectoryPath"); writer.value(getProjectDirectory().concat("unpacked"));
            writer.name("indexesVersion"); writer.value("1.16");
            writer.name("replaceExistingResult"); writer.value(false);
            writer.endObject();
            writer.close();
            System.out.println("Successfully created a default config file.");
        } catch (IOException e) {
            System.out.println("Failed to write default config file!");
            e.printStackTrace();
        }
    }

    private static String getDefaultMcDirPath() {
        return System.getenv("APPDATA").replace('\\', '/').concat("/.minecraft");
    }

    private static String getProjectPath() {
        URL url = LegacyMCAssetsUnpacker.class.getProtectionDomain().getCodeSource().getLocation();

        try {
            return url.toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return url.getPath();
    }

    private static String getProjectDirectory() {
        File file = new File(getProjectPath());

        while (!file.isDirectory()) {
            file = file.getParentFile();
        }

        return file.getPath().replace('\\', '/').concat("/");
    }

    private static void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                }
                file.delete();
            }
        }
        dir.delete();
    }
}
