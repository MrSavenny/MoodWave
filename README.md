# MoodWave · Etapas 2 y 3

## Abrir y ejecutar en Windows

1. En Android Studio elige **File → Open** y abre `C:\Users\dando\AndroidStudioProjects\MoodWave_Etapa2`, que contiene `settings.gradle.kts`. Si usas el ZIP de entrega, extráelo antes en otra carpeta.
2. Instala Android SDK Platform 37 desde SDK Manager si hace falta. Sincroniza el proyecto con **Sync Project with Gradle Files**. Usa el JBR incluido con Android Studio.
3. En la terminal PowerShell del proyecto ejecuta:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:ANDROID_HOME = "$env:USERPROFILE\AppData\Local\Android\Sdk"
.\gradlew.bat testDebugUnitTest assembleDebug
```

4. En Device Manager inicia un emulador Android 8.0 (API 26) o posterior. Selecciona la configuración **app**, el dispositivo y **Run ▶**. El APK de desarrollo se genera en `app/build/outputs/apk/debug/app-debug.apk`.
5. Para ejecutar las pruebas instrumentadas, usa un emulador de prueba dedicado y ejecuta `.\gradlew.bat --no-configuration-cache connectedDebugAndroidTest`.

## Recorrido de aceptación

- **Estadísticas → Demostración → Generar 90 días de prueba**. Selecciona **90 días** y abre **¿Por qué veo esto?**. Para Ejercicio deben aparecer 30 registros con actividad y 60 sin ella. Cambia a 7 días y comprueba el aviso de muestra insuficiente.
- Abre **Reportes, respaldo y datos de prueba** y toca **Reporte PDF mensual**. El diálogo muestra el origen, mes y tamaño de muestra; en demostración propone un mes con al menos 14 días registrados. Elige el periodo y pulsa **Guardar PDF**. Abre el archivo desde Archivos y revisa el mes, origen, días distintos, gráfica y conclusión. Selecciona **Reales** para un reporte personal.
- **Exportar JSON sin cifrar** guarda todos los registros, incluidos los de demostración. El formato es texto legible, esquema 1. Para probar la restauración, exporta en un emulador de prueba, borra solo la demo y usa **Restaurar respaldo** para seleccionar el archivo. La primera importación recupera los faltantes. Repite y debe indicar cero añadidos.
- La importación valida el archivo completo antes de escribir en Room. Compara todos los campos de contenido, excepto el ID local, y preserva la cantidad de registros idénticos presente en el respaldo. Un registro editado y su versión respaldada se conservan como entradas distintas. Límite de 15 MB y 30 000 registros.

## Alcance y pruebas

La matriz de los siete módulos, los criterios estadísticos y la evaluación del módulo D se encuentran en [README_ETAPA2.md](README_ETAPA2.md). Los índices 1–20 siguen el orden que usa la aplicación y no constituyen una escala clínica. La media móvil incluye observaciones anteriores al rango visible. El PDF excluye el texto del diario.

Las pruebas JVM cubren cálculos manuales, rachas, huecos, umbrales, lenguaje, detección del patrón sintético, límites de febrero bisiesto y restauración repetida. La prueba instrumentada usa un respaldo sintético, una base Room en memoria y el lector PDF de Android. El procedimiento manual anterior cubre el selector de archivos y la navegación.

La matriz actualizada de requisitos y los límites de verificación están en [docs/ENTREGA_FINAL.md](docs/ENTREGA_FINAL.md).

## Nivel 3 de IA · opción B, estimación del día siguiente

En **Estadísticas → Demostración → 90 días**, abre **Insights** y busca **Estimación para mañana**. El módulo B complementa las explicaciones del módulo D. Opera en el teléfono y usa únicamente el origen y periodo seleccionados.

Cada fecha aporta una observación con el índice y la energía promedio del día y el conjunto de actividades registradas. El modelo busca los cinco días históricos más similares sumando las distancias absolutas de ánimo y energía normalizadas por sus escalas y la distancia de Jaccard entre actividades. Promedia los índices observados al día siguiente de esos cinco ejemplos. Solo entran pares de fechas calendario consecutivas.

Para estimar mañana hacen falta un registro de hoy y 14 pares previos. La validación recorre las fechas en orden: predice cada día con los pares ya conocidos, mide el error y después incorpora el resultado. El error absoluto medio (MAE) está en puntos del índice 1–20. El modelo y la referencia que repite el ánimo del día anterior se evalúan sobre las mismas fechas. En 7 días se muestra el estado de datos insuficientes. Ante las señales de malestar detectadas por el panel, la tarjeta de predicción se oculta.
Evaluación reproducible con la demo de 90 días (semilla 2027): 89 pares consecutivos, 14 iniciales para entrenamiento y 75 predicciones evaluadas. MAE del modelo: 3.06 puntos. MAE de repetir el último ánimo: 5.31 puntos. El resultado corresponde a datos sintéticos.


## Vistas del panel

Dentro de Estadísticas, el selector superior alterna entre tres vistas. **Estadísticas** reúne la tendencia y el resumen numérico. **Actividades** muestra la distribución de menciones, los tamaños de muestra y las comparaciones con ánimo y energía. **Insights** presenta las conclusiones, su explicación y la estimación del módulo B. Las tres comparten el origen de los registros y el periodo seleccionado.

Capturas del emulador con datos simulados: [Estadísticas](docs/capturas/estadisticas.png), [Actividades](docs/capturas/actividades.png) e [Insights](docs/capturas/insights.png).

<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 49 PM (4)" src="https://github.com/user-attachments/assets/f7946398-9046-4ef6-83aa-8ccd8597e809" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 49 PM (3)" src="https://github.com/user-attachments/assets/b8bb02e0-7b18-4f3d-87b1-cecbf24d0b8a" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 49 PM (2)" src="https://github.com/user-attachments/assets/f060bf3c-c3db-418d-922b-63d488d03dbc" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 49 PM (1)" src="https://github.com/user-attachments/assets/f996edc9-0dc5-4e32-bda7-c1e8e7b27c77" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 49 PM" src="https://github.com/user-attachments/assets/3fd33997-a848-4881-8f14-6a40a7239724" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 48 PM (5)" src="https://github.com/user-attachments/assets/39690400-c5ec-40d3-a3a3-2e61168868d0" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 48 PM (4)" src="https://github.com/user-attachments/assets/9c7f27e9-2f91-42be-b0db-1e97caca46af" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 48 PM (3)" src="https://github.com/user-attachments/assets/a913a69c-91ea-4c82-8832-2d8e997e7b92" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 48 PM (2)" src="https://github.com/user-attachments/assets/8b69db39-0c68-4e89-b673-1d579304d929" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 48 PM (1)" src="https://github.com/user-attachments/assets/e8ba4578-f967-496f-872a-21dc321f2314" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 48 PM" src="https://github.com/user-attachments/assets/871cf2b0-c442-4942-8b08-7ad3a3b6f737" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 47 PM (2)" src="https://github.com/user-attachments/assets/58d4f80b-7a2b-463b-ad8e-e9ce6384ec73" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 47 PM (1)" src="https://github.com/user-attachments/assets/428932cd-46e9-4e68-895d-30afad9efaa5" />
<img width="720" height="1600" alt="WhatsApp Image 2026-11-05 at 3 56 47 PM" src="https://github.com/user-attachments/assets/e0c3615f-1f4a-4d59-812e-c11cb56b8454" />


## Historial, perfil y apariencia

En Inicio, toca **Historial de emociones** para desplegar la lista y toca cada entrada para ver sus detalles. Desde **Ajustes → Apariencia** puedes elegir **Sistema**, **Claro** u **Oscuro**. La opción Sistema sigue los cambios de Android y la selección se conserva al reiniciar.

**Elegir foto de mi Galería** abre el selector del sistema. MoodWave guarda una miniatura privada de la foto elegida para poder mostrarla al volver a abrir la app. Puedes reemplazarla con otra foto o con un emoji.

## Correcciones verificadas del reporte y de Inicio

La sugerencia de bienestar se muestra directamente encima del historial. El historial conserva su apertura independiente. Seguridad muestra únicamente la etiqueta «PIN de seguridad».

El reporte se genera antes de abrir el selector de destino. Su recepción se mantiene en la raíz de la app para completar la escritura aunque la pantalla de estadísticas desaparezca al bloquearse. Verificación manual en el emulador: se abrió Guardar PDF, se terminó el proceso de MoodWave mientras el selector permanecía abierto y después se guardó en Descargas. El archivo resultante tuvo 35 142 bytes y una página legible. También pasaron las 14 pruebas unitarias y assembleDebug.


## Respaldo protegido con contraseña

1. Abre **Estadísticas → Reportes, respaldo y datos de prueba → Crear respaldo con contraseña**.
2. Escribe y repite una contraseña de 12 a 128 caracteres. Guarda la contraseña para poder restaurar en otro dispositivo.
3. Elige el destino del archivo con extensión **.mwbackup**.
4. Para recuperarlo, pulsa **Restaurar respaldo**, selecciona el archivo y escribe su contraseña.
5. Revisa el número de registros y confirma **Restaurar**. Se conservan los datos actuales y se añaden las entradas que faltan.

La contraseña del respaldo es independiente del PIN de la app. La exportación manual no la guarda y no puede recuperarla. Si activas el respaldo automático en nube, su contraseña se almacena protegida por Android Keystore para poder cifrar en segundo plano. La exportación JSON legible sigue disponible con una confirmación explícita. Restaurar también acepta los JSON de la integración anterior.

### Formato y decisiones técnicas

El contenido es el mismo JSON validado de esquema 1, envuelto en un formato binario versionado. La cabecera contiene MWBK, versión 1, 600 000 iteraciones de PBKDF2-HMAC-SHA256, sal aleatoria de 16 bytes y nonce aleatorio de 12 bytes. AES-256-GCM autentica la cabecera y el contenido con una etiqueta de 128 bits. Una contraseña incorrecta o una alteración impiden obtener registros para importar. La longitud máxima del contenido es 15 MB y 30 000 registros.

Se utiliza una contraseña portable para poder restaurar en otro dispositivo. Android Keystore no se usa para vincular este archivo al teléfono de origen. El motor de cifrado usa las primitivas de Java/Android y se ejecuta fuera del hilo principal. Los archivos que se preparan antes de abrir el selector contienen el respaldo ya cifrado. Las contraseñas de exportación manual no forman parte del estado guardado de la actividad ni de las preferencias. La configuración opcional de nube se guarda cifrada con Android Keystore.

La importación usa la transacción Room existente y valida el contenido completo antes de escribir. Una segunda restauración del mismo archivo devuelve cero entradas nuevas.

Referencias de implementación: [criptografía en Android](https://developer.android.com/privacy-and-security/cryptography), [almacenamiento criptográfico de OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html) y [factor de trabajo de PBKDF2 en OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html).

Las pruebas de cifrado verifican contenido Unicode, sal y nonce distintos por exportación, contraseña incorrecta, cabecera alterada, contenido manipulado, truncamiento y límites de tamaño. La prueba Android restaura 90 registros en Room, repite la importación, carga el JSON anterior y comprueba que una contraseña incorrecta deja intacta la base de datos.

### Verificación de esta integración

- assembleDebug y 19 pruebas unitarias completadas.
- Tres pruebas instrumentadas completadas en un Pixel 7 de prueba, incluida la restauración cifrada en Room y la lectura del PDF.
- Exportación real desde el selector de Android de 90 registros sintéticos, con contraseña. El archivo generado tuvo 24 085 bytes.
- Se recreó el proceso de la app mientras el selector estaba abierto y se completó el guardado.
- Un descifrador independiente en Python verificó la cabecera, derivó la clave y recuperó los mismos 90 registros del archivo descargado.
- Se comprobó desde la interfaz el rechazo de una contraseña equivocada y la confirmación de restauración de los 90 registros con la contraseña correcta.


## Recordatorios, Health Connect y nube

- **Ajustes → Recordatorio diario**: activa las notificaciones, elige la hora y usa la prueba. La notificación abre el registro de ánimo. Android puede retrasar el aviso por ahorro de batería; se usa una ventana de 15 minutos, sin permiso de alarma exacta. Se reprograma tras reiniciar o cambiar la zona horaria.
- **Ajustes → Sueño y pasos · Health Connect**: concede solo los permisos que quieras. Consulta siete días y permite revocar el acceso. Sin permiso o sin registros se muestra ausencia de datos. No se escriben datos de salud ni se incluyen en respaldos. Requiere Health Connect disponible en el dispositivo.
- **Ajustes → Respaldo en la nube**: permanece desconectado por defecto. Necesita una carpeta WebDAV HTTPS existente, usuario, contraseña de aplicación y una contraseña de respaldo de 12 caracteres como mínimo. No se incluye un proveedor ni una cuenta. La casilla de consentimiento activa una copia diaria y el primer envío; también puedes solicitar un envío manual.
- Los envíos requieren conexión y usan WorkManager con reintentos exponenciales ante errores de red, HTTP 408, 429 y 5xx (hasta cinco intentos). HTTP 401/403 exige corregir credenciales. Cada copia recibe un nombre único; `If-None-Match: *` evita reemplazos. Si un reintento encuentra su archivo, compara el contenido antes de considerarlo enviado.
- **Ver respaldos del servidor** permite restaurar con confirmación, sin duplicar entradas idénticas, o eliminar una copia remota con confirmación. **Desconectar** cancela los envíos y elimina la configuración local, pero conserva los archivos remotos.
- La contraseña del respaldo y las credenciales del servidor se almacenan cifradas con una clave Android Keystore dentro de `noBackupFilesDir`. Solo viaja el diario cifrado; no se siguen redirecciones HTTP. Conserva la contraseña fuera de la app para recuperar las copias tras reinstalar.
- **Borrar TODOS mis registros** borra el diario local y desconecta la nube. Los PDF, respaldos exportados y copias remotas requieren eliminación en su destino.

Documentación técnica usada: [Health Connect](https://developer.android.com/health-and-fitness/health-connect/get-started) y [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager).


El recordatorio usa un selector de 12 horas con AM/PM (localizado según el teléfono); el botón confirma la hora guardada en ese mismo formato.
