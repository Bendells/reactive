package com.example.fullstack;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("non-blocking-endpoint")
public class NonBlockingEndpoint {
    @GET
    public Uni<String> hello() {
        return Uni.createFrom().item("Hello").onItem().
                transform(s -> s + " World but in Async");
    }
}
