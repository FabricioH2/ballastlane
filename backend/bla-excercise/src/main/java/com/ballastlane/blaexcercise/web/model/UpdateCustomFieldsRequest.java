package com.ballastlane.blaexcercise.web.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Body of PUT /api/local-pokemon/{id} (US04). Only our own fields exist here:
 * if the client also sends "name" or other official data, it is ignored.
 * PUT replaces the three fields: a missing or null field is cleared.
 * The limits match the database columns.
 */
public record UpdateCustomFieldsRequest(
        @Size(max = 100)
        String localizedName,

        @Size(max = 100)
        String region,

        @Size(max = 20)
        Set<@NotBlank @Size(max = 50) String> tags) {
}
