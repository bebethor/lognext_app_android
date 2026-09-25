# Corrección del rechazo de Google Play — 23 septiembre 2026

## Login correcto pero sin datos — revisión del flujo de API

Se identificó que el login solicitaba `User.Read` de Graph y luego intentaba adquirir silenciosamente el token del backend. Si este segundo paso fallaba, el error se descartaba y se abría la app, con peticiones sin Authorization. Home también ocultaba los errores de carga. Esto es compatible con la diferencia entre una instalación nueva y una con tokens previos, pero no se dispone de una traza del móvil que confirme la causa del incidente.

El login ahora solicita directamente el scope `api://8cebd652-bfab-4e10-ae5a-c67214ac9d03/access_as_user`. La restauración comprueba acceso al token del backend, se delega en MSAL la caché y renovación en lugar de guardar indefinidamente el token en memoria, y las peticiones sin token fallan explícitamente. Home muestra los errores y permite reintentar. No se cambiaron permisos en Entra ni la URL de la API.

Validar con un APK nuevo firmado con la misma clave: cerrar sesión, volver a entrar y comprobar datos con la misma cuenta y red que Android Studio. Si Microsoft rechaza el scope o exige consentimiento, recoger el error visible para el administrador. Si la API responde 401/403 o falla la conexión, recoger el mensaje mostrado antes de atribuir el problema a permisos o al servidor.

## Instalación directa de APK — actualización del 24 septiembre

El APK local `app/debug/app-debug.apk` se comprobó con apksigner y está firmado con la clave de subida, SHA-1 `05:A0:42:E4:29:BA:2A:81:8E:53:D3:65:15:67:BC:6E:B0:00:95:58`. Esta firma es distinta de las de distribución de Play y de la de desarrollo.

Se añadió una cuarta configuración MSAL para admitir instalaciones directas con esa clave. En el mismo registro de Entra indicado abajo, añadir una plataforma Android con paquete `com.lognext.nexterandroid` y Signature hash `BaBC5Cm6KoGOU9NlFWe8brAAlVg=`. URI esperada:

```text
msauth://com.lognext.nexterandroid/BaBC5Cm6KoGOU9NlFWe8brAAlVg%3D
```

Conservar las entradas de Play y desarrollo. Después de registrar esta firma, generar un APK nuevo con los cambios y firmarlo con la misma clave de subida para instalarlo. El APK anterior no incorpora esta corrección. Este registro adicional sirve para instalación directa; las pruebas finales de distribución siguen requiriendo instalación desde Play.

## Diagnóstico

Google rechazó la versión 100100 (10.1.0) por un elemento que no responde. La captura IN_APP_EXPERIENCE-4482.png marca SIGN IN en la pantalla inicial.

La configuración MSAL incluía únicamente el hash `254KWMWKtDuxHRVMIEJSTVi/tmE=` (SHA-1 `DB:9E:0A:58:C5:8A:B4:3B:B1:1D:15:4C:20:42:52:4D:58:BF:B6:61`). No coincide con las firmas de distribución consultadas en Play Console. MSAL 2.2.3 activa la validación de la firma por defecto y rechaza esta discrepancia al inicializarse. Además, HomeScreen descartaba AuthState.Error.message, dejando el mismo botón sin explicación.

Esto explica la captura, aunque no disponemos del registro de ejecución del dispositivo del revisor. La validación final requiere una instalación distribuida por Play.

## Acción necesaria del administrador de Microsoft Entra

En **Registros de aplicaciones → Lognext → Autenticación → Añadir una plataforma → Android**, registrar ambas firmas de distribución para la aplicación existente:

- Application (client) ID: `02734a6d-c892-48c3-9388-b548ac422deb`
- Tenant ID: `b3c806dc-e5d8-44d0-b9e2-aedd316928ea`
- Package name: `com.lognext.nexterandroid`

| Firma de Play | SHA-1 del certificado | Signature hash para Entra |
| --- | --- | --- |
| Anterior, usada desde 9 septiembre 2026 | `85:F2:E8:24:46:B2:53:FE:45:54:0E:BD:83:25:30:97:74:05:40:AC` | `hfLoJEayU/5FVA69gyUwl3QFQKw=` |
| Actual, clave clásica | `9C:62:3C:D6:9C:AD:7E:CE:92:E8:50:C1:48:FF:08:07:4A:57:DA:80` | `nGI81pytfs6S6FDBSP8IB0pX2oA=` |

URI esperadas en la configuración Android, con el hash codificado para URL:

```text
msauth://com.lognext.nexterandroid/hfLoJEayU%2F5FVA69gyUwl3QFQKw%3D
msauth://com.lognext.nexterandroid/nGI81pytfs6S6FDBSP8IB0pX2oA%3D
```

Conservar el registro de la firma de desarrollo existente. No usar la huella de la **clave de subida** como sustituta de las firmas de distribución. No cambiar permisos, secretos, acceso condicional ni MFA para solucionar esta discrepancia.

El administrador debe confirmar que las dos entradas existen y están guardadas. No se han realizado cambios en Entra desde esta tarea.

## Cambios Android

- Selección de configuración MSAL según el certificado instalado, admitiendo la firma existente y ambas firmas de Play. Se usa la misma consulta de firmas que MSAL 2.2.3 para respetar el historial de rotación.
- Callbacks exactos de las dos firmas de Play en AndroidManifest.xml. La validación de firma de MSAL permanece activada.
- Los errores de autenticación se muestran y el botón permite reintentar.
- La adquisición silenciosa del token del backend se ejecuta en Dispatchers.IO.
- Los callbacks ignoran continuaciones canceladas y se propaga la cancelación de corrutinas.
- Nueva versión 100101 (10.1.1).

## Antes de reenviar

1. Obtener confirmación del administrador de Entra.
2. Generar y firmar el AAB release 100101 con la clave de subida habitual. La compilación Gradle de este repositorio no configura firma release: su AAB de salida está sin firmar. `app/release/app-release.aab` es el artefacto anterior y no debe reenviarse.
3. Subirlo al canal de pruebas internas e instalar **desde Google Play**. Una instalación directa de Android Studio no valida la firma de Play; Internal App Sharing también puede usar otra firma.
4. Comprobar instalación limpia: SIGN IN abre Microsoft, cancelar permite reintentar, credenciales de prueba válidas completan el acceso, cerrar y abrir restaura sesión y cerrar sesión permite volver a entrar. Repetir sin red y comprobar que cualquier error sea visible. Probar con y sin Microsoft Authenticator, si se usa en la organización, y en versiones Android afectadas por la rotación de firma.
5. Comprobar Home, Jornada, People y Más con la cuenta de revisión. Verificar que Google dispone de credenciales e instrucciones de acceso válidas en Contenido de la aplicación → Acceso a la aplicación.
6. Tras las pruebas, enviar la nueva versión a revisión. El correo de Google indica que no es necesario apelar para reenviar una corrección menor.

Referencia de Microsoft sobre este fallo: https://learn.microsoft.com/en-us/troubleshoot/entra/entra-id/app-integration/android-app-authentication-fails-after-published-to-google-play-store
