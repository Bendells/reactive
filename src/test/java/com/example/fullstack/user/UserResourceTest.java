package com.example.fullstack.user;

import com.example.fullstack.task.Task;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

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

        var response = RestAssured.given()
                .when()
                .get("/api/v1/users")
                .body().prettyPrint();

        var responseUser = RestAssured.given()
                .body("{\"name\":\"test\",\"password\":\"test\", \"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/users");

        var responsePretty = responseUser.prettyPrint();
        responseUser
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
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(403);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void createDuplicate() {
        RestAssured.given()
                .body("{\"name\":\"user\",\"password\":\"test\",\"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/users")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    void update() {
        var user = RestAssured.given()
                .body("{\"name\":\"to-update\",\"password\":\"test\",\"roles\":[\"user\"]}")
                .contentType(ContentType.JSON)
                .when()
                .post("/api/v1/users")
                .as(User.class);
        user.name = "updated";
        RestAssured.given()
                .body(user)
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v1/users/" + user.id)
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
                .when()
                .put("/api/v1/users/0")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "admin", roles = "admin")
    @RunOnVertxContext
    void delete(final UniAsserter uniAsserter) {
        final List<Long> ids = new ArrayList<>();

        // Execute not needed, just wanted to personally play with uniAsserter functions
        uniAsserter.execute(
                () -> {
                    var toDelete = RestAssured.given()
                            .body("{\"name\":\"to-delete\",\"password\":\"test\"}")
                            .contentType(ContentType.JSON)
                            .post("/api/v1/users")
                            .as(User.class);
                    ids.add(toDelete.id);
                    }
                );

        uniAsserter.execute(
                () ->
                    RestAssured.given()
                            .when().delete("/api/v1/users/" + ids.get(0))
                            .then()
                            .statusCode(204)
        );

        uniAsserter.assertThat(
                () ->
                        Panache.withSession(() -> User.findById
                                (ids.get(0))),
                deletedUser ->
                        MatcherAssert.assertThat(deletedUser, Matchers.nullValue())
        );
    }

    @Test
    @TestSecurity(user = "admin", roles = "user")
    @RunOnVertxContext
    void changePassword(final UniAsserter uniAsserter) {
        // execute not needed, just used to personally understand the object
        uniAsserter.execute(
                () -> RestAssured.given()
                .body("{\"currentPassword\": \"quarkus\", \"newPassword\": \"changed\"}")
                .contentType(ContentType.JSON)
                .when()
                .put("/api/v1/users/self/password")
                .then()
                .statusCode(200));

        uniAsserter.assertThat(
                () ->
                        Panache.withSession(() -> User.<User>findById(0L)),
                changedUser ->
                        Assertions.assertTrue(BcryptUtil.matches("changed",
                                changedUser.password))
        );

    }
}