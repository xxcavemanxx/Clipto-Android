package clipto.presentation.lockscreen

import android.Manifest
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.fingerprint.FingerprintManager
import android.os.Build
import androidx.core.content.ContextCompat

object FingerprintUtils {

    fun isFingerprintAvailable(context: Context): Boolean {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return false
            }
            //USE_FINGERPRINT has normal protection level. So we do not have to ask for permission at run time
            //but we should check we have permission anyway
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
            //Get an instance of KeyguardManager and FingerprintManager
            val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    ?: return false
            val fingerprintManager = context.getSystemService(Context.FINGERPRINT_SERVICE) as? FingerprintManager
                    ?: return false

            //Check whether the device has a fingerprint sensor
            if (!fingerprintManager.isHardwareDetected) {
                return false
            }

            //Check that the user has registered at least one fingerprint
            return if (!fingerprintManager.hasEnrolledFingerprints()) {
                false
            } else keyguardManager.isKeyguardSecure
        } catch (e: Exception) {
            return false
        }
    }
}