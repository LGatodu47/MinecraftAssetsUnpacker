package io.github.lgatodu47.asset_unpacker;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/**
 * Configuration record for {@link MinecraftAssetUnpacker}.
 * Use this record to launch the asset unpacker in dev environment.
 *
 * @param mcDir The Minecraft home directory.
 * @param assetsIndex The asset index version.
 * @param outputDir The assets output directory. <strong>NOTE: any existing file will be replaced!</strong>
 */
public record AssetUnpackerConfig(Path mcDir, String assetsIndex, Path outputDir) {
    /**
     * A pattern that matches for version names.
     */
    public static final Pattern VERSION_PATTERN = Pattern.compile("(\\d+\\.\\d+(\\.?(\\d+)?)+)");

    /**
     * Parses an array of String with arguments to the actual config.
     * Here are the list of arguments:
     * <li>anything containing "help": will print a help message to the console.</li>
     * <li>mcd: path to a Minecraft home directory. If it is absent, it will try to resolve it itself.</li>
     * <li>ai: version of the asset index. <strong>This argument is mandatory.</strong></li>
     * <li>od: path to an output directory. It defaults to 'runDir/unpacked/assetIndexVersion' if not specified.</li>
     * <li>reo: a boolean controlling whether the existing files the output directory should be replaced or not
     * In other words, the program won't run if this option is missing or set to false and the output directory
     * is not empty.</li>
     *
     * @param args The array of the arguments, as those passed by java in the {@link MinecraftAssetUnpacker#main(String[])} method.
     * @return A parsed config if all the arguments are valid and well-parsed.
     * @throws ConfigParsingError If the parsing stops for some reason: a missing/invalid argument or simply the display of a help message.
     */
    public static AssetUnpackerConfig parseArgs(String... args) throws ConfigParsingError {
        if(args.length == 0) {
            throw new ConfigParsingError("help");
        }

        Path mcDir = null;
        String assetsIndex = null;
        Path outputDir = null;
        boolean replaceExistingOutput = false;

        for(int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if(i == 0 && arg.contains("help")) {
                throw new ConfigParsingError("help");
            }
            if(!arg.startsWith("-")) {
                continue;
            }
            int indexOfValue = arg.indexOf("=");
            if(indexOfValue == -1) {
                continue;
            }
            String argName = arg.substring(1, indexOfValue);
            String value = arg.substring(indexOfValue + 1);

            switch (argName.toLowerCase()) {
                case "mcd" -> {
                    @SuppressWarnings("DuplicateExpressions")
                    Path path = Path.of(value);
                    if(Files.isDirectory(path)) {
                        mcDir = path;
                    }
                }
                case "ai" -> {
                    if(VERSION_PATTERN.matcher(value).matches()) {
                        assetsIndex = value;
                    }
                }
                case "od" -> {
                    @SuppressWarnings("DuplicateExpressions")
                    Path path = Path.of(value);
                    if(Files.isDirectory(path)) {
                        outputDir = path;
                    }
                }
                case "reo" -> replaceExistingOutput = Boolean.parseBoolean(value);
            }
        }

        if(assetsIndex == null) {
            throw new ConfigParsingError("No asset index was specified.");
        }

        if(mcDir == null) {
            String appData = System.getenv("APPDATA");
            if(appData == null) {
                throw new ConfigParsingError("No minecraft directory was specified.");
            }
            Path path = Path.of(appData).resolve(".minecraft");
            if(!Files.isDirectory(path)) {
                throw new ConfigParsingError("No minecraft directory was found.");
            }
            mcDir = path;
        }

        if(outputDir == null) {
            outputDir = MinecraftAssetUnpacker.RUNTIME_PATH.resolve("unpacked/".concat(assetsIndex));
            if(Files.isDirectory(outputDir)) {
                try(DirectoryStream<Path> files = Files.newDirectoryStream(outputDir)) {
                    if(files.iterator().hasNext() && !replaceExistingOutput) {
                        throw new ConfigParsingError("Output directory is not empty.");
                    }
                } catch (IOException ignored) {
                }
            }
        }

        return new AssetUnpackerConfig(mcDir, assetsIndex, outputDir);
    }
}
