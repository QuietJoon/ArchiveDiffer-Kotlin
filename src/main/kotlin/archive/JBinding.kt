package archive

import Path
import net.sf.sevenzipjbinding.*
import java.io.File


fun jBindingChecker(aState: Boolean, aSZJBPath: Path): Boolean {
    return try {
        if (aState)
            SevenZip.initSevenZipFromPlatformJAR()
        else
            SevenZip.initSevenZipFromPlatformJAR(File(aSZJBPath))
        true
    } catch (e: SevenZipNativeInitializationException) {
        println("Fail to initialize 7-Zip-JBinding library")
        e.printStackTrace()
        false
    }
}

fun jBindingClear(aSZJBPath: Path) {
    File(aSZJBPath).deleteRecursively()
}
