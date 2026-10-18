# GymList

App Android para diseñar tu rutina de gimnasio y seguirla mientras entrenas, pensada para
alguien que acaba de empezar.

## Qué hace ahora mismo

- **Catálogo de 643 ejercicios**, todos con imágenes y explicación en español: ilustración de la
  posición inicial y la final, para qué sirve el ejercicio, qué músculos trabaja, los pasos de
  ejecución numerados y los fallos típicos a evitar.
- **Marcadores de series.** Cada ejercicio muestra una fila por serie con su objetivo de
  repeticiones, lo que has hecho de verdad y el peso usado. El botón ✓ la marca como hecha.
- **Temporizador de descanso hacia atrás.** Al marcar una serie arranca solo, con el descanso
  configurado en la rutina. Se puede pausar, ajustar ±15 s o saltar. Al llegar a cero vibra y
  pita, porque el móvil suele estar en el banco y no en la mano.
  La barra del descanso vive encima de la barra de navegación, así que **sigue contando** aunque
  te vayas al calendario o abras la ficha de otro ejercicio.
- **Botón (i)** en cada ejercicio, en todas las pantallas, que abre esa explicación.
- **Seguimiento tipo calendario.** Vista de mes con los días entrenados marcados; al tocar un día
  salen los entrenamientos de ese día, y cada uno abre su ficha con todas las series.
- **Diseño de rutinas.** Crear rutinas, añadir ejercicios del catálogo (con buscador y filtros por
  zona, dificultad y "sin material"), reordenarlos y ajustar series / repeticiones / descanso.
- **Plan personalizado.** Siete preguntas (objetivo, experiencia, días por semana, tiempo por
  sesión, dónde entrenas y con qué material, molestias y zona prioritaria) y la app monta un plan
  semanal completo: reparte los días (cuerpo completo, torso/pierna o empuje/tirón/pierna), elige
  ejercicios que se pueden hacer con ese material sin cargar las zonas con molestias, y les pone
  series, repeticiones y descanso según el objetivo y el nivel. Antes de guardarlo se puede
  cambiar cualquier ejercicio por otro que trabaje lo mismo. Cada día se guarda como una rutina
  normal. No usa red ni IA: son reglas y una tabla de ejercicios por patrón de movimiento.
- El peso de cada serie se **prerellena con el último que usaste** en ese ejercicio.
- Si cierras la app a media sesión, al volver la retomas donde la dejaste.
- Todo funciona **sin conexión**: las imágenes van dentro de la app.

Las tres rutinas que vienen cargadas son solo ejemplos: se pueden editar y borrar.

## Abrir el proyecto

Android Studio → *Open* → carpeta `GymList`. El `local.properties` ya apunta al SDK de esta
máquina. Requisitos que ya cumple el entorno: JDK 21 (el que trae Android Studio), SDK 36.

```bash
./gradlew :app:assembleDebug      # APK de depuración
./gradlew :app:installDebug       # instalar en el dispositivo/emulador conectado
./gradlew :app:testDebugUnitTest  # tests del generador de planes (en la JVM, sin emulador)
```

El APK de release pesa unos 20 MB, de los que 18 MB son las imágenes de los ejercicios.

## Catálogo de ejercicios y licencia

Los ejercicios vienen del **catálogo libre de RepDB** (https://repdb.co), que incluye nombre,
descripción, instrucciones y consejos ya en español, más una o dos imágenes por ejercicio.

```
app/src/main/assets/exercises_es.json      643 ejercicios (700 KB)
app/src/main/assets/exercise_images/       1138 imágenes WebP de 512 px (18 MB)
```

> **Estos ficheros no están en el repositorio.** La licencia de RepDB no permite republicar el
> catálogo como dataset, así que están en `.gitignore`. El código compila sin ellos, pero la app
> arranca con el catálogo vacío. El catálogo original está en https://repdb.co.

> ⚠️ **La atribución es obligatoria.** La licencia permite usar los datos y las imágenes dentro
> de la app, gratis y también comercialmente, a cambio de un enlace visible al usuario. Está en
> la pantalla **Acerca de** (icono ⓘ de la barra superior) y **no se puede quitar** sin
> incumplir la licencia. La licencia completa está en `LICENSE-RepDB-datos.md`.
>
> Otras dos condiciones a tener en cuenta: no se puede republicar el catálogo *como catálogo*
> (dentro de la app sí, que es lo que hacemos), y las imágenes no se pueden usar para entrenar
> ni alimentar modelos generativos.

### Sustituir la imagen de un ejercicio por una tuya

Las imágenes propias tienen prioridad sobre las del catálogo. Copia de 1 a 4 ficheros a
`app/src/main/res/drawable/` con este nombre:

```
ex_<id_del_ejercicio>_1.webp  …  ex_<id_del_ejercicio>_4.webp
```

donde `<id_del_ejercicio>` es el id del catálogo **con los guiones cambiados por guiones bajos**
(los nombres de recurso de Android no admiten guiones). Por ejemplo, para `goblet-squat`:

```
app/src/main/res/drawable/ex_goblet_squat_1.webp
app/src/main/res/drawable/ex_goblet_squat_2.webp
```

Si un ejercicio no tiene ninguna imagen, el diálogo (i) te dice qué nombres está buscando.

## Cómo está montado

Kotlin + Jetpack Compose (Material 3), Room para la base de datos local y navegación con
`navigation-compose`. Sin backend: todo se guarda en el móvil.

```
app/src/main/
├── assets/                    Catálogo de ejercicios e imágenes
└── java/com/gymlist/app/
    ├── GymListApplication.kt  Contenedor de dependencias (a mano) y carga del catálogo
    ├── MainActivity.kt
    ├── data/
    │   ├── model/Entities.kt  Exercise, Routine, RoutineItem, WorkoutSession, SetLog
    │   ├── local/             Room: base de datos, DAOs y conversores
    │   ├── plan/              Plan personalizado: respuestas, tabla de ejercicios por
    │   │                      patrón de movimiento (PlanCatalog) y generador
    │   └── repo/              GymRepository, ExerciseCatalog (lee los assets),
    │                          Taxonomy (traducciones) y las rutinas de ejemplo
    ├── ui/
    │   ├── GymListApp.kt      Armazón: pestañas, navegación y barra de descanso
    │   ├── home/              "Hoy": retomar o empezar entrenamiento, resumen
    │   ├── workout/           Entrenamiento en curso + temporizador de descanso
    │   ├── routines/          Lista de rutinas y editor de rutina
    │   ├── plan/              Cuestionario del plan, revisión y guardado
    │   ├── calendar/          Calendario de seguimiento y ficha de un entrenamiento
    │   ├── library/           Catálogo, filtros y selector de ejercicios
    │   ├── about/             Créditos y atribución
    │   ├── common/            Diálogo (i), campos numéricos, piezas compartidas
    │   └── theme/
    └── util/                  Formatos, estadísticas, aviso de descanso, imágenes
```

### Decisiones que conviene conocer

- **El catálogo se vuelve a insertar (upsert) en cada arranque**, así una actualización de la app
  trae ejercicios nuevos o textos corregidos. Las rutinas de ejemplo solo se crean la primera
  vez, para no resucitar las que hayas borrado.
- **Las series se crean todas al empezar el entrenamiento**, desmarcadas. Marcar una es un
  simple `UPDATE`, y si dejas la sesión a medias queda registrado lo que faltó por hacer.
- **El nombre del ejercicio y de la rutina se copian en el histórico.** Si luego renombras o
  borras una rutina, los entrenamientos pasados no cambian.
- **El temporizador usa el reloj monótono del sistema** (`SystemClock.elapsedRealtime`), no la
  hora del móvil, para que no se descuadre si cambia la hora o la zona horaria.
- **El `WorkoutViewModel` se crea a nivel de Activity**, no de pantalla, para que el descanso
  siga contando al cambiar de pestaña.
- **Solo puede haber un entrenamiento en curso.** Si intentas empezar otro, la app te lleva al
  que ya tienes abierto en vez de crear uno huérfano.
- **El plan personalizado sale de una tabla hecha a mano** (`data/plan/PlanCatalog.kt`), no del
  campo `equipment` del catálogo: para cada hueco de una rutina (sentadilla, remo, press...) hay
  una lista de ejercicios en orden de preferencia, con el material que necesitan de verdad (el
  press con mancuernas también necesita banco), el nivel mínimo y las zonas que suelen cargar.
  Los tests comprueban que todos los id existen en el catálogo y que cualquier combinación de
  respuestas da un plan que respeta material, nivel y molestias.
- **El plan se guarda como rutinas normales**, sin tablas nuevas en la base de datos: cada día es
  una rutina que se edita como cualquier otra. Por eso generar un plan no toca el esquema ni
  obliga a migrar.

## Limitaciones conocidas

- Si Android mata el proceso de la app durante un descanso, el temporizador se pierde (el
  entrenamiento y las series marcadas no: eso está en la base de datos). Para que sobreviva
  haría falta un servicio en primer plano con notificación.
- No hay copia de seguridad en la nube ni sincronización entre dispositivos.
- Las explicaciones de los ejercicios son informativas y no son consejo médico.
