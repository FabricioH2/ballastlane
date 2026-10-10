package com.ballastlane.blaexcercise.web;

import com.ballastlane.blaexcercise.application.port.in.ListPokemonService;
import com.ballastlane.blaexcercise.web.model.PageResponse;
import com.ballastlane.blaexcercise.web.model.PokemonSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pokemon")
public class PokemonController {

    private final ListPokemonService listPokemonService;

    public PokemonController(ListPokemonService listPokemonService) {
        this.listPokemonService = listPokemonService;
    }

    @GetMapping
    public PageResponse<PokemonSummaryResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(listPokemonService.list(page, size), PokemonSummaryResponse::from);
    }

}
