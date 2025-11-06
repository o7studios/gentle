package gentle.async;

import lombok.experimental.UtilityClass;

@UtilityClass
class Utils {

    final int cores;

    static {
        cores = Runtime.getRuntime().availableProcessors();
    }
}
