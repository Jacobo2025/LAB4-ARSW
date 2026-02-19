package edu.eci.arsw.blueprints.controllers;

import edu.eci.arsw.blueprints.controllers.response.ApiResponse;
import edu.eci.arsw.blueprints.model.Blueprint;
import edu.eci.arsw.blueprints.model.Point;
import edu.eci.arsw.blueprints.persistence.BlueprintNotFoundException;
import edu.eci.arsw.blueprints.persistence.BlueprintPersistenceException;
import edu.eci.arsw.blueprints.services.BlueprintsServices;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/blueprints")
public class BlueprintsAPIController {

    private final BlueprintsServices services;

    public BlueprintsAPIController(BlueprintsServices services) { this.services = services; }

    // GET /blueprints -> 200 ok
    @GetMapping
    public ResponseEntity<ApiResponse<Set<Blueprint>>> getAll() {
        ApiResponse<Set<Blueprint>> apiResponse = new ApiResponse<>(200, "execute ok", services.getAllBlueprints());
        return ResponseEntity.ok(apiResponse);
    }

    // GET /blueprints/{author} -> 200 OK o 404 Not Found
    @GetMapping("/{author}")
    public ResponseEntity<ApiResponse<Set<Blueprint>>> byAuthor(@PathVariable String author) {
        try {
            Set<Blueprint> result = services.getBlueprintsByAuthor(author);
            ApiResponse<Set<Blueprint>> apiResponse = new ApiResponse<>(200, "OK", result);
            return ResponseEntity.ok(apiResponse);
        } catch (BlueprintNotFoundException e) {
            ApiResponse<Set<Blueprint>> apiResponse = new ApiResponse<>(404, "Not Found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }

    // GET /blueprints/{author}/{bpname} -> 200 OK o 404 Not Found
    @GetMapping("/{author}/{bpname}")
    public ResponseEntity<ApiResponse<Blueprint>> byAuthorAndName(@PathVariable String author, @PathVariable String bpname) {
        try {
            Blueprint op = services.getBlueprint(author,bpname);
            ApiResponse<Blueprint> apiResponse = new ApiResponse<>(200, "OK", op);
            return ResponseEntity.ok(apiResponse);
        } catch (BlueprintNotFoundException e) {
            ApiResponse<Blueprint> apiResponse = new ApiResponse<>(404, "Not Found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }

    // POST /blueprints -> 201 Created o 400 Bad Request
    @PostMapping
    public ResponseEntity<ApiResponse<Blueprint>> add(@Valid @RequestBody NewBlueprintRequest req) {
        try {
            Blueprint bp = new Blueprint(req.author(), req.name(), req.points());
            services.addNewBlueprint(bp);
            ApiResponse<Blueprint> apiResponse = new ApiResponse<>(201, "CREATED", bp);
            return ResponseEntity.status(HttpStatus.CREATED).body(apiResponse);
        } catch (BlueprintPersistenceException e) {
            ApiResponse<Blueprint> apiResponse = new ApiResponse<>(409, "CONFLICT", null);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(apiResponse);
        }
    }

    // PUT /blueprints/{author}/{bpname}/points
    @PutMapping("/{author}/{bpname}/points")
    public ResponseEntity<ApiResponse<Blueprint>> addPoint(@PathVariable String author, @PathVariable String bpname,
                                      @RequestBody Point p) {
        try {
            services.addPoint(author, bpname, p.getX(), p.getY());
            Blueprint bp = services.getBlueprint(author,bpname);
            ApiResponse<Blueprint> apiResponse = new ApiResponse<>(202, "ACEPTED", bp);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(apiResponse);
        } catch (BlueprintNotFoundException e) {
            ApiResponse<Blueprint> apiResponse = new ApiResponse<>(404, "Not Found", null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
        }
    }

    public record NewBlueprintRequest(
            @NotBlank String author,
            @NotBlank String name,
            @Valid java.util.List<Point> points
    ) { }
}
