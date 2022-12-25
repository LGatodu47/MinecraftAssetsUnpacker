package io.github.lgatodu47.asset_unpacker;

/**
 * An error thrown when config parsing encounters a problem.
 * @see AssetUnpackerConfig#parseArgs(String...)
 */
public class ConfigParsingError extends Throwable {
    public ConfigParsingError(String message) {
        super(message);
    }
}
