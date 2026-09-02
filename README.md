# GTA_SA-mapApp

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-0095D5?style=for-the-badge&logo=kotlin&logoColor=white)
![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=for-the-badge&logo=android&logoColor=white)
![Mapbox](https://img.shields.io/badge/Mapbox-000000?style=for-the-badge&logo=mapbox&logoColor=white)

Una aplicación Android interactiva basada en el icónico minimapa de *Grand Theft Auto: San Andreas*. Esta herramienta permite a los usuarios rastrear su ubicación en tiempo real y guardar puntos de interés (Pisos Francos, Tiendas) en un mapa con la estética clásica del videojuego.

> **💡 Nota para reclutadores:** Este proyecto demuestra habilidades en arquitectura Android moderna, inyección de mapas complejos, persistencia de datos locales y consumo de APIs de geolocalización.

---

## ✨ Características Principales (Casos de Uso)

*   **📍 Radar en Tiempo Real:** Rastreo de la ubicación y orientación del usuario usando `LocationPuck` de Mapbox, simulando el comportamiento del personaje en el juego.
*   **🛣️ Trazado de Rutas (Directions API):** Cálculo de la ruta más corta desde la posición actual hasta un destino marcado en el mapa.
*   **💾 Gestión de Puntos Relevantes (CRUD):** Creación, lectura, actualización y eliminación de marcadores personalizados (ej. Pisos Francos, Gimnasios) almacenados localmente.
*   **🗺️ Buscador Híbrido:** Búsqueda en tiempo real de los puntos guardados por el usuario, con cámara animada hacia el resultado.
*   **🎮 UI/UX Temática:** Interfaz construida íntegramente de forma declarativa con Jetpack Compose, incluyendo un HUD ocultable.

---

## 🛠️ Tecnologías y Arquitectura

El proyecto sigue las mejores prácticas de desarrollo en Android utilizando el stack tecnológico moderno:

*   **Lenguaje:** Kotlin
*   **UI:** Jetpack Compose (Animaciones, LazyGrids, Transiciones de estado).
*   **Mapas y Rutas:** Mapbox Maps SDK v11 y Mapbox Directions API.
*   **Persistencia Local:** Room Database (SQLite) implementando el patrón DAO para operaciones CRUD complejas con `INNER JOIN`.
*   **Redes / API:** Retrofit 2 & OkHttp (Para solicitudes a la API de Geocoding y Rutas de Mapbox).
*   **Geometría Espacial:** Mapbox Turf para el cálculo de distancias (enforzando reglas de negocio, ej. distancias mínimas entre POIs).

---

## 📱 Capturas de Pantalla / Demo

![alt text](image-1.png)
![alt text](image.png)
![alt text](image-2.png)
![alt text](image-3.png)
![alt text](image-4.png)

| Mapa Principal | Formulario de Puntos (Compose) | Gestión de Base de Datos (Room) |
| :---: | :---: | :---: |
| <img src="URL_A_TU_IMAGEN_O_GIF_1" width="200"/> | <img src="URL_A_TU_IMAGEN_O_GIF_2" width="200"/> | <img src="URL_A_TU_IMAGEN_O_GIF_3" width="200"/> |

---

## 🚀 Instalación y Pruebas

Si deseas clonar el proyecto y compilarlo localmente:

1.  Clona este repositorio:
    ```bash
    git clone [https://github.com/raul-burga-t/GTA_SA-mapApp.git](https://github.com/raul-burga-t/GTA_SA-mapApp.git)
    ```
2.  Abre el proyecto en **Android Studio**.
3.  Obtén un Token Público en [Mapbox](https://www.mapbox.com/) y colócalo en el archivo `strings.xml`:
    ```xml
    <string name="mapbox_access_token">sk.eyJ1IjoicmF1bDIwMDUi...</string>
    ```
4.  Sincroniza Gradle y ejecuta la aplicación en un dispositivo físico (Recomendado para el uso de GPS).

---

## 👨‍💻 Sobre el Desarrollador

Desarrollado por **Raúl Armando Burga Tupayachi**. 

Estudiante de Ingeniería Informática con enfoque en el desarrollo móvil nativo Android y arquitectura de software.

*   [LinkedIn](www.linkedin.com/in/raul-armando-burga-tupayachi-04a569433)
*   [Portafolio / GitHub](https://github.com/raul-burga-t)