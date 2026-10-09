package com.lacivita.turnos.business.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Buscador público. Cada prueba usa palabras inventadas al azar (solo letras) para no encontrar negocios de
 * otras pruebas: el contexto y la base son compartidos.
 */
@IntegrationTest
class BusinessSearchIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    ApiClient api;
    Session owner;
    String name;
    String place;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        name = randomWord();
        place = randomWord();
    }

    @Test
    void findsABusinessDespiteATypoMissingAccentsAndUppercase() {
        String slug = business("Barbería " + capitalized(name), "BARBERSHOP", true, place);

        assertThat(slugsFor("BARBERIA " + withTypo(name))).containsExactly(slug);
    }

    @Test
    void findsByNeighborhoodAndByTheKindOfBusiness() {
        String slug = business("Estudio " + capitalized(name), "HAIR_SALON", true, place);

        assertThat(slugsFor(place)).containsExactly(slug);
        assertThat(slugsFor("peluqueria " + place)).containsExactly(slug);
        assertThat(slugsFor("barberia " + place)).isEmpty();
    }

    @Test
    void everyWordHasToMatch() {
        business("Barbería " + capitalized(name), "BARBERSHOP", true, place);

        assertThat(slugsFor(name + " " + randomWord())).isEmpty();
    }

    @Test
    void aBusinessThatDoesNotWantToAppearIsNotListed() {
        business("Barbería " + capitalized(name), "BARBERSHOP", false, place);

        assertThat(slugsFor(name)).isEmpty();
    }

    @Test
    void theResultsSayWhereEachBusinessIsAndComeInPages() {
        business("Barbería " + capitalized(name), "BARBERSHOP", true, place);
        business("Barbería " + capitalized(name) + " Norte", "BARBERSHOP", true, place);

        var firstPage = api.get(null, "/api/public/businesses?q=%s&size=1".formatted(name));

        assertThat(firstPage).hasStatusOk();
        assertThat(firstPage).bodyJson().extractingPath("$.hasMore").isEqualTo(true);
        assertThat(firstPage)
                .bodyJson()
                .extractingPath("$.items[0].places[0]")
                .isEqualTo(capitalized(place) + ", CABA");
        assertThat(api.get(null, "/api/public/businesses?q=%s&size=1&page=1".formatted(name)))
                .bodyJson()
                .extractingPath("$.hasMore")
                .isEqualTo(false);
    }

    /** Crea un negocio con una sucursal en ese barrio y devuelve su slug. */
    private String business(String businessName, String category, boolean searchable, String neighborhood) {
        String slug = "busqueda-" + UUID.randomUUID().toString().substring(0, 8);
        var created = api.post(owner, "/api/businesses", """
                {"name":"%s","slug":"%s","category":"%s","searchable":%s}""".formatted(businessName, slug, category, searchable));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        String businessId = ApiClient.read(created, "$.id");
        assertThat(api.post(
                        owner, "/api/businesses/" + businessId + "/branches", """
                        {"name":"Sede","street":"Av. Siempre Viva 742","neighborhood":"%s","city":"CABA"}""".formatted(capitalized(neighborhood))))
                .hasStatus(HttpStatus.CREATED);
        return slug;
    }

    private List<String> slugsFor(String query) {
        var result = api.get(null, "/api/public/businesses?q=" + query);
        assertThat(result).hasStatusOk();
        try {
            return JsonPath.read(result.getResponse().getContentAsString(), "$.items[*].slug");
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** Siete letras al azar: no coincide con palabras de otros negocios. */
    private static String randomWord() {
        var random = ThreadLocalRandom.current();
        var word = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            word.append((char) ('a' + random.nextInt(26)));
        }
        return word.toString();
    }

    /** La misma palabra con una letra del medio cambiada. */
    private static String withTypo(String word) {
        char middle = word.charAt(3);
        char replacement = middle == 'x' ? 'y' : 'x';
        return word.substring(0, 3) + replacement + word.substring(4);
    }

    private static String capitalized(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }
}
