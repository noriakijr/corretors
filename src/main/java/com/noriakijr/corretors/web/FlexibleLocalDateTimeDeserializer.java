package com.noriakijr.corretors.web;

import java.time.LocalDate;
import java.time.LocalDateTime;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class FlexibleLocalDateTimeDeserializer extends ValueDeserializer<LocalDateTime> {

	@Override
	public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
		String text = parser.getValueAsString();
		if (text == null || text.isBlank()) {
			return null;
		}
		String trimmed = text.trim();
		if (trimmed.length() == 10) {
			return LocalDate.parse(trimmed).atStartOfDay();
		}
		return LocalDateTime.parse(trimmed);
	}

}
