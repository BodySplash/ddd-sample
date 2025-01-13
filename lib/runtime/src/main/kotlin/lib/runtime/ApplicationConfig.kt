package lib.runtime

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import java.nio.file.Path
import kotlin.text.equals

object ApplicationConfig {
    fun load(): Config {
        val config: Config = if ("production".equals(System.getenv("ENV"), ignoreCase = true)) {
            ConfigFactory.load()
        } else {
            ConfigFactory
                .load("dev")

        }
        return if (config.hasPath("secretsFile")) {
            config.withFallback(
                ConfigFactory.parseFile(
                    Path.of(config.getString("secretsFile")).toFile()
                )
            )
        } else config
    }
}
