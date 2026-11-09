# Polo Launcher

Pantalla de inicio para la unidad Android del Polo. Todo funciona sin internet.

- Hora y fecha grandes. Se corrigen solas con la hora del GPS en cuanto hay señal.
- Amanecer y anochecer calculados con el GPS.
- Velocidad, rumbo y altitud por GPS.
- Recordatorios (ITV, seguro, impuesto, aceite, distribución, filtros, bujías, frenos, refrigerante, batería). Se editan desde la app: toca la tarjeta de recordatorios.
- 5 botones que abren apps: CarPlay (Zlink), Música (Bluetooth Music del sistema), Radio FM, Repostaje (registro propio de la app: km, litros y euros, calcula el consumo) y Ajustes. La primera vez intenta adivinar la app. Si no la encuentra, te deja elegirla. Mantén pulsado un botón para cambiar su app.

## Compilar el APK (elige una forma)

### A) Con GitHub, sin instalar nada
1. Crea una cuenta/repositorio en github.com (privado vale).
2. Sube todo el contenido de esta carpeta, incluida la carpeta oculta `.github`.
3. Pestaña **Actions** > **Build APK** > **Run workflow**.
4. Al terminar (unos 3-5 min), entra en la ejecución y descarga `PoloLauncher-apk` (dentro va `app-debug.apk`).

### B) Con Android Studio
1. Abre esta carpeta como proyecto.
2. Espera a que sincronice.
3. Menú **Build > Build APK(s)**. El archivo queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Instalar en la unidad
1. Copia `app-debug.apk` a un pendrive y conéctalo a la unidad.
2. Ábrelo con un gestor de archivos de la unidad y pulsa instalar (puede pedir permitir "orígenes desconocidos").
3. Abre la app **Polo Launcher** y acepta el permiso de ubicación.
4. Pulsa el botón Home. Android pregunta qué inicio usar: elige **Polo Launcher** y "Siempre".

Para volver al inicio original: Ajustes > Aplicaciones > Aplicaciones predeterminadas > Inicio.

## Si no sale el permiso de ubicación
Por ADB (con la unidad conectada al ordenador):
`adb shell pm grant es.jorge.pololauncher android.permission.ACCESS_FINE_LOCATION`

## Notas
- Los intervalos de mantenimiento que vienen de serie (aceite 10.000 km, distribución 90.000 km / 5 años, etc.) son orientativos. Cámbialos por los de tu manual o tu taller.
- Los km se ponen a mano (botón "Km actuales"). Sin ellos, los recordatorios por km no avisan.
- Esta app no se ha podido probar en una unidad real antes de entregarla: si algo falla, dime qué ves.

## Vídeo de bienvenida
Si existe el archivo `app/src/main/res/raw/intro.mp4`, la app lo reproduce a pantalla completa una vez cada vez que se enciende la unidad, y después muestra el inicio. Se salta tocando la pantalla. Si el vídeo falla o tarda demasiado (25 s), se cierra solo. Si el archivo no existe, la app arranca directa. El nombre tiene que ser exactamente `intro.mp4`, en minúsculas.
