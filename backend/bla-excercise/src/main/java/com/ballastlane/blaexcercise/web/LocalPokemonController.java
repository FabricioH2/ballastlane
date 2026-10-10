package com.ballastlane.blaexcercise.web;

import com.ballastlane.blaexcercise.application.port.in.SyncPokemonService;
import com.ballastlane.blaexcercise.domain.model.LocalPokemon;
import com.ballastlane.blaexcercise.web.model.LocalPokemonResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/local-pokemon")
public class LocalPokemonController {

    private final SyncPokemonService syncPokemonService;

    public LocalPokemonController(SyncPokemonService syncPokemonService) {
        this.syncPokemonService = syncPokemonService;
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
}
