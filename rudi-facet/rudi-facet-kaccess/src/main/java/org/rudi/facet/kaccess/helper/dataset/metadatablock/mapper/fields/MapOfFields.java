package org.rudi.facet.kaccess.helper.dataset.metadatablock.mapper.fields;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;

import org.rudi.facet.dataverse.bean.DatasetMetadataBlockElementField;
import org.rudi.facet.dataverse.fields.FieldSpec;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
class MapOfFields {
	private final Map<String, Field> fieldsByName;

	static MapOfFields from(Map<String, Object> map) {
		return new MapOfFields(map.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, entry -> objectToField(entry.getValue()))));
	}

	@SuppressWarnings("unchecked")
	private static Field objectToField(Object object) {
		if (object instanceof DatasetMetadataBlockElementField) {
			final var blockElementField = (DatasetMetadataBlockElementField) object;
			return new FieldFromDatasetMetadataBlockElementField(blockElementField);
		}
		return new ChildField((Map<String, Object>) object);
	}

	static MapOfFields empty() {
		return from(Collections.emptyMap());
	}

	public boolean contains(FieldSpec fieldSpec) {
		return fieldsByName.containsKey(fieldSpec.getName());
	}

	/**
	 * @return le champ correspondant s'il existe, {@link MissingOptionnalField#INSTANCE} sinon pour éviter les NullPointerException, si et seulement si
	 *         le champ n'est pas obligatoire.
	 */
	@Nonnull
	public Field get(FieldSpec fieldSpec) {
		final var fieldName = fieldSpec.getName();
		if (!fieldsByName.containsKey(fieldName)) {
			return MissingOptionnalField.INSTANCE;
		}
		return fieldsByName.get(fieldName);
	}

}
