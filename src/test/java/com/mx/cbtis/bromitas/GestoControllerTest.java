package com.mx.cbtis.bromitas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.mx.cbtis.controller.GestorController;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.servlet.ModelAndView;

import com.mx.cbtis.modelo.Comentarios;
import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Tareas;
import com.mx.cbtis.modelo.Usuario;
import com.sun.net.httpserver.HttpServer;

/**
 * Pruebas automáticas del frontend (cliente Ejercicio5Cliente).
 *
 * Se prueba GestorController, que es la capa del front que consume la API.
 * Durante las pruebas se levanta un servidor simulado en http://localhost:8081
 * que responde como Ejercicio5MyAPIGestor, por eso la API real debe estar DETENIDA
 * al ejecutar estas pruebas (el puerto 8081 debe estar libre).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GestorControllerTest {

    private static HttpServer apiSimulada;

    /** Respuesta configurada por ruta: código HTTP y cuerpo JSON. */
    private static final Map<String, Object[]> respuestas = new ConcurrentHashMap<>();
    /** Cuerpo recibido por la API simulada en cada ruta. */
    private static final Map<String, String> recibido = new ConcurrentHashMap<>();

    private final GestorController controller = new GestorController();

    @BeforeAll
    static void iniciarApiSimulada() throws IOException {
        apiSimulada = HttpServer.create(new InetSocketAddress("localhost", 8081), 0);
        apiSimulada.createContext("/", intercambio -> {
            String ruta = intercambio.getRequestURI().getPath();
            recibido.put(ruta, new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            Object[] r = respuestas.getOrDefault(ruta, new Object[] { 404, "{\"status\":404,\"error\":\"Not Found\"}" });
            byte[] cuerpo = ((String) r[1]).getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders((Integer) r[0], cuerpo.length);
            try (OutputStream os = intercambio.getResponseBody()) {
                os.write(cuerpo);
            }
        });
        apiSimulada.start();
    }

    @AfterAll
    static void detenerApiSimulada() {
        apiSimulada.stop(0);
    }

    @BeforeEach
    void limpiar() {
        respuestas.clear();
        recibido.clear();
    }

    private static void api(String ruta, int codigo, String json) {
        respuestas.put(ruta, new Object[] { codigo, json });
    }

    // ------------------------------------------------------------------
    // CASOS DE ÉXITO
    // ------------------------------------------------------------------

    @Test
    @Order(1)
    @DisplayName("AUT-E01 · Servicio de consulta: la API devuelve los proyectos y el front los procesa correctamente")
    void consultarProyectos_devuelveLista() {
        api("/buscarProyectos", 200, "["
                + "{\"id_proyecto\":197,\"nombre\":\"My API Rest\",\"estado_proyecto\":\"En proceso\",\"fondo\":\"/Images/animacion1.gif\"},"
                + "{\"id_proyecto\":206,\"nombre\":\"Proyecto de Prueba\",\"estado_proyecto\":\"Terminado\",\"fondo\":\"/Images/animacion1.gif\"}]");

        ExtendedModelMap modelo = new ExtendedModelMap();
        ResponseEntity<Object> respuesta = controller.consultarProyectos(modelo);

        assertEquals(200, respuesta.getStatusCode().value());
        @SuppressWarnings("unchecked")
        List<Proyectos> proyectos = (List<Proyectos>) respuesta.getBody();
        assertEquals(2, proyectos.size());
        assertEquals("My API Rest", proyectos.get(0).getNombre());
        assertEquals(206, proyectos.get(1).getId_proyecto().intValue());
        assertTrue(modelo.containsAttribute("listaProyectos"));
    }

    @Test
    @Order(2)
    @DisplayName("AUT-E02 · Login: credenciales válidas, el front envía los datos y recibe el usuario")
    void verificarLogin_credencialesValidas() {
        api("/getUsuarioLogin", 200, "{\"id_usuario\":131,\"nombre\":\"Ana Paola Ramírez Vázquez\","
                + "\"correo_electronico\":\"ana22533@cbtis75.edu.mx\",\"rol\":\"Estudiante\",\"imagen_usuario\":\"#70FAB7\"}");
        Usuario credenciales = new Usuario();
        credenciales.setCorreo_electronico("ana22533@cbtis75.edu.mx");
        credenciales.setContrasena("0722");

        ResponseEntity<Usuario> respuesta = controller.verificarUsuario(credenciales);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(131, respuesta.getBody().getId_usuario().intValue());
        assertEquals("Ana Paola Ramírez Vázquez", respuesta.getBody().getNombre());
        assertTrue(recibido.get("/getUsuarioLogin").contains("\"correo_electronico\":\"ana22533@cbtis75.edu.mx\""));
        assertTrue(recibido.get("/getUsuarioLogin").contains("\"contrasena\":\"0722\""));
    }

    @Test
    @Order(3)
    @DisplayName("AUT-E03 · Formulario de proyecto: datos válidos, se envían a la API y redirige a Inicio")
    void guardarProyecto_redirigeAInicio() {
        api("/addProyecto", 200, "1");
        Proyectos proyecto = new Proyectos();
        proyecto.setNombre("Proyecto Front");
        proyecto.setDescripcion("Creado desde la interfaz");
        proyecto.setFecha_inicio("2026-10-17");
        proyecto.setFecha_finalizacion("2026-12-15");
        proyecto.setEstado_proyecto("En proceso");
        proyecto.setFondo("/Images/naranja.png");

        ModelAndView vista = controller.setSaveProyecto(proyecto);

        assertEquals("redirect:/inicio", vista.getViewName());
        assertTrue(recibido.get("/addProyecto").contains("\"nombre\":\"Proyecto Front\""));
        assertTrue(recibido.get("/addProyecto").contains("\"estado_proyecto\":\"En proceso\""));
    }

    @Test
    @Order(4)
    @DisplayName("AUT-E04 · Vista del proyecto: la consulta de tareas finaliza y devuelve las tareas del proyecto")
    void obtenerTareas_devuelveTareasDelProyecto() {
        api("/getTarea", 200, "[{\"id_tarea\":199,\"nombre\":\"Base de datos\",\"lista\":\"terminado\",\"estado\":\"danger\",\"id_proyecto\":197}]");
        Tareas filtro = new Tareas();
        filtro.setId_proyecto(197);

        ResponseEntity<List<Tareas>> respuesta = controller.obtenerTareas(filtro, new ExtendedModelMap());

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals(1, respuesta.getBody().size());
        assertEquals("Base de datos", respuesta.getBody().get(0).getNombre());
        assertTrue(recibido.get("/getTarea").contains("\"id_proyecto\":197"));
    }

    @Test
    @Order(5)
    @DisplayName("AUT-E05 · Formulario de tarea: campos vacíos se envían como null y la tarea se guarda")
    void guardarTarea_fechaVaciaSeEnviaComoNull() {
        api("/addTareas", 200, "1");
        Tareas tarea = new Tareas();
        tarea.setId_tarea(209);
        tarea.setNombre("Endpoints");
        tarea.setDescripcion("Probar endpoints");
        tarea.setFecha_vencimiento("");
        tarea.setEstado("0");
        tarea.setLista("encurso");
        tarea.setId_proyecto(197);

        ModelAndView vista = controller.setSaveTareas(tarea);

        assertEquals("redirect:/proyecto", vista.getViewName());
        assertEquals(Integer.valueOf(197), vista.getModel().get("id_proyecto"));
        assertTrue(recibido.get("/addTareas").contains("\"fecha_vencimiento\":null"));
        assertTrue(recibido.get("/addTareas").contains("\"lista\":\"encurso\""));
    }

    // ------------------------------------------------------------------
    // CASOS DE FRACASO
    // ------------------------------------------------------------------

    @Test
    @Order(6)
    @DisplayName("AUT-F01 · Servicio de consulta: la API devuelve error 500 y el front lo detecta sin procesar datos")
    void consultarProyectos_errorDelServidor() {
        api("/buscarProyectos", 500, "{\"status\":500,\"error\":\"Internal Server Error\"}");

        HttpServerErrorException error = assertThrows(HttpServerErrorException.class,
                () -> controller.consultarProyectos(new ExtendedModelMap()));

        assertEquals(500, error.getStatusCode().value());
    }

    @Test
    @Order(7)
    @DisplayName("AUT-F02 · Login: el usuario no existe, el front recibe la marca \"000\" para mostrar la alerta")
    void verificarLogin_usuarioInexistente() {
        api("/getUsuarioLogin", 200, "{\"id_usuario\":null,\"nombre\":\"000\"}");
        Usuario credenciales = new Usuario();
        credenciales.setCorreo_electronico("ana22533@cbtis75.edu.mx");
        credenciales.setContrasena("incorrecta");

        ResponseEntity<Usuario> respuesta = controller.verificarUsuario(credenciales);

        assertEquals("000", respuesta.getBody().getNombre());
        assertNull(respuesta.getBody().getId_usuario());
    }

    @Test
    @Order(8)
    @DisplayName("AUT-F03 · Vista de edición: el proyecto solicitado no existe y se devuelve sin datos")
    void actualizarProyecto_proyectoInexistente() {
        api("/buscarProyecto", 200, "{\"id_proyecto\":null,\"nombre\":null,\"descripcion\":null}");
        Proyectos solicitud = new Proyectos();
        solicitud.setId_proyecto(9999);

        ResponseEntity<Object> respuesta = controller.updateProyecto(solicitud);

        assertEquals(200, respuesta.getStatusCode().value());
        Proyectos proyecto = (Proyectos) respuesta.getBody();
        assertNull(proyecto.getId_proyecto());
        assertNull(proyecto.getNombre());
    }

    @Test
    @Order(9)
    @DisplayName("AUT-F04 · Login: la API no encuentra el recurso (404) y el front lo detecta como error")
    void verificarLogin_recursoNoEncontrado() {
        // No se configura respuesta: la API simulada devuelve 404
        Usuario credenciales = new Usuario();
        credenciales.setCorreo_electronico("ana22533@cbtis75.edu.mx");
        credenciales.setContrasena("0722");

        HttpClientErrorException error = assertThrows(HttpClientErrorException.class,
                () -> controller.verificarUsuario(credenciales));

        assertEquals(404, error.getStatusCode().value());
    }

    @Test
    @Order(10)
    @DisplayName("AUT-F05 · Formulario de tarea: la API rechaza la tarea (500) y el front no redirige como si se hubiera guardado")
    void guardarTarea_apiRechaza() {
        api("/addTareas", 500, "{\"status\":500,\"error\":\"Internal Server Error\"}");
        Tareas tarea = new Tareas();
        tarea.setNombre("Tarea rechazada");
        tarea.setId_proyecto(197);

        assertThrows(HttpServerErrorException.class, () -> controller.setSaveTareas(tarea));
    }

    // ------------------------------------------------------------------
    // VISTAS Y RESTO DE SERVICIOS
    // ------------------------------------------------------------------

    @Test
    @Order(11)
    @DisplayName("AUT-V01 · Vistas estáticas: home, tutorial, manual y login devuelven su plantilla")
    void vistasEstaticas() {
        assertEquals("home", controller.getHome());
        assertEquals("tutorial", controller.getTutorial());
        assertEquals("manual", controller.getManual());
        assertEquals("login", controller.getLogin());
    }

    @Test
    @Order(12)
    @DisplayName("AUT-V02 · Registro e Inicio: preparan el objeto vacío del formulario")
    void vistasConFormulario() {
        ExtendedModelMap modelo = new ExtendedModelMap();
        assertEquals("register", controller.getRegister(modelo));
        assertTrue(modelo.get("usuario") instanceof Usuario);

        ExtendedModelMap modeloInicio = new ExtendedModelMap();
        assertEquals("inicio", controller.getInicio(modeloInicio));
        assertTrue(modeloInicio.get("proyectos") instanceof Proyectos);
    }

    @Test
    @Order(13)
    @DisplayName("AUT-E06 · Registro de usuario: se envía a la API y redirige a Login")
    void guardarUsuario_redirigeALogin() {
        api("/addUsuario", 200, "1");
        Usuario usuario = new Usuario();
        usuario.setNombre("Luis");
        usuario.setCorreo_electronico("luis@cbtis75.edu.mx");

        ModelAndView vista = controller.setSaveUsuario(usuario);

        assertEquals("redirect:/login", vista.getViewName());
        assertTrue(recibido.get("/addUsuario").contains("\"correo_electronico\":\"luis@cbtis75.edu.mx\""));
    }

    @Test
    @Order(14)
    @DisplayName("AUT-E07 · Eliminar proyecto: envía DELETE con el proyecto y redirige a Inicio")
    void eliminarProyecto_redirigeAInicio() {
        api("/deleteProyecto", 200, "1");
        Proyectos proyecto = new Proyectos();
        proyecto.setId_proyecto(206);

        ModelAndView vista = controller.eliminarProyecto(proyecto);

        assertEquals("redirect:/inicio", vista.getViewName());
        assertTrue(recibido.get("/deleteProyecto").contains("\"id_proyecto\":206"));
    }

    @Test
    @Order(15)
    @DisplayName("AUT-E08 · Consultar usuarios: devuelve la lista y la agrega al modelo")
    void consultarUsuarios_devuelveLista() {
        api("/buscarUsuarios", 200, "[{\"id_usuario\":131,\"nombre\":\"Ana\"},{\"id_usuario\":132,\"nombre\":\"Luis\"}]");
        ExtendedModelMap modelo = new ExtendedModelMap();

        ResponseEntity<Object> respuesta = controller.consultarUsuarios(modelo);

        assertEquals(200, respuesta.getStatusCode().value());
        @SuppressWarnings("unchecked")
        List<Usuario> usuarios = (List<Usuario>) respuesta.getBody();
        assertEquals(2, usuarios.size());
        assertEquals("Luis", usuarios.get(1).getNombre());
        assertTrue(modelo.containsAttribute("listaUsuarios"));
    }

    @Test
    @Order(16)
    @DisplayName("AUT-E09 · Comentarios de una tarea: se envía el id_tarea y se devuelven los comentarios")
    void obtenerComentarios_devuelveComentarios() {
        api("/getComentario", 200, "[{\"id_comentario\":10,\"id_usuario\":\"131\",\"id_tarea\":199,\"texto_comentario\":\"Listo\"}]");
        Comentarios filtro = new Comentarios();
        filtro.setId_tarea(199);
        ExtendedModelMap modelo = new ExtendedModelMap();

        ResponseEntity<List<Comentarios>> respuesta = controller.obtenerComentarios(filtro, modelo);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals("Listo", respuesta.getBody().get(0).getTexto_comentario());
        assertTrue(recibido.get("/getComentario").contains("\"id_tarea\":199"));
        assertTrue(modelo.containsAttribute("comentarios"));
    }

    @Test
    @Order(17)
    @DisplayName("AUT-V03 · Vista del proyecto: carga los usuarios y prepara tarea y comentario vacíos")
    void vistaProyecto_cargaUsuarios() {
        api("/buscarUsuarios", 200, "[{\"id_usuario\":131,\"nombre\":\"Ana\"}]");
        ExtendedModelMap modelo = new ExtendedModelMap();

        assertEquals("proyecto", controller.getProyecto(modelo));

        assertTrue(modelo.get("tarea") instanceof Tareas);
        assertTrue(modelo.get("comentarios") instanceof Comentarios);
        @SuppressWarnings("unchecked")
        List<Usuario> usuarios = (List<Usuario>) modelo.get("listaUsuarios");
        assertEquals(1, usuarios.size());

        ExtendedModelMap modeloTarea = new ExtendedModelMap();
        assertEquals("proyecto", controller.getTarea(modeloTarea));
        assertTrue(modeloTarea.get("tareas") instanceof Tareas);
    }

    @Test
    @Order(18)
    @DisplayName("AUT-E10 · Formulario de tarea completo: los campos con valor se envían tal cual")
    void guardarTarea_camposConValor() {
        api("/addTareas", 200, "1");
        Tareas tarea = new Tareas();
        tarea.setNombre("Pruebas");
        tarea.setDescripcion("   ");
        tarea.setFecha_vencimiento("2026-11-20");
        tarea.setEstado("warning");
        tarea.setId_proyecto(197);

        controller.setSaveTareas(tarea);

        String cuerpo = recibido.get("/addTareas");
        assertTrue(cuerpo.contains("\"fecha_vencimiento\":\"2026-11-20\""));
        assertTrue(cuerpo.contains("\"estado\":\"warning\""));
        // Una descripción solo con espacios se envía como null
        assertTrue(cuerpo.contains("\"descripcion\":null"));
    }

    @Test
    @Order(19)
    @DisplayName("AUT-E11 · Formulario de tarea: fecha con espacios y estado vacío se envían como null")
    void guardarTarea_fechaYEstadoEnBlanco() {
        api("/addTareas", 200, "1");
        Tareas tarea = new Tareas();
        tarea.setNombre("Documentación");
        tarea.setFecha_vencimiento("  ");
        tarea.setEstado("");
        tarea.setId_proyecto(197);

        controller.setSaveTareas(tarea);

        String cuerpo = recibido.get("/addTareas");
        assertTrue(cuerpo.contains("\"fecha_vencimiento\":null"));
        assertTrue(cuerpo.contains("\"estado\":null"));
    }

    @Test
    @Order(20)
    @DisplayName("AUT-E12 · Guardar comentario: se envía a la API y vuelve a la vista del proyecto")
    void guardarComentario() {
        api("/addComentario", 200, "1");
        Comentarios comentario = new Comentarios();
        comentario.setId_tarea(199);
        comentario.setTexto_comentario("Revisado");

        assertEquals("proyecto", controller.setSaveComentario(comentario));
        assertTrue(recibido.get("/addComentario").contains("\"texto_comentario\":\"Revisado\""));
    }

    @Test
    @Order(21)
    @DisplayName("AUT-E13 · Editar tarea: consulta la tarea por id_tarea")
    void actualizarTarea_consultaPorId() {
        api("/buscarTarea", 200, "{\"id_tarea\":199,\"nombre\":\"Base de datos\",\"id_proyecto\":197}");
        Tareas solicitud = new Tareas();
        solicitud.setId_tarea(199);

        ResponseEntity<Object> respuesta = controller.updateTarea(solicitud);

        assertEquals(200, respuesta.getStatusCode().value());
        assertEquals("Base de datos", ((Tareas) respuesta.getBody()).getNombre());
    }

    @Test
    @Order(22)
    @DisplayName("AUT-E14 · Eliminar tarea: envía DELETE con la tarea y redirige al proyecto")
    void eliminarTarea_redirigeAProyecto() {
        api("/deleteTareas", 200, "1");
        Tareas tarea = new Tareas();
        tarea.setId_tarea(209);

        ModelAndView vista = controller.eliminarTarea(tarea);

        assertEquals("redirect:/proyecto", vista.getViewName());
        assertTrue(recibido.get("/deleteTareas").contains("\"id_tarea\":209"));
    }

    @Test
    @Order(23)
    @DisplayName("AUT-E15 · Verificar y cambiar tareas: reenvían la tarea a la API y devuelven su respuesta")
    void verificarYCambiarTareas() {
        api("/addTareas", 200, "1");
        Tareas tarea = new Tareas();
        tarea.setId_tarea(209);
        tarea.setLista("terminado");

        assertEquals(1, controller.verificarTarea(tarea).getBody().intValue());
        assertEquals(1, controller.cambiosTareas(tarea).getBody().intValue());
        assertTrue(recibido.get("/addTareas").contains("\"lista\":\"terminado\""));
    }

    @Test
    @Order(24)
    @DisplayName("AUT-E16 · Consultar todas las tareas y todos los comentarios")
    void consultarTareasYComentarios() {
        api("/buscarTareas", 200, "[{\"id_tarea\":199},{\"id_tarea\":209}]");
        api("/buscarComentarios", 200, "[{\"id_comentario\":10}]");

        @SuppressWarnings("unchecked")
        List<Tareas> tareas = (List<Tareas>) controller.consultarTareas().getBody();
        @SuppressWarnings("unchecked")
        List<Comentarios> comentarios = (List<Comentarios>) controller.consultarComentarios().getBody();

        assertEquals(2, tareas.size());
        assertEquals(1, comentarios.size());
        assertEquals(10, comentarios.get(0).getId_comentario().intValue());
    }
}