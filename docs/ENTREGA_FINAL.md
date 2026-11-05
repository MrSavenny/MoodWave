# Entrega de MoodWave · 4 de noviembre de 2026

## Correspondencia con PRY2-20271.pdf

| Requisito | Implementación / evidencia |
| --- | --- |
| Estadísticas semanales y mensuales | Promedio, mediana, moda, desviación muestral, días y racha; pruebas con cálculos esperados |
| Tendencias | Rangos 7/30/90, huecos y media móvil |
| Actividad y ánimo/energía | Medias, diferencia y tamaños de muestra |
| Correlaciones justificadas | Diferencia de medias y d de Cohen; límites descritos en README_ETAPA2 |
| Tres gráficas | Línea, barras y dispersión |
| Lenguaje y umbrales | Plantillas sin causalidad/diagnóstico; 14 días y 5 observaciones por grupo |
| Generador | 90 días sintéticos, semilla reproducible y separación de datos reales |
| Health Connect | Lectura consentida de sueño/pasos de siete días; permisos parciales, rechazo, ausencia de datos y revocación |
| PDF mensual | PDF real con resumen, gráficas y conclusiones; exportación validada previamente tras recrear proceso |
| Respaldo | JSON esquema 1, validación antes de restaurar, transacción y deduplicación |
| Cifrado | AES-256-GCM, PBKDF2-HMAC-SHA256; exportación protegida y contraseña independiente del PIN |
| Nube | WebDAV HTTPS opcional, WorkManager, restricción de red, reintentos, versiones independientes, restauración y borrado remoto |
| Recordatorios | Hora configurable, permiso Android, aviso diario inexacto y apertura del registro |
| IA elegida: D | Plantillas explicables, pruebas de lenguaje, 4/4 casos sintéticos frente a baseline 2/4 |
| Extensión IA B | KNN temporal local: MAE 3.06 frente a 5.31 en 75 predicciones sintéticas; usa ánimo, energía y actividades, todavía no usa sueño/pasos |
| Cold start y explicación | Tres días no generan patrones; cada conclusión expone muestras, rango y método |
| Seguridad y control | Aviso no médico, apoyo ante señales de riesgo, consulta, exportación y borrado del diario; nube requiere consentimiento |
| Entregables | APK debug, fuentes con README y capturas, video del módulo de IA |

El módulo de IA seleccionado para satisfacer el enunciado es **D**. No se afirma haber implementado todas las alternativas A–E: el documento solicita elegir al menos una. La extensión B está documentada con sus variables y límites.

## Pruebas realizadas

- `testDebugUnitTest assembleDebug assembleDebugAndroidTest`: BUILD SUCCESSFUL; 26 pruebas JVM, cero fallos.
- `connectedDebugAndroidTest`: cuatro pruebas, cero fallos, en Pixel 7 virtual con Android 17. Se usó una copia aislada con applicationId `com.example.moodwave.finalqa`, sin tocar los registros de la instalación del usuario.
- Las pruebas de nube usan MockWebServer local: contenido cifrado, PUT condicional, conflicto idéntico/diferente, listado WebDAV, rechazo de XML con DOCTYPE, redirecciones y validación HTTPS/reintentos. No se enviaron datos a servicios externos.
- Keystore Android: configuración guardada cifrada, recuperación de credenciales y eliminación comprobadas.
- Interfaz real: permisos de Health Connect concedidos y lectura sin datos; pantalla de nube desconectada; permiso de notificaciones y aviso de prueba; pulsarlo abrió el formulario de emoción.
- Las verificaciones anteriores de galería, apariencia, PDF y respaldo cifrado están detalladas en README.md.

## Límites de verificación y uso

No hay un servicio de nube contratado/configurado: falta probar una cuenta WebDAV real, sus certificados y políticas, y un ciclo prolongado de reintentos en segundo plano. La prueba local valida el cliente HTTP y los conflictos, no acredita una sincronización real en producción. La app permanece desconectada hasta que se introduzcan los datos y se autorice el envío.

El emulador no contiene registros de un wearable. Se comprobó el flujo real de permisos y la lectura vacía; falta validar valores reales de sueño/pasos con un proveedor que escriba en Health Connect. Tampoco se esperó un día completo para comprobar una alarma diaria; sí se probaron los cálculos de horario y la notificación con su destino.

Las métricas de IA corresponden a datos artificiales. No demuestran eficacia clínica ni capacidad predictiva en personas. Room conserva la versión 3 del esquema; Health Connect se consulta sin copiar datos a la base, y la nube utiliza el formato de respaldo existente. La base local Room no está cifrada; el requisito implementado del PDF es cifrado del respaldo.

## Recorrido para presentar

1. Abrir el proyecto en Android Studio y ejecutar `app`, o instalar el APK de desarrollo.
2. Estadísticas → Insights → Reportes, respaldo y datos de prueba → Generar 90 días de prueba.
3. Seleccionar Demostración y 90 d. Mostrar Estadísticas, Actividades e Insights.
4. Abrir ¿Por qué veo esto? y explicar n, comparación, tamaño de efecto y ausencia de causalidad.
5. Mostrar la estimación y su evaluación temporal. Cambiar a 7 d para mostrar datos insuficientes.
6. Exportar un PDF mensual y un respaldo con contraseña.
7. En Ajustes mostrar recordatorios, sueño/pasos y nube opcional. No introducir credenciales en una grabación.

El video `demo/MoodWave-IA.mp4` muestra la interfaz ejecutándose en el emulador con datos sintéticos. Las capturas se encuentran en `capturas/`.


## Ajustes de hora y reporte PDF

El selector del recordatorio usa formato de 12 horas con AM/PM localizado, y el botón muestra la hora seleccionada con su periodo. El reporte PDF filtra siempre por el origen seleccionado (datos reales o demostración) y por el mes del diálogo. Como la demo abarca 90 días, el diálogo recomienda por defecto el mes más reciente con al menos 14 días registrados; también enseña origen, periodo, cantidad de registros y días distintos antes de exportar. Un mes corto se identifica como tal y explica el umbral. El PDF imprime el origen y distingue falta de muestra por días, por grupo de actividad o por tamaño de efecto.
