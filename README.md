# Gesticar

**Autor:** Hans Marmolejo

Gesticar es una aplicación Android desarrollada con Jetpack Compose para administrar órdenes de trabajo (OT) de un taller mecánico. Permite que administradores y mecánicos gestionen clientes, vehículos y el estado de las OTs de forma sencilla.

## Funcionalidades principales

- Autenticación de usuarios con roles de Administrador y Mecánico.
- Creación de nuevas órdenes de trabajo asignando mecánicos y registrando detalles de clientes y vehículos.
- Búsqueda y consulta de órdenes de trabajo existentes para visualizar su información detallada.
- Gestión de tareas, presupuestos y estados asociados a cada OT.

## Requisitos previos

- [Android Studio](https://developer.android.com/studio) Giraffe o superior.
- JDK 17 instalado y configurado en el sistema.
- Emulador Android o dispositivo físico con Android 8.0 (API 26) o superior.

## Pasos para ejecutar el proyecto

1. Clona el repositorio:
   ```bash
   git clone <url-del-repositorio>
   cd gesticar
   ```
2. Abre el proyecto en Android Studio usando la opción **Open an Existing Project** y selecciona la carpeta del repositorio.
3. Permite que Gradle sincronice las dependencias del proyecto.
4. Configura un dispositivo de prueba (emulador o físico) desde el panel **Device Manager**.
5. Presiona el botón **Run** (▶️) en Android Studio para compilar e instalar la aplicación.

Una vez iniciada, podrás autenticarte con las credenciales configuradas en la base de datos local y comenzar a gestionar las órdenes de trabajo del taller.
