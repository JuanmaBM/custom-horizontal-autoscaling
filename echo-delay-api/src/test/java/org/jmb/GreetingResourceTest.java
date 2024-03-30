package org.jmb;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

@QuarkusTest
class GreetingResourceTest {

    @Test
    void testBaseEndpoint() {
        given()
          .when().get("/")
          .then()
             .statusCode(200);
    }

    @Test
    void testBodyHasOKStatus() {
        given()
            .when().get("/")
            .then()
                .body("status", equalTo("OK"));
    }

    @Test
    void whenExistsQueryParamThenShouldReturnItInBody() {
        given()
            .when().get("/?message=test-message")
            .then()
                .body("message", equalTo("test-message"));
    }

}