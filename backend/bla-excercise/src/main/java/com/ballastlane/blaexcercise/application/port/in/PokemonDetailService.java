package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.PokemonDetail;

public interface PokemonDetailService {

    PokemonDetail getDetail(String idOrName);

}
