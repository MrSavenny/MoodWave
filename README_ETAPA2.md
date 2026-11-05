# MoodWave — Etapa 2: Descubre tus patrones

Este directorio es una **continuación de la aplicación original** enviada por el usuario. No elimina la pantalla de registro, el calendario, la configuración, el bloqueo. El contenido analítico anterior se conserva en `StatsViewAesthetic`, pero la pestaña **Estadísticas** muestra el panel revisado `AnalyticsDashboard` para evitar publicar patrones sin tamaño de muestra suficiente.

## Instrucciones para probar en Android Studio

1. Abrir la carpeta del proyecto (contiene `settings.gradle.kts`).
2. Permitir `Sync Project with Gradle Files` e instalar Android SDK 37 si la instalación lo solicita. Mantener las versiones de Gradle/AGP que trae este proyecto, salvo que Android Studio muestre una incompatibilidad concreta.
3. Ejecutar la app en un dispositivo o emulador.
4. Entrar a **Estadísticas** y seleccionar **Demostración**.
5. Pulsar **Generar 90 días de prueba** y confirmar.
6. Revisar 7, 30 y 90 días. En 90 días, la comparación **Ejercicio** debe mostrar `n=30` con actividad y `n=60` sin ella y el índice de ánimo mayor en el primer grupo.
7. Pulsar **¿Por qué veo esto?** para ver fechas, promedios y método.
8. Pulsar **Borrar solo datos DEMO**. Los registros reales no deben alterarse.
9. Crear registros reales; seleccionar **Reales**. Las conclusiones solo aparecerán cuando se cumplan los umbrales.
10. En Reportes, respaldo y datos de prueba, crear un respaldo con contraseña y restaurarlo. La exportación JSON sin cifrar sigue disponible con confirmación explícita.

La integración 2 se compiló con el SDK 37 instalado y el JBR de Android Studio. Gradle generó el APK debug y pasaron 19 pruebas unitarias en la integración con cifrado.

## Nuevos archivos y modificaciones

- `analytics/MoodAnalytics.kt` — motor de estadísticas, tendencia, comparación por actividad e insights sin dependencias Android.
- `analytics/SyntheticDataGenerator.kt` — generador determinista de 90 días (semilla 2027).
- `ui/AnalyticsDashboard.kt` — interfaz Compose con gráficas de línea, barras y dispersión; selección de periodo y fuente; explicaciones y controles de datos.
- `data/MoodBackup.kt` — exportación/importación JSON versionado (`schemaVersion=1`) con validación de escala/fecha.
- `data/MoodDao.kt` — inserción masiva, borrado DEMO y eliminación de todos los registros.
- `ui/MoodViewModel.kt` — tareas de base de datos en corrutinas IO.
- `ui/HomeScreen.kt` — la pestaña Estadísticas apunta al nuevo panel.
- `data/MoodDatabase.kt` — se eliminó `fallbackToDestructiveMigration(true)` para no permitir borrados silenciosos en migraciones.
- `src/test/.../MoodAnalyticsTest.kt` — pruebas estadísticas, escenarios, lenguaje y detección heurística de riesgo.

## Criterios del Nivel 1

| Criterio | Cómo se atiende |
| --- | --- |
| Promedio, mediana, moda, desviación y rachas | `stats()` con estadístico poblacional descriptivo y desviación **muestral**; resumen semanal, mensual y del rango seleccionado |
| Tendencias 7/30/90 con media móvil | `trend()` con huecos para días sin registro y media móvil de 7 días calendario si hay >=3 días observados |
| Actividad vs. ánimo y energía | `activityResults()` muestra media de los grupos y diferencia contra media general; siempre n |
| Correlaciones justificadas | Diferencia de medias entre registros con y sin la actividad; d de Cohen con desviación combinada como tamaño de efecto |
| Tres tipos de gráficas | Línea (tendencia), barras (actividad vs. ánimo), dispersión (energía vs. ánimo) |
| Lenguaje responsable | Insights basados en plantillas solo exploratorias; nunca afirman causalidad o diagnóstico |
| Umbral | >=14 **días distintos** para investigar relaciones y >=5 observaciones en **cada grupo** para comparación |
| Datos sintéticos | 90 días reproducibles. Ejercicio cada tercer día con bienestar plantado mayor; datos reales y DEMO se analizan por separado |

### Advertencias estadísticas

`moodLevel` es el **orden de las 20 emociones de la interfaz original**, no un instrumento clínico validado ni una magnitud intervalar. Calcular promedios y d de Cohen sobre ese índice es una aproximación descriptiva para la práctica académica; no constituye un test de salud ni permite comparar personas. Se preservan las etiquetas y el valor original en Room.

La opción *diferencia de medias* se eligió porque las actividades son binarias/categóricas y se contrasta el índice emocional entre dos grupos de registros. Se admite que varias actividades ocurran el mismo día, que haya factores de confusión y que los registros de un usuario no sean independientes. No se calculan valores p; no se habla de significación estadística. Se muestran hasta 3 asociaciones ordenadas por frecuencia de actividad, y se advierte la multiplicidad de comparaciones. Los umbrales de d (0.2/0.5/0.8) son orientativos, no validan un resultado. También se exige diferencia absoluta >=1 unidad del índice.

Una racha usa días **distintos** (varios registros el mismo día cuentan una sola vez). La racha actual puede finalizar ayer si todavía no se ha registrado hoy. Para la media móvil se usan los registros de los 7 días calendario previos incluido el actual, ignorando huecos y exigiendo >=3 días observados, sin imputar ceros.

## Módulo de IA seleccionado: D. Generación de insights en lenguaje natural

Implementación **local determinista con plantillas**, opción expresamente permitida en el enunciado. Entradas: muestras con actividad, sin actividad, tamaño de efecto, medias y fechas. Salida: texto descriptivo, explicación y límites. No se emplea nube ni modelo generativo externo.

**Cold start**: con 3 días se enseñan estadísticas descriptivas, pero no se producen patrones. Con 90 días se analizan asociaciones si ambos grupos tienen suficientes registros y tamaño de efecto mínimo.

**Evaluación planteada**: cuatro casos con verdad esperada: efecto positivo plantado, efecto negativo plantado, efecto nulo equilibrado y solo 3 días. Métrica: accuracy de presencia/ausencia de algún insight. El baseline siempre responde «sin patrón» y acertaría 2/4 = 50%. La implementación está diseñada para acertar 4/4 = 100% en estos cuatro casos artificiales (`MoodAnalyticsTest.evaluationComparedToAlwaysNoPatternBaseline`). **El motor puro se compiló y evaluó de forma independiente con `kotlinc`: obtuvo 4/4 en esos escenarios simulados; no son datos de personas reales**. JUnit confirmó esos resultados en la integración 2. Otras pruebas verifican n, moda, desviación, tendencias y frases responsables.

## Ética y privacidad

- Mensaje fijo: «MoodWave no realiza diagnósticos médicos ni sustituye atención profesional».
- Si se observan frases de autolesión, un detector local sencillo prioriza un texto de apoyo y emergencias. También advierte ante >=5 días consecutivos con índice <=5. La heurística tiene falsos positivos/negativos y **no es un detector clínico**, especialmente con negaciones, ironía o español coloquial.
- Se puede revisar, exportar en JSON y eliminar todo el historial; las importaciones son **aditivas**, con restauración transaccional que conserva los registros existentes y su multiplicidad.
- `MoodBackup` genera JSON **sin cifrado**; no enviarlo a terceros. El botón Crear respaldo con contraseña protege ese contenido con AES-256-GCM y PBKDF2-HMAC-SHA256.
- Se desactivó el backup automático de Android para no enviar el diario privado a las copias del sistema. El usuario decide cuándo exportar archivos explícitamente.
- El esquema Room sigue en `version=3`, **sin cambios de columnas**. La app original usaba borrado destructivo en migraciones. Ahora se ha deshabilitado, por lo que una instalación más antigua con esquema diferente exigirá migración explícita o respaldo previo. No desinstalar sin haber respaldado los registros.

## Avances vs. pendientes del enunciado

**Implementado en código de esta entrega**: Nivel 1 completo como primera integración; Nivel 3 opción D con pruebas automatizadas; controles de datos, aviso y recursos básicos de apoyo; respaldo cifrado y compatibilidad con JSON anteriores.

**Integración 2**: el reporte PDF usa el mes calendario elegido, una serie diaria, barras por actividad cuando hay suficiente muestra y una conclusión del motor local. Identifica el origen personal o sintético.

**Integración avanzada actual**: Health Connect (lectura de sueño/pasos), respaldo cifrado WebDAV con WorkManager y recordatorios configurables implementados. Consulta `docs/ENTREGA_FINAL.md` para pruebas y límites; un servidor real y los datos de un wearable requieren configuración externa.

## Entregables finales

- APK o demo funcional de Android Studio.
- Repositorio con `README_ETAPA2.md`, capturas y decisiones técnicas.
- Video demostrativo mostrando generación de registros, patrones, explicaciones, casos con datos insuficientes y eliminación DEMO.
