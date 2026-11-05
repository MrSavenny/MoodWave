# Evaluación de cumplimiento — PRY2-20271

## Estimación de cobertura de implementación: 96%

Es una estimación de cobertura de funciones, no una calificación oficial. Separa lo que está implementado en código de lo que requiere datos o servicios externos para validarse en el teléfono.

| Área | Estado | Evidencia / límite |
| --- | --- | --- |
| Núcleo obligatorio | Implementado | Estadísticas, tendencias 7/30/90, actividad vs. ánimo/energía, diferencia de medias, tres gráficas, lenguaje descriptivo y umbrales de 14 días/5 registros. Pruebas JVM y demo reproducible de 90 días documentadas. |
| Tamaño de muestra y efecto | Implementado | Muestra n y d de Cohen, diferencia relevante y umbrales visibles. No calcula valores p ni afirma significación estadística. La pantalla advierte que explorar varias actividades puede producir diferencias por azar. |
| Health Connect | Integrado; datos externos aún no visibles | El usuario confirmó permisos de pasos y sueño en el teléfono. La consulta reciente devolvió registros vacíos para los siete días mostrados. La interfaz ahora distingue “Sin registros” de “Sin permiso” e indica cómo comprobar el periodo. |
| Reporte PDF y respaldo cifrado | Implementado | PDF mensual, JSON versionado y respaldo AES-GCM/PBKDF2 probados según `docs/ENTREGA_FINAL.md`. |
| Nube WebDAV | Implementado; producción sin validar | El cliente y conflictos se prueban con MockWebServer. No hay servicio WebDAV/cuenta externa disponible para probar ciclo real, conectividad intermitente y reintentos prolongados. |
| Recordatorios | Implementado; ciclo real pendiente | Preferencia, permisos, programación y apertura del registro implementados. Falta esperar un ciclo en el teléfono tras reinicio y con sus restricciones de batería. |
| Módulo IA | Cumplido con opción D | Insights locales por plantillas, explicables y con evaluación sintética. Se incluye extensión B de predicción con MAE sintético frente a baseline; no usa sueño/pasos. |
| Ética, privacidad y control | Implementado con límites documentados | Aviso no médico, consulta/exportación/borrado, consentimiento para nube y mensaje de apoyo. Ante señales locales de riesgo o ánimo bajo sostenido se pausan patrones y predicción. El detector heurístico puede fallar y no es clínico. Room no cifra el diario local. |
| Entregables | Incluidos | Fuentes, README, capturas, APK anterior y video. Los cambios recientes de seguridad/interfaz deben compilarse de nuevo antes de reinstalar el APK. |

## Validaciones que requieren ayuda desde el teléfono o un servicio externo

1. En Health Connect, confirma primero que la entrada de pasos/sueño aparece en la propia app Health Connect en una fecha dentro de los últimos siete días. Después, en MoodWave, toca **Actualizar**. El permiso activo por sí solo no crea ni garantiza que existan registros para leer.
2. Para validación de producción en nube hace falta una carpeta WebDAV HTTPS de prueba. Introduce usuario, contraseña de aplicación y consentimiento solo en MoodWave; nunca envíes credenciales por chat. Revisa envío, listado, restauración y desconexión.
3. Para el recordatorio, comprueba que llega al horario elegido y se vuelve a programar al reiniciar; el modo de ahorro de batería puede retrasar alarmas.
4. Revisión humana de las frases de apoyo y del detector. La app no sustituye servicios de emergencia ni atención profesional.

Una mejora opcional es cifrar también la base Room local si el alcance del curso lo exige; el PDF exige explícitamente el cifrado del respaldo exportado, que ya está implementado.

## Cómo interpretar el porcentaje

La opción D satisface la regla de elegir al menos un módulo de IA. Las métricas de IA usan datos artificiales y no prueban eficacia en personas. El descuento reconoce validaciones reales de Health Connect, WebDAV y alarmas que dependen del entorno del usuario, no módulos principales ausentes.
