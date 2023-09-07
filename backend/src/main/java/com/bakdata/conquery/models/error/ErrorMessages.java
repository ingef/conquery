package com.bakdata.conquery.models.error;

import java.sql.SQLException;

import c10n.annotations.De;
import c10n.annotations.En;
import com.bakdata.conquery.models.forms.util.Alignment;
import com.bakdata.conquery.models.forms.util.Resolution;
import com.bakdata.conquery.models.query.entity.Entity;

public interface ErrorMessages {

	@En("An unknown error occurred")
	@De("Ein unerwarteter Fehler ist aufgetreten.")
	String unknownError();

	@En("An unknown error occurred, while creating the query.")
	@De("Ein unerwarteter Fehler ist beim Erzeugen der Anfrage aufgetreten.")
	String executionCreationUnspecified();

	@En("Could not find an ${1} element called `${0}`")
	@De("Es konnte kein ${1} Objekt mit der Id `${0}` gefunden werden.")
	String executionCreationResolve(String id, String clazz);

	@En("There are ${0} columns in the format but ${1} in at least one row")
	@De("Das Format gibt ${0} Spalten vor, mindestens eine Zeile enthält aber ${1} Spalten.")
	String externalResolveFormatError(int formatRowLength, int dataRowLength);

	@En("Entities must be unique.")
	@De("Einträge müssen eindeutig sein.")
	String externalEntityUnique();

	@En("None of the provided Entities could be resolved.")
	@De("Keine der Zeilen konnte aufgelöst werden.")
	String externalResolveEmpty();

	@En("Do not know labels ${0}.")
	@De("Die Bezeichnung/-en ${0} sind unbekannt.")
	String missingFlags(String labels);

	@En("Alignment ${0} and resolution ${0} are not compatible.")
	@De("Die Korrektur ${0} und die Auflösung ${0} sind nicht kompatibel.")
	String dateContextMismatch(Alignment alignment, Resolution resolution);

	@En("Failed to run query job for entity ${0}.")
	@De("Die Anfrage ist für ${0} fehlgeschlagen.")
	String unknownQueryExecutionError(Entity entity);

	@En("Unexpected error while processing execution.")
	@De("Es ist ein unerwarteter Fehler beim verarbeiten der Anfrage aufgetreten.")
	String executionProcessingError();

	@En("Query took too long.")
	@De("Die Anfrage lief zu lange und wurde abgebrochen.")
	String executionTimeout();

	@En("No secondaryId could be selected.")
	@De("Die ausgewählte Analyseebenen konnte in keinem der ausgewählten Konzepten gefunden werden.")
	String noSecondaryIdSelected();

	@En("Something went wrong while querying the database: ${0}.")
	@De("Etwas ist beim Anfragen des Servers fehlgeschlagen: ${0}.")
	String sqlError(SQLException error);

}
