package com.hans.gesticar.model

//enum class define un tipo cerrado de valores posibles.
//OtState representa el ciclo de vida de una Orden de Trabajo.
enum class OtState { BORRADOR, DIAGNOSTICO, PRESUPUESTO, PEND_APROB, EN_EJECUCION, FINALIZADA, CANCELADA }

//Tipifica cada ítem del presupuesto.
//REP: repuesto (tiene cantidad y PU).
//MO: mano de obra (puede ser por horas o por tarea).
enum class ItemTipo { REP, MO }

/*Clasifica fotos/evidencias por momento:
INGRESO: estado inicial del vehículo (abre discusión de daños previos).
EJECUCION: avances, piezas reemplazadas.
CIERRE: verificación final (requisito para cerrar en tus reglas).
Útil para filtrar/mostrar evidencias por tab en la UI.*/
enum class EvidenciaEtapa { INGRESO, EJECUCION, CIERRE }