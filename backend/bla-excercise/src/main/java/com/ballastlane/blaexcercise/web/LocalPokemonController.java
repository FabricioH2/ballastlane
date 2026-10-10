package com.ballastlane.blaexcercise.web;

import com.ballastlane.blaexcercise.application.port.in.LocalPokemonService;
import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.web.model.LocalPokemonResponse;
import com.ballastlane.blaexcercise.web.model.PageResponse;
import com.ballastlane.blaexcercise.web.model.UpdateCustomFieldsRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/local-pokemon")
public class LocalPokemonController {

    private final SyncPokemonService syncPokemonService;
    private final LocalPokemonService localPokemonService;

    public LocalPokemonController(SyncPokemonService syncPokemonService, LocalPokemonService localPokemonService) {
        this.syncPokemonService = syncPokemonService;
        this.localPokemonService = localPokemonService;
    }

    @PostMapping("/sync/{idOrName}")
    public ResponseEntity<LocalPokemonResponse> sync(@PathVariable String idOrName) {
        LocalPokemon synced = syncPokemonService.sync(idOrName);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/local-pokemon/{id}")
                .buildAndExpand(synced.id())
                .toUri();

        return ResponseEntity.created(location).body(LocalPokemonResponse.from(synced));
    }

    @GetMapping
    public PageResponse<LocalPokemonResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(localPokemonService.list(page, size), LocalPokemonResponse::from);
    }

    @GetMapping("/{id}")
    public LocalPokemonResponse get(@PathVariable long id) {
        return LocalPokemonResponse.from(localPokemonService.get(id));
    }

    /**
     * US04: edit our own fields.
     */
    @PutMapping("/{id}")
    public LocalPokemonResponse updateCustomFields(@PathVariable long id,
                                                   @Valid @RequestBody UpdateCustomFieldsRequest request) {
        LocalPokemon updated = localPokemonService.updateCustomFields(
                id, request.localizedName(), request.region(), request.tags());
        return LocalPokemonResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) {
        localPokemonService.delete(id);
    }
}
