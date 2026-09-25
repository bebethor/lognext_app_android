package com.lognext.nexterandroid.core.auth

import android.content.Context
import android.content.pm.PackageManager
import android.util.Base64
import com.lognext.nexterandroid.R
import java.security.MessageDigest

internal object MsalSigningConfiguration {
    const val DEVELOPMENT_HASH = "254KWMWKtDuxHRVMIEJSTVi/tmE="
    const val PLAY_ORIGINAL_HASH = "hfLoJEayU/5FVA69gyUwl3QFQKw="
    const val PLAY_CURRENT_HASH = "nGI81pytfs6S6FDBSP8IB0pX2oA="
    const val UPLOAD_HASH = "BaBC5Cm6KoGOU9NlFWe8brAAlVg="

    fun resourceForHash(hash: String): Int = when (hash) {
        DEVELOPMENT_HASH -> R.raw.msal_auth_config
        PLAY_ORIGINAL_HASH -> R.raw.msal_auth_config_play_original
        PLAY_CURRENT_HASH -> R.raw.msal_auth_config_play_current
        UPLOAD_HASH -> R.raw.msal_auth_config_upload
        else -> error("Esta firma de la aplicación no está configurada para iniciar sesión. Contacta con soporte de Lognext.")
    }

    @Suppress("DEPRECATION")
    fun resourceForInstalledApp(context: Context): Int {
        // Match MSAL 2.2.3's signature lookup, including the original signer after key rotation.
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
        val signature = requireNotNull(info.signatures?.firstOrNull()) {
            "No se pudo comprobar la firma de la aplicación."
        }
        val hash = Base64.encodeToString(
            MessageDigest.getInstance("SHA-1").digest(signature.toByteArray()),
            Base64.NO_WRAP
        )
        return resourceForHash(hash)
    }
}
