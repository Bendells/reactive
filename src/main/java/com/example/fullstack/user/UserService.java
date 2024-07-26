package com.example.fullstack.user;

import com.example.fullstack.project.Project;
import com.example.fullstack.task.Task;
import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import org.hibernate.ObjectNotFoundException;

import java.util.List;

@ApplicationScoped
public class UserService {
    public Uni<User> findById(final long id) {
        return User.<User>findById(id)
                .onItem().ifNull().failWith( () -> new ObjectNotFoundException(id, "User with id " + id + " not found")
                        );
    }

    public Uni<User> findByName(final String name) {
        return User.find("name", name).firstResult();
    }

    public Uni<List<User>> getAll() {
        return User.listAll();
    }

    @WithTransaction
    public Uni<User> create(final User user) {
        user.password = BcryptUtil.bcryptHash(user.password);
        return user.persistAndFlush();
    }

    @WithTransaction
    public Uni<User> update(final User user) {
        return findById(user.id)
                .chain(u -> User.getSession())
                .chain(s -> s.merge(user));
    }

    @WithTransaction
    public Uni<Void> delete(final User user) {
        return deleteById(user.id);
    }

    @WithTransaction
    public Uni<Void> deleteById(final long id) {
        return findById(id)
                .chain(u -> Uni.combine().all().unis(
                        Task.delete("user.id", u.id),
                        Project.delete("user.id", u.id)
                ).asTuple().chain(t -> u.delete()));
    }

    public Uni<User> getCurrentUser() {
        return User.find("order by ID").firstResult();
    }
}
