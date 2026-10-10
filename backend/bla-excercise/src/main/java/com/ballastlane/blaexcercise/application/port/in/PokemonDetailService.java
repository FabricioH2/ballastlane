package com.ballastlane.blaexcercise.application.port.in;

import com.ballastlane.blaexcercise.domain.model.PokemoDetail;

public interface PokemonDetailService {

    PokemoDetail getDetail(String idOrName);

}
