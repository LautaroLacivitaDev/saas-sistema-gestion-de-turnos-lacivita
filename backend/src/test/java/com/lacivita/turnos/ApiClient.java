package com.lacivita.turnos;

import static com.lacivita.turnos.SpaCsrf.spaCsrf;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MockMvcTester.MockMvcRequestBuilder;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Cliente de la API para las pruebas de integración: registra personas, guarda su cookie de sesión y
 * agrega el token CSRF en cada escritura.
 */
public class ApiClient {

    private final MockMvcTester mvc;

    public ApiClient(MockMvcTester mvc) {
        this.mvc = mvc;
    }

    /** Persona con sesión iniciada. */
    public record Session(UUID userId, String email, Cookie cookie) {}

    public Session registerNewUser(String name) {
        String email = "persona-" + UUID.randomUUID() + "@example.com";
        var result = mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name":"%s","email":"%s","password":"clave-segura-1"}""".formatted(name, email))
                .with(spaCsrf())
                .exchange();
        requireStatus(result, HttpStatus.CREATED);
        return new Session(
                UUID.fromString(read(result, "$.id")),
                email,
                result.getResponse().getCookie("SESSION"));
    }

    /** Inicia sesión de nuevo (por ejemplo, después de cambiarle el rol de plataforma a la persona). */
    public Session login(Session session) {
        var result = mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"clave-segura-1"}""".formatted(session.email()))
                .with(spaCsrf())
                .exchange();
        requireStatus(result, HttpStatus.OK);
        return new Session(
                session.userId(), session.email(), result.getResponse().getCookie("SESSION"));
    }

    public UUID createBusiness(Session owner, String slug) {
        var result = post(owner, "/api/businesses", """
                {"name":"Negocio %s","slug":"%s","category":"BARBERSHOP"}""".formatted(slug, slug));
        requireStatus(result, HttpStatus.CREATED);
        return UUID.fromString(read(result, "$.id"));
    }

    public UUID createBranch(Session owner, UUID businessId, String name) {
        var result = post(owner, "/api/businesses/" + businessId + "/branches", """
                {"name":"%s","street":"Av. Siempre Viva 742","city":"CABA"}""".formatted(name));
        requireStatus(result, HttpStatus.CREATED);
        return UUID.fromString(read(result, "$.id"));
    }

    /** Registra a una persona y la suma al equipo del negocio con una invitación aceptada. */
    public Session joinTeam(
            Session owner, UUID businessId, String role, UUID branchId, RecordingMailer mailer, String name) {
        var person = registerNewUser(name);
        requireStatus(
                post(owner, "/api/businesses/" + businessId + "/invitations", """
                        {"email":"%s","role":"%s","branchIds":["%s"]}""".formatted(
                                person.email(), role, branchId)),
                HttpStatus.CREATED);
        String token = mailer.lastTokenSentTo(person.email()).orElseThrow();
        requireStatus(post(person, "/api/invitations/accept", """
                {"token":"%s"}""".formatted(token)), HttpStatus.OK);
        return person;
    }

    public MvcTestResult get(Session session, String uri) {
        return withSession(mvc.get().uri(uri), session).exchange();
    }

    public MvcTestResult post(Session session, String uri, String json) {
        return withSession(mvc.post().uri(uri), session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(spaCsrf())
                .exchange();
    }

    public MvcTestResult put(Session session, String uri, String json) {
        return withSession(mvc.put().uri(uri), session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .with(spaCsrf())
                .exchange();
    }

    public MvcTestResult delete(Session session, String uri) {
        return withSession(mvc.delete().uri(uri), session).with(spaCsrf()).exchange();
    }

    public static String read(MvcTestResult result, String jsonPath) {
        try {
            Object value = JsonPath.read(result.getResponse().getContentAsString(), jsonPath);
            return String.valueOf(value);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static MockMvcRequestBuilder withSession(MockMvcRequestBuilder request, Session session) {
        return session == null ? request : request.cookie(session.cookie());
    }

    private static void requireStatus(MvcTestResult result, HttpStatus expected) {
        int actual = result.getResponse().getStatus();
        if (actual != expected.value()) {
            try {
                throw new IllegalStateException("Se esperaba %d y llegó %d: %s"
                        .formatted(
                                expected.value(), actual, result.getResponse().getContentAsString()));
            } catch (UnsupportedEncodingException ex) {
                throw new IllegalStateException(ex);
            }
        }
    }
}
