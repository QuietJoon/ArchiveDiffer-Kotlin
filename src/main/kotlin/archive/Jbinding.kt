package archive

import net.sf.sevenzipjbinding.*


fun jBindingChecker(): Boolean{
    return try {
        SevenZip.initSevenZipFromPlatformJAR()
        true
    } catch (e: SevenZipNativeInitializationException) {
        println("Fail to initialize 7-Zip-JBinding library")
        e.printStackTrace()
        false
    }
}
