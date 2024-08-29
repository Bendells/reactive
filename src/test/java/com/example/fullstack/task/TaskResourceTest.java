package com.example.fullstack.task;

import com.example.fullstack.user.User;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.http.ContentType;
import jakarta.ws.rs.core.Response;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class TaskResourceTest {

    @Test
    @TestSecurity(user = "user", roles = "user")
    void list() {
        given()
                .body("{\"title\":\"to-be-listed\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/tasks").as(Task.class);
        given()
                .when().get("/api/v1/tasks")
                .then()
                .statusCode(200)
                .body("$",
                        allOf(
                                hasItem(
                                        hasEntry("title", "to-be-listed")
                                ),
                                everyItem(
                                        hasEntry(is("user"), (Matcher)hasEntry("name", "user"))
                                )
                        )
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void create() {
        given()
                .body("{\"title\":\"task-create\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/tasks")
                .then()
                .statusCode(201)
                .body(
                        "title", is("task-create"),
                        "created", not(emptyString())
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void update() {
        var toUpdate = given()
                .body("{\"title\":\"to-update\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/tasks").as(Task.class);
        toUpdate.title = "updated";
        given()
                .body(toUpdate)
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks/" + toUpdate.id)
                .then()
                .statusCode(200)
                .body(
                        "title", is("updated"),
                        "version", is(toUpdate.version + 1)
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void updateNotFound() {
        given()
                .body("{\"title\":\"updated\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks/1337")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @RunOnVertxContext
    void updateForbidden(final UniAsserter uniAsserter) {
        uniAsserter.assertThat(() ->
                        Panache.withSession(() -> User.<User>findById(0L).chain(admin -> {
                                    Task task = new Task();
                                    task.title = "admins-task";
                                    task.user = admin;
                                    return task.<Task>persistAndFlush();
                                })
                        ),
                adminTask -> {
                    given()
                            .body("{\"title\":\"to-update\"}")
                            .contentType(ContentType.JSON)
                            .when().put("/api/v1/tasks/" + adminTask.id)
                            .then()
                            .statusCode(401);
                }
        );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @RunOnVertxContext
    void delete(final UniAsserter uniAsserter) {

        uniAsserter.assertThat(
                () ->
                        Panache.withSession(() -> User.<User>findById(1L).chain(user -> {
                                    Task task = new Task();
                                    task.title = "to-delete";
                                    task.user = user;
                                    return task.<Task>persistAndFlush();
                                })
                        )
                ,
                toDeleteTask -> {
                    given()
                            .when().delete("/api/v1/tasks/" + toDeleteTask.id)
                            .then()
                            .statusCode(204);
                    given()
                    .when().delete("/api/v1/tasks/" + toDeleteTask.id)
                            .then()
                            .statusCode(404);
                }
        );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    @RunOnVertxContext
    void setComplete(final UniAsserter uniAsserter) {

        var toSetComplete = given()
                .body("{\"title\":\"to-set-complete\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/tasks").as(Task.class);

        given()
                .body("\"true\"")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/tasks/" + toSetComplete.id + "/complete")
                .then()
                .statusCode(200);

        uniAsserter.assertThat(
                () ->
                        Panache.withSession(() -> Task.findById(toSetComplete.id))
                ,
                taskCompleted ->
                        assertThat(taskCompleted,
                                allOf(
                                        hasProperty("complete", notNullValue()),
                                        hasProperty("version", is(toSetComplete.version + 1))
                                ))
                );
    }
}