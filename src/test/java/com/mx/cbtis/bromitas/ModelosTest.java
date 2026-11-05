package com.mx.cbtis.bromitas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.mx.cbtis.modelo.Asignaciones;
import com.mx.cbtis.modelo.Comentarios;
import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Tareas;
import com.mx.cbtis.modelo.Usuario;

/**
 * Pruebas de los modelos del front: constructores, getters y setters.
 */
class ModelosTest {

    @Test
    @DisplayName("Usuario: constructor completo, getters y setters")
    void usuario() {
        Usuario u = new Usuario(1, "Ana", "ana@cbtis75.edu.mx", "0722", "Estudiante", "#70FAB7");
        assertEquals(1, u.getId_usuario());
        assertEquals("Ana", u.getNombre());
        assertEquals("ana@cbtis75.edu.mx", u.getCorreo_electronico());
        assertEquals("0722", u.getContrasena());
        assertEquals("Estudiante", u.getRol());
        assertEquals("#70FAB7", u.getImagen_usuario());

        Usuario vacio = new Usuario();
        assertNull(vacio.getId_usuario());
        vacio.setId_usuario(2);
        vacio.setNombre("Luis");
        vacio.setCorreo_electronico("luis@cbtis75.edu.mx");
        vacio.setContrasena("1234");
        vacio.setRol("Docente");
        vacio.setImagen_usuario("#000000");
        assertEquals(2, vacio.getId_usuario());
        assertEquals("Luis", vacio.getNombre());
        assertEquals("luis@cbtis75.edu.mx", vacio.getCorreo_electronico());
        assertEquals("1234", vacio.getContrasena());
        assertEquals("Docente", vacio.getRol());
        assertEquals("#000000", vacio.getImagen_usuario());
    }

    @Test
    @DisplayName("Proyectos: constructor completo, getters y setters")
    void proyectos() {
        Proyectos p = new Proyectos(197, "My API Rest", "API del gestor", "2026-10-01", "2026-12-15", "En proceso",
                "/Images/animacion1.gif");
        assertEquals(197, p.getId_proyecto());
        assertEquals("My API Rest", p.getNombre());
        assertEquals("API del gestor", p.getDescripcion());
        assertEquals("2026-10-01", p.getFecha_inicio());
        assertEquals("2026-12-15", p.getFecha_finalizacion());
        assertEquals("En proceso", p.getEstado_proyecto());
        assertEquals("/Images/animacion1.gif", p.getFondo());

        Proyectos vacio = new Proyectos();
        assertNull(vacio.getId_proyecto());
        vacio.setId_proyecto(206);
        vacio.setNombre("Proyecto de Prueba");
        vacio.setDescripcion("Descripción");
        vacio.setFecha_inicio("2026-11-01");
        vacio.setFecha_finalizacion("2026-11-30");
        vacio.setEstado_proyecto("Terminado");
        vacio.setFondo("/Images/naranja.png");
        assertEquals(206, vacio.getId_proyecto());
        assertEquals("Proyecto de Prueba", vacio.getNombre());
        assertEquals("Descripción", vacio.getDescripcion());
        assertEquals("2026-11-01", vacio.getFecha_inicio());
        assertEquals("2026-11-30", vacio.getFecha_finalizacion());
        assertEquals("Terminado", vacio.getEstado_proyecto());
        assertEquals("/Images/naranja.png", vacio.getFondo());
    }

    @Test
    @DisplayName("Tareas: constructor completo, getters y setters")
    void tareas() {
        Tareas t = new Tareas(199, "Base de datos", "Crear tablas", "2026-11-10", "terminado", "danger", 197);
        assertEquals(199, t.getId_tarea());
        assertEquals("Base de datos", t.getNombre());
        assertEquals("Crear tablas", t.getDescripcion());
        assertEquals("2026-11-10", t.getFecha_vencimiento());
        assertEquals("terminado", t.getLista());
        assertEquals("danger", t.getEstado());
        assertEquals(197, t.getId_proyecto());

        Tareas vacio = new Tareas();
        assertNull(vacio.getId_tarea());
        vacio.setId_tarea(209);
        vacio.setNombre("Endpoints");
        vacio.setDescripcion("Probar endpoints");
        vacio.setFecha_vencimiento("2026-11-20");
        vacio.setLista("encurso");
        vacio.setEstado("warning");
        vacio.setId_proyecto(206);
        assertEquals(209, vacio.getId_tarea());
        assertEquals("Endpoints", vacio.getNombre());
        assertEquals("Probar endpoints", vacio.getDescripcion());
        assertEquals("2026-11-20", vacio.getFecha_vencimiento());
        assertEquals("encurso", vacio.getLista());
        assertEquals("warning", vacio.getEstado());
        assertEquals(206, vacio.getId_proyecto());
    }

    @Test
    @DisplayName("Asignaciones: constructor completo, getters y setters")
    void asignaciones() {
        Asignaciones a = new Asignaciones(5, 131, 199, "2026-11-01");
        assertEquals(5, a.getId_asignacion());
        assertEquals(131, a.getId_usuario());
        assertEquals(199, a.getId_tarea());
        assertEquals("2026-11-01", a.getFecha_asignacion());

        Asignaciones vacio = new Asignaciones();
        assertNull(vacio.getId_asignacion());
        vacio.setId_asignacion(6);
        vacio.setId_usuario(132);
        vacio.setId_tarea(209);
        vacio.setFecha_asignacion("2026-11-05");
        assertEquals(6, vacio.getId_asignacion());
        assertEquals(132, vacio.getId_usuario());
        assertEquals(209, vacio.getId_tarea());
        assertEquals("2026-11-05", vacio.getFecha_asignacion());
    }

    @Test
    @DisplayName("Comentarios: constructor completo, getters y setters")
    void comentarios() {
        Comentarios c = new Comentarios(10, "131", 199, "Listo", "2026-11-01 10:00");
        assertEquals(10, c.getId_comentario());
        assertEquals("131", c.getId_usuario());
        assertEquals(199, c.getId_tarea());
        assertEquals("Listo", c.getTexto_comentario());
        assertEquals("2026-11-01 10:00", c.getFecha_hora());

        Comentarios vacio = new Comentarios();
        assertNull(vacio.getId_comentario());
        vacio.setId_comentario(11);
        vacio.setId_usuario("132");
        vacio.setId_tarea(209);
        vacio.setTexto_comentario("Revisar");
        vacio.setFecha_hora("2026-11-05 12:30");
        assertEquals(11, vacio.getId_comentario());
        assertEquals("132", vacio.getId_usuario());
        assertEquals(209, vacio.getId_tarea());
        assertEquals("Revisar", vacio.getTexto_comentario());
        assertEquals("2026-11-05 12:30", vacio.getFecha_hora());
    }
}
