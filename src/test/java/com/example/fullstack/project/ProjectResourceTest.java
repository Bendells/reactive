package com.example.fullstack.project;

import com.example.fullstack.task.Task;
import io.quarkus.hibernate.reactive.panache.Panache;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.vertx.RunOnVertxContext;
import io.quarkus.test.vertx.UniAsserter;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@QuarkusTest
class ProjectResourceTest {

    @Test
    @TestSecurity(user = "user", roles = "user")
    void list() {
        given()
                .when().get("/api/v1/projects")
                .then()
                .statusCode(200)
                .body("$",
                        hasItem(allOf(
                                hasEntry("name", "Work")
                        ))
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void create() {
        given()
                .body("{\"name\":\"project-create\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/projects")
                .then()
                .statusCode(201)
                .body(
                        "name", is("project-create"),
                        "created", not(emptyString())
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void createDuplicate() {
        given()
                .body("{\"name\":\"create-existent\"}")
                .contentType(ContentType.JSON)
                .post("/api/v1/projects");
        given()
                .body("{\"name\":\"create-existent\"}")
                .contentType(ContentType.JSON)
                .when().post("/api/v1/projects")
                .then()
                .statusCode(409);
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void update() {
        var toUpdate = given().body("{\"name\":\"to-update\"}").contentType(ContentType.JSON)
                .post("/api/v1/projects").as(Project.class);
        toUpdate.name = "updated";
        given()
                .body(toUpdate)
                .contentType(ContentType.JSON)
                .when().put("/api/v1/projects/" + toUpdate.id)
                .then()
                .statusCode(200)
                .body(
                        "name", is("updated"),
                        "version", is(toUpdate.version + 1)
                );
    }

    @Test
    @TestSecurity(user = "user", roles = "user")
    void updateNotFound() {
        given()
                .body("{\"name\":\"to-update\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/projects/1337")
                .then()
                .statusCode(404);
    }

    @Test
    @TestSecurity(user = "admin", roles = "user")
    void updateUnauthorized() {
        given()
                .body("{\"name\":\"to-update\"}")
                .contentType(ContentType.JSON)
                .when().put("/api/v1/projects/0")
                .then()
                .statusCode(401);
    }


    @Test
    @TestSecurity(user = "user", roles = "user")
    @RunOnVertxContext
    void delete(final UniAsserter uniAsserter) {
    final List<Long> ids = new ArrayList<>();
        /*uniAsserter.execute(
                () -> {
                    var toDelete = given().body("{\"name\":\"to-delete\"}").contentType(ContentType.JSON)
                            .post("/api/v1/projects").as(Project.class);
                    ids.add(toDelete.id);
                    }
                );

        uniAsserter.execute(
                () -> {
                        var dependentTask = given()
                                .body("{\"title\":\"dependent-task\",\"project\":{\"id\":" + ids.get(0) + "}}").contentType(ContentType.JSON)
                                .post("/api/v1/tasks").as(Task.class);
                        ids.add(dependentTask.id);
                    }
        );

        uniAsserter.execute();

        uniAsserter.assertThat(
                () ->
                        Panache.withSession(() -> User.findById
                                (ids.get(0))),
                deletedUser ->
                        MatcherAssert.assertThat(deletedUser, Matchers.nullValue())
        ); */

        var toDelete = given().body("{\"name\":\"to-delete\"}").contentType(ContentType.JSON)
                .post("/api/v1/projects").as(Project.class);
        var dependentTask = given()
                .body("{\"title\":\"dependent-task\",\"project\":{\"id\":" + toDelete.id + "}}").contentType(ContentType.JSON)
                .post("/api/v1/tasks").as(Task.class);
        given()
                .when().delete("/api/v1/projects/" + toDelete.id)
                .then()
                .statusCode(204);

        uniAsserter.assertThat(
                () -> Panache.withSession(() -> Task.<Task>findById(dependentTask.id))
                ,
                project -> assertThat(project.project, nullValue())

        );
    }
}