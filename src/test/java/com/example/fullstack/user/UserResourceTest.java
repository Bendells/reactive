package com.example.fullstack.user;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

@QuarkusTest
class UserResourceTest {

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void list() {
        RestAssured.given()
                .when().get("/api/v1/users")
                .then()
                .statusCode(200)
                .body("$.size()", Matchers.greaterThanOrEqualTo(1),
                        "[0].name", Matchers.is("admin"),
                        "[0].password", Matchers.nullValue());
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void create() {
        RestAssured.given()
                .body("{\"name\":\"test\",\"password\":\"test\", \"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .then()
                .statusCode(201)
                .body(
                        "name", Matchers.is("test"),
                        "password", Matchers.nullValue(),
                        "created", Matchers.not(Matchers.emptyString())
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void createUnauthorized() {
        RestAssured.given()
                .body("{\"name\":\"test-unauthorized\",\"password\":\"test\",\"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void createDuplicate() {
        RestAssured.given()
                .body("{\"name\":\"user\",\"password\":\"test\",\"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void update() {
        var user = RestAssured.given()
                .body("{\"name\":\"to-update\",\"password\":\"test\",\"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/users")
                .as(User.class);
        user.name = "updated";
        RestAssured.given()
                .body(user)
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users/" + user.id)
                .then()
                .statusCode(200)
                .body(
                        "name", Matchers.is("updated"),
                        "version", Matchers.is(user.version + 1)
                );
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void updateOptimisticLock() {
        RestAssured.given()
                .body("{\"name\":\"updated\",\"version\":1337}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/users/0")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void delete() {
        var toDelete = RestAssured.given()
                .body("{\"name\":\"to-delete\",\"password\":\"test\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/users")
                .as(User.class);
        RestAssured.given()
                .when().delete("/api/v1/users/" + toDelete.id)
                .then()
                .statusCode(204);
        MatcherAssert.assertThat(User.findById
                (toDelete.id).await().indefinitely(), Matchers.nullValue());
    }
}